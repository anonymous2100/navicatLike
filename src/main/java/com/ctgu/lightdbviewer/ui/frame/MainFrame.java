package com.ctgu.lightdbviewer.ui.frame;

import com.ctgu.lightdbviewer.config.AppConfig;
import com.ctgu.lightdbviewer.config.ConnectionImportExport;
import com.ctgu.lightdbviewer.config.SavedConnection;
import com.ctgu.lightdbviewer.db.DbType;
import com.ctgu.lightdbviewer.jdbc.ConnectionManager;
import com.ctgu.lightdbviewer.model.DbConfig;
import com.ctgu.lightdbviewer.ui.ConnectionDialog;
import com.ctgu.lightdbviewer.ui.common.MenuBuilder;
import com.ctgu.lightdbviewer.ui.common.ToolbarIconFactory;
import com.ctgu.lightdbviewer.ui.explorer.ObjectExplorerPanel;
import com.ctgu.lightdbviewer.ui.status.StatusBarPanel;
import com.ctgu.lightdbviewer.ui.workspace.TabManager;
import com.ctgu.lightdbviewer.ui.workspace.WorkspaceTabs;
import com.ctgu.lightdbviewer.ui.workspace.tab.ObjectListTab;
import com.ctgu.lightdbviewer.util.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.sql.SQLException;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class MainFrame extends JFrame
{
  private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);
  private final WorkspaceTabs workspace = new WorkspaceTabs();
  private final StatusBarPanel statusBar = new StatusBarPanel();
  private final TabManager tabManager = new TabManager(workspace, statusBar);
  private final ObjectExplorerPanel explorer = new ObjectExplorerPanel(tabManager);
  private final java.util.List<String> favorites = new java.util.ArrayList<>();

  public MainFrame()
  {
    setTitle("LightDB");
    setSize(1200, 800);
    setMinimumSize(new Dimension(960, 640));
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    setLocationRelativeTo(null);
    // 添加窗口关闭监听器
    addWindowListener(new java.awt.event.WindowAdapter()
    {
      @Override
      public void windowClosing(java.awt.event.WindowEvent e)
      {
        shutdown();
      }
    });
    setJMenuBar(createMenuBar());
    add(createToolbar(), BorderLayout.NORTH);
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, explorer, workspace);
    split.setDividerLocation(250);
    split.setResizeWeight(0);
    // 去掉 SplitPane 及两侧面板的多余边框
    split.setBorder(BorderFactory.createEmptyBorder());
    explorer.setBorder(BorderFactory.createEmptyBorder());
    add(split, BorderLayout.CENTER);
    add(statusBar, BorderLayout.SOUTH);
    registerGlobalShortcuts();
    statusBar.setMessage("就绪");
    statusBar.setContext("未连接");
        statusBar.clearElapsed();
  }

  private JMenuBar createMenuBar()
  {
    JMenuBar menuBar = new JMenuBar();
    menuBar.add(createMenuMenu());        // 1. 菜单
    menuBar.add(createEditMenu());        // 2. 编辑
    menuBar.add(createViewMenu());        // 3. 查看
    menuBar.add(createFavoritesMenu());   // 4. 收藏夹
    menuBar.add(createToolsMenu());       // 5. 工具
    menuBar.add(createWindowMenu());      // 6. 窗口
    menuBar.add(createHelpMenu());        // 7. 帮助
    return menuBar;
  }

  private JMenu createMenuMenu()
  {
    JMenu menu = new JMenu("菜单");
    JMenuItem newConn = new JMenuItem("新建连接");
    newConn.addActionListener(e -> openConnectionDialog());
    menu.add(newConn);
    JMenuItem closeConn = new JMenuItem("关闭连接");
    closeConn.addActionListener(e -> {
      ConnectionManager.close();
      statusBar.setContext("未连接");
      statusBar.setMessage("连接已关闭");
      explorer.refreshTree();
    });
    menu.add(closeConn);
    menu.addSeparator();
    JMenuItem importConn = new JMenuItem("导入连接");
    importConn.addActionListener(e -> importConnections());
    menu.add(importConn);
    JMenuItem exportConn = new JMenuItem("导出连接");
    exportConn.addActionListener(e -> exportConnections());
    menu.add(exportConn);
    menu.addSeparator();
    JMenuItem closeWindow = new JMenuItem("关闭窗口");
    closeWindow.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
    closeWindow.addActionListener(e -> dispose());
    menu.add(closeWindow);
    JMenuItem exit = new JMenuItem("退出");
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK)); exit.addActionListener(e -> shutdown());
    menu.add(exit);
    return menu;
  }

  private JMenu createEditMenu()
  {
    return MenuBuilder.createEditMenu();
  }

  private JMenu createViewMenu()
  {
    JMenu view = new JMenu("查看");
    JCheckBoxMenuItem navPane = new JCheckBoxMenuItem("导航窗格", true);
    navPane.addActionListener(e -> {
      explorer.setVisible(navPane.isSelected());
      revalidate();
    });
    view.add(navPane);
    JCheckBoxMenuItem infoPane = new JCheckBoxMenuItem("信息窗格", true);
    infoPane.addActionListener(e -> statusBar.setVisible(infoPane.isSelected()));
    view.add(infoPane);
    view.addSeparator();
    JMenuItem listView = new JMenuItem("列表");
    listView.addActionListener(e -> statusBar.setMessage("切换到列表视图（待实现）"));
    view.add(listView);
    JMenuItem detailView = new JMenuItem("详细信息");
    detailView.addActionListener(e -> statusBar.setMessage("切换到详细信息视图（待实现）"));
    view.add(detailView);
    view.addSeparator();
    JMenuItem sort = new JMenuItem("排序");
    sort.addActionListener(e -> statusBar.setMessage("排序（待实现）"));
    view.add(sort);
    JMenuItem selectColumns = new JMenuItem("选择");
    selectColumns.addActionListener(e -> statusBar.setMessage("选择列（待实现）"));
    view.add(selectColumns);
    return view;
  }

  private JMenu createFavoritesMenu()
  {
    JMenu menu = new JMenu("收藏夹");
        JMenuItem addFav = new JMenuItem("添加到收藏夹"); addFav.addActionListener(e -> {
    String tableName = tabManager.getActiveTableName();
    if(tableName != null && !favorites.contains(tableName))
    {
      favorites.add(tableName);
      statusBar.setMessage("已添加收藏：" + tableName);
    }
    else
    {
      statusBar.setMessage("无可收藏的对象或已存在");
    }
  }); menu.add(addFav);
    menu.addSeparator();
    // 动态构建收藏列表
    menu.addMenuListener(new javax.swing.event.MenuListener()
    {
      @Override
      public void menuSelected(javax.swing.event.MenuEvent e)
      {
        // 移除动态区域（索引 2 之后的项）
        while(menu.getItemCount() > 3)
        {
          menu.remove(3);
        }
        if(favorites.isEmpty())
        {
          JMenuItem empty = new JMenuItem("（无收藏）");
              empty.setEnabled(false); menu.add(empty);
        }
        else
        {
          for(String fav : favorites)
          {
            JMenuItem item = new JMenuItem(fav);
            item.addActionListener(ev -> tabManager.openTable(fav));
            menu.add(item);
          }
        }
      }

      @Override
      public void menuDeselected(javax.swing.event.MenuEvent e)
      {
      }

      @Override
      public void menuCanceled(javax.swing.event.MenuEvent e)
      {
      }
    }); JMenuItem clearFav = new JMenuItem("清空收藏夹");
      clearFav.addActionListener(e -> {
        favorites.clear();
        statusBar.setMessage("收藏夹已清空");
      }); menu.add(clearFav);
    return menu;
  }

  private JMenu createToolsMenu()
  {
    JMenu menu = new JMenu("工具");

    JMenuItem dataTransfer = new JMenuItem("数据传输");
    dataTransfer.addActionListener(e -> statusBar.setMessage("数据传输（待实现）"));
        menu.add(dataTransfer);

    JMenuItem dataGen = new JMenuItem("数据生成");
    dataGen.addActionListener(e -> statusBar.setMessage("数据生成（待实现）"));
        menu.add(dataGen);

    JMenuItem dataDict = new JMenuItem("数据字典");
    dataDict.addActionListener(e -> statusBar.setMessage("数据字典（待实现）"));
        menu.add(dataDict);

    JMenuItem dataSync = new JMenuItem("数据同步");
    dataSync.addActionListener(e -> statusBar.setMessage("数据同步（待实现）"));
        menu.add(dataSync);

    JMenuItem structSync = new JMenuItem("结构同步");
    structSync.addActionListener(e -> statusBar.setMessage("结构同步（待实现）"));
        menu.add(structSync);

    JMenuItem historyLog = new JMenuItem("历史日志");
    historyLog.addActionListener(e -> statusBar.setMessage("历史日志（待实现）"));
        menu.add(historyLog);

    menu.addSeparator();

    JMenuItem options = new JMenuItem("选项");
    options.addActionListener(e -> {
      SettingsDialog dialog = new SettingsDialog(this);
      dialog.setVisible(true);
    });
    menu.add(options);

    return menu;
  }

  private JMenu createWindowMenu()
  {
    JMenu menu = new JMenu("窗口");
    // 动态列出已打开的标签页
    menu.addMenuListener(new javax.swing.event.MenuListener()
    {
      @Override
      public void menuSelected(javax.swing.event.MenuEvent e)
      {
        menu.removeAll();
        // 列出当前打开的窗口（标签页）
        int tabCount = workspace.getTabCount();
        if(tabCount == 0)
        {
          JMenuItem empty = new JMenuItem("（无打开的窗口）");
          empty.setEnabled(false);
          menu.add(empty);
        }
        else
        {
          for(int i = 0; i < tabCount; i++)
          {
            String title = workspace.getTitleAt(i);
            JMenuItem item = new JMenuItem(title);
            int idx = i;
            item.addActionListener(ev -> workspace.setSelectedIndex(idx));
            if(i == workspace.getSelectedIndex())
              item.setFont(item.getFont().deriveFont(Font.BOLD));
            menu.add(item);
          }
        }
        menu.addSeparator();
        JMenuItem nextWindow = new JMenuItem("下一个窗口");
            nextWindow.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.CTRL_DOWN_MASK));
        nextWindow.addActionListener(ev -> {
          int count = workspace.getTabCount();
          if(count > 1)
          {
            int next = (workspace.getSelectedIndex() + 1) % count;
            workspace.setSelectedIndex(next);
          }
        });
        menu.add(nextWindow);
      }

      @Override
      public void menuDeselected(javax.swing.event.MenuEvent e)
      {
      }

      @Override
      public void menuCanceled(javax.swing.event.MenuEvent e)
      {
      }
    });

    return menu;
  }

  private JMenu createHelpMenu()
  {
    JMenu menu = new JMenu("帮助");
    JMenuItem appHelp = new JMenuItem("应用帮助");
    appHelp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
    appHelp.addActionListener(e -> statusBar.setMessage("应用帮助（待实现）"));
        menu.add(appHelp); JMenuItem onlineDoc = new JMenuItem("在线文档");
    onlineDoc.addActionListener(e -> {
      try
      {
        Desktop.getDesktop().browse(new java.net.URI("https://github.com"));
      }
      catch(Exception ex)
      {
        statusBar.setMessage("无法打开浏览器");
      }
    }); menu.add(onlineDoc);
    menu.addSeparator();
    JMenuItem checkUpdate = new JMenuItem("检查更新");
        checkUpdate.addActionListener(e -> JOptionPane.showMessageDialog(this, "当前已是最新版本", "检查更新",
            JOptionPane.INFORMATION_MESSAGE)); menu.add(checkUpdate);
    JMenuItem newFeatures = new JMenuItem("应用新功能");
        newFeatures.addActionListener(e -> statusBar.setMessage("应用新功能（待实现）")); menu.add(newFeatures);
    menu.addSeparator();
    JMenuItem about = new JMenuItem("关于");
    about.addActionListener(
        e -> JOptionPane.showMessageDialog(this, "LightDB Viewer\n版本: 0.0.0\n\n一款轻量级数据库查看工具。\n\n作者：lihuahui", "关于",
            JOptionPane.INFORMATION_MESSAGE));
    menu.add(about);
    return menu;
  }

  private JToolBar createToolbar()
  {
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable(false);
    toolBar.setBorder(
        BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground")),
            BorderFactory.createEmptyBorder(2, 4, 2, 4)));
    // ---- 连接 & 查询 ----
    JButton connect = ToolbarIconFactory.createToolbarButton("连接", ToolbarIconFactory.connectIcon(), "新建数据库连接");
        connect.addActionListener(e -> openConnectionDialog());
    JButton newQuery = ToolbarIconFactory.createToolbarButton("新建查询", ToolbarIconFactory.newQueryIcon(), "新建 SQL 查询编辑器");
        newQuery.addActionListener(e -> tabManager.openQuery()); toolBar.add(connect);
    toolBar.add(newQuery);
    toolBar.addSeparator(new Dimension(12, 0));

    // ---- 对象浏览按钮 ----
    JButton tablesBtn = ToolbarIconFactory.createToolbarButton("表", ToolbarIconFactory.tableIcon(), "查看当前数据库的所有表");
        tablesBtn.addActionListener(e -> tabManager.openObjectList(ObjectListTab.ObjectType.TABLES));

    JButton viewsBtn = ToolbarIconFactory.createToolbarButton("视图", ToolbarIconFactory.viewIcon(), "查看当前数据库的所有视图");
        viewsBtn.addActionListener(e -> tabManager.openObjectList(ObjectListTab.ObjectType.VIEWS));

    JButton functionsBtn = ToolbarIconFactory.createToolbarButton("函数", ToolbarIconFactory.functionIcon(), "查看当前数据库的所有函数");
        functionsBtn.addActionListener(e -> tabManager.openObjectList(ObjectListTab.ObjectType.FUNCTIONS));

    JButton rolesBtn = ToolbarIconFactory.createToolbarButton("角色", ToolbarIconFactory.roleIcon(), "查看数据库角色/用户");
    rolesBtn.addActionListener(e -> tabManager.openObjectList(ObjectListTab.ObjectType.ROLES));

    JButton otherBtn = ToolbarIconFactory.createToolbarButton("其他", ToolbarIconFactory.otherIcon(), "查看存储过程、触发器等其他对象");
        otherBtn.addActionListener(e -> tabManager.openObjectList(ObjectListTab.ObjectType.OTHER));

    JButton queriesBtn = ToolbarIconFactory.createToolbarButton("查询", ToolbarIconFactory.queryIcon(), "查看当前数据库活跃查询");
        queriesBtn.addActionListener(e -> tabManager.openObjectList(ObjectListTab.ObjectType.QUERIES));

    JButton backupBtn = ToolbarIconFactory.createToolbarButton("备份", ToolbarIconFactory.backupIcon(), "数据库备份信息");
        backupBtn.addActionListener(e -> tabManager.openObjectList(ObjectListTab.ObjectType.BACKUP));

    toolBar.add(tablesBtn);
    toolBar.add(viewsBtn);
    toolBar.add(functionsBtn);
    toolBar.add(rolesBtn);
    toolBar.add(otherBtn);
    toolBar.add(queriesBtn);
    toolBar.add(backupBtn);

    return toolBar;
  }

  public void openConnectionDialog()
  {
    ConnectionDialog dialog = new ConnectionDialog(cfg -> connectTo(cfg));
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);
  }

  /**
   * 执行连接逻辑（可从对话框或自动连接调用）
   */
  public void connectTo(DbConfig cfg)
  {
    try
    {
      String jdbcUrl = buildJdbcUrl(cfg.type, cfg.host, cfg.port, cfg.database);
      ConnectionManager.connect(jdbcUrl, cfg.username, cfg.password);
      boolean dbSelected = cfg.database != null && !cfg.database.trim().isEmpty();
      ConnectionManager.setDatabaseSelected(dbSelected);
      // 获取实际连接到的数据库名（PostgreSQL 未指定 database 时默认连到与用户同名的库）
      String actualDb = ConnectionManager.getCurrentDatabase();
      if(actualDb == null || actualDb.isBlank())
      {
        try (Connection conn = ConnectionManager.get())
        {
          actualDb = conn.getCatalog();
        }
        catch(Exception e)
        {
          logger.warn("获取实际数据库名失败", e);
        }
      }
      String ctxDb = (actualDb != null && !actualDb.isBlank()) ? actualDb : "(全部数据库)";
      String label = (cfg.name != null && !cfg.name.isBlank()) ? cfg.name : cfg.host + ":" + cfg.port;
      statusBar.setContext(ConnectionManager.getDatabaseProduct() + " @ " + label + " / " + ctxDb);
      statusBar.setMessage("已连接 " + label);
      explorer.refreshTree();
    }
    catch(SQLException ex)
    {
      statusBar.setMessage("连接失败: " + ex.getMessage());
      JOptionPane.showMessageDialog(this, ex.getMessage(), "连接失败", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * 启动时自动连接第一个已保存的连接，并展开树到第一张表
   */
  public void autoConnectSaved()
  {
    java.util.List<SavedConnection> conns = AppConfig.getInstance().getConnections();
    if(conns.isEmpty())
      return;
    SavedConnection sc = conns.get(0);
    DbConfig cfg = sc.toDbConfig();
    try
    {
      String jdbcUrl = buildJdbcUrl(cfg.type, cfg.host, cfg.port, cfg.database);
      ConnectionManager.connect(jdbcUrl, cfg.username, cfg.password);
      boolean dbSelected = cfg.database != null && !cfg.database.trim().isEmpty();
      ConnectionManager.setDatabaseSelected(dbSelected);
      String label = (cfg.name != null && !cfg.name.isBlank()) ? cfg.name : cfg.host + ":" + cfg.port;
      // 获取实际连接到的数据库名
      String actualDb = ConnectionManager.getCurrentDatabase();
      if(actualDb == null || actualDb.isBlank())
      {
        try (Connection conn = ConnectionManager.get())
        {
          actualDb = conn.getCatalog();
        }
        catch(Exception e)
        {
          logger.warn("自动连接时获取实际数据库名失败", e);
        }
      } String ctxDb = (actualDb != null && !actualDb.isBlank()) ? actualDb : "(全部数据库)";
      statusBar.setContext(ConnectionManager.getDatabaseProduct() + " @ " + label + " / " + ctxDb);
      statusBar.setMessage("自动连接: " + label);
      explorer.refreshTree();
      // 自动展开树并打开第一张表
      SwingUtilities.invokeLater(() -> explorer.autoExpandAndOpen());
    }
    catch(Exception ex)
    {
      statusBar.setMessage("自动连接失败: " + ex.getMessage());
    }
  }

  private String buildJdbcUrl(DbType type, String host, int port, String db)
  {
    boolean hasDb = db != null && !db.trim().isEmpty();
    if(type == DbType.MYSQL)
    {
      return hasDb ? "jdbc:mysql://" + host + ":" + port + "/" + db : "jdbc:mysql://" + host + ":" + port + "/";
    }
    return hasDb ? "jdbc:postgresql://" + host + ":" + port + "/" + db : "jdbc:postgresql://" + host + ":" + port + "/";
  }

  private void registerGlobalShortcuts()
  {
    JRootPane root = getRootPane();
    InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap am = root.getActionMap();

    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "RUN_SQL");
    am.put("RUN_SQL", new AbstractAction()
    {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e)
      {
        tabManager.executeCurrentQuery();
      }
    });

    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "RUN_SQL_F5");
    am.put("RUN_SQL_F5", new AbstractAction()
    {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e)
      {
        tabManager.executeCurrentQuery();
      }
    });

    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK), "CLOSE_TAB");
    am.put("CLOSE_TAB", new AbstractAction()
    {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e)
      {
        tabManager.closeCurrentTab();
      }
    });
  }

  /**
   * 应用关闭处理
   */
  private void shutdown()
  {
    // 保存配置
    AppConfig.getInstance().save();
    // 关闭数据库连接
    ConnectionManager.shutdown();
    // 退出应用
    System.exit(0);
  }

  /**
   * 导入连接配置
   */
  private void importConnections()
  {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("导入连接配置");
    chooser.setFileFilter(new javax.swing.filechooser.FileFilter()
    {
      @Override
      public boolean accept(File f)
      {
        return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml");
      }

      @Override
      public String getDescription()
      {
        return "XML文件 (*.xml)";
      }
    });

    if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
    {
      try
      {
        ConnectionImportExport importData = ConnectionImportExport.importFromFile(chooser.getSelectedFile().getAbsolutePath());
        int result = JOptionPane.showConfirmDialog(this,
            "找到 " + importData.getConnections().size() + " 个连接配置。\n是否要导入这些连接？（同名连接将被覆盖）", "确认导入",
            JOptionPane.YES_NO_OPTION);
        if(result == JOptionPane.YES_OPTION)
        {
          importData.mergeToAppConfig(true);
          statusBar.setMessage("成功导入 " + importData.getConnections().size() + " 个连接配置");
              explorer.refreshTree();
        }
      }
      catch(Exception e)
      {
        ErrorHandler.handleError(this, e, "导入连接配置失败");
      }
    }
  }

  /**
   * 导出连接配置
   */
  private void exportConnections()
  {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("导出连接配置");
    chooser.setSelectedFile(new File("connections.xml"));
    chooser.setFileFilter(new javax.swing.filechooser.FileFilter()
    {
      @Override
      public boolean accept(File f)
      {
        return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml");
      }

      @Override
      public String getDescription()
      {
        return "XML文件 (*.xml)";
      }
    });

    if(chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
    {
      File file = chooser.getSelectedFile();
      if(!file.getName().toLowerCase().endsWith(".xml"))
      {
        file = new File(file.getAbsolutePath() + ".xml");
      }
      try
      {
        ConnectionImportExport exporter = new ConnectionImportExport();
        exporter.exportToFile(file.getAbsolutePath());
        statusBar.setMessage("成功导出连接配置： " + file.getName());
      }
      catch(Exception e)
      {
        ErrorHandler.handleError(this, e, "导出连接配置失败");
      }
    }
  }
}



