#include "MainWindow.h"
#include "ConnectionDialog.h"
#include "SettingsDialog.h"
#include "ThemeManager.h"
#include "FontManager.h"
#include "ui/explorer/ObjectExplorerWidget.h"
#include "ui/workspace/WorkspaceTabWidget.h"
#include "ui/workspace/TabManager.h"
#include "ui/status/StatusBarWidget.h"
#include "config/AppSettings.h"
#include "io/ConnectionXmlIO.h"
#include "pool/ConnectionPool.h"
#include <QApplication>
#include <QFileDialog>
#include <QMessageBox>
#include <QCloseEvent>
#include <QShortcut>
#include "common/Logging.h"

MainWindow::MainWindow(QWidget* parent)
    : QMainWindow(parent)
{
    setWindowTitle("LightDB");
    resize(1200, 800);
    setMinimumSize(960, 640);

    createCentralWidget();
    createMenuBar();
    createToolBar();
    createStatusBar();
    registerShortcuts();

    m_statusBar->setMessage(tr("Ready"));
    m_statusBar->setContext(tr("Not connected"));
    m_statusBar->clearElapsed();

    qCInfo(lcLightDB) << "MainWindow created";
}

MainWindow::~MainWindow() = default;

void MainWindow::createCentralWidget()
{
    m_explorer = new ObjectExplorerWidget(nullptr, this);
    m_workspace = new WorkspaceTabWidget(this);
    m_tabManager = new TabManager(m_workspace, nullptr, this);

    m_splitter = new QSplitter(Qt::Horizontal, this);
    m_splitter->addWidget(m_explorer);
    m_splitter->addWidget(m_workspace);
    m_splitter->setStretchFactor(0, 0);
    m_splitter->setStretchFactor(1, 1);
    m_splitter->setSizes({250, 950});

    setCentralWidget(m_splitter);
}

void MainWindow::createMenuBar()
{
    QMenuBar* mb = menuBar();
    mb->addMenu(createFileMenu());
    mb->addMenu(createEditMenu());
    mb->addMenu(createViewMenu());
    mb->addMenu(createFavoritesMenu());
    mb->addMenu(createToolsMenu());
    mb->addMenu(createWindowMenu());
    mb->addMenu(createHelpMenu());
}

QMenu* MainWindow::createFileMenu()
{
    QMenu* menu = new QMenu(tr("File"), this);

    m_newConnAction = menu->addAction(tr("New Connection..."));
    connect(m_newConnAction, &QAction::triggered, this, &MainWindow::openConnectionDialog);

    m_closeConnAction = menu->addAction(tr("Close Connection"));
    connect(m_closeConnAction, &QAction::triggered, this, [this]() {
        ConnectionPool::instance().closeCurrent();
        m_statusBar->setContext(tr("Not connected"));
        m_statusBar->setMessage(tr("Connection closed"));
        m_explorer->refreshTree();
    });

    menu->addSeparator();

    m_importAction = menu->addAction(tr("Import Connections..."));
    connect(m_importAction, &QAction::triggered, this, &MainWindow::importConnections);

    m_exportAction = menu->addAction(tr("Export Connections..."));
    connect(m_exportAction, &QAction::triggered, this, &MainWindow::exportConnections);

    menu->addSeparator();

    m_exitAction = menu->addAction(tr("Exit"));
    m_exitAction->setShortcut(QKeySequence("Ctrl+Q"));
    connect(m_exitAction, &QAction::triggered, this, &MainWindow::shutdown);

    return menu;
}

QMenu* MainWindow::createEditMenu()
{
    QMenu* menu = new QMenu(tr("Edit"), this);
    menu->addAction(tr("Undo"))->setShortcut(QKeySequence::Undo);
    menu->addAction(tr("Redo"))->setShortcut(QKeySequence::Redo);
    menu->addSeparator();
    menu->addAction(tr("Cut"))->setShortcut(QKeySequence::Cut);
    menu->addAction(tr("Copy"))->setShortcut(QKeySequence::Copy);
    menu->addAction(tr("Paste"))->setShortcut(QKeySequence::Paste);
    menu->addSeparator();
    m_settingsAction = menu->addAction(tr("Settings..."));
    connect(m_settingsAction, &QAction::triggered, this, &MainWindow::openSettings);
    return menu;
}

QMenu* MainWindow::createViewMenu()
{
    QMenu* menu = new QMenu(tr("View"), this);
    menu->addAction(tr("Toggle Object Explorer"));
    menu->addSeparator();
    menu->addAction(tr("Zoom In"))->setShortcut(QKeySequence::ZoomIn);
    menu->addAction(tr("Zoom Out"))->setShortcut(QKeySequence::ZoomOut);
    return menu;
}

QMenu* MainWindow::createFavoritesMenu()
{
    QMenu* menu = new QMenu(tr("Favorites"), this);
    menu->addAction(tr("Add to Favorites"));
    menu->addAction(tr("Manage Favorites..."));
    return menu;
}

QMenu* MainWindow::createToolsMenu()
{
    QMenu* menu = new QMenu(tr("Tools"), this);
    menu->addAction(tr("Query Builder"));
    menu->addAction(tr("Data Export..."));
    menu->addAction(tr("Data Import..."));
    return menu;
}

