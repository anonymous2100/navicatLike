#include "MetadataService.h"
#include <QSqlQuery>
#include <QSqlRecord>
#include <QSqlError>
#include <QSqlField>
#include <QSqlIndex>
#include "common/Logging.h"

QStringList MetadataService::listSchemas(QSqlDatabase db)
{
    QStringList schemas;
    QString driver = db.driverName().toLower();
    if (driver == "qpsql" || driver == "qpsql7") {
        QSqlQuery q(db);
        if (q.exec("SELECT schema_name FROM information_schema.schemata "
                    "WHERE schema_name NOT IN ('pg_catalog','information_schema','pg_toast') "
                    "AND schema_name NOT LIKE 'pg_%' "
                    "ORDER BY schema_name")) {
            while (q.next())
                schemas << q.value(0).toString();
        }
    }
    // MySQL doesn't need schema level
    return schemas;
}

QStringList MetadataService::listDatabases(QSqlDatabase db)
{
    QStringList databases;
    QString driver = db.driverName().toLower();
    if (driver == "qmysql" || driver == "qmariadb") {
        QSqlQuery q(db);
        if (q.exec("SHOW DATABASES")) {
            while (q.next())
                databases << q.value(0).toString();
        }
    } else {
        // PostgreSQL
        QSqlQuery q(db);
        if (q.exec("SELECT datname FROM pg_database WHERE datistemplate = false ORDER BY datname")) {
            while (q.next())
                databases << q.value(0).toString();
        }
    }
    return databases;
}

QStringList MetadataService::listTables(QSqlDatabase db, const QString& schema)
{
    QStringList tables;
    QSqlRecord rec = db.record("");
    // Use QSqlDatabase::tables()
    QStringList tblList = db.tables(QSql::Tables);
    for (const auto& t : tblList) {
        if (schema.isEmpty() || true) // QSqlDatabase::tables already filters
            tables << t;
    }
    return tables;
}

QStringList MetadataService::listViews(QSqlDatabase db, const QString& schema)
{
    QStringList views;
    // QSqlDatabase::tables with QSql::Views
    views = db.tables(QSql::Views);
    return views;
}

QStringList MetadataService::listProcedures(QSqlDatabase db, const QString& schema)
{
    QStringList procedures;
    QString driver = db.driverName().toLower();
    if (driver == "qpsql" || driver == "qpsql7") {
        QString sql = "SELECT proname FROM pg_proc p "
                       "JOIN pg_namespace n ON p.pronamespace = n.oid "
                       "WHERE n.nspname = ? AND p.prokind = 'p' ORDER BY proname";
        QSqlQuery q(db);
        q.prepare(sql);
        q.addBindValue(schema.isEmpty() ? resolveDefaultSchema(db) : schema);
        if (q.exec()) {
            while (q.next())
                procedures << q.value(0).toString();
        }
    } else {
        QSqlQuery q(db);
        QString sql = "SELECT ROUTINE_NAME FROM information_schema.ROUTINES "
                       "WHERE ROUTINE_TYPE = 'PROCEDURE' AND ROUTINE_SCHEMA = ? "
                       "ORDER BY ROUTINE_NAME";
        q.prepare(sql);
        q.addBindValue(schema.isEmpty() ? resolveCatalog(db) : schema);
        if (q.exec()) {
            while (q.next())
                procedures << q.value(0).toString();
        }
    }
    return procedures;
}

QStringList MetadataService::listFunctions(QSqlDatabase db, const QString& schema)
{
    QStringList functions;
    QString driver = db.driverName().toLower();
    if (driver == "qpsql" || driver == "qpsql7") {
        QString sql = "SELECT proname FROM pg_proc p "
                       "JOIN pg_namespace n ON p.pronamespace = n.oid "
                       "WHERE n.nspname = ? AND p.prokind = 'f' ORDER BY proname";
        QSqlQuery q(db);
        q.prepare(sql);
        q.addBindValue(schema.isEmpty() ? resolveDefaultSchema(db) : schema);
        if (q.exec()) {
            while (q.next())
                functions << q.value(0).toString();
        }
    } else {
        QSqlQuery q(db);
        QString sql = "SELECT ROUTINE_NAME FROM information_schema.ROUTINES "
                       "WHERE ROUTINE_TYPE = 'FUNCTION' AND ROUTINE_SCHEMA = ? "
                       "ORDER BY ROUTINE_NAME";
        q.prepare(sql);
        q.addBindValue(schema.isEmpty() ? resolveCatalog(db) : schema);
        if (q.exec()) {
            while (q.next())
                functions << q.value(0).toString();
        }
    }
    return functions;
}

QStringList MetadataService::listTriggers(QSqlDatabase db, const QString& schema)
{
    QStringList triggers;
    QString driver = db.driverName().toLower();
    QString sql;
    if (driver == "qpsql" || driver == "qpsql7") {
        sql = "SELECT DISTINCT trigger_name FROM information_schema.triggers "
              "WHERE trigger_schema = ? ORDER BY trigger_name";
    } else {
        sql = "SELECT DISTINCT trigger_name FROM information_schema.triggers "
              "WHERE trigger_schema = ? ORDER BY trigger_name";
    }
    QSqlQuery q(db);
    q.prepare(sql);
    q.addBindValue(schema.isEmpty() ? resolveDefaultSchema(db) : schema);
    if (q.exec()) {
        QSet<QString> seen;
        while (q.next()) {
            QString name = q.value(0).toString();
            if (!seen.contains(name)) {
                seen.insert(name);
                triggers << name;
            }
        }
    }
    return triggers;
}

