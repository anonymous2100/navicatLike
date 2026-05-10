#include "PagedTableModel.h"

PagedTableModel::PagedTableModel(QObject* parent)
    : QAbstractTableModel(parent)
{
}

void PagedTableModel::setData(const QStringList& headers,
                               const QVector<QVector<QVariant>>& rows)
{
    beginResetModel();
    m_headers = headers;
    m_rows = rows;
    endResetModel();
}

void PagedTableModel::clear()
{
    beginResetModel();
    m_headers.clear();
    m_rows.clear();
    endResetModel();
}

int PagedTableModel::rowCount(const QModelIndex&) const
{
    return m_rows.size();
}

int PagedTableModel::columnCount(const QModelIndex&) const
{
    return m_headers.size();
}

QVariant PagedTableModel::data(const QModelIndex& index, int role) const
{
    if (!index.isValid()) return {};
    int r = index.row(), c = index.column();
    if (r >= m_rows.size() || c >= m_headers.size()) return {};

    if (role == Qt::DisplayRole || role == Qt::EditRole)
        return m_rows[r][c];
    return {};
}

QVariant PagedTableModel::headerData(int section, Qt::Orientation o, int role) const
{
    if (o == Qt::Horizontal && role == Qt::DisplayRole) {
        if (section < m_headers.size())
            return m_headers[section];
    }
    return QAbstractTableModel::headerData(section, o, role);
}
