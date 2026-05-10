#include "I18nManager.h"
#include <QApplication>
#include <QLocale>

I18nManager::I18nManager(QObject* parent)
    : QObject(parent)
{
}

I18nManager& I18nManager::instance()
{
    static I18nManager mgr;
    return mgr;
}

QStringList I18nManager::availableLanguages() const
{
    return { "en_US", "zh_CN" };
}

QString I18nManager::currentLanguage() const
{
    return m_currentLanguage;
}

void I18nManager::setLanguage(const QString& language)
{
    if (language == m_currentLanguage) return;

    qApp->removeTranslator(&m_translator);
    QString qmPath = QString(":/i18n/LightDBViewer_%1.qm").arg(language);
    if (m_translator.load(qmPath)) {
        qApp->installTranslator(&m_translator);
        m_currentLanguage = language;
    }
}
