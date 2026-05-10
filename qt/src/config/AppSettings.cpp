#include "AppSettings.h"
#include "crypto/PasswordCipher.h"
#include "common/Logging.h"
#include <QCoreApplication>
#include <QDir>

AppSettings& AppSettings::instance()
{
    static AppSettings s;
    return s;
}

void AppSettings::load()
{
    qCInfo(lcLightDB) << "Loading application settings...";

    m_fontFamily = m_settings.value("ui/font/family", m_fontFamily).toString();
    m_fontSize = m_settings.value("ui/font/size", m_fontSize).toInt();
    m_editorFontFamily = m_settings.value("editor/font/family", m_editorFontFamily).toString();
    m_editorFontSize = m_settings.value("editor/font/size", m_editorFontSize).toInt();
    m_themeName = m_settings.value("ui/theme", m_themeName).toString();
    m_bgColorHex = m_settings.value("ui/bg/color", m_bgColorHex).toString();

    loadConnections();
}

void AppSettings::save()
{
    m_settings.setValue("ui/font/family", m_fontFamily);
    m_settings.setValue("ui/font/size", m_fontSize);
    m_settings.setValue("editor/font/family", m_editorFontFamily);
    m_settings.setValue("editor/font/size", m_editorFontSize);
    m_settings.setValue("ui/theme", m_themeName);
    m_settings.setValue("ui/bg/color", m_bgColorHex);

    saveConnections();
    m_settings.sync();
    qCInfo(lcLightDB) << "Settings saved.";
}

QString AppSettings::fontFamily() const { return m_fontFamily; }
void AppSettings::setFontFamily(const QString& f) { m_fontFamily = f; }
int AppSettings::fontSize() const { return m_fontSize; }
void AppSettings::setFontSize(int s) { m_fontSize = s; }
QString AppSettings::editorFontFamily() const { return m_editorFontFamily; }
void AppSettings::setEditorFontFamily(const QString& f) { m_editorFontFamily = f; }
int AppSettings::editorFontSize() const { return m_editorFontSize; }
void AppSettings::setEditorFontSize(int s) { m_editorFontSize = s; }
QString AppSettings::themeName() const { return m_themeName; }
void AppSettings::setThemeName(const QString& n) { m_themeName = n; }
QString AppSettings::bgColorHex() const { return m_bgColorHex; }
void AppSettings::setBgColorHex(const QString& h) { m_bgColorHex = h; }

std::vector<ConnectionInfo> AppSettings::connections() const
{
    return m_connections;
}

void AppSettings::addOrUpdateConnection(const ConnectionInfo& conn)
{
    for (auto& c : m_connections) {
        if (c.name == conn.name) {
            c = conn;
            return;
        }
    }
    m_connections.push_back(conn);
}

void AppSettings::removeConnection(const ConnectionInfo& conn)
{
    m_connections.erase(
        std::remove_if(m_connections.begin(), m_connections.end(),
                       [&](const ConnectionInfo& c) { return c.name == conn.name; }),
        m_connections.end());
}

void AppSettings::loadConnections()
{
    m_connections.clear();
    int count = m_settings.value("connection/count", 0).toInt();
    for (int i = 0; i < count; ++i) {
        QString prefix = QString("connection/%1/").arg(i);
        ConnectionInfo ci;
        ci.name = m_settings.value(prefix + "name", QString("Connection %1").arg(i + 1)).toString();
        ci.type = dbTypeFromString(m_settings.value(prefix + "type", "POSTGRESQL").toString());
        ci.host = m_settings.value(prefix + "host", "localhost").toString();
        ci.port = m_settings.value(prefix + "port", ci.defaultPort()).toInt();
        ci.database = m_settings.value(prefix + "database", "").toString();
        ci.username = m_settings.value(prefix + "username", "").toString();

        QString encPwd = m_settings.value(prefix + "password_enc", "").toString();
        try {
            ci.password = PasswordCipher::decrypt(encPwd);
        } catch (const PasswordCipher::Exception& e) {
            qCWarning(lcLightDB) << "Failed to decrypt password for connection:" << ci.name << ":" << e.what();
            ci.password.clear();
        }
        m_connections.push_back(ci);
    }
    qCInfo(lcLightDB) << "Loaded" << m_connections.size() << "saved connections.";
}

void AppSettings::saveConnections()
{
    // Remove old connection keys
    m_settings.remove("connection");

    m_settings.setValue("connection/count", static_cast<int>(m_connections.size()));
    for (size_t i = 0; i < m_connections.size(); ++i) {
        const auto& ci = m_connections[i];
        QString prefix = QString("connection/%1/").arg(static_cast<int>(i));
        m_settings.setValue(prefix + "name", ci.name);
        m_settings.setValue(prefix + "type", dbTypeToString(ci.type));
        m_settings.setValue(prefix + "host", ci.host);
        m_settings.setValue(prefix + "port", ci.port);
        m_settings.setValue(prefix + "database", ci.database);
        m_settings.setValue(prefix + "username", ci.username);

        try {
            m_settings.setValue(prefix + "password_enc", PasswordCipher::encrypt(ci.password));
        } catch (const PasswordCipher::Exception& e) {
            qCWarning(lcLightDB) << "Failed to encrypt password for connection:" << ci.name << ":" << e.what();
            m_settings.setValue(prefix + "password_enc", "");
        }
    }
}
