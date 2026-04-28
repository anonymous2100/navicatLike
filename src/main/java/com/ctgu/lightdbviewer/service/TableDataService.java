package com.ctgu.lightdbviewer.service;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 * @date 2026-04-23 18:32
 */

import com.ctgu.lightdbviewer.jdbc.ConnectionManager;
import com.ctgu.lightdbviewer.ui.table.EditableResultTableModel;

import java.sql.*;

/**
 * TableDataService
 * <p>
 * 职责：
 * - 加载表数据（供 TableDataTab 使用）
 * - 不负责写入（写入统一走 JdbcRowWriter）
 * <p>
 * 说明：
 * Phase 2 默认 LIMIT 100
 * 每次加载都是干净状态（CLEAN）
 */
public class TableDataService
{
  private TableDataService()
  {
  }

  /**
   * 按页加载表数据
   *
   * @param tableName 表名（可含 schema 前缀，如 public.users）
   * @param page      页码（0 起始）
   * @param pageSize  每页行数
   * @return 含本页数据的模型
   */
  public static EditableResultTableModel loadPage(String tableName, int page, int pageSize) throws SQLException
  {
    try (Connection conn = ConnectionManager.get())
    {
      int offset = page * pageSize;
      String sql = "SELECT * FROM " + ConnectionManager.quoteIdentifier(conn, tableName) + " LIMIT " + pageSize + " OFFSET " + offset;
      try (Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY); ResultSet rs = stmt.executeQuery(
          sql))
      {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        EditableResultTableModel model = new EditableResultTableModel();
        for(int i = 1; i <= colCount; i++)
        {
          model.addColumn(meta.getColumnLabel(i));
        }
        while(rs.next())
        {
          Object[] row = new Object[colCount];
          for(int i = 0; i < colCount; i++)
          {
            row[i] = safeGetValue(rs, meta, i + 1);
          }
          model.addRow(row);
        }
        model.clearChanges();
        return model;
      }
    }
  }

  /**
   * 查询表总行数（用于计算最后一页）
   */
  public static int countRows(String tableName) throws SQLException
  {
    try (Connection conn = ConnectionManager.get())
    {
      try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(
          "SELECT COUNT(*) FROM " + ConnectionManager.quoteIdentifier(conn, tableName)))
      {
        return rs.next() ? rs.getInt(1) : 0;
      }
    }
  }

  /**
   * 加载表数据
   */
  public static EditableResultTableModel load(String tableName) throws SQLException
  {
    return loadPage(tableName, 0, 100);
  }

  private static Object safeGetValue(ResultSet rs, ResultSetMetaData meta, int col) throws SQLException
  {
    int sqlType = meta.getColumnType(col);
    return switch(sqlType)
    {
      case Types.VARCHAR, Types.CHAR, Types.LONGVARCHAR, Types.NVARCHAR, Types.NCHAR, Types.LONGNVARCHAR, Types.CLOB, Types.NCLOB,
           Types.SQLXML, Types.OTHER   // PostgreSQL text / json / jsonb
          -> rs.getString(col);
      case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> "<binary>";
      default -> rs.getObject(col);
    };
  }
}
