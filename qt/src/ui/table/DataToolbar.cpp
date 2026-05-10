#include "DataToolbar.h"

DataToolbar::DataToolbar(QWidget* parent)
    : QToolBar(tr("Data"), parent)
{
    setMovable(false);

    addAction(tr("Refresh"))->connect(addAction(tr("Refresh")), &QAction::triggered, this, &DataToolbar::refresh);
    addSeparator();
    addAction(tr("Save"))->connect(addAction(tr("Save")), &QAction::triggered, this, &DataToolbar::save);
    addAction(tr("Revert"))->connect(addAction(tr("Revert")), &QAction::triggered, this, &DataToolbar::revert);
    addSeparator();
    addAction(tr("+ Row"))->connect(addAction(tr("+ Row")), &QAction::triggered, this, &DataToolbar::addRow);
    addAction(tr("- Row"))->connect(addAction(tr("- Row")), &QAction::triggered, this, &DataToolbar::deleteRow);
    addSeparator();
    addAction(tr("< Prev"))->connect(addAction(tr("< Prev")), &QAction::triggered, this, &DataToolbar::prevPage);
    m_pageLabel = addAction("Page 0");
    m_pageLabel->setEnabled(false);
    m_nextAction = addAction(tr("Next >"));
    connect(m_nextAction, &QAction::triggered, this, &DataToolbar::nextPage);
}

void DataToolbar::setPage(int page)
{
    m_pageLabel->setText(QString("Page %1").arg(page));
}

void DataToolbar::setHasNext(bool hasNext)
{
    m_nextAction->setEnabled(hasNext);
}
