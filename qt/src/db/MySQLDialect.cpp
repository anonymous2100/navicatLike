#include "MySQLDialect.h"

QString MySQLDialect::getLimitSql(const QString& sql, int offset, int limit) const
{
    return sql + QString(" LIMIT %1 OFFSET %2").arg(limit).arg(offset);
}

QString MySQLDialect::getCurrentTimeFunction() const
{
    return "NOW()";
}

QString MySQLDialect::getTablesSql(const QString& schema) const
{
    if (!schema.isEmpty()) {
        return "SELECT TABLE_NAME FROM information_schema.TABLES "
               "WHERE TABLE_SCHEMA = '" + escapeStringLiteral(schema) + "' "
               "AND TABLE_TYPE = 'BASE TABLE' "
               "ORDER BY TABLE_NAME";
    }
    return "SELECT TABLE_NAME FROM information_schema.TABLES "
           "WHERE TABLE_SCHEMA = DATABASE() "
           "AND TABLE_TYPE = 'BASE TABLE' "
           "ORDER BY TABLE_NAME";
}

QString MySQLDialect::getViewsSql(const QString& schema) const
{
    if (!schema.isEmpty()) {
        return "SELECT TABLE_NAME FROM information_schema.VIEWS "
               "WHERE TABLE_SCHEMA = '" + escapeStringLiteral(schema) + "' "
               "ORDER BY TABLE_NAME";
    }
    return "SELECT TABLE_NAME FROM information_schema.VIEWS "
           "WHERE TABLE_SCHEMA = DATABASE() "
           "ORDER BY TABLE_NAME";
}

QString MySQLDialect::getColumnsSql(const QString& schema, const QString& table) const
{
    QString schemaCondition = !schema.isEmpty()
        ? "TABLE_SCHEMA = '" + escapeStringLiteral(schema) + "'"
        : "TABLE_SCHEMA = DATABASE()";

    return "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_KEY, "
           "COLUMN_DEFAULT, EXTRA, COLUMN_COMMENT, CHARACTER_MAXIMUM_LENGTH, "
           "NUMERIC_PRECISION, NUMERIC_SCALE "
           "FROM information_schema.COLUMNS "
           "WHERE " + schemaCondition + " "
           "AND TABLE_NAME = '" + escapeStringLiteral(table) + "' "
           "ORDER BY ORDINAL_POSITION";
}

QString MySQLDialect::getPrimaryKeysSql(const QString& schema, const QString& table) const
{
    QString schemaCondition = !schema.isEmpty()
        ? "TABLE_SCHEMA = '" + escapeStringLiteral(schema) + "'"
        : "TABLE_SCHEMA = DATABASE()";

    return "SELECT COLUMN_NAME "
           "FROM information_schema.KEY_COLUMN_USAGE "
           "WHERE " + schemaCondition + " "
           "AND TABLE_NAME = '" + escapeStringLiteral(table) + "' "
           "AND CONSTRAINT_NAME = 'PRIMARY' "
           "ORDER BY ORDINAL_POSITION";
}

QString MySQLDialect::getForeignKeysSql(const QString& schema, const QString& table) const
{
    QString schemaCondition = !schema.isEmpty()
        ? "TABLE_SCHEMA = '" + escapeStringLiteral(schema) + "'"
        : "TABLE_SCHEMA = DATABASE()";

    return "SELECT COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME "
           "FROM information_schema.KEY_COLUMN_USAGE "
           "WHERE " + schemaCondition + " "
           "AND TABLE_NAME = '" + escapeStringLiteral(table) + "' "
           "AND REFERENCED_TABLE_NAME IS NOT NULL "
           "ORDER BY COLUMN_NAME";
}

QString MySQLDialect::getIndexesSql(const QString& schema, const QString& table) const
{
    QString schemaCondition = !schema.isEmpty()
        ? "TABLE_SCHEMA = '" + escapeStringLiteral(schema) + "'"
        : "TABLE_SCHEMA = DATABASE()";

    return "SELECT INDEX_NAME, COLUMN_NAME, NON_UNIQUE "
           "FROM information_schema.STATISTICS "
           "WHERE " + schemaCondition + " "
           "AND TABLE_NAME = '" + escapeStringLiteral(table) + "' "
           "ORDER BY INDEX_NAME, SEQ_IN_INDEX";
}

bool MySQLDialect::supportsSchema() const
{
    return false;
}

QString MySQLDialect::getDefaultSchema() const
{
    return {};
}

QString MySQLDialect::escapeIdentifier(const QString& identifier) const
{
    QString escaped = identifier;
    escaped.replace("`", "``");
    return "`" + escaped + "`";
}

QString MySQLDialect::escapeStringLiteral(const QString& literal) const
{
    if (literal.isNull()) return "NULL";
    QString escaped = literal;
    escaped.replace("\\", "\\\\");
    escaped.replace("'", "''");
    escaped.replace("\0", "\\0");
    escaped.replace("\n", "\\n");
    escaped.replace("\r", "\\r");
    escaped.replace("\x1a", "\\Z");
    return "'" + escaped + "'";
}

bool MySQLDialect::supportsCubeRollup() const
{
    return true;
}

QString MySQLDialect::getSequencesSql(const QString& schema) const
{
    QString schemaCondition = !schema.isEmpty()
        ? "TABLE_SCHEMA = '" + escapeStringLiteral(schema) + "'"
        : "TABLE_SCHEMA = DATABASE()";

    return "SELECT TABLE_NAME, COLUMN_NAME "
           "FROM information_schema.COLUMNS "
           "WHERE EXTRA LIKE '%auto_increment%' "
           "AND " + schemaCondition +
           " ORDER BY TABLE_NAME, COLUMN_NAME";
}
