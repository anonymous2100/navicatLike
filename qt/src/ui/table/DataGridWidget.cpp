#include "DataGridWidget.h"
#include "EditableTableModel.h"
#include "DataToolbar.h"
#include "RowStateDelegate.h"
#include "TypedCellDelegate.h"
#include <QVBoxLayout>
#include <QHeaderView>

DataGridWidget::DataGridWidget(QWidget* parent)
    : QWidget(parent)
{
    auto* layout = new QVBoxLayout(this);
    layout->setContentsMargins(0, 0, 0, 0);

    m_toolbar = new DataToolbar(this);
    m_tableView = new QTableView(this);
    m_tableView->setSelectionBehavior(QAbstractItemView::SelectRows);
    m_tableView->setSelectionMode(QAbstractItemView::SingleSelection);
    m_tableView->setAlternatingRowColors(true);
    m_tableView->setShowGrid(true);
    m_tableView->verticalHeader()->setVisible(true);
    m_tableView->horizontalHeader()->setStretchLastSection(true);
    m_tableView->setSortingEnabled(false);

    layout->addWidget(m_toolbar);
    layout->addWidget(m_tableView);

    connect(m_toolbar, &DataToolbar::prevPage, this, [this]() {
        if (m_currentPage > 0) emit pageChanged(m_currentPage - 1);
    });
    connect(m_toolbar, &DataToolbar::nextPage, this, [this]() {
        if (m_hasMore) emit pageChanged(m_currentPage + 1);
    });
    connect(m_toolbar, &DataToolbar::save, this, &DataGridWidget::saveRequested);
    connect(m_toolbar, &DataToolbar::revert, this, &DataGridWidget::revertRequested);
    connect(m_toolbar, &DataToolbar::addRow, this, &DataGridWidget::addRowRequested);
    connect(m_toolbar, &DataToolbar::deleteRow, this, &DataGridWidget::deleteRowRequested);
    connect(m_toolbar, &DataToolbar::refresh, this, &DataGridWidget::refreshRequested);
}

void DataGridWidget::setModel(EditableTableModel* model)
{
    m_model = model;
    m_tableView->setModel(model);

    // Install delegates
    auto* rowStateDelegate = new RowStateDelegate(this);
    m_tableView->setItemDelegate(rowStateDelegate);

    // For cell editing, use TypedCellDelegate
    if (!model->columnInfos().empty()) {
        auto* typedDelegate = new TypedCellDelegate(model->columnInfos(), this);
        m_tableView->setItemDelegateForColumn(0, nullptr); // will be set per column as needed
        // Install typed delegate for all columns for editing
        for (int c = 0; c < m_tableView->model()->columnCount(); ++c) {
            m_tableView->setItemDelegateForColumn(c, typedDelegate);
        }
    }

    m_tableView->resizeColumnsToContents();
}

EditableTableModel* DataGridWidget::editableModel() const { return m_model; }
void DataGridWidget::setHasMore(bool h) { m_hasMore = h; m_toolbar->setHasNext(h); }
void DataGridWidget::setCurrentPage(int p) { m_currentPage = p; m_toolbar->setPage(p); }
int DataGridWidget::currentPage() const { return m_currentPage; }

int DataGridWidget::currentRow() const
{
    auto idx = m_tableView->currentIndex();
    return idx.isValid() ? idx.row() : -1;
}
