package com.ctgu.lightdbviewer.jdbc;

import com.ctgu.lightdbviewer.metadata.ColumnInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 安全的SQL构建工具，防止SQL注入
 * <p>
 * 使用参数化查询而不是字符串拼接
 */
/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public final class SqlBuilder
{
  private SqlBuilder()
  {
  }

  /**
   * 创建安全的INSERT语句
   *
   * @param table 表名
   * @param cols  列信息   * @return SQL语句和参数设置器
   */
  public static PreparedStatementBuilder createInsert(Connection conn, String table, List<ColumnInfo> cols) throws SQLException
  {
    List<ColumnInfo> insertCols = cols.stream().filter(c -> !c.autoIncrement).toList();

    if(insertCols.isEmpty())
    {
      throw new SQLException("没有可插入的列");
    }

    String colPart = insertCols.stream().map(ColumnInfo::getName).collect(Collectors.joining(", "));

    String paramPart = insertCols.stream().map(c -> "?").collect(Collectors.joining(", "));

    String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", table, colPart, paramPart);

    PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);

    return new PreparedStatementBuilder(stmt, insertCols);
  }

  /**
   * 创建安全的UPDATE语句
   *
   * @param table 表名
   * @param cols  列信息   * @return SQL语句和参数设置器
   */
  public static PreparedStatementBuilder createUpdate(Connection conn, String table, List<ColumnInfo> cols) throws SQLException
  {
    List<ColumnInfo> pkCols = cols.stream().filter(c -> c.primaryKey).toList();
    List<ColumnInfo> nonPkCols = cols.stream().filter(c -> !c.primaryKey).toList();
    if(pkCols.isEmpty())
    {
      throw new SQLException("更新操作需要主键");
    }
    String setPart = nonPkCols.stream().map(c -> c.name + " = ?").collect(Collectors.joining(", "));
    String wherePart = pkCols.stream().map(c -> c.name + " = ?").collect(Collectors.joining(" AND "));
    String sql = String.format("UPDATE %s SET %s WHERE %s", table, setPart, wherePart);
    PreparedStatement stmt = conn.prepareStatement(sql);
    return new PreparedStatementBuilder(stmt, nonPkCols, pkCols);
  }

  /**
   * 创建安全的DELETE语句
   *
   * @param table  表名
   * @param pkCols 主键列   * @return SQL语句和参数设置器
   */
  public static PreparedStatementBuilder createDelete(Connection conn, String table, List<ColumnInfo> pkCols) throws SQLException
  {
    if(pkCols.isEmpty())
    {
      throw new SQLException("删除操作需要主键");
    }
    String wherePart = pkCols.stream().map(c -> c.name + " = ?").collect(Collectors.joining(" AND "));
    String sql = String.format("DELETE FROM %s WHERE %s", table, wherePart);
    PreparedStatement stmt = conn.prepareStatement(sql);
    return new PreparedStatementBuilder(stmt, pkCols);
  }

  /**
   * 创建安全的SELECT语句
   *
   * @param table 表名
   * @param cols  列信息   * @param where WHERE条件（可选）
   * @return SQL语句
   */
  public static String createSelect(String table, List<ColumnInfo> cols, String where)
  {
    String colPart = cols.stream().map(ColumnInfo::getName).collect(Collectors.joining(", "));
    String sql = String.format("SELECT %s FROM %s", colPart, table);
    if(where != null && !where.trim().isEmpty())
    {
      if(where.contains(";") || where.contains("--") || where.contains("/*"))
        throw new IllegalArgumentException("WHERE子句包含非法字符");
      sql += " WHERE " + where;
    }
    return sql;
  }

  /**
   * PreparedStatement构建器，提供类型安全的参数设置   */
  public static class PreparedStatementBuilder
  {
    private final PreparedStatement statement;
    private final List<ColumnInfo> columns;
    private final List<ColumnInfo> whereColumns;
    private int paramIndex = 1;

    private PreparedStatementBuilder(PreparedStatement statement, List<ColumnInfo> columns)
    {
      this(statement, columns, List.of());
    }

    private PreparedStatementBuilder(PreparedStatement statement, List<ColumnInfo> columns, List<ColumnInfo> whereColumns)
    {
      this.statement = statement;
      this.columns = columns;
      this.whereColumns = whereColumns;
    }

    /**
     * 设置INSERT语句的参数     */
    public PreparedStatementBuilder setInsertParameters(Object[] values) throws SQLException
    {
      if(values.length != columns.size())
      {
            throw new SQLException("参数数量不匹配");
      }
      for(int i = 0; i < values.length; i++)
      {
        setParameter(i + 1, columns.get(i), values[i]);
      }
      return this;
    }

    /**
     * 设置UPDATE语句的参数     */
    public PreparedStatementBuilder setUpdateParameters(Object[] newValues, Object[] oldPkValues) throws SQLException
    {
      if(newValues.length != columns.size())
      {
            throw new SQLException("更新参数数量不匹配");
      }
      if(oldPkValues.length != whereColumns.size())
      {
            throw new SQLException("主键参数数量不匹配");
      }
      // 设置SET子句参数
      for(int i = 0; i < newValues.length; i++)
      {
        setParameter(paramIndex++, columns.get(i), newValues[i]);
      }
      // 设置WHERE子句参数
      for(int i = 0; i < oldPkValues.length; i++)
      {
        setParameter(paramIndex++, whereColumns.get(i), oldPkValues[i]);
      }
      return this;
    }

    /**
     * 设置DELETE语句的参数     */
    public PreparedStatementBuilder setDeleteParameters(Object[] pkValues) throws SQLException
    {
      if(pkValues.length != whereColumns.size())
      {
            throw new SQLException("主键参数数量不匹配");
      }
      for(int i = 0; i < pkValues.length; i++)
      {
        setParameter(paramIndex++, whereColumns.get(i), pkValues[i]);
      }
      return this;
    }

    /**
     * 根据列类型设置参数     */
    private void setParameter(int index, ColumnInfo col, Object value) throws SQLException
    {
      if(value == null)
      {
        statement.setNull(index, col.jdbcType);
      }
      else
      {
        switch(col.jdbcType)
        {
        case java.sql.Types.VARCHAR:
        case java.sql.Types.CHAR:
        case java.sql.Types.LONGVARCHAR:
        case java.sql.Types.NVARCHAR:
        case java.sql.Types.NCHAR:
        case java.sql.Types.LONGNVARCHAR:
          statement.setString(index, value.toString());
          break;
        case java.sql.Types.INTEGER:
          statement.setInt(index, ((Number)value).intValue());
          break;
        case java.sql.Types.BIGINT:
          statement.setLong(index, ((Number)value).longValue());
          break;
        case java.sql.Types.SMALLINT:
          statement.setShort(index, ((Number)value).shortValue());
          break;
        case java.sql.Types.TINYINT:
          statement.setByte(index, ((Number)value).byteValue());
          break;
        case java.sql.Types.FLOAT:
        case java.sql.Types.REAL:
          statement.setFloat(index, ((Number)value).floatValue());
          break;
        case java.sql.Types.DOUBLE:
          statement.setDouble(index, ((Number)value).doubleValue());
          break;
        case java.sql.Types.DECIMAL:
        case java.sql.Types.NUMERIC:
          if(value instanceof Number n)
            statement.setBigDecimal(index, new java.math.BigDecimal(n.toString()));
          else
            statement.setObject(index, value);
          break;
        case java.sql.Types.BOOLEAN:
        case java.sql.Types.BIT:
          if(value instanceof Boolean b)
            statement.setBoolean(index, b);
          else
            statement.setBoolean(index, Boolean.parseBoolean(value.toString()));
          break;
        case java.sql.Types.DATE:
        case java.sql.Types.TIME:
        case java.sql.Types.TIMESTAMP:
          statement.setObject(index, value);
        default:
          statement.setObject(index, value);
        }
      }
    }

    /**
     * 获取构建好的PreparedStatement
     */
    public PreparedStatement build()
    {
      return statement;
    }
  }
}


