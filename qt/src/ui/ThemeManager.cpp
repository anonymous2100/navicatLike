#include "ThemeManager.h"
#include <QApplication>
#include <QFile>
#include <QWidget>
#include <QScrollArea>
#include <QTableView>
#include <QTreeView>
#include <QPlainTextEdit>
#include <QToolBar>
#include <QMenuBar>
#include <QAbstractButton>
#include <QLabel>
#include <QComboBox>
#include "common/Logging.h"

ThemeManager::ThemeManager(QObject* parent)
    : QObject(parent)
{
    loadThemeList();
}

ThemeManager& ThemeManager::instance()
{
    static ThemeManager tm;
    return tm;
}

void ThemeManager::loadThemeList()
{
    // Built-in themes mapped to QSS resource files
    m_themes = {
        { "Light",    ":/themes/light.qss" },
        { "Dark",     ":/themes/dark.qss" },
        { "Dracula",  ":/themes/dracula.qss" },
    };

    // Eye-care background color presets
    m_bgPresets = {
        { u8"Green Bean Paste  #C7EDCC", "#C7EDCC" },
        { u8"Galaxy White      #FFFFFF", "#FFFFFF" },
        { u8"Almond Yellow     #FAF9DE", "#FAF9DE" },
        { u8"Autumn Leaf Brown #FFF2E2", "#FFF2E2" },
        { u8"Rouge Red         #FDE6E0", "#FDE6E0" },
        { u8"Ocean Blue        #DCE2F1", "#DCE2F1" },
        { u8"Wisteria Purple   #E9EBFE", "#E9EBFE" },
        { u8"Aurora Grey       #EAEAEF", "#EAEAEF" },
        { u8"Meadow Green      #E3EDCD", "#E3EDCD" },
        { u8"PC Manager Green  #CCE8CF", "#CCE8CF" },
        { u8"WPS Eye Care      #6E7B6C", "#6E7B6C" },
    };
}

QVector<ThemeEntry> ThemeManager::availableThemes() const
{
    return m_themes;
}

QVector<QPair<QString, QString>> ThemeManager::bgPresets() const
{
    return m_bgPresets;
}

void ThemeManager::applyTheme(const QString& themeName)
{
    QString qssPath;
    for (const auto& t : m_themes) {
        if (t.name == themeName) {
            qssPath = t.qssPath;
            break;
        }
    }

    if (qssPath.isEmpty()) {
        // Default to light
        qssPath = ":/themes/light.qss";
    }

    QFile qssFile(qssPath);
    if (qssFile.open(QIODevice::ReadOnly | QIODevice::Text)) {
        QString styleSheet = QString::fromUtf8(qssFile.readAll());
        qApp->setStyleSheet(styleSheet);
        qCInfo(lcLightDB) << "Applied theme:" << themeName;
    } else {
        qCWarning(lcLightDB) << "Failed to load theme:" << qssPath;
    }
}

static void applyBgColorRecursive(QWidget* w, const QColor& color);

void ThemeManager::applyBgColor(const QColor& color)
{
    if (!color.isValid()) return;

    // Apply background color to all top-level windows' widgets
    for (QWidget* w : QApplication::topLevelWidgets()) {
        applyBgColorRecursive(w, color);
    }
}

static void applyBgColorRecursive(QWidget* w, const QColor& color)
{
    if (!w) return;

    // Skip themed controls
    if (qobject_cast<QToolBar*>(w) || qobject_cast<QMenuBar*>(w)
        || qobject_cast<QAbstractButton*>(w) || qobject_cast<QLabel*>(w)
        || qobject_cast<QComboBox*>(w)) {
        return;
    }

    // Apply to text editors and panels
    if (qobject_cast<QPlainTextEdit*>(w) || qobject_cast<QAbstractScrollArea*>(w)) {
        w->setStyleSheet(w->styleSheet() + QString("background-color: %1;").arg(color.name()));
    }

    // If it's a container, recurse
    for (QObject* child : w->children()) {
        if (auto* childWidget = qobject_cast<QWidget*>(child)) {
            applyBgColorRecursive(childWidget, color);
        }
    }
}

QColor ThemeManager::parseBgColor(const QString& hex)
{
    if (hex.isEmpty() || hex == "none" || hex == "default")
        return QColor();
    QColor c(hex);
    return c.isValid() ? c : QColor();
}

QString ThemeManager::colorToHex(const QColor& c)
{
    return QString("#%1%2%3")
        .arg(c.red(), 2, 16, QChar('0'))
        .arg(c.green(), 2, 16, QChar('0'))
        .arg(c.blue(), 2, 16, QChar('0'))
        .toUpper();
}
