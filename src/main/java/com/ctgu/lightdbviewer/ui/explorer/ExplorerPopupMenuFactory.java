package com.ctgu.lightdbviewer.ui.explorer;

import com.ctgu.lightdbviewer.ui.common.ConfirmDialog;
import com.ctgu.lightdbviewer.ui.workspace.TabManager;

import javax.swing.*;

/**
 * @author lihuahui
 * @version 1.0
 * @date 2026-04-23 18:29
 * @description: ExplorerPopupMenuFactory
 * <p>
 * 统一管理 Object Explorer（左侧树）的右键菜单。
 * <p>
 * 设计原则（对齐 Navicat）：
 * 右键 = 打开“操作入口”，不是立即执行危险动作
 * 危险操作必须确认
 * 菜单结构集中管理，避免散落
 * <p>
 */
public final class ExplorerPopupMenuFactory
{

  private ExplorerPopupMenuFactory()
  {
    // utility class
  }

  /**
   * 表节点右键菜单
   */
  public static JPopupMenu createTableMenu(String tableName, TabManager tabManager)
  {
    JPopupMenu menu = new JPopupMenu();
    JMenuItem open = new JMenuItem("打开表数据");
    open.addActionListener(e -> tabManager.openTable(tableName));
    menu.add(open);
    JMenuItem selectTop = new JMenuItem("查询前 100 行");
    selectTop.addActionListener(e -> tabManager.openQuery("SELECT * FROM " + tableName + " LIMIT 100;"));
    menu.add(selectTop);
    JMenuItem design = new JMenuItem("设计表结构");
    design.addActionListener(e -> tabManager.openTableStructure(tableName));
    menu.add(design);
    menu.addSeparator();
    JMenuItem truncate = new JMenuItem("清空表数据");
    truncate.addActionListener(e -> {
      if(ConfirmDialog.confirmDangerousTwice(null, "清空表数据", "确定要清空表 \"" + tableName + "\" 中的所有数据吗？\n\n此操作不可撤销。", "TRUNCATE"))
      {
        tabManager.truncateTable(tableName);
      }
    });
    menu.add(truncate);
    JMenuItem dropTable = new JMenuItem("删除表");
    dropTable.addActionListener(e -> {
      if(ConfirmDialog.confirmDangerousTwice(null, "删除表", "确定要删除表 \"" + tableName + "\" 吗？\n\n删除后数据无法恢复。", "DROP"))
      {
        tabManager.dropObject("表", "DROP TABLE", tableName);
      }
    });
    menu.add(dropTable);
    menu.addSeparator();
    JMenuItem refresh = new JMenuItem("刷新");
    refresh.addActionListener(e -> tabManager.refresh());
    menu.add(refresh);
    return menu;
  }

  /**
   * 视图节点右键菜单
   */
  public static JPopupMenu createViewMenu(String viewName, TabManager tabManager)
  {
    JPopupMenu menu = new JPopupMenu();
    JMenuItem open = new JMenuItem("打开视图数据");
    open.addActionListener(e -> tabManager.openTable(viewName));
    menu.add(open);
    JMenuItem query = new JMenuItem("查询视图");
    query.addActionListener(e -> tabManager.openQuery("SELECT * FROM " + viewName + " LIMIT 100;"));
    menu.add(query);
    menu.addSeparator();
    JMenuItem dropView = new JMenuItem("删除视图");
    dropView.addActionListener(e -> {
      if(ConfirmDialog.confirmDangerousTwice(null, "删除视图", "确定要删除视图 \"" + viewName + "\" 吗？\n\n删除后数据无法恢复。", "DROP"))
      {
        tabManager.dropObject("视图", "DROP VIEW", viewName);
      }
    });
    menu.add(dropView);
    menu.addSeparator();
    JMenuItem refresh = new JMenuItem("刷新");
    refresh.addActionListener(e -> tabManager.refresh());
    menu.add(refresh);
    return menu;
  }

