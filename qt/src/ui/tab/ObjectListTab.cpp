#include "ObjectListTab.h"
#include <QVBoxLayout>
#include <QStandardItemModel>
#include <QHeaderView>

ObjectListTab::ObjectListTab(const QString& objectName, const QString& listType,
                               QWidget* parent)
    : AbstractTab(parent)
    , m_objectName(objectName)
    , m_listType(listType)
{
    auto* layout = new QVBoxLayout(this);
    layout->setContentsMargins(0, 0, 0, 0);

    m_tableView = new QTableView(this);
    m_tableView->setSelectionBehavior(QAbstractItemView::SelectRows);
    m_tableView->horizontalHeader()->setStretchLastSection(true);
    layout->addWidget(m_tableView);
}

void ObjectListTab::refresh()
{
    auto* model = new QStandardItemModel(0, 1, this);
    model->setHorizontalHeaderLabels({ m_listType });
    m_tableView->setModel(model);
}
