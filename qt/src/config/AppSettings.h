#pragma once
#include <QSettings>
#include <QString>
#include <vector>
#include "common/Types.h"

class AppSettings
{
public:
    static AppSettings& instance();

    void load();
    void save();

    // Font
    QString fontFamily() const;
    void setFontFamily(const QString& family);
    int fontSize() const;
    void setFontSize(int size);
    QString editorFontFamily() const;
    void setEditorFontFamily(const QString& family);
    int editorFontSize() const;
    void setEditorFontSize(int size);

    // Theme
    QString themeName() const;
    void setThemeName(const QString& name);
    QString bgColorHex() const;
    void setBgColorHex(const QString& hex);

    // Connections
    std::vector<ConnectionInfo> connections() const;
    void addOrUpdateConnection(const ConnectionInfo& conn);
    void removeConnection(const ConnectionInfo& conn);

private:
    AppSettings() = default;
    void loadConnections();
    void saveConnections();

    QSettings m_settings;
    QString m_fontFamily = "Microsoft YaHei";
    int m_fontSize = 16;
    QString m_editorFontFamily = "Consolas";
    int m_editorFontSize = 16;
    QString m_themeName = "light";
    QString m_bgColorHex = "#C7EDCC";
    std::vector<ConnectionInfo> m_connections;
};