  public static JPopupMenu createRoutineMenu(ExplorerNodeType routineType, String routineName, TabManager tabManager)
  {
    JPopupMenu menu = new JPopupMenu();
    JMenuItem openQuery = new JMenuItem("打开查询模板");
    openQuery.addActionListener(e -> tabManager.openQuery(templateSql(routineType, routineName)));
    menu.add(openQuery);
    menu.addSeparator();
    JMenuItem dropRoutine = new JMenuItem("删除");
    dropRoutine.addActionListener(e -> {
      String typeLabel = routineTypeLabel(routineType);
      if(ConfirmDialog.confirmDangerousTwice(null, "删除" + typeLabel, "确定要删除" + typeLabel + " \"" + routineName + "\" 吗？\n\n删除后数据无法恢复。", "DROP"))
      {
        tabManager.dropObject(typeLabel, routineDropPrefix(routineType), routineName);
      }
    });
    menu.add(dropRoutine);
    menu.addSeparator();
    JMenuItem refresh = new JMenuItem("刷新");
    refresh.addActionListener(e -> tabManager.refresh());
    menu.add(refresh);
    return menu;
  }

  private static String routineTypeLabel(ExplorerNodeType type)
  {
    return switch(type)
    {
      case FUNCTION -> "函数";
      case PROCEDURE -> "存储过程";
      case TRIGGER -> "触发器";
      default -> "对象";
    };
  }

  private static String routineDropPrefix(ExplorerNodeType type)
  {
    return switch(type)
    {
      case FUNCTION -> "DROP FUNCTION";
      case PROCEDURE -> "DROP PROCEDURE";
      case TRIGGER -> "DROP TRIGGER";
      default -> "DROP";
    };
  }

  /**
   * Tables 根节点（非单个表）
   */
  public static JPopupMenu createTablesRootMenu(TabManager tabManager)
  {
    JPopupMenu menu = new JPopupMenu();
    JMenuItem newQuery = new JMenuItem("新建查询");
    newQuery.addActionListener(e -> tabManager.openQuery());
    menu.add(newQuery);
    JMenuItem design = new JMenuItem("设计表结构...");
    design.setEnabled(false);
    menu.add(design);
    menu.addSeparator();
    JMenuItem refresh = new JMenuItem("刷新");
    refresh.addActionListener(e -> tabManager.refresh());
    menu.add(refresh);
    return menu;
  }

  /**
   * 数据库节点右键菜单
   */
  public static JPopupMenu createDatabaseMenu(String dbName, TabManager tabManager)
  {
    JPopupMenu menu = new JPopupMenu();
    JMenuItem newQuery = new JMenuItem("新建查询");
    newQuery.addActionListener(e -> tabManager.openQuery());
    menu.add(newQuery);
    menu.addSeparator();
    JMenuItem dropDb = new JMenuItem("删除数据库");
    dropDb.addActionListener(e -> {
      if(ConfirmDialog.confirmDangerousTwice(null, "删除数据库", "确定要删除数据库 \"" + dbName + "\" 吗？\n\n所有数据将被永久删除！", "DROP"))
      {
        tabManager.dropObject("数据库", "DROP DATABASE", dbName);
      }
    });
    menu.add(dropDb);
    menu.addSeparator();
    JMenuItem refresh = new JMenuItem("刷新");
    refresh.addActionListener(e -> tabManager.refresh());
    menu.add(refresh);
    return menu;
  }

  /**
   * 数据库连接根节点
   */
  public static JPopupMenu createConnectionMenu(TabManager tabManager)
  {
    JPopupMenu menu = new JPopupMenu();
    JMenuItem newQuery = new JMenuItem("新建查询");
    newQuery.addActionListener(e -> tabManager.openQuery());
    menu.add(newQuery);
    menu.addSeparator();
    JMenuItem refresh = new JMenuItem("刷新");
    refresh.addActionListener(e -> tabManager.refresh());
    menu.add(refresh);
    return menu;
  }

  private static String templateSql(ExplorerNodeType routineType, String routineName)
  {
    return switch(routineType)
    {
      case PROCEDURE -> "CALL " + routineName + "();";
      case FUNCTION -> "SELECT " + routineName + "();";
      case TRIGGER -> "-- Trigger: " + routineName + System.lineSeparator();
      default -> "";
    };
  }
}