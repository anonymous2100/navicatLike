#include "ExplorerItemDelegate.h"
#include <QPainter>

void ExplorerItemDelegate::paint(QPainter* painter, const QStyleOptionViewItem& option,
                                  const QModelIndex& index) const
{
    QStyleOptionViewItem opt = option;
    initStyleOption(&opt, index);

    // Draw selection background
    if (opt.state & QStyle::State_Selected) {
        painter->fillRect(opt.rect, opt.palette.highlight());
    }

    // Draw icon if available
    if (!opt.icon.isNull()) {
        QRect iconRect = QRect(opt.rect.left() + 4, opt.rect.top() + 2, 16, 16);
        opt.icon.paint(painter, iconRect);
    }

    // Draw text
    QRect textRect = opt.rect.adjusted(24, 0, -4, 0);
    painter->setPen(opt.state & QStyle::State_Selected
                        ? opt.palette.highlightedText().color()
                        : opt.palette.text().color());
    painter->drawText(textRect, Qt::AlignVCenter, opt.text);
}

QSize ExplorerItemDelegate::sizeHint(const QStyleOptionViewItem& option,
                                      const QModelIndex& index) const
{
    QSize s = QStyledItemDelegate::sizeHint(option, index);
    s.setHeight(s.height() + 4);
    return s;
}
