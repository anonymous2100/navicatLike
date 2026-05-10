#pragma once
#include <QSqlDatabase>
#include <QString>
#include <QStringList>
#include <QHash>
#include <QVariant>
#include <vector>
#include "common/Types.h"

enum class ChangeType
{
    INSERT,
    UPDATE,
    DELETE
};

struct RowChange
{
    ChangeType type;
    QHash<QString, QVariant> newValues;
    QHash<QString, QVariant> oldValues;
};

class RowWriter
{
public:
    RowWriter(QSqlDatabase db, const QString& table, const QStringList& primaryKeys);

    void applyChanges(const std::vector<RowChange>& changes);

private:
    void applyInserts(const std::vector<RowChange>& changes);
    void applyUpdates(const std::vector<RowChange>& changes);
    void applyDeletes(const std::vector<RowChange>& changes);

    QString quote(const QString& name) const;

    QSqlDatabase m_db;
    QString m_table;
    QStringList m_primaryKeys;
    QString m_quoteChar;
};