QMenu* MainWindow::createWindowMenu()
{
    QMenu* menu = new QMenu(tr("Window"), this);
    m_closeTabAction = menu->addAction(tr("Close Tab"));
    m_closeTabAction->setShortcut(QKeySequence("Ctrl+W"));
    connect(m_closeTabAction, &QAction::triggered, m_workspace, &WorkspaceTabWidget::closeCurrentTab);
    menu->addAction(tr("Close All Tabs"));
    menu->addSeparator();
    menu->addAction(tr("Next Tab"))->setShortcut(QKeySequence("Ctrl+Tab"));
    menu->addAction(tr("Previous Tab"))->setShortcut(QKeySequence("Ctrl+Shift+Tab"));
    return menu;
}

QMenu* MainWindow::createHelpMenu()
{
    QMenu* menu = new QMenu(tr("Help"), this);
    QAction* about = menu->addAction(tr("About LightDB..."));
    connect(about, &QAction::triggered, this, &MainWindow::openAbout);
    return menu;
}

void MainWindow::createToolBar()
{
    m_toolBar = addToolBar(tr("Main"));
    m_toolBar->setMovable(false);

    m_toolBar->addAction(tr("New Connection"), this, &MainWindow::openConnectionDialog);
    m_toolBar->addAction(tr("New Query"), this, [this]() {
        m_tabManager->openQuery();
    });
    m_toolBar->addSeparator();
    m_toolBar->addAction(tr("Settings"), this, &MainWindow::openSettings);
}

void MainWindow::createStatusBar()
{
    m_statusBar = new StatusBarWidget(this);
    setStatusBar(m_statusBar);
}

void MainWindow::registerShortcuts()
{
    // Run SQL: Ctrl+Enter or F5
    QShortcut* runSql = new QShortcut(QKeySequence("Ctrl+Return"), this);
    connect(runSql, &QShortcut::activated, this, [this]() {
        m_tabManager->executeCurrentQuery();
    });
    QShortcut* runSqlF5 = new QShortcut(QKeySequence("F5"), this);
    connect(runSqlF5, &QShortcut::activated, runSql, &QShortcut::activated);
}

void MainWindow::openConnectionDialog()
{
    ConnectionDialog dlg(this);
    if (dlg.exec() == QDialog::Accepted) {
        ConnectionInfo info = dlg.connectionInfo();
        try {
            ConnectionPool::instance().connect(
                info.host, info.port, info.database,
                info.username, info.password, info.type);
            m_statusBar->setContext(info.displayName());
            m_statusBar->setMessage(tr("Connected"));
            m_explorer->refreshTree();

            // Save connection
            AppSettings::instance().addOrUpdateConnection(info);
            AppSettings::instance().save();
        } catch (const std::exception& e) {
            QMessageBox::critical(this, tr("Connection Failed"), e.what());
            m_statusBar->setContext(tr("Not connected"));
            m_statusBar->setMessage(tr("Connection failed"));
        }
    }
}

void MainWindow::importConnections()
{
    QString filePath = QFileDialog::getOpenFileName(
        this, tr("Import Connections"), QString(),
        tr("XML Files (*.xml);;All Files (*)"));
    if (filePath.isEmpty()) return;

    QString error;
    auto conns = ConnectionXmlIO::importConnections(filePath, error);
    if (!error.isEmpty() && conns.empty()) {
        QMessageBox::warning(this, tr("Import Failed"), error);
        return;
    }
    for (const auto& c : conns) {
        AppSettings::instance().addOrUpdateConnection(c);
    }
    AppSettings::instance().save();
    m_statusBar->setMessage(tr("Imported %1 connections").arg(conns.size()));
}

void MainWindow::exportConnections()
{
    QString filePath = QFileDialog::getSaveFileName(
        this, tr("Export Connections"), "connections.xml",
        tr("XML Files (*.xml);;All Files (*)"));
    if (filePath.isEmpty()) return;

    QString error;
    auto conns = AppSettings::instance().connections();
    if (!ConnectionXmlIO::exportConnections(conns, filePath, error)) {
        QMessageBox::warning(this, tr("Export Failed"), error);
        return;
    }
    m_statusBar->setMessage(tr("Exported %1 connections").arg(conns.size()));
}

void MainWindow::openSettings()
{
    SettingsDialog dlg(this);
    if (dlg.exec() == QDialog::Accepted) {
        refreshWindow();
    }
}

void MainWindow::openAbout()
{
    QMessageBox::about(this, tr("About LightDB"),
        tr("<h3>LightDB Viewer</h3>"
           "<p>Version 0.1.0</p>"
           "<p>A lightweight database GUI management tool.</p>"
           "<p>Support: MySQL, PostgreSQL</p>"));
}

void MainWindow::refreshWindow()
{
    // Reapply theme and fonts
    AppSettings& s = AppSettings::instance();
    ThemeManager::instance().applyTheme(s.themeName());
    FontManager::applyUiFont(s.fontFamily(), s.fontSize());
    FontManager::applyEditorFont(s.editorFontFamily(), s.editorFontSize());
    QColor bg = ThemeManager::parseBgColor(s.bgColorHex());
    if (bg.isValid()) ThemeManager::instance().applyBgColor(bg);
}

void MainWindow::closeEvent(QCloseEvent* event)
{
    shutdown();
    event->accept();
}

void MainWindow::shutdown()
{
    qCInfo(lcLightDB) << "Shutting down...";
    ConnectionPool::instance().shutdown();
    AppSettings::instance().save();
    QApplication::quit();
}

StatusBarWidget* MainWindow::statusBarWidget() const { return m_statusBar; }
TabManager* MainWindow::tabManager() const { return m_tabManager; }
