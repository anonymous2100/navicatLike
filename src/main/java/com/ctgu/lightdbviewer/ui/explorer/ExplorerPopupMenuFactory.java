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
      boolean ok = ConfirmDialog.confirmDangerousTwice(null, "清空表数据", """
          此操作将删除表中所有行：
          "%s"
          
          此操作不可撤销。
          """.formatted(tableName), tableName);

      if(ok)
      {
        tabManager.truncateTable(tableName);
      }
    });
    menu.add(truncate);
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
    JMenuItem refresh = new JMenuItem("刷新");
    refresh.addActionListener(e -> tabManager.refresh());
    menu.add(refresh);
    return menu;
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