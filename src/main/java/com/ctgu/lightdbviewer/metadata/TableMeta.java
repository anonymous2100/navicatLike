package com.ctgu.lightdbviewer.metadata;

import java.util.*;

/**
 * TableMeta
 * <p>
 * 描述「一张表」的完整元数据
 * 这是整个客户端中
 * - TableDataTab
 * - TableStructureTab
 * - JdbcRowWriter
 * - TypedCellEditor
 * <p>
 * 的统一数据来源
 * <p>
 * 一张表 = 一TableMeta 实例
 * UI / Service / JDBC 全部围绕它转
 */
/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class TableMeta
{
  /**
   * 表名
   */
  private final String tableName;
  /**
   * schema（PostgreSQL 用；MySQL 可为空）
   */
  private final String schema;
  /**
   * 所有列（保持数据库原始顺序
   */
  private final List<ColumnInfo> columns = new ArrayList<>();
  /**
   * 主键列名（保持顺序）
   */
  private final List<String> primaryKeys = new ArrayList<>();
  /**
   * 外键映射：列-> 外键信息
   */
  private final Map<String, ForeignKeyMeta> foreignKeys = new LinkedHashMap<>();

  public TableMeta(String tableName, String schema)
  {
    this.tableName = tableName;
    this.schema = schema;
  }

  public void addColumn(ColumnInfo column)
  {
    columns.add(column);
    if(column.pk)
    {
      primaryKeys.add(column.name);
    }
    if(column.foreignKey)
    {
      foreignKeys.put(column.name, new ForeignKeyMeta(column.name, column.fkTable, column.fkColumn));
    }
  }

  public List<ColumnInfo> getColumns()
  {
    return Collections.unmodifiableList(columns);
  }

  public ColumnInfo getColumn(String name)
  {
    for(ColumnInfo c : columns)
    {
      if(c.name.equals(name))
      {
        return c;
      }
    }
    return null;
  }

  public List<String> getPrimaryKeys()
  {
    return Collections.unmodifiableList(primaryKeys);
  }

  public boolean hasPrimaryKey()
  {
    return !primaryKeys.isEmpty();
  }

  public boolean isForeignKey(String columnName)
  {
    return foreignKeys.containsKey(columnName);
  }

  public ForeignKeyMeta getForeignKey(String columnName)
  {
    return foreignKeys.get(columnName);
  }

  public Collection<ForeignKeyMeta> getForeignKeys()
  {
    return foreignKeys.values();
  }

  /**
   * 是否存在自增
   */
  public boolean hasAutoIncrementColumn()
  {
    return columns.stream().anyMatch(c -> c.autoIncrement);
  }

  /**
   * 是否存在 ENUM 
   */
  public boolean hasEnumColumn()
  {
    return columns.stream().anyMatch(c -> c.enumType);
  }

  /**
   * 是否存在 JSON 列（typeName 判断
   */
  public boolean hasJsonColumn()
  {
    return columns.stream().anyMatch(c -> c.typeName != null && c.typeName.toLowerCase().contains("json"));
  }

  public String getTableName()
  {
    return tableName;
  }

  public String getSchema()
  {
    return schema;
  }

  @Override
  public String toString()
  {
    return "TableMeta{" + "table='" + tableName + '\'' + ", columns=" + columns.size() + ", primaryKeys=" + primaryKeys + '}';
  }

  public static class ForeignKeyMeta
  {
    /**
     * 本表列名
     */
    public final String column;
    /**
     * 引用
     */
    public final String refTable;
    /**
     * 引用
     */
    public final String refColumn;

    public ForeignKeyMeta(String column, String refTable, String refColumn)
    {
      this.column = column;
      this.refTable = refTable;
      this.refColumn = refColumn;
    }

    @Override
    public String toString()
    {
      return column + " -> " + refTable + "." + refColumn;
    }
  }
}


