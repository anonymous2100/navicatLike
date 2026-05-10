#include "SqlToolbar.h"
#include <QAction>

SqlToolbar::SqlToolbar(QWidget* parent)
    : QToolBar(tr("SQL"), parent)
{
    setMovable(false);

    QAction* runAction = addAction(tr("Run (Ctrl+Enter)"));
    runAction->setShortcut(QKeySequence("Ctrl+Return"));
    connect(runAction, &QAction::triggered, this, &SqlToolbar::execute);

    QAction* stopAction = addAction(tr("Stop"));
    connect(stopAction, &QAction::triggered, this, &SqlToolbar::stop);

    addSeparator();

    QAction* fmtAction = addAction(tr("Format"));
    connect(fmtAction, &QAction::triggered, this, &SqlToolbar::format);
}