QVector<QStringList> MetadataService::listRoles(QSqlDatabase db)
{
    QVector<QStringList> roles;
    QString driver = db.driverName().toLower();
    if (driver == "qpsql" || driver == "qpsql7") {
        QSqlQuery q(db);
        if (q.exec("SELECT rolname, CASE WHEN rolsuper THEN 'Yes' ELSE 'No' END, "
                    "CASE WHEN rolcreatedb THEN 'Yes' ELSE 'No' END, "
                    "CASE WHEN rolcanlogin THEN 'Yes' ELSE 'No' END "
                    "FROM pg_roles ORDER BY rolname")) {
            while (q.next())
                roles << QStringList({q.value(0).toString(), q.value(1).toString(),
                                       q.value(2).toString(), q.value(3).toString()});
        }
    } else {
        QSqlQuery q(db);
        if (q.exec("SELECT user, host FROM mysql.user ORDER BY user")) {
            while (q.next())
                roles << QStringList({q.value(0).toString(), q.value(1).toString(), "", ""});
        } else {
            roles << QStringList({db.userName(), "current", "", ""});
        }
    }
    return roles;
}

std::vector<ColumnInfo> MetadataService::columns(QSqlDatabase db, const QString& tableName)
{
    std::vector<ColumnInfo> result;
    QString schema = resolveDefaultSchema(db);

    // Use QSqlRecord for basic column info
    QSqlRecord rec = db.record(tableName);
    QHash<QString, ColumnInfo> colMap;

    for (int i = 0; i < rec.count(); ++i) {
        QSqlField f = rec.field(i);
        ColumnInfo ci;
        ci.name = f.name();
        ci.dataType = f.metaType().name();
        ci.nullable = !f.requiredStatus();
        ci.autoIncrement = f.isAutoValue();
        ci.charMaxLength = f.length();
        colMap[ci.name] = ci;
    }

    // Primary keys
    try {
        QStringList pks = primaryKeys(db, tableName);
        for (const auto& pk : pks) {
            if (colMap.contains(pk)) {
                colMap[pk].primaryKey = true;
            }
        }
    } catch (...) {}

    // Detect enums
    for (auto it = colMap.begin(); it != colMap.end(); ++it) {
        try {
            detectEnum(db, it.value());
        } catch (...) {}
    }

    for (auto it = colMap.begin(); it != colMap.end(); ++it) {
        result.push_back(it.value());
    }
    return result;
}

QStringList MetadataService::primaryKeys(QSqlDatabase db, const QString& tableName)
{
    QStringList pks;
    QSqlRecord rec = db.record(tableName);
    for (int i = 0; i < rec.count(); ++i) {
        QSqlField f = rec.field(i);
        if (f.requiredStatus() && f.isAutoValue()) {
            pks << f.name();
        }
    }
    // If the heuristic above fails, query the database
    if (pks.isEmpty()) {
        QSqlIndex pkIdx = db.primaryIndex(tableName);
        for (int i = 0; i < pkIdx.count(); ++i) {
            pks << pkIdx.fieldName(i);
        }
    }
    return pks;
}

QString MetadataService::resolveDefaultSchema(QSqlDatabase db)
{
    QString driver = db.driverName().toLower();
    if (driver == "qpsql" || driver == "qpsql7") return "public";
    return {};
}

QString MetadataService::resolveCatalog(QSqlDatabase db)
{
    QString driver = db.driverName().toLower();
    if (driver == "qmysql" || driver == "qmariadb") return db.databaseName();
    return {};
}

void MetadataService::detectEnum(QSqlDatabase db, ColumnInfo& c)
{
    if (c.dataType.isEmpty()) return;
    QString type = c.dataType.toLower();

    // MySQL ENUM
    if (type.startsWith("enum")) {
        c.enumValues = parseMysqlEnum(c.dataType);
        return;
    }

    // PostgreSQL ENUM
    if (isPostgresEnum(db, c.dataType)) {
        c.enumValues = loadPostgresEnumValues(db, c.dataType);
    }
}

QStringList MetadataService::parseMysqlEnum(const QString& typeName)
{
    int start = typeName.indexOf('(');
    int end = typeName.lastIndexOf(')');
    if (start < 0 || end < 0) return {};

    QString body = typeName.mid(start + 1, end - start - 1);
    QStringList result;
    for (const auto& p : body.split(",")) {
        result << p.trimmed().replace("'", "");
    }
    return result;
}

bool MetadataService::isPostgresEnum(QSqlDatabase db, const QString& typeName)
{
    QSqlQuery q(db);
    q.prepare("SELECT 1 FROM pg_type WHERE typname = ? AND typtype = 'e'");
    q.addBindValue(typeName);
    return q.exec() && q.next();
}

QStringList MetadataService::loadPostgresEnumValues(QSqlDatabase db, const QString& typeName)
{
    QStringList values;
    QSqlQuery q(db);
    q.prepare("SELECT enumlabel FROM pg_enum "
               "JOIN pg_type t ON t.oid = pg_enum.enumtypid "
               "WHERE t.typname = ? ORDER BY enumsortorder");
    q.addBindValue(typeName);
    if (q.exec()) {
        while (q.next())
            values << q.value(0).toString();
    }
    return values;
}
