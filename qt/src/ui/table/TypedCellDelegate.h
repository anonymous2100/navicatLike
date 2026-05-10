#pragma once
#include <QStyledItemDelegate>
#include "common/Types.h"
#include <vector>

class TypedCellDelegate : public QStyledItemDelegate
{
    Q_OBJECT

public:
    explicit TypedCellDelegate(const std::vector<ColumnInfo>& colInfos, QObject* parent = nullptr);

    QWidget* createEditor(QWidget* parent, const QStyleOptionViewItem& option,
                          const QModelIndex& index) const override;
    void setEditorData(QWidget* editor, const QModelIndex& index) const override;
    void setModelData(QWidget* editor, QAbstractItemModel* model,
                      const QModelIndex& index) const override;

private:
    std::vector<ColumnInfo> m_colInfos;
};
