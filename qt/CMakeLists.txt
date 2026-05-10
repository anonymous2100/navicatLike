cmake_minimum_required(VERSION 3.20)
project(LightDBViewer VERSION 0.1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTORCC ON)
set(CMAKE_AUTOUIC ON)

# Qt6 components
find_package(Qt6 REQUIRED COMPONENTS
    Core
    Gui
    Widgets
    Sql
    Xml
    Concurrent
    LinguistTools
)

# OpenSSL for AES-256-GCM
if(NOT DEFINED OPENSSL_ROOT_DIR)
    if(EXISTS "D:/Program Files/OpenSSL-Win64")
        set(OPENSSL_ROOT_DIR "D:/Program Files/OpenSSL-Win64")
    elseif(EXISTS "C:/Program Files/OpenSSL-Win64")
        set(OPENSSL_ROOT_DIR "C:/Program Files/OpenSSL-Win64")
    endif()
endif()
find_package(OpenSSL REQUIRED)

# === Source files ===
set(SOURCES
    src/main.cpp

    # config
    src/config/AppSettings.cpp

    # crypto
    src/crypto/PasswordCipher.cpp

    # io
    src/io/ConnectionXmlIO.cpp

    # db
    src/db/MySQLDialect.cpp
    src/db/PostgreSQLDialect.cpp
    src/db/DialectFactory.cpp

    # pool
    src/pool/ConnectionPool.cpp

    # sql
    src/sql/SqlBuilder.cpp
    src/sql/SqlExecutor.cpp
    src/sql/RowWriter.cpp

    # meta
    src/meta/MetadataService.cpp

    # service
    src/service/QueryService.cpp
    src/service/TableDataService.cpp
    src/service/TransactionService.cpp

    # ui
    src/ui/MainWindow.cpp
    src/ui/ThemeManager.cpp
    src/ui/ConnectionDialog.cpp
    src/ui/SettingsDialog.cpp
    src/ui/FontManager.cpp

    # ui/explorer
    src/ui/explorer/ObjectExplorerWidget.cpp
    src/ui/explorer/ExplorerTreeModel.cpp
    src/ui/explorer/ExplorerItemDelegate.cpp

    # ui/workspace
    src/ui/workspace/WorkspaceTabWidget.cpp
    src/ui/workspace/TabManager.cpp

    # ui/tab
    src/ui/tab/TableDataTab.cpp
    src/ui/tab/QueryTab.cpp
    src/ui/tab/DesignTableTab.cpp
    src/ui/tab/ObjectListTab.cpp

    # ui/table
    src/ui/table/EditableTableModel.cpp
    src/ui/table/PagedTableModel.cpp
    src/ui/table/RowStateDelegate.cpp
    src/ui/table/TypedCellDelegate.cpp
    src/ui/table/DataGridWidget.cpp
    src/ui/table/DataToolbar.cpp

    # ui/sql
    src/ui/sql/SqlEditorWidget.cpp
    src/ui/sql/SqlHighlighter.cpp
    src/ui/sql/SqlResultPanel.cpp
    src/ui/sql/SqlToolbar.cpp

    # ui/common
    src/ui/common/ConfirmDialog.cpp
    src/ui/common/MenuBuilder.cpp
    src/ui/common/LoadingIndicator.cpp

    # ui/status
    src/ui/status/StatusBarWidget.cpp

    # i18n
    src/i18n/I18nManager.cpp
)

set(HEADERS
    src/common/Types.h
    src/common/Logging.h

    src/config/AppSettings.h

    src/crypto/PasswordCipher.h

    src/io/ConnectionXmlIO.h

    src/db/Dialect.h
    src/db/MySQLDialect.h
    src/db/PostgreSQLDialect.h
    src/db/DialectFactory.h

    src/pool/ConnectionPool.h

    src/sql/SqlBuilder.h
    src/sql/SqlExecutor.h
    src/sql/RowWriter.h

    src/meta/MetadataService.h

    src/service/QueryService.h
    src/service/TableDataService.h
    src/service/TransactionService.h

    src/ui/MainWindow.h
    src/ui/ThemeManager.h
    src/ui/ConnectionDialog.h
    src/ui/SettingsDialog.h
    src/ui/FontManager.h

    src/ui/explorer/ObjectExplorerWidget.h
    src/ui/explorer/ExplorerTreeModel.h
    src/ui/explorer/ExplorerItemDelegate.h

    src/ui/workspace/WorkspaceTabWidget.h
    src/ui/workspace/TabManager.h
    src/ui/workspace/AbstractTab.h

    src/ui/tab/TableDataTab.h
    src/ui/tab/QueryTab.h
    src/ui/tab/DesignTableTab.h
    src/ui/tab/ObjectListTab.h

    src/ui/table/EditableTableModel.h
    src/ui/table/PagedTableModel.h
    src/ui/table/RowStateDelegate.h
    src/ui/table/TypedCellDelegate.h
    src/ui/table/DataGridWidget.h
    src/ui/table/DataToolbar.h

    src/ui/sql/SqlEditorWidget.h
    src/ui/sql/SqlHighlighter.h
    src/ui/sql/SqlResultPanel.h
    src/ui/sql/SqlToolbar.h

    src/ui/common/UiConstants.h
    src/ui/common/ConfirmDialog.h
    src/ui/common/MenuBuilder.h
    src/ui/common/LoadingIndicator.h

    src/ui/status/StatusBarWidget.h

    src/i18n/I18nManager.h
)

# === Resources ===
set(RESOURCES
    resources/resources.qrc
)

# === Translations ===
qt_add_translations(LightDBViewer
    TS_FILES
        resources/i18n/LightDBViewer_en_US.ts
        resources/i18n/LightDBViewer_zh_CN.ts
    QM_FILES_OUTPUT_VARIABLE QM_FILES
)

# === Executable ===
qt_add_executable(LightDBViewer
    ${SOURCES}
    ${HEADERS}
    ${RESOURCES}
    ${QM_FILES}
)

target_include_directories(LightDBViewer PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/src
)

target_link_libraries(LightDBViewer PRIVATE
    Qt6::Core
    Qt6::Gui
    Qt6::Widgets
    Qt6::Sql
    Qt6::Xml
    Qt6::Concurrent
    OpenSSL::SSL
    OpenSSL::Crypto
)

# Windows: set subsystem to avoid console window
if(WIN32)
    set_target_properties(LightDBViewer PROPERTIES
        WIN32_EXECUTABLE TRUE
    )
endif()

# === Tests ===
option(BUILD_TESTS "Build tests" ON)
if(BUILD_TESTS)
    enable_testing()
    find_package(Qt6 REQUIRED COMPONENTS Test)
    find_package(GTest QUIET)
    if(NOT GTest_FOUND)
        include(FetchContent)
        FetchContent_Declare(googletest
            GIT_REPOSITORY https://github.com/google/googletest.git
            GIT_TAG v1.15.2
        )
        FetchContent_MakeAvailable(googletest)
    endif()

    add_subdirectory(tests)
endif()
