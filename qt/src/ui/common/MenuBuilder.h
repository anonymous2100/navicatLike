#pragma once
#include <QMenu>
#include <QAction>
#include <functional>

class MenuBuilder
{
public:
    static QAction* addAction(QMenu* menu, const QString& text,
                               std::function<void()> callback,
                               const QKeySequence& shortcut = {});
};
