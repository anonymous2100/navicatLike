#include "EditableTableModel.h"
#include <QColor>
#include <QBrush>

EditableTableModel::EditableTableModel(const QStringList& headers,
                                         const QVector<QVector<QVariant>>& data,
                                         const std::vector<ColumnInfo>& colInfos,
                                         QObject* parent)
    : QAbstractTableModel(parent)
    , m_headers(headers)
    , m_data(data)
    , m_colInfos(colInfos)
{
}

int EditableTableModel::rowCount(const QModelIndex&) const
{
    return m_data.size();
}

int EditableTableModel::columnCount(const QModelIndex&) const
{
    return m_headers.size();
}

QVariant EditableTableModel::data(const QModelIndex& index, int role) const
{
    if (!index.isValid()) return {};

    int row = index.row();
    int col = index.column();
    RowState state = rowState(row);

    if (role == Qt::DisplayRole || role == Qt::EditRole) {
        if (state == RowState::DELETED)
            return QVariant();
        if (row < m_data.size() && col < m_data[row].size())
            return m_data[row][col];
        return QVariant();
    }

    if (role == Qt::BackgroundRole) {
        switch (state) {
        case RowState::INSERTED:  return QBrush(QColor("#C8E6C9")); // light green
        case RowState::MODIFIED:  return QBrush(QColor("#FFF9C4")); // light yellow
        case RowState::DELETED:   return QBrush(QColor("#FFCDD2")); // light red
        default: break;
        }
        return QVariant();
    }

    if (role == Qt::ForegroundRole) {
        if (state == RowState::DELETED)
            return QBrush(QColor("#AAAAAA"));
        return QVariant();
    }

    if (role == Qt::TextAlignmentRole) {
        // Right-align numeric columns
        if (col < static_cast<int>(m_colInfos.size())) {
            const QString& dt = m_colInfos[col].dataType.toLower();
            if (dt.contains("int") || dt.contains("numeric") || dt.contains("float")
                || dt.contains("double") || dt.contains("decimal") || dt.contains("real"))
                return static_cast<int>(Qt::AlignRight | Qt::AlignVCenter);
        }
        return QVariant();
    }

    return QVariant();
}

QVariant EditableTableModel::headerData(int section, Qt::Orientation o, int role) const
{
    if (o == Qt::Horizontal && role == Qt::DisplayRole) {
        if (section < m_headers.size())
            return m_headers[section];
    }
    return QAbstractTableModel::headerData(section, o, role);
}

bool EditableTableModel::setData(const QModelIndex& index, const QVariant& value, int role)
{
    if (!index.isValid() || role != Qt::EditRole) return false;

    int row = index.row();
    int col = index.column();

    if (row >= m_data.size() || col >= m_data[row].size()) return false;

    // Track original value before first change
    if (m_rowStates.value(row, RowState::NORMAL) == RowState::NORMAL
        && !m_originalRowData.contains(row)) {
        m_originalRowData[row] = m_data[row];
    }

    m_data[row][col] = value;

    RowState current = m_rowStates.value(row, RowState::NORMAL);
    if (current != RowState::INSERTED) {
        m_rowStates[row] = RowState::MODIFIED;
    }

    emit dataChanged(index, index, {role});
    return true;
}

Qt::ItemFlags EditableTableModel::flags(const QModelIndex& index) const
{
    Qt::ItemFlags f = QAbstractTableModel::flags(index);
    if (m_rowStates.value(index.row(), RowState::NORMAL) != RowState::DELETED) {
        f |= Qt::ItemIsEditable;
    }
    return f;
}

void EditableTableModel::appendEmptyRow()
{
    int newRow = m_data.size();
    beginInsertRows(QModelIndex(), newRow, newRow);

    QVector<QVariant> empty(m_headers.size());
    m_data.append(empty);
    m_rowStates[newRow] = RowState::INSERTED;
    m_nextInsertId--;

    endInsertRows();
}

void EditableTableModel::markRowAsDeleted(int row)
{
    if (row < 0 || row >= m_data.size()) return;

    if (m_rowStates.value(row) == RowState::INSERTED) {
        // Undo insert: just remove the row
        beginRemoveRows(QModelIndex(), row, row);
        m_data.removeAt(row);
        m_rowStates.remove(row);
        // Re-key states
        QHash<int, RowState> newStates;
        for (auto it = m_rowStates.begin(); it != m_rowStates.end(); ++it) {
            newStates[it.key() >= row ? it.key() - 1 : it.key()] = it.value();
        }
        m_rowStates = newStates;
        endRemoveRows();
        return;
    }

    if (!m_originalRowData.contains(row)) {
        m_originalRowData[row] = m_data[row];
    }
    m_rowStates[row] = RowState::DELETED;
    emit dataChanged(index(row, 0), index(row, m_headers.size() - 1));
}

std::vector<RowChange> EditableTableModel::changes() const
{
    std::vector<RowChange> result;
    for (auto it = m_rowStates.begin(); it != m_rowStates.end(); ++it) {
        int row = it.key();
        RowState state = it.value();

        RowChange change;
        if (state == RowState::INSERTED) {
            change.type = ChangeType::INSERT;
            for (int c = 0; c < m_headers.size(); ++c)
                change.newValues[m_headers[c]] = m_data[row][c];
        } else if (state == RowState::MODIFIED) {
            change.type = ChangeType::UPDATE;
            for (int c = 0; c < m_headers.size(); ++c) {
                change.newValues[m_headers[c]] = m_data[row][c];
                if (m_originalRowData.contains(row))
                    change.oldValues[m_headers[c]] = m_originalRowData[row][c];
            }
        } else if (state == RowState::DELETED) {
            change.type = ChangeType::DELETE;
            for (int c = 0; c < m_headers.size(); ++c) {
                if (m_originalRowData.contains(row))
                    change.oldValues[m_headers[c]] = m_originalRowData[row][c];
            }
        }
        if (state == RowState::INSERTED || state == RowState::MODIFIED || state == RowState::DELETED)
            result.push_back(change);
    }
    return result;
}

void EditableTableModel::clearChanges()
{
    m_rowStates.clear();
    m_originalRowData.clear();
    m_nextInsertId = -1;
    emit dataChanged(index(0, 0), index(rowCount() - 1, columnCount() - 1));
}

bool EditableTableModel::hasChanges() const
{
    return !m_rowStates.isEmpty();
}

EditableTableModel::RowState EditableTableModel::rowState(int row) const
{
    return m_rowStates.value(row, RowState::NORMAL);
}
