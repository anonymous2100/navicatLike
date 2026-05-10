#include "ExplorerTreeModel.h"
#include <QIcon>

// --- ExplorerTreeItem ---
ExplorerTreeItem::ExplorerTreeItem(const QString& name, ExplorerNodeType type, ExplorerTreeItem* parent)
    : m_name(name), m_nodeType(type), m_parent(parent)
{
}

ExplorerTreeItem::~ExplorerTreeItem()
{
    qDeleteAll(m_children);
}

ExplorerTreeItem* ExplorerTreeItem::child(int row)
{
    return (row >= 0 && row < m_children.size()) ? m_children.at(row) : nullptr;
}

int ExplorerTreeItem::childCount() const
{
    return m_children.size();
}

int ExplorerTreeItem::row() const
{
    if (m_parent)
        return m_parent->m_children.indexOf(const_cast<ExplorerTreeItem*>(this));
    return 0;
}

ExplorerTreeItem* ExplorerTreeItem::parent() const
{
    return m_parent;
}

void ExplorerTreeItem::appendChild(ExplorerTreeItem* child)
{
    child->m_parent = this;
    m_children.append(child);
}

void ExplorerTreeItem::removeChildren()
{
    qDeleteAll(m_children);
    m_children.clear();
}

// --- ExplorerTreeModel ---
ExplorerTreeModel::ExplorerTreeModel(QObject* parent)
    : QAbstractItemModel(parent)
{
}

ExplorerTreeModel::~ExplorerTreeModel()
{
    qDeleteAll(m_rootItems);
}

void ExplorerTreeModel::clear()
{
    beginResetModel();
    qDeleteAll(m_rootItems);
    m_rootItems.clear();
    endResetModel();
}

void ExplorerTreeModel::appendRootItem(ExplorerTreeItem* item)
{
    beginInsertRows(QModelIndex(), m_rootItems.size(), m_rootItems.size());
    m_rootItems.append(item);
    endInsertRows();
}

ExplorerTreeItem* ExplorerTreeModel::itemFromIndex(const QModelIndex& index) const
{
    if (index.isValid())
        return static_cast<ExplorerTreeItem*>(index.internalPointer());
    return nullptr;
}

QModelIndex ExplorerTreeModel::index(int row, int column, const QModelIndex& parent) const
{
    if (!hasIndex(row, column, parent))
        return {};

    ExplorerTreeItem* parentItem = parent.isValid()
        ? static_cast<ExplorerTreeItem*>(parent.internalPointer())
        : nullptr;

    ExplorerTreeItem* childItem = parentItem
        ? parentItem->child(row)
        : (row < m_rootItems.size() ? m_rootItems.at(row) : nullptr);

    return childItem ? createIndex(row, column, childItem) : QModelIndex();
}

QModelIndex ExplorerTreeModel::parent(const QModelIndex& index) const
{
    if (!index.isValid())
        return {};

    auto* childItem = static_cast<ExplorerTreeItem*>(index.internalPointer());
    ExplorerTreeItem* parentItem = childItem->parent();

    if (!parentItem)
        return {};

    return createIndex(parentItem->row(), 0, parentItem);
}

int ExplorerTreeModel::rowCount(const QModelIndex& parent) const
{
    if (!parent.isValid())
        return m_rootItems.size();

    auto* item = static_cast<ExplorerTreeItem*>(parent.internalPointer());
    return item ? item->childCount() : 0;
}

int ExplorerTreeModel::columnCount(const QModelIndex&) const
{
    return 1;
}

QVariant ExplorerTreeModel::data(const QModelIndex& index, int role) const
{
    if (!index.isValid()) return {};

    auto* item = static_cast<ExplorerTreeItem*>(index.internalPointer());
    if (!item) return {};

    if (role == Qt::DisplayRole)
        return item->name();

    if (role == Qt::DecorationRole) {
        switch (item->nodeType()) {
        case ExplorerNodeType::DATABASE:    return QIcon(); // TODO: add icons
        case ExplorerNodeType::TABLE:       return QIcon();
        case ExplorerNodeType::VIEW:        return QIcon();
        case ExplorerNodeType::FUNCTION:    return QIcon();
        case ExplorerNodeType::PROCEDURE:   return QIcon();
        case ExplorerNodeType::TRIGGER:     return QIcon();
        default: return QIcon();
        }
    }

    return {};
}
