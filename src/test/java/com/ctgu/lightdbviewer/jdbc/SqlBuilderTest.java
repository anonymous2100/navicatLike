package com.ctgu.lightdbviewer.jdbc;

import com.ctgu.lightdbviewer.metadata.ColumnInfo;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SQL构建器测试
 */
class SqlBuilderTest
{
  @Test
  void testCreateInsertStatement() throws SQLException
  {
    List<ColumnInfo> columns = createTestColumns();
    String sql = SqlBuilder.createSelect("test_table", columns, null);

    assertTrue(sql.contains("SELECT"));
    assertTrue(sql.contains("FROM test_table"));
    assertTrue(sql.contains("id"));
    assertTrue(sql.contains("name"));
    assertTrue(sql.contains("email"));
  }

  @Test
  void testCreateSelectWithWhere() throws SQLException
  {
    List<ColumnInfo> columns = createTestColumns();
    String sql = SqlBuilder.createSelect("test_table", columns, "id > 10");

    assertTrue(sql.contains("WHERE id > 10"));
  }

  @Test
  void testCreateSelectWithEmptyColumns() throws SQLException
  {
    List<ColumnInfo> columns = new ArrayList<>();
    String sql = SqlBuilder.createSelect("test_table", columns, null);

    assertTrue(sql.contains("SELECT"));
    assertTrue(sql.contains("FROM test_table"));
  }

  @Test
  void testCreateInsertWithoutAutoIncrement() throws SQLException
  {
    List<ColumnInfo> columns = createTestColumns();
    String insertSql = com.ctgu.lightdbviewer.jdbc.JdbcExecutor.generateInsertSql("test_table", columns);

    assertTrue(insertSql.contains("INSERT INTO test_table"));
    assertTrue(insertSql.contains("name"));
    assertTrue(insertSql.contains("email"));

    // id 是自增列，不应该出现在INSERT中
    assertFalse(insertSql.contains(":id"));
  }

  @Test
  void testCreateUpdateWithPrimaryKey() throws SQLException
  {
    List<ColumnInfo> columns = createTestColumns();
    String updateSql = com.ctgu.lightdbviewer.jdbc.JdbcExecutor.generateUpdateSql("test_table", columns);

    assertTrue(updateSql.contains("UPDATE test_table"));
    assertTrue(updateSql.contains("SET"));
    assertTrue(updateSql.contains("WHERE"));
    assertTrue(updateSql.contains(":id")); // 主键应该在WHERE子句中
  }

  @Test
  void testCreateUpdateWithoutPrimaryKey() throws SQLException
  {
    List<ColumnInfo> columns = new ArrayList<>();
    ColumnInfo col1 = new ColumnInfo();
    col1.name = "name";
    col1.primaryKey = false;
    columns.add(col1);

    String updateSql = com.ctgu.lightdbviewer.jdbc.JdbcExecutor.generateUpdateSql("test_table", columns);

    assertTrue(updateSql.contains("无法生成UPDATE语句：缺少主键"));
  }

  @Test
  void testCreateDeleteWithPrimaryKey() throws SQLException
  {
    List<ColumnInfo> columns = createTestColumns();
    String deleteSql = com.ctgu.lightdbviewer.jdbc.JdbcExecutor.generateDeleteSql("test_table", columns);

    assertTrue(deleteSql.contains("DELETE FROM test_table"));
    assertTrue(deleteSql.contains("WHERE"));
    assertTrue(deleteSql.contains(":id"));
  }

  @Test
  void testCreateDeleteWithoutPrimaryKey() throws SQLException
  {
    List<ColumnInfo> columns = new ArrayList<>();
    ColumnInfo col1 = new ColumnInfo();
    col1.name = "name";
    col1.primaryKey = false;
    columns.add(col1);

    String deleteSql = com.ctgu.lightdbviewer.jdbc.JdbcExecutor.generateDeleteSql("test_table", columns);

    assertTrue(deleteSql.contains("无法生成DELETE语句：缺少主键"));
  }

  @Test
  void testCreateSelectAllColumns() throws SQLException
  {
    List<ColumnInfo> columns = createTestColumns();
    String sql = SqlBuilder.createSelect("test_table", columns, null);

    assertTrue(sql.contains("id"));
    assertTrue(sql.contains("name"));
    assertTrue(sql.contains("email"));
  }

  private List<ColumnInfo> createTestColumns()
  {
    List<ColumnInfo> columns = new ArrayList<>();
    ColumnInfo idCol = new ColumnInfo();
    idCol.name = "id";
    idCol.jdbcType = java.sql.Types.INTEGER;
    idCol.primaryKey = true;
    idCol.autoIncrement = true;
    columns.add(idCol);
    ColumnInfo nameCol = new ColumnInfo();
    nameCol.name = "name";
    nameCol.jdbcType = java.sql.Types.VARCHAR;
    nameCol.primaryKey = false;
    nameCol.autoIncrement = false;
    columns.add(nameCol);
    ColumnInfo emailCol = new ColumnInfo();
    emailCol.name = "email";
    emailCol.jdbcType = java.sql.Types.VARCHAR;
    emailCol.primaryKey = false;
    emailCol.autoIncrement = false;
    columns.add(emailCol);
    return columns;
  }
}
