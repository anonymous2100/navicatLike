#include "MenuBuilder.h"

QAction* MenuBuilder::addAction(QMenu* menu, const QString& text,
                                  std::function<void()> callback,
                                  const QKeySequence& shortcut)
{
    QAction* action = menu->addAction(text);
    if (!shortcut.isEmpty())
        action->setShortcut(shortcut);
    QObject::connect(action, &QAction::triggered, menu, callback);
    return action;
}
