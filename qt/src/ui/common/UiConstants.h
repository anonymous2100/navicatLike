#pragma once
#include <QString>

namespace UiConstants
{
    // Application
    constexpr const char* APP_NAME = "LightDB Viewer";
    constexpr const char* APP_VERSION = "0.1.0";
    constexpr const char* ORG_NAME = "LightDB";

    // Window defaults
    constexpr int DEFAULT_WIDTH = 1200;
    constexpr int DEFAULT_HEIGHT = 800;
    constexpr int MIN_WIDTH = 960;
    constexpr int MIN_HEIGHT = 640;

    // Splitter
    constexpr int EXPLORER_DEFAULT_WIDTH = 250;

    // Pagination
    constexpr int DEFAULT_PAGE_SIZE = 200;
    constexpr int MAX_QUERY_ROWS = 10000;

    // Database
    constexpr const char* DEFAULT_MYSQL_HOST = "localhost";
    constexpr int DEFAULT_MYSQL_PORT = 3306;
    constexpr const char* DEFAULT_PG_HOST = "localhost";
    constexpr int DEFAULT_PG_PORT = 5432;

    // Fonts
    constexpr const char* DEFAULT_UI_FONT = "Microsoft YaHei";
    constexpr int DEFAULT_UI_FONT_SIZE = 16;
    constexpr const char* DEFAULT_EDITOR_FONT = "Consolas";
    constexpr int DEFAULT_EDITOR_FONT_SIZE = 16;

    // Theme
    constexpr const char* DEFAULT_THEME = "light";
    constexpr const char* DEFAULT_BG_COLOR = "#C7EDCC";
}
