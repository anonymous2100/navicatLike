#include "DesignTableTab.h"
#include "meta/MetadataService.h"
#include "pool/ConnectionPool.h"
#include <QVBoxLayout>
#include <QStandardItemModel>
#include <QHeaderView>

DesignTableTab::DesignTableTab(const QString& tableName, QWidget* parent)
    : AbstractTab(parent)
    , m_tableName(tableName)
{
    auto* layout = new QVBoxLayout(this);
    layout->setContentsMargins(0, 0, 0, 0);

    m_tabs = new QTabWidget(this);

    // Columns tab
    m_columnsTable = new QTableView(this);
    m_columnsTable->setModel(new QStandardItemModel(this));
    m_columnsTable->horizontalHeader()->setStretchLastSection(true);
    m_columnsTable->setSelectionBehavior(QAbstractItemView::SelectRows);
    m_tabs->addTab(m_columnsTable, tr("Columns"));

    // Indexes tab
    m_indexesTable = new QTableView(this);
    m_indexesTable->setModel(new QStandardItemModel(this));
    m_indexesTable->horizontalHeader()->setStretchLastSection(true);
    m_tabs->addTab(m_indexesTable, tr("Indexes"));

    // Foreign Keys tab
    m_fksTable = new QTableView(this);
    m_fksTable->setModel(new QStandardItemModel(this));
    m_fksTable->horizontalHeader()->setStretchLastSection(true);
    m_tabs->addTab(m_fksTable, tr("Foreign Keys"));

    layout->addWidget(m_tabs);
}

void DesignTableTab::refresh()
{
    loadColumns();
    loadIndexes();
    loadForeignKeys();
}

void DesignTableTab::loadColumns()
{
    try {
        QSqlDatabase db = ConnectionPool::instance().getConnection();
        m_columns = MetadataService::columns(db, m_tableName);

        auto* model = new QStandardItemModel(m_columns.size(), 8, this);
        model->setHorizontalHeaderLabels({
            tr("Column"), tr("Type"), tr("Nullable"),
            tr("PK"), tr("Auto Inc"), tr("Default"),
            tr("Char Length"), tr("Comment")
        });

        for (size_t i = 0; i < m_columns.size(); ++i) {
            const auto& c = m_columns[i];
            model->setItem(i, 0, new QStandardItem(c.name));
            model->setItem(i, 1, new QStandardItem(c.dataType));
            model->setItem(i, 2, new QStandardItem(c.nullable ? "YES" : "NO"));
            model->setItem(i, 3, new QStandardItem(c.primaryKey ? "YES" : ""));
            model->setItem(i, 4, new QStandardItem(c.autoIncrement ? "YES" : ""));
            model->setItem(i, 5, new QStandardItem(c.defaultValue));
            model->setItem(i, 6, new QStandardItem(QString::number(c.charMaxLength)));
            model->setItem(i, 7, new QStandardItem(c.comment));
        }

        m_columnsTable->setModel(model);
        m_columnsTable->resizeColumnsToContents();
    } catch (const std::exception& e) {
        qWarning("Failed to load columns: %s", e.what());
    }
}

void DesignTableTab::loadIndexes()
{
    auto* model = new QStandardItemModel(0, 3, this);
    model->setHorizontalHeaderLabels({ tr("Index Name"), tr("Columns"), tr("Unique") });
    m_indexesTable->setModel(model);
    m_indexesTable->resizeColumnsToContents();
}

void DesignTableTab::loadForeignKeys()
{
    auto* model = new QStandardItemModel(0, 4, this);
    model->setHorizontalHeaderLabels({
        tr("Column"), tr("Referenced Table"),
        tr("Referenced Column"), tr("Constraint")
    });
    m_fksTable->setModel(model);
    m_fksTable->resizeColumnsToContents();
}
