#include "ObjectExplorerWidget.h"
#include "ExplorerTreeModel.h"
#include "ExplorerItemDelegate.h"
#include "ui/workspace/TabManager.h"
#include "pool/ConnectionPool.h"
#include "meta/MetadataService.h"
#include <QVBoxLayout>
#include <QMenu>
#include <QHeaderView>

ObjectExplorerWidget::ObjectExplorerWidget(TabManager* tabManager, QWidget* parent)
    : QWidget(parent)
    , m_tabManager(tabManager)
{
    auto* layout = new QVBoxLayout(this);
    layout->setContentsMargins(0, 0, 0, 0);

    m_treeModel = new ExplorerTreeModel(this);
    m_treeView = new QTreeView(this);
    m_treeView->setModel(m_treeModel);
    m_treeView->setHeaderHidden(true);
    m_treeView->setAnimated(true);
    m_treeView->setIndentation(16);
    m_treeView->setEditTriggers(QAbstractItemView::NoEditTriggers);
    m_treeView->setContextMenuPolicy(Qt::CustomContextMenu);

    auto* delegate = new ExplorerItemDelegate(this);
    m_treeView->setItemDelegate(delegate);

    layout->addWidget(m_treeView);

    connect(m_treeView, &QTreeView::doubleClicked,
            this, &ObjectExplorerWidget::onItemDoubleClicked);
    connect(m_treeView, &QTreeView::expanded,
            this, &ObjectExplorerWidget::onItemExpanded);
    connect(m_treeView, &QTreeView::customContextMenuRequested,
            this, &ObjectExplorerWidget::onContextMenu);
}

void ObjectExplorerWidget::refreshTree()
{
    m_treeModel->clear();
    if (!ConnectionPool::instance().isConnected())
        return;

    // Add root node for each database
    auto databases = MetadataService::listDatabases(ConnectionPool::instance().getConnection());
    for (const auto& db : databases) {
        auto* rootItem = new ExplorerTreeItem(db, ExplorerNodeType::DATABASE);
        // Add category placeholders
        rootItem->appendChild(new ExplorerTreeItem("Tables", ExplorerNodeType::TABLES_FOLDER));
        rootItem->appendChild(new ExplorerTreeItem("Views", ExplorerNodeType::VIEWS_FOLDER));
        rootItem->appendChild(new ExplorerTreeItem("Functions", ExplorerNodeType::FUNCTIONS_FOLDER));
        rootItem->appendChild(new ExplorerTreeItem("Procedures", ExplorerNodeType::PROCEDURES_FOLDER));
        rootItem->appendChild(new ExplorerTreeItem("Triggers", ExplorerNodeType::TRIGGERS_FOLDER));
        m_treeModel->appendRootItem(rootItem);
    }
    m_treeView->expandAll();
}

void ObjectExplorerWidget::loadChildren(const QModelIndex& parentIndex)
{
    auto* item = m_treeModel->itemFromIndex(parentIndex);
    if (!item || item->loaded()) return;

    QSqlDatabase db = ConnectionPool::instance().getConnection();
    switch (item->nodeType()) {
    case ExplorerNodeType::TABLES_FOLDER: {
        auto tables = MetadataService::listTables(db);
        for (const auto& t : tables)
            item->appendChild(new ExplorerTreeItem(t, ExplorerNodeType::TABLE));
        break;
    }
    case ExplorerNodeType::VIEWS_FOLDER: {
        auto views = MetadataService::listViews(db);
        for (const auto& v : views)
            item->appendChild(new ExplorerTreeItem(v, ExplorerNodeType::VIEW));
        break;
    }
    case ExplorerNodeType::FUNCTIONS_FOLDER: {
        auto funcs = MetadataService::listFunctions(db);
        for (const auto& f : funcs)
            item->appendChild(new ExplorerTreeItem(f, ExplorerNodeType::FUNCTION));
        break;
    }
    case ExplorerNodeType::PROCEDURES_FOLDER: {
        auto procs = MetadataService::listProcedures(db);
        for (const auto& p : procs)
            item->appendChild(new ExplorerTreeItem(p, ExplorerNodeType::PROCEDURE));
        break;
    }
    case ExplorerNodeType::TRIGGERS_FOLDER: {
        auto trigs = MetadataService::listTriggers(db);
        for (const auto& t : trigs)
            item->appendChild(new ExplorerTreeItem(t, ExplorerNodeType::TRIGGER));
        break;
    }
    default:
        break;
    }
    item->setLoaded(true);
}

void ObjectExplorerWidget::onItemDoubleClicked(const QModelIndex& index)
{
    auto* item = m_treeModel->itemFromIndex(index);
    if (!item) return;

    switch (item->nodeType()) {
    case ExplorerNodeType::TABLE:
        m_tabManager->openTable(item->name());
        break;
    case ExplorerNodeType::VIEW:
        m_tabManager->openObjectList(item->name(), "VIEW");
        break;
    default:
        break;
    }
}

void ObjectExplorerWidget::onItemExpanded(const QModelIndex& index)
{
    loadChildren(index);
}

void ObjectExplorerWidget::onContextMenu(const QPoint& pos)
{
    QModelIndex idx = m_treeView->indexAt(pos);
    if (!idx.isValid()) return;

    auto* item = m_treeModel->itemFromIndex(idx);
    if (!item) return;

    QMenu menu(this);
    switch (item->nodeType()) {
    case ExplorerNodeType::TABLE:
        menu.addAction(tr("Open Table"), this, [this, item]() {
            m_tabManager->openTable(item->name());
        });
        menu.addAction(tr("Design Table"), this, [this, item]() {
            m_tabManager->openDesignTable(item->name());
        });
        break;
    case ExplorerNodeType::DATABASE:
        menu.addAction(tr("New Query"), this, [this]() {
            m_tabManager->openQuery();
        });
        menu.addAction(tr("Refresh"), this, &ObjectExplorerWidget::refreshTree);
        break;
    default:
        break;
    }
    if (!menu.isEmpty())
        menu.exec(m_treeView->viewport()->mapToGlobal(pos));
}
