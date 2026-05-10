#pragma once
#include <QAbstractTableModel>
#include <QStringList>
#include <QVariant>
#include <QVector>
#include <QHash>
#include "common/Types.h"
#include "sql/RowWriter.h"

class EditableTableModel : public QAbstractTableModel
{
    Q_OBJECT

public:
    EditableTableModel(const QStringList& headers,
                        const QVector<QVector<QVariant>>& data,
                        const std::vector<ColumnInfo>& colInfos,
                        QObject* parent = nullptr);

    int rowCount(const QModelIndex& parent = {}) const override;
    int columnCount(const QModelIndex& parent = {}) const override;
    QVariant data(const QModelIndex& index, int role) const override;
    QVariant headerData(int section, Qt::Orientation o, int role) const override;
    bool setData(const QModelIndex& index, const QVariant& value, int role) override;
    Qt::ItemFlags flags(const QModelIndex& index) const override;

    const std::vector<ColumnInfo>& columnInfos() const { return m_colInfos; }

    // Row state tracking
    void appendEmptyRow();
    void markRowAsDeleted(int row);
    std::vector<RowChange> changes() const;
    void clearChanges();
    bool hasChanges() const;

    // Row states
    enum RowState { NORMAL, INSERTED, MODIFIED, DELETED };
    RowState rowState(int row) const;

private:
    QStringList m_headers;
    QVector<QVector<QVariant>> m_data;
    std::vector<ColumnInfo> m_colInfos;
    QHash<int, RowState> m_rowStates;
    QHash<int, QVector<QVariant>> m_originalRowData; // keyed by data row index
    int m_nextInsertId = -1;
};
