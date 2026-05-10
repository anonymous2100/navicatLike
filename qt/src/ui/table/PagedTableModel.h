#pragma once
#include <QAbstractTableModel>
#include <QStringList>
#include <QVariant>
#include <QVector>

class PagedTableModel : public QAbstractTableModel
{
    Q_OBJECT

public:
    PagedTableModel(QObject* parent = nullptr);

    void setData(const QStringList& headers, const QVector<QVector<QVariant>>& rows);
    void clear();

    int rowCount(const QModelIndex& parent = {}) const override;
    int columnCount(const QModelIndex& parent = {}) const override;
    QVariant data(const QModelIndex& index, int role) const override;
    QVariant headerData(int section, Qt::Orientation o, int role) const override;

private:
    QStringList m_headers;
    QVector<QVector<QVariant>> m_rows;
};
