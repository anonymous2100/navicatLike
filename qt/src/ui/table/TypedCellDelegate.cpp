#include "TypedCellDelegate.h"
#include <QComboBox>
#include <QSpinBox>
#include <QDoubleSpinBox>
#include <QDateTimeEdit>
#include <QLineEdit>

TypedCellDelegate::TypedCellDelegate(const std::vector<ColumnInfo>& colInfos, QObject* parent)
    : QStyledItemDelegate(parent)
    , m_colInfos(colInfos)
{
}

QWidget* TypedCellDelegate::createEditor(QWidget* parent, const QStyleOptionViewItem&,
                                          const QModelIndex& index) const
{
    int col = index.column();
    if (col < 0 || col >= static_cast<int>(m_colInfos.size()))
        return new QLineEdit(parent);

    const auto& ci = m_colInfos[col];
    QString dt = ci.dataType.toLower();

    // ENUM → combo box
    if (!ci.enumValues.isEmpty()) {
        auto* cb = new QComboBox(parent);
        cb->addItems(ci.enumValues);
        return cb;
    }

    // Boolean
    if (dt.contains("bool") || dt.contains("bit")) {
        auto* cb = new QComboBox(parent);
        cb->addItems({ "true", "false" });
        return cb;
    }

    // Integer types
    if (dt.contains("int") || dt.contains("serial") || dt.contains("smallint")
        || dt.contains("tinyint") || dt.contains("bigint")) {
        auto* sb = new QSpinBox(parent);
        sb->setRange(INT_MIN, INT_MAX);
        return sb;
    }

    // Numeric/float types
    if (dt.contains("numeric") || dt.contains("decimal") || dt.contains("float")
        || dt.contains("double") || dt.contains("real") || dt.contains("money")) {
        auto* dsb = new QDoubleSpinBox(parent);
        dsb->setRange(-1e15, 1e15);
        dsb->setDecimals(ci.numericScale > 0 ? ci.numericScale : 2);
        return dsb;
    }

    // Date/time types
    if (dt.contains("date") && !dt.contains("datetime") && !dt.contains("timestamp")) {
        auto* de = new QDateTimeEdit(parent);
        de->setCalendarPopup(true);
        de->setDisplayFormat("yyyy-MM-dd");
        return de;
    }
    if (dt.contains("time") || dt.contains("datetime") || dt.contains("timestamp")) {
        auto* dte = new QDateTimeEdit(parent);
        dte->setCalendarPopup(true);
        dte->setDisplayFormat("yyyy-MM-dd HH:mm:ss");
        return dte;
    }

    // Default: text editor
    return new QLineEdit(parent);
}

void TypedCellDelegate::setEditorData(QWidget* editor, const QModelIndex& index) const
{
    QVariant val = index.data(Qt::EditRole);

    if (auto* cb = qobject_cast<QComboBox*>(editor)) {
        int idx = cb->findText(val.toString());
        if (idx >= 0) cb->setCurrentIndex(idx);
    } else if (auto* sb = qobject_cast<QSpinBox*>(editor)) {
        sb->setValue(val.toInt());
    } else if (auto* dsb = qobject_cast<QDoubleSpinBox*>(editor)) {
        dsb->setValue(val.toDouble());
    } else if (auto* de = qobject_cast<QDateTimeEdit*>(editor)) {
        de->setDateTime(val.toDateTime());
    } else if (auto* le = qobject_cast<QLineEdit*>(editor)) {
        le->setText(val.toString());
    }
}

void TypedCellDelegate::setModelData(QWidget* editor, QAbstractItemModel* model,
                                      const QModelIndex& index) const
{
    if (auto* cb = qobject_cast<QComboBox*>(editor)) {
        model->setData(index, cb->currentText());
    } else if (auto* sb = qobject_cast<QSpinBox*>(editor)) {
        model->setData(index, sb->value());
    } else if (auto* dsb = qobject_cast<QDoubleSpinBox*>(editor)) {
        model->setData(index, dsb->value());
    } else if (auto* de = qobject_cast<QDateTimeEdit*>(editor)) {
        model->setData(index, de->dateTime());
    } else if (auto* le = qobject_cast<QLineEdit*>(editor)) {
        model->setData(index, le->text());
    }
}
