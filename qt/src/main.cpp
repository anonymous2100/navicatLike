#include <QApplication>
#include <QDir>
#include <QFont>
#include <QFontDatabase>
#include <QStandardPaths>
#include "common/Logging.h"
#include "config/AppSettings.h"
#include "ui/MainWindow.h"
#include "ui/ThemeManager.h"
#include "ui/FontManager.h"

Q_LOGGING_CATEGORY(lcLightDB, "lightdb")

int main(int argc, char* argv[])
{
    QApplication app(argc, argv);
    app.setApplicationName("LightDB Viewer");
    app.setApplicationVersion("0.1.0");
    app.setOrganizationName("LightDB");

    qCInfo(lcLightDB) << "LightDB Viewer starting...";

    // Load configuration
    AppSettings& settings = AppSettings::instance();
    settings.load();

    // Apply theme
    QString themeName = settings.themeName();
    ThemeManager::instance().applyTheme(themeName);

    // Apply fonts
    FontManager::applyUiFont(settings.fontFamily(), settings.fontSize());
    FontManager::applyEditorFont(settings.editorFontFamily(), settings.editorFontSize());

    // Apply background color
    QColor bgColor = ThemeManager::parseBgColor(settings.bgColorHex());
    if (bgColor.isValid()) {
        ThemeManager::instance().applyBgColor(bgColor);
    }

    // Create and show main window
    MainWindow window;
    window.show();

    // Open connection dialog after window is shown
    QMetaObject::invokeMethod(&window, "openConnectionDialog", Qt::QueuedConnection);

    int result = app.exec();

    // Save config on exit
    settings.save();
    qCInfo(lcLightDB) << "LightDB Viewer shutting down.";

    return result;
}
