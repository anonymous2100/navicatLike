#include "SqlExecutor.h"
#include <QSqlError>
#include <QSqlRecord>
#include <QSqlField>
#include "common/Logging.h"
#include <stdexcept>

QueryResult SqlExecutor::executeQuery(QSqlDatabase db, const QString& sql,
                                       int offset, int limit)
{
    qCDebug(lcLightDB) << "Executing query:" << sql;

    QSqlQuery query(db);
    if (!query.exec(sql)) {
        throw std::runtime_error(query.lastError().text().toStdString());
    }

    QueryResult result;

    // Column names
    QSqlRecord rec = query.record();
    for (int i = 0; i < rec.count(); ++i) {
        result.columnNames << rec.fieldName(i);
    }

    // Skip to offset
    for (int i = 0; i < offset && query.next(); ++i) {}

    // Read up to limit rows
    int rowCount = 0;
    while (query.next() && rowCount < limit) {
        QVector<QVariant> row;
        for (int i = 0; i < rec.count(); ++i) {
            row.append(safeGetValue(query, i));
        }
        result.rows.append(row);
        ++rowCount;
    }

    result.totalRows = rowCount;
    result.hasMore = query.next(); // Check if there are more rows

    qCDebug(lcLightDB) << "Query returned" << result.totalRows << "rows";
    return result;
}

std::vector<ColumnInfo> SqlExecutor::getTableColumns(QSqlDatabase db,
                                                       const QString& schema,
                                                       const QString& table)
{
    std::vector<ColumnInfo> columns;

    // Use QSqlRecord for column metadata
    QSqlRecord rec = db.record(table);
    if (rec.isEmpty()) {
        // Fallback: try information_schema
        QString sql = QString(
            "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_KEY, "
            "COLUMN_DEFAULT, EXTRA, COLUMN_COMMENT, CHARACTER_MAXIMUM_LENGTH, "
            "NUMERIC_PRECISION, NUMERIC_SCALE "
            "FROM information_schema.COLUMNS "
            "WHERE TABLE_NAME = '%1' "
            "ORDER BY ORDINAL_POSITION").arg(table);
        if (!schema.isEmpty()) {
            sql += QString(" AND TABLE_SCHEMA = '%1'").arg(schema);
        }

        QSqlQuery q(db);
        if (!q.exec(sql)) {
            qCWarning(lcLightDB) << "Fallback column query failed:" << q.lastError().text();
            return columns;
        }

        while (q.next()) {
            ColumnInfo ci;
            ci.name = q.value(0).toString();
            ci.dataType = q.value(1).toString();
            ci.nullable = q.value(2).toString().toUpper() == "YES";
            ci.primaryKey = q.value(3).toString() == "PRI";
            ci.defaultValue = q.value(4).toString();
            ci.charMaxLength = q.value(7).toInt();
            ci.numericPrecision = q.value(8).toInt();
            ci.numericScale = q.value(9).toInt();
            columns.push_back(ci);
        }
        return columns;
    }

    // Build from QSqlRecord
    for (int i = 0; i < rec.count(); ++i) {
        QSqlField field = rec.field(i);
        ColumnInfo ci;
        ci.name = field.name();
        ci.dataType = field.metaType().name();
        ci.nullable = !field.requiredStatus();
        ci.autoIncrement = field.isAutoValue();
        ci.charMaxLength = field.length();
        columns.push_back(ci);
    }

    return columns;
}

QVariant SqlExecutor::safeGetValue(const QSqlQuery& query, int col)
{
    QVariant val = query.value(col);

    if (val.isNull())
        return QVariant(); // null

    // For string types, return QString directly
    if (val.metaType().id() == QMetaType::QString)
        return val.toString();

    // For byte arrays (BLOB), return size info
    if (val.metaType().id() == QMetaType::QByteArray) {
        QByteArray bytes = val.toByteArray();
        if (bytes.size() > 100)
            return QString("<binary (%1 bytes)>").arg(bytes.size());
        return QString("<binary>");
    }

    return val;
}

QString SqlExecutor::generateInsertSql(const QString& table,
                                        const std::vector<ColumnInfo>& cols)
{
    QStringList colNames, params;
    for (const auto& c : cols) {
        if (c.autoIncrement) continue;
        colNames << c.name;
        params << ":" + c.name;
    }

    return QString("INSERT INTO %1 (\n    %2\n) VALUES (\n    %3\n);")
        .arg(table, colNames.join(",\n    "), params.join(",\n    "));
}

QString SqlExecutor::generateUpdateSql(const QString& table,
                                        const std::vector<ColumnInfo>& cols)
{
    std::vector<ColumnInfo> pkCols, nonPkCols;
    for (const auto& c : cols) {
        if (c.primaryKey) pkCols.push_back(c);
        else nonPkCols.push_back(c);
    }

    if (pkCols.empty())
        return "-- Cannot generate UPDATE: missing primary key";

    QStringList setParts, whereParts;
    for (const auto& c : nonPkCols)
        setParts << "    " + c.name + " = :" + c.name;
    for (const auto& c : pkCols)
        whereParts << c.name + " = :" + c.name;

    return QString("UPDATE %1\nSET\n%2\nWHERE\n    %3;")
        .arg(table, setParts.join(",\n"), whereParts.join(" AND "));
}

QString SqlExecutor::generateDeleteSql(const QString& table,
                                        const std::vector<ColumnInfo>& cols)
{
    auto it = std::find_if(cols.begin(), cols.end(),
                            [](const ColumnInfo& c) { return c.primaryKey; });
    auto end = std::find_if(cols.rbegin(), cols.rend(),
                             [](const ColumnInfo& c) { return c.primaryKey; });

    // Filter all PK columns
    std::vector<ColumnInfo> pkCols;
    for (const auto& c : cols) {
        if (c.primaryKey) pkCols.push_back(c);
    }

    if (pkCols.empty())
        return "-- Cannot generate DELETE: missing primary key";

    QStringList whereParts;
    for (const auto& c : pkCols)
        whereParts << c.name + " = :" + c.name;

    return QString("DELETE FROM %1\nWHERE\n    %2;")
        .arg(table, whereParts.join(" AND "));
}
