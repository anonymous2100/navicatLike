package com.ctgu.lightdbviewer.jdbc;

import com.ctgu.lightdbviewer.metadata.ColumnInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JDBC执行器，提供安全的数据库操作
 * <p>
 * 安全改进 * - 使用参数化查询防止SQL注入
 * - 添加日志记录
 * - 改进异常处理
 */
/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class JdbcExecutor
{
  private static final Logger logger = LoggerFactory.getLogger(JdbcExecutor.class);

  /**
   * 执行查询（仅用于不涉及用户输入的查询   *
   * @param conn 数据库连   * @param sql  SQL语句
   * @return 查询结果模型
   * @throws SQLException SQL异常
   */
  public static TableModel query(Connection conn, String sql) throws SQLException
  {
    logger.debug("执行查询: {}", sql);
    try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql))
    {
      ResultSetMetaData meta = rs.getMetaData();
      DefaultTableModel model = new DefaultTableModel();
      int cols = meta.getColumnCount();
      for(int i = 1; i <= cols; i++)
      {
        model.addColumn(meta.getColumnLabel(i));
      }
      while(rs.next())
      {
        Object[] row = new Object[cols];
        for(int i = 0; i < cols; i++)
        {
          row[i] = safeGetValue(rs, meta, i + 1);
        }
        model.addRow(row);
      }
      logger.debug("查询返回 {} 行", model.getRowCount());
      return model;
    }
    catch(SQLException e)
    {
      logger.error("查询执行失败: {}", sql, e);
      throw e;
    }
  }

  /**
   * 获取表的列信息（使用DatabaseMetaData，安全）
   *
   * @param conn   数据库连   * @param schema 模式   * @param table  表名
   * @return 列信息列   * @throws SQLException SQL异常
   */
  public static List<ColumnInfo> getTableColumns(Connection conn, String schema, String table) throws SQLException
  {
    logger.debug("获取表列信息: schema={}, table={}", schema, table);
    DatabaseMetaData meta = conn.getMetaData();
    Map<String, ColumnInfo> map = new LinkedHashMap<>();
    try (ResultSet cols = meta.getColumns(null, schema, table, "%"))
    {
      while(cols.next())
      {
        ColumnInfo c = new ColumnInfo();
        c.name = cols.getString("COLUMN_NAME");
        c.jdbcType = cols.getInt("DATA_TYPE");
        c.typeName = cols.getString("TYPE_NAME");
        c.nullable = DatabaseMetaData.columnNullable == cols.getInt("NULLABLE");
        c.autoIncrement = "YES".equalsIgnoreCase(cols.getString("IS_AUTOINCREMENT"));
        map.put(c.name, c);
      }
    }
    try (ResultSet pks = meta.getPrimaryKeys(null, schema, table))
    {
      while(pks.next())
      {
        ColumnInfo c = map.get(pks.getString("COLUMN_NAME"));
        if(c != null)
        {
          c.pk = true;
          c.primaryKey = true;
        }
      }
    }
    logger.debug("获取了 {} 个列", map.size());
    return new ArrayList<>(map.values());
  }

  /**
   * 生成INSERT SQL模板（用于显示，不直接执行）
   */
  public static String generateInsertSql(String table, List<ColumnInfo> cols)
  {
    List<ColumnInfo> insertCols = cols.stream().filter(c -> !c.autoIncrement).toList();
    String colPart = insertCols.stream().map(c -> c.name).collect(Collectors.joining(",\n    "));
    String valPart = insertCols.stream().map(c -> ":" + c.name).collect(Collectors.joining(",\n    "));
    return """
        INSERT INTO %s (
            %s
        ) VALUES (
            %s
        );
        """.formatted(table, colPart, valPart);
  }

  /**
   * 生成UPDATE SQL模板（用于显示，不直接执行）
   */
  public static String generateUpdateSql(String table, List<ColumnInfo> cols)
  {
    List<ColumnInfo> pkCols = cols.stream().filter(ColumnInfo::isPrimaryKey).toList();
    List<ColumnInfo> nonPkCols = cols.stream().filter(c -> !c.primaryKey).toList();
    if(pkCols.isEmpty())
    {
      return "-- 无法生成UPDATE语句：缺少主键";
    }
    String setPart = nonPkCols.stream().map(c -> "    " + c.name + " = :" + c.name).collect(Collectors.joining(",\n"));
    String wherePart = pkCols.stream().map(c -> c.name + " = :" + c.name).collect(Collectors.joining(" AND "));
    return """
        UPDATE %s
        SET
        %s
        WHERE
            %s;
        """.formatted(table, setPart, wherePart);
  }

  /**
   * 生成DELETE SQL模板（用于显示，不直接执行）
   */
  public static String generateDeleteSql(String table, List<ColumnInfo> cols)
  {
    List<ColumnInfo> pkCols = cols.stream().filter(ColumnInfo::isPrimaryKey).toList();
    if(pkCols.isEmpty())
    {
      return "-- 无法生成DELETE语句：缺少主键";
    }
    String wherePart = pkCols.stream().map(c -> c.name + " = :" + c.name).collect(Collectors.joining(" AND "));
    return """
        DELETE FROM %s
        WHERE
            %s;
        """.formatted(table, wherePart);
  }

  /**
   * 安全读取列值：对文本类列强制用 getString() 确保中文正常显示   * 对二进制列返"<binary>"；其余用 getObject()   */
  private static Object safeGetValue(ResultSet rs, ResultSetMetaData meta, int col) throws SQLException
  {
    int sqlType = meta.getColumnType(col);
    return switch(sqlType)
    {
      case Types.VARCHAR, Types.CHAR, Types.LONGVARCHAR, Types.NVARCHAR, Types.NCHAR, Types.LONGNVARCHAR, Types.CLOB, Types.NCLOB,
           Types.SQLXML, Types.OTHER     // PostgreSQL json / jsonb / text
          -> rs.getString(col);
      case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB ->
      {
        byte[] bytes = rs.getBytes(col);
        if(bytes == null)
        {
          yield null;
        }
        else if(bytes.length > 100)
        {
          yield "<binary (" + bytes.length + " bytes)>";
        }
        else
        {
          yield "<binary>";
        }
      }
      default -> rs.getObject(col);
    };
  }
}


