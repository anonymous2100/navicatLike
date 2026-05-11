package com.ctgu.lightdbviewer.ui.workspace;


import com.ctgu.lightdbviewer.jdbc.ConnectionManager;
import com.ctgu.lightdbviewer.model.DbConfig;
import com.ctgu.lightdbviewer.ui.status.StatusBarPanel;
import com.ctgu.lightdbviewer.ui.workspace.tab.AbstractTab;
import com.ctgu.lightdbviewer.ui.workspace.tab.DesignTableTab;
import com.ctgu.lightdbviewer.ui.workspace.tab.ObjectListTab;
import com.ctgu.lightdbviewer.ui.workspace.tab.QueryTab;
import com.ctgu.lightdbviewer.ui.workspace.tab.TableDataTab;

import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class TabManager
{
  private final WorkspaceTabs tabs;
  private final StatusBarPanel status;
  private final Map<String, AbstractTab> opened = new HashMap<>();
  private int queryTabCounter = 1;
  private Runnable openConnectionAction;
  private Consumer<DbConfig> reconnectAction;
  private Runnable refreshTreeAction;

  public TabManager(WorkspaceTabs tabs, StatusBarPanel status)
  {
    this.tabs = tabs;
    this.status = status;
  }

  public void setOpenConnectionAction(Runnable action)
  {
    this.openConnectionAction = action;
  }

  public void setReconnectAction(Consumer<DbConfig> action)
  {
    this.reconnectAction = action;
  }

  public void setRefreshTreeAction(Runnable action)
  {
    this.refreshTreeAction = action;
  }

  public void openConnectionDialog()
  {
    if(openConnectionAction != null)
      openConnectionAction.run();
  }

  public void reconnect(DbConfig cfg)
  {
    if(reconnectAction != null)
      reconnectAction.accept(cfg);
  }

  private String key(String type, String name)
  {
    return type + ":" + name;
  }

  /** 返回当前激活标签页对应的表名（含 schema 前缀），若非表格页则返回 null */
  public String getActiveTableName()
  {
    AbstractTab tab = getCurrentTab();
    if(tab instanceof TableDataTab tableTab)
      return tableTab.getTableName();
    return null;
  }

  public void openTable(String table)
  {
    String k = key("TABLE", table);
    AbstractTab existing = opened.get(k);
    if(existing != null && existing.getParent() == null)
    {
      opened.remove(k);
      existing = null;
    }

    if(existing == null)
    {
      TableDataTab tab = new TableDataTab(table, status);
      opened.put(k, tab);
      tabs.addTab(table, tab);
      status.setMessage("已打开表: " + table);
    }
    tabs.setSelectedComponent(opened.get(k));
  }

  public void openQuery()
  {
    // 如果当前标签页是表格页，自动生成 SELECT * FROM <table>
    AbstractTab currentTab = getCurrentTab();
    String initialSql = null;
    if(currentTab instanceof TableDataTab tableTab)
    {
      initialSql = "SELECT * FROM " + tableTab.getTableName() + ";";
    }
    openQuery(initialSql);
  }

  public void openQuery(String initialSql)
  {
    String title = "Query " + queryTabCounter++;
    QueryTab tab = new QueryTab(title, initialSql, status);
    tabs.addTab(title, tab);
    tabs.setSelectedComponent(tab);
    status.setMessage("已打开 SQL 编辑器");
  }

  public void openTableStructure(String table)
  {
    String k = key("DESIGN", table);
    AbstractTab existing = opened.get(k);
    if(existing != null && existing.getParent() == null)
    {
      opened.remove(k);
      existing = null;
    }
    if(existing == null)
    {
      DesignTableTab tab = new DesignTableTab(table, status);
      opened.put(k, tab);
      tabs.addTab("Design: " + shortName(table), tab);
      status.setMessage("Opened design: " + table);
    }
    tabs.setSelectedComponent(opened.get(k));
  }

  /** 取最后一个 '.' 之后的名称（public.users -> users） */
  private static String shortName(String qualified)
  {
    int dot = qualified.lastIndexOf('.');
    return dot >= 0 ? qualified.substring(dot + 1) : qualified;
  }

  public void openObjectList(ObjectListTab.ObjectType objectType)
  {
    if(!ConnectionManager.isConnected())
    {
      status.setMessage("请先连接数据库");
      return;
    }
    String dbName = ConnectionManager.getCurrentDatabase();
    String title = objectType.label + " (" + dbName + ")";
    ObjectListTab tab = new ObjectListTab(objectType, status, this);
    tabs.addTab(title, tab);
    tabs.setSelectedComponent(tab);
  }

  /**
   * 单击连接节点 / DATABASES_FOLDER -> 右侧展示数据库列表。
   * 同一连接只保留一个该标签页（key 复用）。
   */
  public void showDatabaseList()
  {
    if(!ConnectionManager.isConnected())
    {
      status.setMessage("请先连接数据库");
      return;
    }
    String k = "VIEW:DATABASES";
    AbstractTab existing = opened.get(k);
    if(existing != null && existing.getParent() == null)
    {
      opened.remove(k);
      existing = null;
    }
    if(existing == null)
    {
      ObjectListTab tab = new ObjectListTab(ObjectListTab.ObjectType.DATABASES, status, this, null, null);
      opened.put(k, tab);
      tabs.addTab("数据库列表", tab);
    }
    tabs.setSelectedComponent(opened.get(k));
  }

  /**
   * 单击数据库节点 / Tables 文件夹 -> 右侧展示表列表。
   * 相同 dbName+schemaName 的标签页复用。
   *
   * @param dbName     数据库名（null = 当前数据库）
   * @param schemaName Schema 名（null = 无 schema 过滤，适合 MySQL）
   */
  public void showTableList(String dbName, String schemaName)
  {
    if(!ConnectionManager.isConnected())
    {
      status.setMessage("请先连接数据库");
      return;
    }
    String k = "VIEW:TABLES:" + dbName + ":" + schemaName;
    AbstractTab existing = opened.get(k);
    if(existing != null && existing.getParent() == null)
    {
      opened.remove(k);
      existing = null;
    }
    if(existing == null)
    {
      String title = schemaName != null
          ? "表列表(" + dbName + "." + schemaName + ")"
          : "表列表(" + (dbName != null ? dbName : "当前库") + ")";
      ObjectListTab tab = new ObjectListTab(ObjectListTab.ObjectType.TABLES, status, this, dbName, schemaName);
      opened.put(k, tab);
      tabs.addTab(title, tab);
    }
    tabs.setSelectedComponent(opened.get(k));
  }

  public void truncateTable(String table)
  {
    try
    {
      try(Connection conn = ConnectionManager.get())
      {
        try(Statement stmt = conn.createStatement())
        {
          stmt.execute("TRUNCATE TABLE " + ConnectionManager.quoteIdentifier(conn, table));
        }
      }
      status.setMessage("表已清空: " + table);
      refresh();
    }
    catch(Exception e)
    {
      status.setMessage("清空失败: " + e.getMessage());
    }
  }

  public void dropObject(String typeLabel, String sqlPrefix, String qualifiedName)
  {
    try
    {
      // 删除数据库前需要先切换到其他库，否则无法删除当前库
      if("DROP DATABASE".equals(sqlPrefix))
      {
        try(Connection conn = ConnectionManager.get())
        {
          String product = conn.getMetaData().getDatabaseProductName().toLowerCase();
          String safeDb = "postgres";
          if(product.contains("mysql"))
          {
            safeDb = "mysql";
          }
          ConnectionManager.switchDatabase(safeDb);
        }
        catch(Exception e)
        {
          // 切换失败时继续尝试删除（MySQL 可能仍能成功）
        }
      }
      try(Connection conn = ConnectionManager.get())
      {
        try(Statement stmt = conn.createStatement())
        {
          stmt.execute(sqlPrefix + " " + ConnectionManager.quoteIdentifier(conn, qualifiedName));
        }
      }
      status.setMessage(typeLabel + " " + qualifiedName + " 已删除");
      refresh();
    }
    catch(Exception e)
    {
      status.setMessage("删除失败: " + e.getMessage());
    }
  }

  public void refresh()
  {
    if(refreshTreeAction != null)
    {
      refreshTreeAction.run();
    }
    status.setMessage("已刷新");
  }

  public void closeCurrentTab()
  {
    int index = tabs.getSelectedIndex();
    if(index >= 0)
    {
      tabs.removeTabAt(index);
      cleanupClosedTableTabs();
    }
  }

  public void closeOtherTabs()
  {
    int selected = tabs.getSelectedIndex();
    if(selected < 0)
      return;
    java.awt.Component keepComp = tabs.getComponentAt(selected);
    for(int i = tabs.getTabCount() - 1; i >= 0; i--)
    {
      if(tabs.getComponentAt(i) != keepComp)
        tabs.removeTabAt(i);
    }
    cleanupClosedTableTabs();
  }

  public void closeAllTabs()
  {
    for(int i = tabs.getTabCount() - 1; i >= 0; i--)
    {
      tabs.removeTabAt(i);
    }
    cleanupClosedTableTabs();
  }

  public void executeCurrentQuery()
  {
    AbstractTab tab = getCurrentTab();
    if(tab instanceof QueryTab queryTab)
    {
      queryTab.runCurrentSql();
    }
  }

  public void cancelCurrentQuery()
  {
    AbstractTab tab = getCurrentTab();
    if(tab instanceof QueryTab queryTab)
    {
      queryTab.cancelCurrentSql();
    }
  }

  private AbstractTab getCurrentTab()
  {
    int index = tabs.getSelectedIndex();
    if(index < 0)
    {
      return null;
    }
    java.awt.Component component = tabs.getComponentAt(index);
    if(component instanceof AbstractTab tab)
    {
      return tab;
    }
    return null;
  }

  private void cleanupClosedTableTabs()
  {
    Iterator<Map.Entry<String, AbstractTab>> it = opened.entrySet().iterator();
    while(it.hasNext())
    {
      Map.Entry<String, AbstractTab> entry = it.next();
      if(entry.getValue().getParent() == null)
      {
        it.remove();
      }
    }
  }
}

