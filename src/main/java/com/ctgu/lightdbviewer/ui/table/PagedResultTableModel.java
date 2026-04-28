package com.ctgu.lightdbviewer.ui.table;

import javax.swing.table.AbstractTableModel;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 *
 * 分页查询结果表模型
 * <p>
 * 特点：
 * - 支持大数据集的分页加载
 * - 内存高效，只保存当前页数据
 * - 支持动态列信息
 */
public class PagedResultTableModel extends AbstractTableModel
{
  private final List<String> columnNames = new ArrayList<>();
  private final List<List<Object>> currentPageData = new ArrayList<>();
  private final int pageSize;
  private int currentPage = 0;
  private int totalRows = -1; // -1 表示未知
  private final ResultSetLazyLoader dataLoader;

  public PagedResultTableModel(ResultSetLazyLoader dataLoader, int pageSize)
  {
    this.dataLoader = dataLoader;
    this.pageSize = Math.max(1, pageSize);
  }

  /**
   * 初始化列信息
   *
   * @param metaData 结果集元数据
   * @throws SQLException 获取元数据失败
   */
  public void initColumns(ResultSetMetaData metaData) throws SQLException
  {
    columnNames.clear();
    int colCount = metaData.getColumnCount();
    for(int i = 1; i <= colCount; i++)
    {
      columnNames.add(metaData.getColumnLabel(i));
    }
    fireTableStructureChanged();
  }

  /**
   * 加载第一页数据
   */
  public void loadFirstPage() throws SQLException
  {
    loadPage(0);
  }

  /**
   * 加载指定页的数据
   *
   * @param pageIndex 页索引（从0开始）
   * @return 是否成功加载
   */
  public boolean loadPage(int pageIndex) throws SQLException
  {
    if(pageIndex < 0)
    {
      return false;
    }

    PageData pageData = dataLoader.loadPage(pageIndex, pageSize);
    if(pageData == null || pageData.data().isEmpty())
    {
      return false;
    }

    currentPageData.clear();
    currentPageData.addAll(pageData.data());

    if(totalRows == -1 && !pageData.hasMore())
    {
      // 如果没有更多数据，可以计算总行数
      totalRows = pageIndex * pageSize + pageData.data().size();
    }

    currentPage = pageIndex;
    fireTableDataChanged();
    return true;
  }

  /**
   * 加载下一页
   *
   * @return 是否成功加载
   */
  public boolean loadNextPage() throws SQLException
  {
    return loadPage(currentPage + 1);
  }

  /**
   * 加载上一页
   *
   * @return 是否成功加载
   */
  public boolean loadPreviousPage() throws SQLException
  {
    if(currentPage > 0)
    {
      return loadPage(currentPage - 1);
    }
    return false;
  }

  /**
   * 刷新当前页
   */
  public void refreshCurrentPage() throws SQLException
  {
    loadPage(currentPage);
  }

  @Override
  public int getRowCount()
  {
    return currentPageData.size();
  }

  @Override
  public int getColumnCount()
  {
    return columnNames.size();
  }

  @Override
  public String getColumnName(int column)
  {
    if(column >= 0 && column < columnNames.size())
    {
      return columnNames.get(column);
    }
    return super.getColumnName(column);
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex)
  {
    if(rowIndex >= 0 && rowIndex < currentPageData.size() &&
        columnIndex >= 0 && columnIndex < columnNames.size())
    {
      return currentPageData.get(rowIndex).get(columnIndex);
    }
    return null;
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex)
  {
    return false; // 分页模型默认不可编辑
  }

  /**
   * 获取当前页索引
   *
   * @return 当前页索引（从0开始）
   */
  public int getCurrentPage()
  {
    return currentPage;
  }

  /**
   * 获取每页大小
   *
   * @return 每页行数
   */
  public int getPageSize()
  {
    return pageSize;
  }

  /**
   * 获取总行数（如果已知）
   *
   * @return 总行数，-1表示未知
   */
  public int getTotalRows()
  {
    return totalRows;
  }

  /**
   * 设置总行数
   *
   * @param totalRows 总行数
   */
  public void setTotalRows(int totalRows)
  {
    this.totalRows = totalRows;
  }

  /**
   * 获取总页数（如果总行数已知）
   *
   * @return 总页数，-1表示未知
   */
  public int getTotalPages()
  {
    if(totalRows < 0)
    {
      return -1;
    }
    return (totalRows + pageSize - 1) / pageSize;
  }

  /**
   * 是否有下一页
   *
   * @return 是否有下一页
   */
  public boolean hasNextPage()
  {
    if(totalRows >= 0)
    {
      return currentPage < getTotalPages() - 1;
    }
    // 如果总行数未知，假设有下一页（除非当前页数据不足pageSize）
    return currentPageData.size() >= pageSize;
  }

  /**
   * 是否有上一页
   *
   * @return 是否有上一页
   */
  public boolean hasPreviousPage()
  {
    return currentPage > 0;
  }

  /**
   * 清除所有数据
   */
  public void clear()
  {
    currentPageData.clear();
    columnNames.clear();
    currentPage = 0;
    totalRows = -1;
    fireTableStructureChanged();
  }

  /**
   * 页数据记录
   */
  public record PageData(List<List<Object>> data, boolean hasMore)
  {
  }

  /**
   * 结果集懒加载器接口
   */
  @FunctionalInterface
  public interface ResultSetLazyLoader
  {
    /**
     * 加载指定页的数据
     *
     * @param pageIndex 页索引（从0开始）
     * @param pageSize  每页大小
     * @return 页数据
     * @throws SQLException 加载失败
     */
    PageData loadPage(int pageIndex, int pageSize) throws SQLException;
  }
}
