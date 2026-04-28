package com.ctgu.lightdbviewer.service;

import com.ctgu.lightdbviewer.jdbc.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 *
 * QueryService
 * <p>
 * 职责：
 * - 执行任意 SQL
 * - 支持 SELECT / INSERT / UPDATE / DELETE
 * - 返回统一的 QueryResult（给 QueryTab 用）
 * - 改进异常处理和日志记录
 * <p>
 * 特点：
 * 只负责"执行 SQL"
 * 不参与表编辑生命周期
 * 不复用 EditableResultTableModel（避免模式污染）
 * 详细的错误日志和用户友好的错误提示
 */
public class QueryService
{
  private static final Logger logger = LoggerFactory.getLogger(QueryService.class);

  /**
   * 执行 SQL
   *
   * @param sql SQL 文本（已去除末尾分号也可）
   */
  public static QueryResult execute(String sql, Consumer<Statement> statementConsumer) throws SQLException
  {
    return executePaged(sql, 0, 200, Integer.MAX_VALUE, statementConsumer);
  }

  public static QueryResult executePaged(String sql, int pageIndex, int pageSize, int maxRows, Consumer<Statement> statementConsumer)
      throws SQLException
  {
    if(sql == null || sql.trim().isEmpty())
    {
      throw new SQLException("SQL语句不能为空");
    }
    logger.debug("执行SQL查询: page={}, size={}, maxRows={}, sql={}", pageIndex, pageSize, maxRows, sql);
    try (Connection conn = ConnectionManager.get())
    {
      String normalized = sql.strip().toLowerCase(Locale.ROOT);
      boolean selectLike = normalized.startsWith("select") || normalized.startsWith("with");
      if(selectLike && pageIndex * pageSize >= maxRows)
      {
        logger.debug("超过最大行数限制，返回空结果");
        return QueryResult.forResultSet(new DefaultTableModel(), 0, pageIndex, pageSize, false, true);
      }
      String executableSql = sql;
      int fetchSize = pageSize;
      int offset = pageIndex * pageSize;
      if(selectLike)
      {
        int allowed = Math.max(0, maxRows - offset);
        fetchSize = Math.min(pageSize, allowed);
        executableSql = sql + " LIMIT " + (fetchSize + 1) + " OFFSET " + offset;
      }
      try (Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY))
      {
        statementConsumer.accept(stmt);
        try
        {
          String product = conn.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
          if(product.contains("mysql"))
          {
            // MySQL streaming: hint to stream resultset rather than buffering
            stmt.setFetchSize(Integer.MIN_VALUE);
          }
          else
          {
            stmt.setFetchSize(fetchSize + 1);
          }
        }
        catch(SQLException ex)
        {
          logger.debug("设置 fetchSize 失败", ex);
        }
        long start = System.currentTimeMillis();
        boolean hasResultSet;
        try
        {
          hasResultSet = stmt.execute(executableSql);
        }
        catch(SQLException e)
        {
          logger.error("SQL执行失败: {}", executableSql, e);
          throw new SQLException("SQL执行失败: " + e.getMessage(), e);
        }
        long cost = System.currentTimeMillis() - start;
        logger.debug("SQL执行完成: 耗时{}ms, 有结果集={}", cost, hasResultSet);
        if(hasResultSet)
        {
          try (ResultSet rs = stmt.getResultSet())
          {
            TableBuildResult built = buildTableModel(rs, fetchSize, selectLike);
            boolean hasNext = selectLike && built.hasMore;
            boolean truncated = selectLike && (offset + built.model.getRowCount() >= maxRows) && hasNext;
            logger.debug("查询结果: 行数={}, 有下一页={}, 已截断={}", built.model.getRowCount(), hasNext, truncated);
            return QueryResult.forResultSet(built.model, cost, pageIndex, pageSize, hasNext, truncated);
          }
          catch(SQLException e)
          {
            logger.error("处理结果集失败", e);
            throw e;
          }
        }
        int updateCount = stmt.getUpdateCount();
        logger.debug("更新操作完成: 影响行数={}", updateCount);
        return QueryResult.forUpdate(updateCount, cost);
      }
      catch(SQLException e)
      {
        logger.error("查询执行异常", e);
        throw e;
      }
    }
    catch(SQLException e)
    {
      logger.error("查询执行异常", e);
      throw e;
    }
  }

  public static void cancel(Statement stmt)
  {
    if(stmt == null)
    {
      return;
    }
    try
    {
      stmt.cancel();
      logger.debug("SQL执行已取消");
    }
    catch(Exception e)
    {
      logger.warn("取消SQL执行失败", e);
    }
  }

  public static class QueryResult
  {
    public final boolean hasResultSet;
    public final DefaultTableModel tableModel;
    public final int updateCount;
    public final long timeMs;
    public final int pageIndex;
    public final int pageSize;
    public final boolean hasNextPage;
    public final boolean truncated;

    private QueryResult(boolean hasResultSet, DefaultTableModel tableModel, int updateCount, long timeMs, int pageIndex, int pageSize,
        boolean hasNextPage, boolean truncated)
    {
      this.hasResultSet = hasResultSet;
      this.tableModel = tableModel;
      this.updateCount = updateCount;
      this.timeMs = timeMs;
      this.pageIndex = pageIndex;
      this.pageSize = pageSize;
      this.hasNextPage = hasNextPage;
      this.truncated = truncated;
    }

    public static QueryResult forResultSet(DefaultTableModel tableModel, long timeMs, int pageIndex, int pageSize, boolean hasNextPage,
        boolean truncated)
    {
      return new QueryResult(true, tableModel, -1, timeMs, pageIndex, pageSize, hasNextPage, truncated);
    }

    public static QueryResult forUpdate(int count, long timeMs)
    {
      return new QueryResult(false, null, count, timeMs, 0, 0, false, false);
    }
  }

  private static TableBuildResult buildTableModel(ResultSet rs, int limit, boolean checkHasMore) throws SQLException
  {
    // 使用流式结果集处理器以支持大结果集的逐行处理，避免一次性将所有数据加载到内存中
    ResultSetMetaData meta = rs.getMetaData();
    int colCount = meta.getColumnCount();
    DefaultTableModel model = new DefaultTableModel()
    {
      @Override
      public boolean isCellEditable(int row, int column)
      {
        return false;
      }
    };
    for(int i = 1; i <= colCount; i++)
    {
      model.addColumn(meta.getColumnLabel(i));
    }
    final boolean[] hasMore = { false };
    final int[] rows = { 0 };
    // 尝试使用 StreamingResultSetProcessor 逐行处理
    try
    {
      com.ctgu.lightdbviewer.jdbc.StreamingResultSetProcessor processor = new com.ctgu.lightdbviewer.jdbc.StreamingResultSetProcessor(rs);
      processor.processRows(row -> {
        if(checkHasMore && rows[0] >= limit)
        {
          hasMore[0] = true;
          // 当达到限制时，通过抛出 SQLException 中断流式处理（外层会捕获并以 hasMore 标记退出）
          throw new SQLException("limit-reached");
        }
        // 将读取到的 Object[] 转换并加入到模型（需在调用线程安全的上下文）
        model.addRow(row);
        rows[0]++;
      });
    }
    catch(SQLException e)
    {
      // 如果是 limit-reached 信号，则为正常终止，否则为真实错误
      if("limit-reached".equals(e.getMessage()))
      {
        // 正常达到限制，hasMore 已由回调设置
      }
      else
      {
        throw e;
      }
    }
    return new TableBuildResult(model, hasMore[0]);
  }

  /**
   * 安全读取列值：对文本类列强制用 getString() 确保中文正常显示；
   * 对二进制列返回 "<binary>"；其余用 getObject()。
   */
  private static Object safeGetValue(ResultSet rs, ResultSetMetaData meta, int col) throws SQLException
  {
    try
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
    catch(SQLException e)
    {
      logger.warn("读取列值失败: column={}", col, e);
      return null;
    }
  }

  private record TableBuildResult(DefaultTableModel model, boolean hasMore)
  {
  }
}
