package com.ctgu.lightdbviewer.db;

/**
 * MySQL数据库方言实现
 */
/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class MySQLDialect implements Dialect
{
  @Override
  public DbType getDbType()
  {
    return DbType.MYSQL;
  }

  @Override
  public String getLimitSql(String sql, int offset, int limit)
  {
    return sql + " LIMIT " + limit + " OFFSET " + offset;
  }

  @Override
  public String getCurrentTimeFunction()
  {
    return "NOW()";
  }

  @Override
  public String getTablesSql(String schema)
  {
    if(schema != null && !schema.isEmpty())
    {
      return "SELECT TABLE_NAME FROM information_schema.TABLES " +
          "WHERE TABLE_SCHEMA = '" + escapeStringLiteral(schema) + "' " +
          "AND TABLE_TYPE = 'BASE TABLE' " +
          "ORDER BY TABLE_NAME";
    }
    return "SELECT TABLE_NAME FROM information_schema.TABLES " +
        "WHERE TABLE_SCHEMA = DATABASE() " +
        "AND TABLE_TYPE = 'BASE TABLE' " +
        "ORDER BY TABLE_NAME";
  }

  @Override
  public String getViewsSql(String schema)
  {
    if(schema != null && !schema.isEmpty())
    {
      return "SELECT TABLE_NAME FROM information_schema.VIEWS " +
          "WHERE TABLE_SCHEMA = '" + escapeStringLiteral(schema) + "' " +
          "ORDER BY TABLE_NAME";
    }
    return "SELECT TABLE_NAME FROM information_schema.VIEWS " +
        "WHERE TABLE_SCHEMA = DATABASE() " +
        "ORDER BY TABLE_NAME";
  }

  @Override
  public String getColumnsSql(String schema, String table)
  {
    String schemaCondition = (schema != null && !schema.isEmpty())
        ? "TABLE_SCHEMA = '" + escapeStringLiteral(schema) + "'"
        : "TABLE_SCHEMA = DATABASE()";

    return "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_KEY, " +
        "COLUMN_DEFAULT, EXTRA, COLUMN_COMMENT, CHARACTER_MAXIMUM_LENGTH, " +
        "NUMERIC_PRECISION, NUMERIC_SCALE " +
        "FROM information_schema.COLUMNS " +
        "WHERE " + schemaCondition + " " +
        "AND TABLE_NAME = '" + escapeStringLiteral(table) + "' " +
        "ORDER BY ORDINAL_POSITION";
  }

  @Override
  public String getPrimaryKeysSql(String schema, String table)
  {
    String schemaCondition = (schema != null && !schema.isEmpty())
        ? "TABLE_SCHEMA = '" + escapeStringLiteral(schema) + "'"
        : "TABLE_SCHEMA = DATABASE()";

    return "SELECT COLUMN_NAME " +
        "FROM information_schema.KEY_COLUMN_USAGE " +
        "WHERE " + schemaCondition + " " +
        "AND TABLE_NAME = '" + escapeStringLiteral(table) + "' " +
        "AND CONSTRAINT_NAME = 'PRIMARY' " +
        "ORDER BY ORDINAL_POSITION";
  }

  @Override
  public String getForeignKeysSql(String schema, String table)
  {
    String schemaCondition = (schema != null && !schema.isEmpty())
        ? "TABLE_SCHEMA = '" + escapeStringLiteral(schema) + "'"
        : "TABLE_SCHEMA = DATABASE()";

    return "SELECT COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME " +
        "FROM information_schema.KEY_COLUMN_USAGE " +
        "WHERE " + schemaCondition + " " +
        "AND TABLE_NAME = '" + escapeStringLiteral(table) + "' " +
        "AND REFERENCED_TABLE_NAME IS NOT NULL " +
        "ORDER BY COLUMN_NAME";
  }

  @Override
  public String getIndexesSql(String schema, String table)
  {
    String schemaCondition = (schema != null && !schema.isEmpty())
        ? "TABLE_SCHEMA = '" + escapeStringLiteral(schema) + "'"
        : "TABLE_SCHEMA = DATABASE()";

    return "SELECT INDEX_NAME, COLUMN_NAME, NON_UNIQUE " +
        "FROM information_schema.STATISTICS " +
        "WHERE " + schemaCondition + " " +
        "AND TABLE_NAME = '" + escapeStringLiteral(table) + "' " +
        "ORDER BY INDEX_NAME, SEQ_IN_INDEX";
  }

  @Override
  public boolean supportsSchema()
  {
    return false; // MySQL使用DATABASE而不是SCHEMA
  }

  @Override
  public String getDefaultSchema()
  {
    return null; // MySQL没有默认SCHEMA概念
  }

  @Override
  public String escapeIdentifier(String identifier)
  {
    return "`" + identifier.replace("`", "``") + "`";
  }

  @Override
  public String escapeStringLiteral(String literal)
  {
    if(literal == null)
    {
      return "NULL";
    }
    // 转义: ' '', \ \\, 以及控制字符
    String escaped = literal
        .replace("\\", "\\\\")
        .replace("'", "''")
        .replace("\0", "\\0")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("", "\\Z");
    return "'" + escaped + "'";
  }

  @Override
  public boolean supportsCubeRollup()
  {
    return true; // MySQL 8.0+ 支持CUBE/ROLLUP
  }

  @Override
  public String getSequencesSql(String schema)
  {
    // MySQL使用AUTO_INCREMENT而不是SEQUENCE
    return "SELECT TABLE_NAME, COLUMN_NAME " +
        "FROM information_schema.COLUMNS " +
        "WHERE EXTRA LIKE '%auto_increment%' " +
        ((schema != null && !schema.isEmpty())
            ? "AND TABLE_SCHEMA = '" + escapeStringLiteral(schema) + "'"
            : "AND TABLE_SCHEMA = DATABASE()") +
        " ORDER BY TABLE_NAME, COLUMN_NAME";
  }
}


