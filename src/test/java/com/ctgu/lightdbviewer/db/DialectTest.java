package com.ctgu.lightdbviewer.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库方言测试
 */
class DialectTest
{
  @Test
  void testMySQLDialectType()
  {
    Dialect dialect = DialectFactory.getDialect(DbType.MYSQL);
    assertEquals(DbType.MYSQL, dialect.getDbType());
    assertTrue(dialect instanceof MySQLDialect);
  }

  @Test
  void testPostgreSQLDialectType()
  {
    Dialect dialect = DialectFactory.getDialect(DbType.POSTGRESQL);
    assertEquals(DbType.POSTGRESQL, dialect.getDbType());
    assertTrue(dialect instanceof PostgreSQLDialect);
  }

  @Test
  void testMySQLLimitSql()
  {
    Dialect dialect = DialectFactory.getDialect(DbType.MYSQL);
    String sql = "SELECT * FROM users";
    String limited = dialect.getLimitSql(sql, 10, 20);

    assertTrue(limited.contains("LIMIT 20"));
    assertTrue(limited.contains("OFFSET 10"));
    assertTrue(limited.contains("SELECT * FROM users"));
  }

  @Test
  void testPostgreSQLLimitSql()
  {
    Dialect dialect = DialectFactory.getDialect(DbType.POSTGRESQL);
    String sql = "SELECT * FROM users";
    String limited = dialect.getLimitSql(sql, 10, 20);

    assertTrue(limited.contains("LIMIT 20"));
    assertTrue(limited.contains("OFFSET 10"));
    assertTrue(limited.contains("SELECT * FROM users"));
  }

  @Test
  void testMySQLSupportsSchema()
  {
    Dialect dialect = DialectFactory.getDialect(DbType.MYSQL);
    assertFalse(dialect.supportsSchema());
    assertNull(dialect.getDefaultSchema());
  }

  @Test
  void testPostgreSQLSupportsSchema()
  {
    Dialect dialect = DialectFactory.getDialect(DbType.POSTGRESQL);
    assertTrue(dialect.supportsSchema());
    assertEquals("public", dialect.getDefaultSchema());
  }

  @Test
  void testMySQLEscapeIdentifier()
  {
    Dialect dialect = DialectFactory.getDialect(DbType.MYSQL);
    assertEquals("`table_name`", dialect.escapeIdentifier("table_name"));
    assertEquals("`table``name`", dialect.escapeIdentifier("table`name"));
  }

  @Test
  void testPostgreSQLEscapeIdentifier()
  {
    Dialect dialect = DialectFactory.getDialect(DbType.POSTGRESQL);
    assertEquals("\"table_name\"", dialect.escapeIdentifier("table_name"));
    assertEquals("\"table\"\"name\"", dialect.escapeIdentifier("table\"name"));
  }

  @Test
  void testMySQLEscapeStringLiteral()
  {
    Dialect dialect = DialectFactory.getDialect(DbType.MYSQL);
    assertEquals("'test'", dialect.escapeStringLiteral("test"));
    assertEquals("'test''s'", dialect.escapeStringLiteral("test's"));
    assertEquals("'test\\\\value'", dialect.escapeStringLiteral("test\\value"));
    assertEquals("NULL", dialect.escapeStringLiteral(null));
  }

  @Test
  void testPostgreSQLEscapeStringLiteral()
  {
    Dialect dialect = DialectFactory.getDialect(DbType.POSTGRESQL);
    assertEquals("'test'", dialect.escapeStringLiteral("test"));
    assertEquals("'test''s'", dialect.escapeStringLiteral("test's"));
    assertEquals("NULL", dialect.escapeStringLiteral(null));
  }

  @Test
  void testDialectFactoryGetByDatabaseName()
  {
    Dialect mysqlDialect = DialectFactory.getDialect("MySQL Community Server");
    assertEquals(DbType.MYSQL, mysqlDialect.getDbType());

    Dialect postgresDialect = DialectFactory.getDialect("PostgreSQL");
    assertEquals(DbType.POSTGRESQL, postgresDialect.getDbType());
  }

  @Test
  void testDialectFactoryInvalidDatabaseName()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      DialectFactory.getDialect("Oracle Database");
    });
  }

  @Test
  void testDialectFactoryNullDatabaseName()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      DialectFactory.getDialect((String) null);
    });
  }

  @Test
  void testDialectFactoryIsSupported()
  {
    assertTrue(DialectFactory.isSupported(DbType.MYSQL));
    assertTrue(DialectFactory.isSupported(DbType.POSTGRESQL));
    assertFalse(DialectFactory.isSupported(null));
  }

  @Test
  void testDialectFactoryGetSupportedTypes()
  {
    DbType[] types = DialectFactory.getSupportedTypes();
    assertNotNull(types);
    assertTrue(types.length >= 2);

    boolean hasMySQL = false;
    boolean hasPostgreSQL = false;

    for(DbType type : types)
    {
      if(type == DbType.MYSQL)
      {
        hasMySQL = true;
      }
      if(type == DbType.POSTGRESQL)
      {
        hasPostgreSQL = true;
      }
    }

    assertTrue(hasMySQL);
    assertTrue(hasPostgreSQL);
  }

  @Test
  void testGetCurrentTimeFunction()
  {
    Dialect mysqlDialect = DialectFactory.getDialect(DbType.MYSQL);
    assertEquals("NOW()", mysqlDialect.getCurrentTimeFunction());

    Dialect postgresDialect = DialectFactory.getDialect(DbType.POSTGRESQL);
    assertEquals("NOW()", postgresDialect.getCurrentTimeFunction());
  }

  @Test
  void testSupportsCubeRollup()
  {
    Dialect mysqlDialect = DialectFactory.getDialect(DbType.MYSQL);
    assertTrue(mysqlDialect.supportsCubeRollup());

    Dialect postgresDialect = DialectFactory.getDialect(DbType.POSTGRESQL);
    assertTrue(postgresDialect.supportsCubeRollup());
  }
}
