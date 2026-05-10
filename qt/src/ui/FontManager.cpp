#include "FontManager.h"
#include <QApplication>
#include <QFont>
#include "common/Logging.h"

void FontManager::applyUiFont(const QString& family, int size)
{
    QFont font(family.isEmpty() ? "Microsoft YaHei" : family, size);
    QApplication::setFont(font);
    qCInfo(lcLightDB) << "Applied UI font:" << family << size;
}

void FontManager::applyEditorFont(const QString& family, int size)
{
    // Editor font is applied per-widget via QPlainTextEdit style
    // This function sets a global default for monospace contexts
    QFont font(family.isEmpty() ? "Consolas" : family, size);
    font.setStyleHint(QFont::Monospace);
    QApplication::setFont(font, "QPlainTextEdit");
    qCInfo(lcLightDB) << "Applied editor font:" << family << size;
}
