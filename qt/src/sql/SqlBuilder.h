#pragma once
#include <QSqlDatabase>
#include <QSqlQuery>
#include <QString>
#include <QStringList>
#include <vector>
#include "common/Types.h"

class SqlBuilder
{
public:
    SqlBuilder() = delete;

    static QSqlQuery createInsert(QSqlDatabase db, const QString& table,
                                   const std::vector<ColumnInfo>& cols);
    static QSqlQuery createUpdate(QSqlDatabase db, const QString& table,
                                   const std::vector<ColumnInfo>& cols);
    static QSqlQuery createDelete(QSqlDatabase db, const QString& table,
                                   const std::vector<ColumnInfo>& pkCols);
    static QString createSelect(const QString& table,
                                 const std::vector<ColumnInfo>& cols,
                                 const QString& where = {});
};
