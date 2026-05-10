#include "RowStateDelegate.h"
#include <QPainter>
#include <QAbstractItemModel>

void RowStateDelegate::paint(QPainter* painter, const QStyleOptionViewItem& option,
                              const QModelIndex& index) const
{
    QStyleOptionViewItem opt = option;
    initStyleOption(&opt, index);

    // Background from model (row state color)
    QVariant bgVar = index.data(Qt::BackgroundRole);
    if (bgVar.isValid()) {
        painter->fillRect(opt.rect, bgVar.value<QBrush>());
    }

    // Paint text (strikethrough for deleted rows)
    QVariant fgVar = index.data(Qt::ForegroundRole);
    if (fgVar.isValid()) {
        opt.palette.setColor(QPalette::Text, fgVar.value<QBrush>().color());
    }

    // Draw control (no focus rectangle state rendering)
    opt.state &= ~QStyle::State_HasFocus;
    QStyledItemDelegate::paint(painter, opt, index);
}
