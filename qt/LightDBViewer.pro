# ===================================================
# LightDB Viewer — Qt6 Project File (qmake)
# ===================================================
# 用法:
#   1. 双击此文件 → 自动用 Qt Creator 打开
#   2. 或在 Qt Creator 中: File → Open File or Project → 选择此 .pro 文件
# ===================================================

QT       += core gui widgets sql xml concurrent
TEMPLATE  = app
CONFIG   += c++17

TARGET    = LightDBViewer
VERSION   = 0.1.0

# ====== Windows: 隐藏控制台窗口 ======
win32:CONFIG(debug, debug|release): CONFIG += console
win32:CONFIG(release, debug|release): CONFIG -= console

# ====== Include paths ======
INCLUDEPATH += $$PWD/src

# ====== OpenSSL ======
win32 {
    OPENSSL_ROOT = "D:/Program Files/OpenSSL-Win64"
    INCLUDEPATH += $$OPENSSL_ROOT/include
    LIBS += -L$$OPENSSL_ROOT/lib/VC/x64/MD -lssl -lcrypto
} else {
    LIBS += -lssl -lcrypto
}

# ====== Source files ======
SOURCES += \
    src/main.cpp \
    src/config/AppSettings.cpp \
    src/crypto/PasswordCipher.cpp \
    src/io/ConnectionXmlIO.cpp \
    src/db/MySQLDialect.cpp \
    src/db/PostgreSQLDialect.cpp \
    src/db/DialectFactory.cpp \
    src/pool/ConnectionPool.cpp \
    src/sql/SqlBuilder.cpp \
    src/sql/SqlExecutor.cpp \
    src/sql/RowWriter.cpp \
    src/meta/MetadataService.cpp \
    src/service/QueryService.cpp \
    src/service/TableDataService.cpp \
    src/service/TransactionService.cpp \
    src/ui/MainWindow.cpp \
    src/ui/ThemeManager.cpp \
    src/ui/ConnectionDialog.cpp \
    src/ui/SettingsDialog.cpp \
    src/ui/FontManager.cpp \
    src/ui/explorer/ObjectExplorerWidget.cpp \
    src/ui/explorer/ExplorerTreeModel.cpp \
    src/ui/explorer/ExplorerItemDelegate.cpp \
    src/ui/workspace/WorkspaceTabWidget.cpp \
    src/ui/workspace/TabManager.cpp \
    src/ui/tab/TableDataTab.cpp \
    src/ui/tab/QueryTab.cpp \
    src/ui/tab/DesignTableTab.cpp \
    src/ui/tab/ObjectListTab.cpp \
    src/ui/table/EditableTableModel.cpp \
    src/ui/table/PagedTableModel.cpp \
    src/ui/table/RowStateDelegate.cpp \
    src/ui/table/TypedCellDelegate.cpp \
    src/ui/table/DataGridWidget.cpp \
    src/ui/table/DataToolbar.cpp \
    src/ui/sql/SqlEditorWidget.cpp \
    src/ui/sql/SqlHighlighter.cpp \
    src/ui/sql/SqlResultPanel.cpp \
    src/ui/sql/SqlToolbar.cpp \
    src/ui/common/ConfirmDialog.cpp \
    src/ui/common/MenuBuilder.cpp \
    src/ui/common/LoadingIndicator.cpp \
    src/ui/status/StatusBarWidget.cpp \
    src/i18n/I18nManager.cpp

HEADERS += \
    src/common/Types.h \
    src/config/AppSettings.h \
    src/crypto/PasswordCipher.h \
    src/io/ConnectionXmlIO.h \
    src/db/Dialect.h \
    src/db/MySQLDialect.h \
    src/db/PostgreSQLDialect.h \
    src/db/DialectFactory.h \
    src/pool/ConnectionPool.h \
    src/sql/SqlBuilder.h \
    src/sql/SqlExecutor.h \
    src/sql/RowWriter.h \
    src/meta/MetadataService.h \
    src/service/QueryService.h \
    src/service/TableDataService.h \
    src/service/TransactionService.h \
    src/ui/MainWindow.h \
    src/ui/ThemeManager.h \
    src/ui/ConnectionDialog.h \
    src/ui/SettingsDialog.h \
    src/ui/FontManager.h \
    src/ui/explorer/ObjectExplorerWidget.h \
    src/ui/explorer/ExplorerTreeModel.h \
    src/ui/explorer/ExplorerItemDelegate.h \
    src/ui/workspace/WorkspaceTabWidget.h \
    src/ui/workspace/TabManager.h \
    src/ui/workspace/AbstractTab.h \
    src/ui/tab/TableDataTab.h \
    src/ui/tab/QueryTab.h \
    src/ui/tab/DesignTableTab.h \
    src/ui/tab/ObjectListTab.h \
    src/ui/table/EditableTableModel.h \
    src/ui/table/PagedTableModel.h \
    src/ui/table/RowStateDelegate.h \
    src/ui/table/TypedCellDelegate.h \
    src/ui/table/DataGridWidget.h \
    src/ui/table/DataToolbar.h \
    src/ui/sql/SqlEditorWidget.h \
    src/ui/sql/SqlHighlighter.h \
    src/ui/sql/SqlResultPanel.h \
    src/ui/sql/SqlToolbar.h \
    src/ui/common/UiConstants.h \
    src/ui/common/ConfirmDialog.h \
    src/ui/common/MenuBuilder.h \
    src/ui/common/LoadingIndicator.h \
    src/ui/status/StatusBarWidget.h \
    src/i18n/I18nManager.h

# ====== Qt Resources ======
RESOURCES += \
    resources/resources.qrc

# ====== Translations ======
TRANSLATIONS += \
    resources/i18n/LightDBViewer_en_US.ts \
    resources/i18n/LightDBViewer_zh_CN.ts

# ====== Build directory ======
CONFIG(debug, debug|release) {
    DESTDIR = $$OUT_PWD/debug
} else {
    DESTDIR = $$OUT_PWD/release
}
