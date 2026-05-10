#include "TableDataTab.h"
#include "ui/table/DataGridWidget.h"
#include "ui/table/EditableTableModel.h"
#include "ui/table/PagedTableModel.h"
#include "service/TableDataService.h"
#include "pool/ConnectionPool.h"
#include <QVBoxLayout>
#include <QLabel>
#include <QMessageBox>
#include "common/Logging.h"

TableDataTab::TableDataTab(const QString& tableName, QWidget* parent)
    : AbstractTab(parent)
    , m_tableName(tableName)
{
    auto* layout = new QVBoxLayout(this);
    layout->setContentsMargins(0, 0, 0, 0);

    m_dataGrid = new DataGridWidget(this);
    layout->addWidget(m_dataGrid);

    connect(m_dataGrid, &DataGridWidget::pageChanged, this, &TableDataTab::onPageChanged);
    connect(m_dataGrid, &DataGridWidget::saveRequested, this, &TableDataTab::onSave);
    connect(m_dataGrid, &DataGridWidget::revertRequested, this, &TableDataTab::onRevert);
    connect(m_dataGrid, &DataGridWidget::addRowRequested, this, &TableDataTab::onAddRow);
    connect(m_dataGrid, &DataGridWidget::deleteRowRequested, this, &TableDataTab::onDeleteRow);
    connect(m_dataGrid, &DataGridWidget::refreshRequested, this, &TableDataTab::onRefresh);
}

void TableDataTab::refresh()
{
    loadPage(0);
}

void TableDataTab::onPageChanged(int page)
{
    loadPage(page);
}

void TableDataTab::loadPage(int page)
{
    m_currentPage = page;
    try {
        QSqlDatabase db = ConnectionPool::instance().getConnection();
        auto data = TableDataService::loadTableData(db, m_tableName, page, m_pageSize);

        auto* model = new EditableTableModel(data.columnNames, data.rows, data.columnInfos, this);
        m_dataGrid->setModel(model);
        m_dataGrid->setHasMore(data.hasMore);
        m_dataGrid->setCurrentPage(page);

        m_originalData = data.rows;
        qCDebug(lcLightDB) << "Loaded page" << page << "of table" << m_tableName;
    } catch (const std::exception& e) {
        QMessageBox::critical(this, tr("Error"), e.what());
    }
}

void TableDataTab::onSave()
{
    auto* model = m_dataGrid->editableModel();
    if (!model) return;

    auto changes = model->changes();
    if (changes.empty()) return;

    try {
        QSqlDatabase db = ConnectionPool::instance().getConnection();
        auto cols = model->columnInfos();
        QStringList pks;
        for (const auto& c : cols) {
            if (c.primaryKey) pks << c.name;
        }
        TableDataService::saveChanges(db, m_tableName, pks, changes);
        model->clearChanges();
        setDirty(false);
        QMessageBox::information(this, tr("Success"), tr("Changes saved."));
    } catch (const std::exception& e) {
        QMessageBox::critical(this, tr("Save Failed"), e.what());
    }
}

void TableDataTab::onRevert()
{
    loadPage(m_currentPage);
    setDirty(false);
}

void TableDataTab::onAddRow()
{
    auto* model = m_dataGrid->editableModel();
    if (model) {
        model->appendEmptyRow();
        setDirty(true);
    }
}

void TableDataTab::onDeleteRow()
{
    auto* model = m_dataGrid->editableModel();
    int row = m_dataGrid->currentRow();
    if (model && row >= 0) {
        model->markRowAsDeleted(row);
        setDirty(true);
    }
}

void TableDataTab::onRefresh()
{
    loadPage(m_currentPage);
}
