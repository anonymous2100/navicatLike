#include "TableDataService.h"
#include "meta/MetadataService.h"
#include <QSqlQuery>
#include <QSqlRecord>
#include <QSqlError>
#include "common/Logging.h"
#include <stdexcept>

TableDataService::TableData TableDataService::loadTableData(
    QSqlDatabase db, const QString& table, int pageIndex, int pageSize)
{
    TableData result;
    result.pageIndex = pageIndex;
    result.pageSize = pageSize;

    // Get column info
    result.columnInfos = MetadataService::columns(db, table);
    for (const auto& ci : result.columnInfos)
        result.columnNames << ci.name;

    // Build SELECT with pagination
    QStringList colNames;
    for (const auto& ci : result.columnInfos)
        colNames << ci.name;

    QString sql = QString("SELECT %1 FROM %2 LIMIT %3 OFFSET %4")
        .arg(colNames.join(", "), table)
        .arg(pageSize + 1)  // +1 to check hasMore
        .arg(pageIndex * pageSize);

    QSqlQuery q(db);
    if (!q.exec(sql))
        throw std::runtime_error(q.lastError().text().toStdString());

    int rowCount = 0;
    while (q.next() && rowCount < pageSize) {
        QVector<QVariant> row;
        QSqlRecord rec = q.record();
        for (int i = 0; i < rec.count(); ++i) {
            row.append(q.value(i));
        }
        result.rows.append(row);
        ++rowCount;
    }

    result.totalRows = rowCount;
    result.hasMore = q.next(); // There's at least one more row
    qCDebug(lcLightDB) << "Loaded" << rowCount << "rows from table" << table;
    return result;
}

void TableDataService::saveChanges(QSqlDatabase db, const QString& table,
                                    const QStringList& primaryKeys,
                                    const std::vector<RowChange>& changes)
{
    RowWriter writer(db, table, primaryKeys);
    writer.applyChanges(changes);
    qCInfo(lcLightDB) << "Saved" << changes.size() << "changes to table" << table;
}
