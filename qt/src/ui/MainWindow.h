#pragma once
#include <QMainWindow>
#include <QMenuBar>
#include <QToolBar>
#include <QSplitter>
#include <QStatusBar>
#include <QAction>
#include <QStringList>
#include <memory>

class ObjectExplorerWidget;
class WorkspaceTabWidget;
class TabManager;
class StatusBarWidget;

class MainWindow : public QMainWindow
{
    Q_OBJECT

public:
    explicit MainWindow(QWidget* parent = nullptr);
    ~MainWindow() override;

    StatusBarWidget* statusBarWidget() const;
    TabManager* tabManager() const;

public slots:
    void openConnectionDialog();
    void refreshWindow();

protected:
    void closeEvent(QCloseEvent* event) override;

private:
    void createMenuBar();
    void createToolBar();
    void createStatusBar();
    void createCentralWidget();
    void registerShortcuts();
    void shutdown();

    void importConnections();
    void exportConnections();
    void openSettings();
    void openAbout();

    // Menu builders
    QMenu* createFileMenu();
    QMenu* createEditMenu();
    QMenu* createViewMenu();
    QMenu* createFavoritesMenu();
    QMenu* createToolsMenu();
    QMenu* createWindowMenu();
    QMenu* createHelpMenu();

    // Actions
    QAction* m_newConnAction = nullptr;
    QAction* m_closeConnAction = nullptr;
    QAction* m_importAction = nullptr;
    QAction* m_exportAction = nullptr;
    QAction* m_settingsAction = nullptr;
    QAction* m_exitAction = nullptr;
    QAction* m_closeTabAction = nullptr;
    QAction* m_runSqlAction = nullptr;

    // Widgets
    ObjectExplorerWidget* m_explorer = nullptr;
    WorkspaceTabWidget* m_workspace = nullptr;
    TabManager* m_tabManager = nullptr;
    StatusBarWidget* m_statusBar = nullptr;
    QSplitter* m_splitter = nullptr;
    QToolBar* m_toolBar = nullptr;

    QStringList m_favorites;
};
