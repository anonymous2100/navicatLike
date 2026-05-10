#include "RowWriter.h"
#include <QSqlError>
#include <QSqlQuery>
#include <stdexcept>

RowWriter::RowWriter(QSqlDatabase db, const QString& table, const QStringList& primaryKeys)
    : m_db(db)
    , m_table(table)
    , m_primaryKeys(primaryKeys)
{
    // Determine quote character from driver
    QString driver = db.driverName().toLower();
    m_quoteChar = (driver == "qmysql") ? "`" : "\"";
}

QString RowWriter::quote(const QString& name) const
{
    if (name.isEmpty()) return name;
    if (name.contains('.')) {
        QStringList parts = name.split('.');
        return quote(parts[0]) + "." + quote(parts[1]);
    }
    QString q = m_quoteChar;
    return q + QString(name).replace(q, q + q) + q;
}

void RowWriter::applyChanges(const std::vector<RowChange>& changes)
{
    m_db.transaction();
    try {
        applyInserts(changes);
        applyUpdates(changes);
        applyDeletes(changes);
        m_db.commit();
    } catch (...) {
        m_db.rollback();
        throw;
    }
}

void RowWriter::applyInserts(const std::vector<RowChange>& changes)
{
    for (const auto& c : changes) {
        if (c.type != ChangeType::INSERT) continue;

        QStringList columns = c.newValues.keys();
        if (columns.isEmpty()) continue;

        QStringList quotedCols;
        QStringList placeholders;
        for (const auto& col : columns) {
            quotedCols << quote(col);
            placeholders << "?";
        }

        QString sql = QString("INSERT INTO %1 (%2) VALUES (%3)")
            .arg(quote(m_table), quotedCols.join(","), placeholders.join(","));

        QSqlQuery q(m_db);
        if (!q.prepare(sql))
            throw std::runtime_error(q.lastError().text().toStdString());

        for (int i = 0; i < columns.size(); ++i) {
            q.bindValue(i, c.newValues.value(columns[i]));
        }
        if (!q.exec())
            throw std::runtime_error(q.lastError().text().toStdString());
    }
}

void RowWriter::applyUpdates(const std::vector<RowChange>& changes)
{
    for (const auto& c : changes) {
        if (c.type != ChangeType::UPDATE) continue;

        QStringList setCols = c.newValues.keys();
        if (setCols.isEmpty()) continue;

        QStringList setParts, whereParts;
        for (const auto& col : setCols)
            setParts << quote(col) + "=?";
        for (const auto& pk : m_primaryKeys)
            whereParts << quote(pk) + "=?";

        QString sql = QString("UPDATE %1 SET %2 WHERE %3")
            .arg(quote(m_table), setParts.join(","), whereParts.join(" AND "));

        QSqlQuery q(m_db);
        if (!q.prepare(sql))
            throw std::runtime_error(q.lastError().text().toStdString());

        int idx = 0;
        // SET values (new)
        for (const auto& col : setCols)
            q.bindValue(idx++, c.newValues.value(col));
        // WHERE values (old PK)
        for (const auto& pk : m_primaryKeys)
            q.bindValue(idx++, c.oldValues.value(pk));

        if (!q.exec())
            throw std::runtime_error(q.lastError().text().toStdString());
    }
}

void RowWriter::applyDeletes(const std::vector<RowChange>& changes)
{
    for (const auto& c : changes) {
        if (c.type != ChangeType::DELETE) continue;

        if (m_primaryKeys.isEmpty())
            throw std::runtime_error("DELETE without primary key is not allowed");

        QStringList whereParts;
        for (const auto& pk : m_primaryKeys)
            whereParts << quote(pk) + "=?";

        QString sql = QString("DELETE FROM %1 WHERE %2")
            .arg(quote(m_table), whereParts.join(" AND "));

        QSqlQuery q(m_db);
        if (!q.prepare(sql))
            throw std::runtime_error(q.lastError().text().toStdString());

        for (int i = 0; i < m_primaryKeys.size(); ++i)
            q.bindValue(i, c.oldValues.value(m_primaryKeys[i]));

        if (!q.exec())
            throw std::runtime_error(q.lastError().text().toStdString());
    }
}
