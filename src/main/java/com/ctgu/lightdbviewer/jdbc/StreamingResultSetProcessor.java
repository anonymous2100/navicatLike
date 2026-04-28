package com.ctgu.lightdbviewer.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lihuahui
 * @version 1.0
 * @description: 流式结果集处理器
 * <p>
 * 特点：
 * - 支持大结果集的流式处理，避免内存溢出
 * - 提供批处理和行级处理
 * - 自动资源管理
 */
public class StreamingResultSetProcessor
{
  private static final Logger logger = LoggerFactory.getLogger(StreamingResultSetProcessor.class);
  private final ResultSet resultSet;
  private final ResultSetMetaData metaData;
  private final int columnCount;

  public StreamingResultSetProcessor(ResultSet resultSet) throws SQLException
  {
    this.resultSet = resultSet;
    this.metaData = resultSet.getMetaData();
    this.columnCount = metaData.getColumnCount();
  }

  /**
   * 处理结果集中的每一行
   *
   * @param rowProcessor 行处理器
   * @throws SQLException 处理失败
   */
  public void processRows(RowProcessor rowProcessor) throws SQLException
  {
    long processedRows = 0;
    try
    {
      while(resultSet.next())
      {
        Object[] row = new Object[columnCount];
        for(int i = 0; i < columnCount; i++)
        {
          row[i] = safeGetValue(i + 1);
        }
        rowProcessor.processRow(row);
        processedRows++;
      }
      logger.debug("流式处理完成，共处理 {} 行", processedRows);
    }
    catch(SQLException e)
    {
      // "limit-reached" 是 QueryService 用于中断流式读取的正常控制流信号，不视为错误
      if("limit-reached".equals(e.getMessage()))
      {
        logger.debug("流式处理已达行数限制，已处理 {} 行", processedRows);
      }
      else
      {
        logger.error("流式处理失败，已处理 {} 行", processedRows, e);
      }
      throw e;
    }
  }

  /**
   * 批量处理结果集
   *
   * @param batchSize      批次大小
   * @param batchProcessor 批处理器
   * @throws SQLException 处理失败
   */
  public void processBatch(int batchSize, BatchProcessor batchProcessor) throws SQLException
  {
    List<Object[]> batch = new ArrayList<>(batchSize);
    long totalProcessed = 0;
    try
    {
      while(resultSet.next())
      {
        Object[] row = new Object[columnCount];
        for(int i = 0; i < columnCount; i++)
        {
          row[i] = safeGetValue(i + 1);
        }
        batch.add(row);
        if(batch.size() >= batchSize)
        {
          batchProcessor.processBatch(batch);
          totalProcessed += batch.size();
          batch.clear();
        }
      }
      // 处理剩余的行
      if(!batch.isEmpty())
      {
        batchProcessor.processBatch(batch);
        totalProcessed += batch.size();
      }
      logger.debug("批量处理完成，共处理 {} 行", totalProcessed);
    }
    catch(SQLException e)
    {
      logger.error("批量处理失败，已处理 {} 行", totalProcessed, e);
      throw e;
    }
  }

  /**
   * 收集所有行到列表（注意：可能导致内存问题）
   *
   * @param maxRows 最大行数限制
   * @return 所有行数据
   * @throws SQLException 收集失败
   */
  public List<Object[]> collectAll(int maxRows) throws SQLException
  {
    List<Object[]> allRows = new ArrayList<>();
    long collectedRows = 0;
    try
    {
      while(resultSet.next() && collectedRows < maxRows)
      {
        Object[] row = new Object[columnCount];
        for(int i = 0; i < columnCount; i++)
        {
          row[i] = safeGetValue(i + 1);
        }
        allRows.add(row);
        collectedRows++;
      }
      if(collectedRows >= maxRows)
      {
        logger.warn("已达到最大行数限制 {}，可能还有更多数据", maxRows);
      }
      else
      {
        logger.debug("收集完成，共 {} 行", collectedRows);
      }
      return allRows;
    }
    catch(SQLException e)
    {
      logger.error("收集数据失败，已收集 {} 行", collectedRows, e);
      throw e;
    }
  }

  /**
   * 获取列信息
   *
   * @return 列名列表
   * @throws SQLException 获取失败
   */
  public List<String> getColumnNames() throws SQLException
  {
    List<String> columnNames = new ArrayList<>();
    for(int i = 1; i <= columnCount; i++)
    {
      columnNames.add(metaData.getColumnLabel(i));
    }
    return columnNames;
  }

  /**
   * 获取列类型
   *
   * @return 列类型列表
   * @throws SQLException 获取失败
   */
  public List<Integer> getColumnTypes() throws SQLException
  {
    List<Integer> columnTypes = new ArrayList<>();
    for(int i = 1; i <= columnCount; i++)
    {
      columnTypes.add(metaData.getColumnType(i));
    }
    return columnTypes;
  }

  /**
   * 安全获取列值
   */
  private Object safeGetValue(int columnIndex) throws SQLException
  {
    try
    {
      int sqlType = metaData.getColumnType(columnIndex);
      return switch(sqlType)
      {
        case java.sql.Types.VARCHAR, java.sql.Types.CHAR, java.sql.Types.LONGVARCHAR, java.sql.Types.NVARCHAR, java.sql.Types.NCHAR,
             java.sql.Types.LONGNVARCHAR, java.sql.Types.CLOB, java.sql.Types.NCLOB, java.sql.Types.SQLXML, java.sql.Types.OTHER ->
            resultSet.getString(columnIndex);
        case java.sql.Types.BINARY, java.sql.Types.VARBINARY, java.sql.Types.LONGVARBINARY, java.sql.Types.BLOB ->
        {
          byte[] bytes = resultSet.getBytes(columnIndex);
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
        default -> resultSet.getObject(columnIndex);
      };
    }
    catch(SQLException e)
    {
      logger.warn("读取列值失败，column={}", columnIndex, e);
      return null;
    }
  }

  /**
   * 行处理器接口
   */
  @FunctionalInterface
  public interface RowProcessor
  {
    /**
     * 处理单行数据
     *
     * @param row 行数据
     * @throws SQLException 处理失败
     */
    void processRow(Object[] row) throws SQLException;
  }

  /**
   * 批处理器接口
   */
  @FunctionalInterface
  public interface BatchProcessor
  {
    /**
     * 处理一批数据
     *
     * @param batch 批数据
     * @throws SQLException 处理失败
     */
    void processBatch(List<Object[]> batch) throws SQLException;
  }
}


