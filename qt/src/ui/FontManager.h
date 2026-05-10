#pragma once
#include <QString>

class FontManager
{
public:
    FontManager() = delete;

    static void applyUiFont(const QString& family, int size);
    static void applyEditorFont(const QString& family, int size);
};
