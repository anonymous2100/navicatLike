#include "QueryService.h"
#include <QSqlQuery>
#include <QSqlRecord>
#include <QSqlError>
#include <QElapsedTimer>
#include "common/Logging.h"
#include <stdexcept>

QueryResult QueryService::execute(QSqlDatabase db, const QString& sql,
                                   int pageIndex, int pageSize, int maxRows)
{
    if (sql.trimmed().isEmpty())
        throw std::runtime_error("SQL statement cannot be empty");

    qCDebug(lcLightDB) << "Executing SQL: page=" << pageIndex << "size=" << pageSize << "sql=" << sql;

    QString trimmed = sql.trimmed();
    QString lower = trimmed.left(20).toLower();
    bool isSelect = lower.startsWith("select") || lower.startsWith("with") || lower.startsWith("explain");

    QElapsedTimer timer;
    timer.start();

    if (isSelect) {
        QString executableSql = trimmed;
        if (!executableSql.endsWith(';'))
            executableSql += " LIMIT " + QString::number(pageSize + 1)
                           + " OFFSET " + QString::number(pageIndex * pageSize);

        QSqlQuery q(db);
        if (!q.exec(executableSql))
            throw std::runtime_error(q.lastError().text().toStdString());

        // Column names
        QSqlRecord rec = q.record();
        QStringList colNames;
        for (int i = 0; i < rec.count(); ++i)
            colNames << rec.fieldName(i);

        // Read rows
        QVector<QVector<QVariant>> rows;
        int rowCount = 0;
        while (q.next() && rowCount < pageSize) {
            QVector<QVariant> row;
            for (int i = 0; i < rec.count(); ++i) {
                QVariant val = q.value(i);
                if (val.metaType().id() == QMetaType::QByteArray) {
                    QByteArray ba = val.toByteArray();
                    row.append(ba.size() > 100
                        ? QVariant(QString("<binary (%1 bytes)>").arg(ba.size()))
                        : QVariant(QString("<binary>")));
                } else {
                    row.append(val);
                }
            }
            rows.append(row);
            ++rowCount;
        }

        bool hasMore = q.next(); // Check if there's more rows
        bool truncated = (pageIndex * pageSize + rows.size() >= maxRows) && hasMore;

        qCDebug(lcLightDB) << "Query returned" << rows.size() << "rows, hasNext=" << hasMore << "truncated=" << truncated;
        return QueryResult::forResultSet(colNames, rows, timer.elapsed(),
                                          pageIndex, pageSize, hasMore, truncated);
    } else {
        // DML: INSERT, UPDATE, DELETE, DDL
        QSqlQuery q(db);
        if (!q.exec(trimmed))
            throw std::runtime_error(q.lastError().text().toStdString());

        int affected = q.numRowsAffected();
        qCDebug(lcLightDB) << "DML executed:" << affected << "rows affected";
        return QueryResult::forUpdate(affected, timer.elapsed());
    }
}

void QueryService::cancel(QSqlQuery& query)
{
    // Qt SQL doesn't have direct cancel support;
    // the query object can be destroyed to interrupt
    query.finish();
    qCDebug(lcLightDB) << "Query cancelled";
}
