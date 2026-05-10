#pragma once
#include <QObject>
#include <QTranslator>
#include <QString>
#include <QStringList>

class I18nManager : public QObject
{
    Q_OBJECT

public:
    static I18nManager& instance();

    QStringList availableLanguages() const;
    QString currentLanguage() const;
    void setLanguage(const QString& language);

private:
    explicit I18nManager(QObject* parent = nullptr);

    QString m_currentLanguage = "en_US";
    QTranslator m_translator;
};
