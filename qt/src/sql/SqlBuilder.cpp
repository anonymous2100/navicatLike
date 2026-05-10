#include "SqlBuilder.h"
#include <QSqlError>
#include <stdexcept>

QSqlQuery SqlBuilder::createInsert(QSqlDatabase db, const QString& table,
                                    const std::vector<ColumnInfo>& cols)
{
    QStringList colNames;
    QStringList placeholders;
    for (const auto& c : cols) {
        if (c.autoIncrement) continue;
        colNames << c.name;
        placeholders << "?";
    }

    if (colNames.isEmpty())
        throw std::runtime_error("No insertable columns");

    QString sql = QString("INSERT INTO %1 (%2) VALUES (%3)")
        .arg(table, colNames.join(", "), placeholders.join(", "));

    QSqlQuery q(db);
    if (!q.prepare(sql)) {
        throw std::runtime_error(q.lastError().text().toStdString());
    }
    return q;
}

QSqlQuery SqlBuilder::createUpdate(QSqlDatabase db, const QString& table,
                                    const std::vector<ColumnInfo>& cols)
{
    std::vector<ColumnInfo> pkCols, nonPkCols;
    for (const auto& c : cols) {
        if (c.primaryKey) pkCols.push_back(c);
        else nonPkCols.push_back(c);
    }

    if (pkCols.empty())
        throw std::runtime_error("UPDATE requires primary key columns");

    QStringList setParts, whereParts;
    for (const auto& c : nonPkCols)
        setParts << c.name + " = ?";
    for (const auto& c : pkCols)
        whereParts << c.name + " = ?";

    QString sql = QString("UPDATE %1 SET %2 WHERE %3")
        .arg(table, setParts.join(", "), whereParts.join(" AND "));

    QSqlQuery q(db);
    if (!q.prepare(sql)) {
        throw std::runtime_error(q.lastError().text().toStdString());
    }
    return q;
}

QSqlQuery SqlBuilder::createDelete(QSqlDatabase db, const QString& table,
                                    const std::vector<ColumnInfo>& pkCols)
{
    if (pkCols.empty())
        throw std::runtime_error("DELETE requires primary key columns");

    QStringList whereParts;
    for (const auto& c : pkCols)
        whereParts << c.name + " = ?";

    QString sql = QString("DELETE FROM %1 WHERE %2")
        .arg(table, whereParts.join(" AND "));

    QSqlQuery q(db);
    if (!q.prepare(sql)) {
        throw std::runtime_error(q.lastError().text().toStdString());
    }
    return q;
}

QString SqlBuilder::createSelect(const QString& table,
                                  const std::vector<ColumnInfo>& cols,
                                  const QString& where)
{
    QStringList colNames;
    for (const auto& c : cols)
        colNames << c.name;

    QString sql = QString("SELECT %1 FROM %2").arg(colNames.join(", "), table);

    if (!where.trimmed().isEmpty()) {
        if (where.contains(";") || where.contains("--") || where.contains("/*"))
            throw std::runtime_error("WHERE clause contains illegal characters");
        sql += " WHERE " + where;
    }
    return sql;
}
