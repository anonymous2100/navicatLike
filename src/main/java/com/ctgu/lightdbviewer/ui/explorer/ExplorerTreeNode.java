package com.ctgu.lightdbviewer.ui.explorer;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class ExplorerTreeNode extends DefaultMutableTreeNode
{
  private final ExplorerNodeType type;
  private final String label;
  /**
   * 所属数据库名，用于懒加载时切换连接
   */
  private String dbName;
  /**
   * 所Schema 名（PostgreSQL
   */
  private String schemaName;
  /**
   * 是否正在后台加载
   */
  private volatile boolean loading = false;
  /**
   * 是否已完成加载（避免重复加载
   */
  private volatile boolean loaded = false;

  public ExplorerTreeNode(ExplorerNodeType type, String label)
  {
    super(label);
    this.type = type;
    this.label = label;
  }

  public ExplorerNodeType getType()
  {
    return type;
  }

  public String getLabel()
  {
    return label;
  }

  public String getDbName()
  {
    return dbName;
  }

  public void setDbName(String dbName)
  {
    this.dbName = dbName;
  }

  public String getSchemaName()
  {
    return schemaName;
  }

  public void setSchemaName(String schemaName)
  {
    this.schemaName = schemaName;
  }

  public boolean isLoading()
  {
    return loading;
  }

  public void setLoading(boolean loading)
  {
    this.loading = loading;
  }

  public boolean isLoaded()
  {
    return loaded;
  }

  public void setLoaded(boolean loaded)
  {
    this.loaded = loaded;
  }

  /**
   * 判断是否需要懒加载（第一个子节点LOADING 占位符）
   */
  public boolean needsLoading()
  {
    if(loading || loaded)
    {
      return false;
    }
    if(getChildCount() != 1)
    {
      return false;
    }
    Object first = getChildAt(0);
    return first instanceof ExplorerTreeNode n && n.getType() == ExplorerNodeType.LOADING;
  }

  @Override
  public String toString()
  {
    // 优先返回 setUserObject() 设置的值（如连接标签），初始值即 label
    Object obj = getUserObject();
    return (obj != null) ? obj.toString() : label;
  }
}


