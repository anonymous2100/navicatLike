#pragma once
#include <QSqlDatabase>
#include <QString>
#include <QStringList>
#include <QVector>
#include <QPair>
#include <vector>
#include "common/Types.h"

class MetadataService
{
public:
    MetadataService() = delete;

    static QStringList listSchemas(QSqlDatabase db);
    static QStringList listDatabases(QSqlDatabase db);
    static QStringList listTables(QSqlDatabase db, const QString& schema = {});
    static QStringList listViews(QSqlDatabase db, const QString& schema = {});
    static QStringList listProcedures(QSqlDatabase db, const QString& schema = {});
    static QStringList listFunctions(QSqlDatabase db, const QString& schema = {});
    static QStringList listTriggers(QSqlDatabase db, const QString& schema = {});
    static QVector<QStringList> listRoles(QSqlDatabase db);

    static std::vector<ColumnInfo> columns(QSqlDatabase db, const QString& tableName);
    static QStringList primaryKeys(QSqlDatabase db, const QString& tableName);

private:
    static QString resolveDefaultSchema(QSqlDatabase db);
    static QString resolveCatalog(QSqlDatabase db);
    static void detectEnum(QSqlDatabase db, ColumnInfo& c);
    static QStringList parseMysqlEnum(const QString& typeName);
    static bool isPostgresEnum(QSqlDatabase db, const QString& typeName);
    static QStringList loadPostgresEnumValues(QSqlDatabase db, const QString& typeName);
};
