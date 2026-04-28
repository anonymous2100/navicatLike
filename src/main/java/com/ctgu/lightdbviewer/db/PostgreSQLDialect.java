package com.ctgu.lightdbviewer.db;

/**
 * PostgreSQL数据库方言实现
 */
/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class PostgreSQLDialect implements Dialect
{
  @Override
  public DbType getDbType()
  {
    return DbType.POSTGRESQL;
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
        "WHERE TABLE_SCHEMA = 'public' " +
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
        "WHERE TABLE_SCHEMA = 'public' " +
        "ORDER BY TABLE_NAME";
  }

  @Override
  public String getColumnsSql(String schema, String table)
  {
    String schemaName = (schema != null && !schema.isEmpty()) ? schema : "public";

    return "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, " +
        "COLUMN_DEFAULT, CHARACTER_MAXIMUM_LENGTH, " +
        "NUMERIC_PRECISION, NUMERIC_SCALE " +
        "FROM information_schema.COLUMNS " +
        "WHERE TABLE_SCHEMA = '" + escapeStringLiteral(schemaName) + "' " +
        "AND TABLE_NAME = '" + escapeStringLiteral(table) + "' " +
        "ORDER BY ORDINAL_POSITION";
  }

  @Override
  public String getPrimaryKeysSql(String schema, String table)
  {
    String schemaName = (schema != null && !schema.isEmpty()) ? schema : "public";

    return "SELECT kcu.COLUMN_NAME " +
        "FROM information_schema.TABLE_CONSTRAINTS tc " +
        "JOIN information_schema.KEY_COLUMN_USAGE kcu " +
        "ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME " +
        "AND tc.TABLE_SCHEMA = kcu.TABLE_SCHEMA " +
        "WHERE tc.TABLE_SCHEMA = '" + escapeStringLiteral(schemaName) + "' " +
        "AND tc.TABLE_NAME = '" + escapeStringLiteral(table) + "' " +
        "AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY' " +
        "ORDER BY kcu.ORDINAL_POSITION";
  }

  @Override
  public String getForeignKeysSql(String schema, String table)
  {
    String schemaName = (schema != null && !schema.isEmpty()) ? schema : "public";

    return "SELECT " +
        "kcu.COLUMN_NAME, " +
        "ccu.TABLE_NAME AS REFERENCED_TABLE_NAME, " +
        "ccu.COLUMN_NAME AS REFERENCED_COLUMN_NAME " +
        "FROM information_schema.TABLE_CONSTRAINTS tc " +
        "JOIN information_schema.KEY_COLUMN_USAGE kcu " +
        "ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME " +
        "AND tc.TABLE_SCHEMA = kcu.TABLE_SCHEMA " +
        "JOIN information_schema.CONSTRAINT_COLUMN_USAGE ccu " +
        "ON ccu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME " +
        "AND ccu.TABLE_SCHEMA = tc.TABLE_SCHEMA " +
        "WHERE tc.TABLE_SCHEMA = '" + escapeStringLiteral(schemaName) + "' " +
        "AND tc.TABLE_NAME = '" + escapeStringLiteral(table) + "' " +
        "AND tc.CONSTRAINT_TYPE = 'FOREIGN KEY' " +
        "ORDER BY kcu.COLUMN_NAME";
  }

  @Override
  public String getIndexesSql(String schema, String table)
  {
    String schemaName = (schema != null && !schema.isEmpty()) ? schema : "public";

    return "SELECT " +
        "i.relname AS INDEX_NAME, " +
        "a.attname AS COLUMN_NAME, " +
        "ix.indisunique AS NON_UNIQUE " +
        "FROM pg_class t " +
        "JOIN pg_index ix ON t.oid = ix.indrelid " +
        "JOIN pg_class i ON i.oid = ix.indexrelid " +
        "JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(ix.indkey) " +
        "JOIN pg_namespace n ON n.oid = t.relnamespace " +
        "WHERE t.relkind = 'r' " +
        "AND n.nspname = '" + escapeStringLiteral(schemaName) + "' " +
        "AND t.relname = '" + escapeStringLiteral(table) + "' " +
        "ORDER BY i.relname, a.attnum";
  }

  @Override
  public boolean supportsSchema()
  {
    return true;
  }

  @Override
  public String getDefaultSchema()
  {
    return "public";
  }

  @Override
  public String escapeIdentifier(String identifier)
  {
    return "\"" + identifier.replace("\"", "\"\"") + "\"";
  }

  @Override
  public String escapeStringLiteral(String literal)
  {
    if(literal == null)
    {
      return "NULL";
    }
    return "'" + literal.replace("'", "''") + "'";
  }

  @Override
  public boolean supportsCubeRollup()
  {
    return true; // PostgreSQL 支持CUBE/ROLLUP
  }

  @Override
  public String getSequencesSql(String schema)
  {
    String schemaCondition = (schema != null && !schema.isEmpty())
        ? "n.nspname = '" + escapeStringLiteral(schema) + "'"
        : "n.nspname = 'public'";

    return "SELECT c.relname AS SEQUENCE_NAME " +
        "FROM pg_class c " +
        "JOIN pg_namespace n ON n.oid = c.relnamespace " +
        "WHERE c.relkind = 'S' " +
        "AND " + schemaCondition + " " +
        "ORDER BY c.relname";
  }
}


