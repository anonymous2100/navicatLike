package com.ctgu.lightdbviewer.jdbc;

import com.ctgu.lightdbviewer.ui.table.ChangeType;
import com.ctgu.lightdbviewer.ui.table.RowChange;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 * @date 2026-04-23 18:33
 */
public class JdbcRowWriter
{
  private final Connection connection;
  private final String table;
  private final List<String> primaryKeys;
  private final String quote;

  public JdbcRowWriter(Connection connection, String table, List<String> primaryKeys)
  {
    this.connection = connection;
    this.table = table;
    this.primaryKeys = primaryKeys;
    this.quote = resolveQuote(connection);
  }

  /**
   * 获取数据库标识符引用符
   */
  private static String resolveQuote(Connection conn)
  {
    try
    {
      String q = conn.getMetaData().getIdentifierQuoteString();
      if(q == null || q.isBlank() || " ".equals(q))
      {
        return "\"";
      }
      return q;
    }
    catch(SQLException e)
    {
      return "\"";
    }
  }

  private String q(String name)
  {
    if(name == null || name.isEmpty())
      return name;
    if(name.contains("."))
    {
      String[] parts = name.split("\\.", 2);
      return q(parts[0]) + "." + q(parts[1]);
    }
    return quote + name.replace(quote, quote + quote) + quote;
  }

  /**
   * 核心入口：提交所有变更
   */
  public void applyChanges(List<RowChange> changes) throws SQLException
  {
    try
    {
      // Navicat 行为：一个 Save = 一个事务
      connection.setAutoCommit(false);
      applyInserts(changes);
      applyUpdates(changes);
      applyDeletes(changes);

      connection.commit();
    }
    catch(SQLException ex)
    {
      connection.rollback();
      throw ex;
    }
  }

  private void applyInserts(List<RowChange> changes) throws SQLException
  {
    for(RowChange c : changes)
    {
      if(c.type != ChangeType.INSERT)
      {
        continue;
      }
      List<String> columns = new ArrayList<>(c.newValues.keySet());
      if(columns.isEmpty())
      {
        continue;
      }
      String quotedCols = columns.stream().map(this::q).collect(java.util.stream.Collectors.joining(","));
      String sql =
          "INSERT INTO " + q(table) + " (" + quotedCols + ") VALUES (" + String.join(",", Collections.nCopies(columns.size(), "?")) + ")";
      try (PreparedStatement ps = connection.prepareStatement(sql))
      {
        for(int i = 0; i < columns.size(); i++)
        {
          ps.setObject(i + 1, c.newValues.get(columns.get(i)));
        }
        ps.executeUpdate();
      }
    }
  }

  private void applyUpdates(List<RowChange> changes) throws SQLException
  {
    for(RowChange c : changes)
    {
      if(c.type != ChangeType.UPDATE)
      {
        continue;
      }
      List<String> setCols = new ArrayList<>(c.newValues.keySet());
      if(setCols.isEmpty())
      {
        continue;
      }
      String setPart = setCols.stream().map(k -> q(k) + "=?").collect(java.util.stream.Collectors.joining(","));
      String wherePart = primaryKeys.stream().map(k -> q(k) + "=?").collect(java.util.stream.Collectors.joining(" AND "));
      String sql = "UPDATE " + q(table) + " SET " + setPart + " WHERE " + wherePart;
      try (PreparedStatement ps = connection.prepareStatement(sql))
      {
        int idx = 1;
        // SET
        for(String cName : setCols)
        {
          ps.setObject(idx++, c.newValues.get(cName));
        }
        // WHERE (use OLD values)
        for(String pk : primaryKeys)
        {
          ps.setObject(idx++, c.oldValues.get(pk));
        }
        ps.executeUpdate();
      }
    }
  }

  private void applyDeletes(List<RowChange> changes) throws SQLException
  {
    for(RowChange c : changes)
    {
      if(c.type != ChangeType.DELETE)
      {
        continue;
      }
      if(primaryKeys.isEmpty())
      {
        throw new SQLException("DELETE without primary key is not allowed");
      }
      String wherePart = primaryKeys.stream().map(k -> q(k) + "=?").collect(java.util.stream.Collectors.joining(" AND "));
      String sql = "DELETE FROM " + q(table) + " WHERE " + wherePart;
      try (PreparedStatement ps = connection.prepareStatement(sql))
      {
        int idx = 1;
        for(String pk : primaryKeys)
        {
          ps.setObject(idx++, c.oldValues.get(pk));
        }
        ps.executeUpdate();
      }
    }
  }
}
