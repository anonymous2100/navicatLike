#pragma once
#include <QObject>
#include <QString>
#include <QColor>
#include <QVector>
#include <QPair>

struct ThemeEntry
{
    QString name;
    QString qssPath; // resource path e.g. "/themes/dark.qss"
};

class ThemeManager : public QObject
{
    Q_OBJECT

public:
    static ThemeManager& instance();

    void applyTheme(const QString& themeName);
    void applyBgColor(const QColor& color);
    static QColor parseBgColor(const QString& hex);
    static QString colorToHex(const QColor& c);

    QVector<ThemeEntry> availableThemes() const;
    QVector<QPair<QString, QString>> bgPresets() const;

private:
    explicit ThemeManager(QObject* parent = nullptr);

    void loadThemeList();

    QVector<ThemeEntry> m_themes;
    QVector<QPair<QString, QString>> m_bgPresets;
};
