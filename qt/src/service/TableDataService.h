#pragma once
#include <QSqlDatabase>
#include <QString>
#include <QStringList>
#include <QVector>
#include <QVariant>
#include "common/Types.h"
#include "sql/RowWriter.h"

class TableDataService
{
public:
    TableDataService() = delete;

    struct TableData
    {
        QStringList columnNames;
        std::vector<ColumnInfo> columnInfos;
        QVector<QVector<QVariant>> rows;
        int totalRows = 0;
        int pageIndex = 0;
        int pageSize = 0;
        bool hasMore = false;
    };

    static TableData loadTableData(QSqlDatabase db, const QString& table,
                                    int pageIndex, int pageSize);
    static void saveChanges(QSqlDatabase db, const QString& table,
                             const QStringList& primaryKeys,
                             const std::vector<RowChange>& changes);
};
