#pragma once
#include <QSqlDatabase>
#include <QString>
#include <QStringList>
#include <QVariant>
#include <QVector>
#include <functional>

struct QueryResult
{
    bool hasResultSet = false;
    QStringList columnNames;
    QVector<QVector<QVariant>> rows;
    int updateCount = 0;
    qint64 elapsedMs = 0;
    int pageIndex = 0;
    int pageSize = 0;
    bool hasNextPage = false;
    bool truncated = false;

    static QueryResult forResultSet(const QStringList& cols,
                                     const QVector<QVector<QVariant>>& data,
                                     qint64 timeMs, int pageIdx, int pageSz,
                                     bool hasNext, bool trunc)
    {
        QueryResult r;
        r.hasResultSet = true;
        r.columnNames = cols;
        r.rows = data;
        r.elapsedMs = timeMs;
        r.pageIndex = pageIdx;
        r.pageSize = pageSz;
        r.hasNextPage = hasNext;
        r.truncated = trunc;
        return r;
    }

    static QueryResult forUpdate(int count, qint64 timeMs)
    {
        QueryResult r;
        r.hasResultSet = false;
        r.updateCount = count;
        r.elapsedMs = timeMs;
        return r;
    }
};

class QueryService
{
public:
    QueryService() = delete;

    static QueryResult execute(QSqlDatabase db, const QString& sql,
                                int pageIndex = 0, int pageSize = 200,
                                int maxRows = 10000);
    static void cancel(QSqlQuery& query);
};
