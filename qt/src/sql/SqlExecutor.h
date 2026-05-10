#pragma once
#include <QSqlDatabase>
#include <QSqlQuery>
#include <QString>
#include <QVector>
#include <QVariant>
#include <vector>
#include "common/Types.h"

struct QueryResult
{
    QStringList columnNames;
    QVector<QVector<QVariant>> rows;
    int totalRows = 0;
    bool hasMore = false;
};

class SqlExecutor
{
public:
    SqlExecutor() = delete;

    static QueryResult executeQuery(QSqlDatabase db, const QString& sql,
                                     int offset = 0, int limit = 200);
    static std::vector<ColumnInfo> getTableColumns(QSqlDatabase db,
                                                     const QString& schema,
                                                     const QString& table);
    static QVariant safeGetValue(const QSqlQuery& query, int col);

    static QString generateInsertSql(const QString& table,
                                      const std::vector<ColumnInfo>& cols);
    static QString generateUpdateSql(const QString& table,
                                      const std::vector<ColumnInfo>& cols);
    static QString generateDeleteSql(const QString& table,
                                      const std::vector<ColumnInfo>& cols);
};
