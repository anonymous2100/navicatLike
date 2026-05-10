#include "PostgreSQLDialect.h"

QString PostgreSQLDialect::getLimitSql(const QString& sql, int offset, int limit) const
{
    return sql + QString(" LIMIT %1 OFFSET %2").arg(limit).arg(offset);
}

QString PostgreSQLDialect::getCurrentTimeFunction() const
{
    return "NOW()";
}

QString PostgreSQLDialect::getTablesSql(const QString& schema) const
{
    QString schemaName = !schema.isEmpty() ? schema : "public";
    return "SELECT TABLE_NAME FROM information_schema.TABLES "
           "WHERE TABLE_SCHEMA = '" + escapeStringLiteral(schemaName) + "' "
           "AND TABLE_TYPE = 'BASE TABLE' "
           "ORDER BY TABLE_NAME";
}

QString PostgreSQLDialect::getViewsSql(const QString& schema) const
{
    QString schemaName = !schema.isEmpty() ? schema : "public";
    return "SELECT TABLE_NAME FROM information_schema.VIEWS "
           "WHERE TABLE_SCHEMA = '" + escapeStringLiteral(schemaName) + "' "
           "ORDER BY TABLE_NAME";
}

QString PostgreSQLDialect::getColumnsSql(const QString& schema, const QString& table) const
{
    QString schemaName = !schema.isEmpty() ? schema : "public";
    return "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, "
           "COLUMN_DEFAULT, CHARACTER_MAXIMUM_LENGTH, "
           "NUMERIC_PRECISION, NUMERIC_SCALE "
           "FROM information_schema.COLUMNS "
           "WHERE TABLE_SCHEMA = '" + escapeStringLiteral(schemaName) + "' "
           "AND TABLE_NAME = '" + escapeStringLiteral(table) + "' "
           "ORDER BY ORDINAL_POSITION";
}

QString PostgreSQLDialect::getPrimaryKeysSql(const QString& schema, const QString& table) const
{
    QString schemaName = !schema.isEmpty() ? schema : "public";
    return "SELECT kcu.COLUMN_NAME "
           "FROM information_schema.TABLE_CONSTRAINTS tc "
           "JOIN information_schema.KEY_COLUMN_USAGE kcu "
           "ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME "
           "AND tc.TABLE_SCHEMA = kcu.TABLE_SCHEMA "
           "WHERE tc.TABLE_SCHEMA = '" + escapeStringLiteral(schemaName) + "' "
           "AND tc.TABLE_NAME = '" + escapeStringLiteral(table) + "' "
           "AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY' "
           "ORDER BY kcu.ORDINAL_POSITION";
}

QString PostgreSQLDialect::getForeignKeysSql(const QString& schema, const QString& table) const
{
    QString schemaName = !schema.isEmpty() ? schema : "public";
    return "SELECT "
           "kcu.COLUMN_NAME, "
           "ccu.TABLE_NAME AS REFERENCED_TABLE_NAME, "
           "ccu.COLUMN_NAME AS REFERENCED_COLUMN_NAME "
           "FROM information_schema.TABLE_CONSTRAINTS tc "
           "JOIN information_schema.KEY_COLUMN_USAGE kcu "
           "ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME "
           "AND tc.TABLE_SCHEMA = kcu.TABLE_SCHEMA "
           "JOIN information_schema.CONSTRAINT_COLUMN_USAGE ccu "
           "ON ccu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME "
           "AND ccu.TABLE_SCHEMA = tc.TABLE_SCHEMA "
           "WHERE tc.TABLE_SCHEMA = '" + escapeStringLiteral(schemaName) + "' "
           "AND tc.TABLE_NAME = '" + escapeStringLiteral(table) + "' "
           "AND tc.CONSTRAINT_TYPE = 'FOREIGN KEY' "
           "ORDER BY kcu.COLUMN_NAME";
}

QString PostgreSQLDialect::getIndexesSql(const QString& schema, const QString& table) const
{
    QString schemaName = !schema.isEmpty() ? schema : "public";
    return "SELECT "
           "i.relname AS INDEX_NAME, "
           "a.attname AS COLUMN_NAME, "
           "ix.indisunique AS NON_UNIQUE "
           "FROM pg_class t "
           "JOIN pg_index ix ON t.oid = ix.indrelid "
           "JOIN pg_class i ON i.oid = ix.indexrelid "
           "JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(ix.indkey) "
           "JOIN pg_namespace n ON n.oid = t.relnamespace "
           "WHERE t.relkind = 'r' "
           "AND n.nspname = '" + escapeStringLiteral(schemaName) + "' "
           "AND t.relname = '" + escapeStringLiteral(table) + "' "
           "ORDER BY i.relname, a.attnum";
}

bool PostgreSQLDialect::supportsSchema() const
{
    return true;
}

QString PostgreSQLDialect::getDefaultSchema() const
{
    return "public";
}

QString PostgreSQLDialect::escapeIdentifier(const QString& identifier) const
{
    QString escaped = identifier;
    escaped.replace("\"", "\"\"");
    return "\"" + escaped + "\"";
}

QString PostgreSQLDialect::escapeStringLiteral(const QString& literal) const
{
    if (literal.isNull()) return "NULL";
    QString escaped = literal;
    escaped.replace("'", "''");
    return "'" + escaped + "'";
}

bool PostgreSQLDialect::supportsCubeRollup() const
{
    return true;
}

QString PostgreSQLDialect::getSequencesSql(const QString& schema) const
{
    QString schemaCondition = !schema.isEmpty()
        ? "n.nspname = '" + escapeStringLiteral(schema) + "'"
        : "n.nspname = 'public'";

    return "SELECT c.relname AS SEQUENCE_NAME "
           "FROM pg_class c "
           "JOIN pg_namespace n ON n.oid = c.relnamespace "
           "WHERE c.relkind = 'S' "
           "AND " + schemaCondition + " "
           "ORDER BY c.relname";
}
