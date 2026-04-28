package com.ctgu.lightdbviewer.ui.explorer;

import com.ctgu.lightdbviewer.config.AppConfig;
import com.ctgu.lightdbviewer.jdbc.ConnectionManager;
import com.ctgu.lightdbviewer.metadata.MetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ctgu.lightdbviewer.ui.workspace.TabManager;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Locale;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ObjectExplorerPanel 左侧对象树（懒加载，层级结构
 * <p>
 * 树结构：
 * [CONNECTION] localhost:5432
 * └── [DATABASES_FOLDER] Databases
 * ├── [DATABASE] mydb
 *   ├── [SCHEMA] public          （PostgreSQL 显示 schema 层）
 *     ├── [TABLES_FOLDER] Tables
 *       └── [TABLE] users
 *     ├── [VIEWS_FOLDER] Views
 *     ├── [FUNCTIONS_FOLDER] Functions
 *     └── [TRIGGERS_FOLDER] Triggers
 *   └── [SCHEMA] app_schema
 * └── [DATABASE] postgres
 * └── (展开时按需加载)
 * <p>
 * MySQL schema 层，DATABASE 节点直接展开为各 Folder 节点
 */
/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class ObjectExplorerPanel extends JPanel
{
  private final TabManager manager;
  private final JTree tree;
  private final ExplorerTreeNode root;
  private final DefaultTreeModel model;
  private static final Logger logger = LoggerFactory.getLogger(ObjectExplorerPanel.class);

  /**
   * 单线程串行执行器：所有懒加载任务排队执行，避免多个任务并发调
   * ConnectionManager.switchDatabase() 引发I/O 竞争错误
   */
  private final ExecutorService loadExecutor = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "explorer-loader");
    t.setDaemon(true);
    return t;
  });

  public ObjectExplorerPanel(TabManager manager)
  {
    this.manager = manager;
    setLayout(new BorderLayout());
    root = new ExplorerTreeNode(ExplorerNodeType.CONNECTION, "Not Connected");
    model = new DefaultTreeModel(root);
    tree = new JTree(model);
    tree.setRootVisible(true);
    tree.setShowsRootHandles(true);
    tree.setCellRenderer(new ExplorerTreeCellRenderer());
    // 行高跟随字体（字体由 UIManager 全局控制，无需显式 setFont
    tree.setRowHeight(AppConfig.getInstance().getFontSize() + 8);
    // 懒加载：展开前检测并触发异步加载
    tree.addTreeWillExpandListener(new TreeWillExpandListener()
    {
      @Override
      public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException
      {
        Object last = event.getPath().getLastPathComponent();
        if(last instanceof ExplorerTreeNode node && node.needsLoading())
        {
          loadAsync(node);
        }
      }

      @Override
      public void treeWillCollapse(TreeExpansionEvent event)
      {
      }
    });
    installMouseBehavior();
    installSelectionBehavior();
    // 去掉 JScrollPane 默认边框，避免左侧面板出现多余方
    JScrollPane treeScroll = new JScrollPane(tree);
    treeScroll.setBorder(BorderFactory.createEmptyBorder());
    add(buildTreeToolbar(), BorderLayout.NORTH);
    add(treeScroll, BorderLayout.CENTER);
    // 去掉面板自身边框
    setBorder(BorderFactory.createEmptyBorder());
  }

  private JToolBar buildTreeToolbar()
  {
    JToolBar bar = new JToolBar();
    bar.setFloatable(false);
    bar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
    JButton expandBtn = makeToolBtn("+", "展开全部");
    expandBtn.addActionListener(e -> expandAll());
    JButton collapseBtn = makeToolBtn("-", "折叠全部");
    collapseBtn.addActionListener(e -> collapseAll());
    bar.add(expandBtn);
    bar.add(collapseBtn);
    bar.addSeparator();
    JButton locateBtn = makeToolBtn("@", "定位当前活动标签页");
    locateBtn.addActionListener(e -> locateActive());
    bar.add(locateBtn);

    return bar;
  }

  private JButton makeToolBtn(String text, String tooltip)
  {
    JButton btn = new JButton(text);
    btn.setToolTipText(tooltip);
    btn.setFocusable(false);
    btn.setMargin(new Insets(1, 6, 1, 6));
    return btn;
  }

  /**
   * 展开全部节点（遇到未加载的节点时触发异步加载，加载完后继续展开
   */
  private void expandAll()
  {
    expandAllFrom(0);
  }

  /**
   * 从指定行号开始逐行展开
   * 遇到 needsLoading 的节点时，触发异步加载并在加载完成后恢复展开
   * 由于 loadAsync 使用串行 executor，不会引发并发问题
   */
  private void expandAllFrom(int startRow)
  {
    int i = startRow;
    while(i < tree.getRowCount())
    {
      TreePath path = tree.getPathForRow(i);
      if(path != null && path.getLastPathComponent() instanceof ExplorerTreeNode node && node.needsLoading())
      {
        // 触发加载，加载完成后从当前行继续展开
        final int resumeRow = i;
        loadAsync(node, () -> expandAllFrom(resumeRow));
        return; // 等待加载完成后回调继
      }
      tree.expandRow(i);
      i++;
    }
  }

  /**
   * 折叠：保留根节点展开，折叠其
   */
  private void collapseAll()
  {
    for(int i = tree.getRowCount() - 1; i > 0; i--)
      tree.collapseRow(i);
  }

  /**
   * 定位当前活动标签页对应的树节点（选中并滚动到可见
   */
  private void locateActive()
  {
    String activeName = manager.getActiveTableName();
    if(activeName == null || activeName.isBlank())
    {
      JOptionPane.showMessageDialog(this, "当前没有活动的表格标签页。", "定位", JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    TreePath found = findTableNodePath(root, activeName);
    if(found != null)
    {
      tree.setSelectionPath(found);
      tree.scrollPathToVisible(found);
    }
    else
    {
      JOptionPane.showMessageDialog(this, "未在已加载的树节点中找到 " + activeName + "\n请先展开对应的数据库节点。", "定位",
          JOptionPane.INFORMATION_MESSAGE);
    }
  }

  /**
   * 递归在已加载节点中查找匹配的 TABLE/VIEW 节点路径
   */
  private TreePath findTableNodePath(ExplorerTreeNode node, String targetName)
  {
    if((node.getType() == ExplorerNodeType.TABLE || node.getType() == ExplorerNodeType.VIEW) && qualifiedName(node).equals(targetName))
    {
      return new TreePath(model.getPathToRoot(node));
    }
    for(int i = 0; i < node.getChildCount(); i++)
    {
      if(node.getChildAt(i) instanceof ExplorerTreeNode child)
      {
        TreePath result = findTableNodePath(child, targetName);
        if(result != null)
          return result;
      }
    }
    return null;
  }

  public void refreshTree()
  {
    root.removeAllChildren();
    root.setLoaded(false);
    if(!ConnectionManager.isConnected())
    {
      ((DefaultTreeModel)tree.getModel()).reload();
      return;
    }
    // 连接标签：host:port (user)
    String connLabel = buildConnectionLabel();
    // root label final，用 setUserObject 改显示文
    root.setUserObject(connLabel);
    // 添加 Databases 文件夹（LOADING 占位符，触发懒加载）
    ExplorerTreeNode dbsFolder = makeFolder(ExplorerNodeType.DATABASES_FOLDER, "Databases", null, null, true);
    root.add(dbsFolder);
    model.reload();
    tree.expandRow(0); // 展开根节点，Databases 文件夹可
  }

  /**
   * 将节点加载任务提交到单线程串行执行器
   * 所有加载任务严格串行，杜绝多个 switchDatabase() 并发竞争同一连接
   */
  private void loadAsync(ExplorerTreeNode node)
  {
    loadAsync(node, null);
  }

  /**
   * 带回调的懒加载版本：加载完成后在 EDT 执行 afterLoad
   * - 若已加载完毕：直接执afterLoad
   * - 若正在加载中：提交一no-op 任务追加到队列后执行 afterLoad（等前面任务完成
   * - 否则正常启动加载
   */
  private void loadAsync(ExplorerTreeNode node, Runnable afterLoad)
  {
    if(node.isLoaded())
    {
      if(afterLoad != null)
      {
        SwingUtilities.invokeLater(afterLoad);
      }
      return;
    }
    if(node.isLoading())
    {
      // 已在加载中，追加一个仅执行回调的任
      if(afterLoad != null)
      {
        loadExecutor.submit(() -> SwingUtilities.invokeLater(afterLoad));
      }
      return;
    }
    node.setLoading(true);
    loadExecutor.submit(() -> {
      // 后台线程：计算子节点（含数据库切换、元数据查询
      List<ExplorerTreeNode> children = computeChildren(node);
      // 回到 EDT 更新树模
      SwingUtilities.invokeLater(() -> {
        node.removeAllChildren();
        for(ExplorerTreeNode child : children)
        {
          model.insertNodeInto(child, node, node.getChildCount());
        }
        node.setLoading(false);
        node.setLoaded(true);
        model.nodeStructureChanged(node);
        // nodeStructureChanged 会折叠节点，invokeLater 重新展开
        TreePath path = new TreePath(model.getPathToRoot(node));
        SwingUtilities.invokeLater(() -> {
          tree.expandPath(path);
          if(afterLoad != null)
          {
            SwingUtilities.invokeLater(afterLoad);
          }
        });
      });
    });
  }

  /**
   * 在连接后自动展开树到第一张表并打开它
   * 必须refreshTree() 调用之后、在 EDT 上调用
   */
  public void autoExpandAndOpen()
  {
    // root DATABASES_FOLDER
    if(root.getChildCount() == 0)
    {
      return;
    }
    Object firstChild = root.getChildAt(0);
    if(!(firstChild instanceof ExplorerTreeNode dbsFolder))
    {
      return;
    }
    if(dbsFolder.getType() != ExplorerNodeType.DATABASES_FOLDER)
    {
      return;
    }
    // 步骤 1：加载数据库列表
    loadAsync(dbsFolder, () -> {
      tree.expandPath(new TreePath(model.getPathToRoot(dbsFolder)));
      if(dbsFolder.getChildCount() == 0)
      {
        return;
      }
      Object firstDb = dbsFolder.getChildAt(0);
      if(!(firstDb instanceof ExplorerTreeNode dbNode))
      {
        return;
      }
      if(!dbNode.needsLoading() && !dbNode.isLoaded())
      {
        return;
      }
      // 步骤 2：加载第一个数据库
      loadAsync(dbNode, () -> {
        tree.expandPath(new TreePath(model.getPathToRoot(dbNode)));
        if(dbNode.getChildCount() == 0)
        {
          return;
        }
        Object firstSub = dbNode.getChildAt(0);
        if(!(firstSub instanceof ExplorerTreeNode subNode))
        {
          return;
        }
        if(subNode.getType() == ExplorerNodeType.SCHEMA)
        {
          // PostgreSQL：先加载 schema，再Tables 文件
          loadAsync(subNode, () -> {
            tree.expandPath(new TreePath(model.getPathToRoot(subNode)));
            triggerTablesFolder(subNode);
          });
        }
        else if(subNode.getType() == ExplorerNodeType.TABLES_FOLDER)
        {
          // MySQL：直接展开 Tables 文件
          triggerTablesFolder(dbNode);
        }
      });
    });
  }

  /**
   * 在给定父节点（SCHEMA DATABASE）中找到 TABLES_FOLDER，展开并打开第一个表
   */
  private void triggerTablesFolder(ExplorerTreeNode parentNode)
  {
    for(int i = 0; i < parentNode.getChildCount(); i++)
    {
      Object child = parentNode.getChildAt(i);
      if(child instanceof ExplorerTreeNode fn && fn.getType() == ExplorerNodeType.TABLES_FOLDER)
      {
        loadAsync(fn, () -> {
          tree.expandPath(new TreePath(model.getPathToRoot(fn)));
          openFirstTableIn(fn);
        });
        return;
      }
    }
  }

  /**
   * 打开 TABLES_FOLDER 子节点中的第一TABLE
   */
  private void openFirstTableIn(ExplorerTreeNode tablesFolder)
  {
    if(tablesFolder.getChildCount() == 0)
    {
      return;
    }
    Object first = tablesFolder.getChildAt(0);
    if(!(first instanceof ExplorerTreeNode tableNode))
    {
      return;
    }
    if(tableNode.getType() != ExplorerNodeType.TABLE)
    {
      return;
    }
    // 选中并滚动到可见
    TreePath path = new TreePath(model.getPathToRoot(tableNode));
    tree.setSelectionPath(path);
    tree.scrollPathToVisible(path);
    // 切换数据+ 打开
    try
    {
      ensureDatabase(tableNode.getDbName());
    }
    catch(Exception e)
    {
      logger.warn("切换数据库失败", e);
    }
    manager.openTable(qualifiedName(tableNode));
  }

  /**
   * 根据节点类型计算子节点列表（在后台线程执行）
   */
  private List<ExplorerTreeNode> computeChildren(ExplorerTreeNode node)
  {
    List<ExplorerTreeNode> result = new ArrayList<>();
    try
    {
      switch(node.getType())
      {
      case DATABASES_FOLDER ->
      {
        for(String db : MetadataService.listDatabases())
        {
          ExplorerTreeNode dbNode = makeFolder(ExplorerNodeType.DATABASE, db, db, null, true);
          result.add(dbNode);
        }
      }
      case DATABASE ->
      {
        String dbName = node.getLabel();
        ConnectionManager.switchDatabase(dbName);
        List<String> schemas = MetadataService.listSchemas();
        if(schemas.isEmpty())
        {
          // MySQL：无 schema 层，直接添加各类型文件夹
          result.addAll(buildObjectFolders(dbName, null));
        }
        else
        {
          // PostgreSQL：以 schema 为二级节
          for(String schema : schemas)
          {
            ExplorerTreeNode schemaNode = makeFolder(ExplorerNodeType.SCHEMA, schema, dbName, schema, true);
            result.add(schemaNode);
          }
        }
      }
      case SCHEMA ->
      {
        String dbName = node.getDbName();
        String schemaName = node.getSchemaName();
        // 确保连接在正确数据库
        ensureDatabase(dbName);
        result.addAll(buildObjectFolders(dbName, schemaName));
      }
      case TABLES_FOLDER ->
      {
        ensureDatabase(node.getDbName());
        for(String t : MetadataService.listTables(node.getSchemaName()))
        {
          ExplorerTreeNode n = leaf(ExplorerNodeType.TABLE, t, node.getDbName(), node.getSchemaName());
          result.add(n);
        }
      }
      case VIEWS_FOLDER ->
      {
        ensureDatabase(node.getDbName());
        for(String v : MetadataService.listViews(node.getSchemaName()))
          result.add(leaf(ExplorerNodeType.VIEW, v, node.getDbName(), node.getSchemaName()));
      }
      case FUNCTIONS_FOLDER ->
      {
        ensureDatabase(node.getDbName());
        for(String f : MetadataService.listFunctions(node.getSchemaName()))
          result.add(leaf(ExplorerNodeType.FUNCTION, f, node.getDbName(), node.getSchemaName()));
      }
      case PROCEDURES_FOLDER ->
      {
        ensureDatabase(node.getDbName());
        for(String p : MetadataService.listProcedures(node.getSchemaName()))
          result.add(leaf(ExplorerNodeType.PROCEDURE, p, node.getDbName(), node.getSchemaName()));
      }
      case TRIGGERS_FOLDER ->
      {
        ensureDatabase(node.getDbName());
        for(String t : MetadataService.listTriggers(node.getSchemaName()))
          result.add(leaf(ExplorerNodeType.TRIGGER, t, node.getDbName(), node.getSchemaName()));
      }
      default ->
      {
        // 叶节点不应触发懒加载
      }
      }
    }
    catch(Exception e)
    {
      result.clear();
      result.add(errorNode(e.getMessage()));
    }
    return result;
  }

  /**
   * 构建 Tables / Views / Functions / Procedures / Triggers 五个文件夹节
   */
  private List<ExplorerTreeNode> buildObjectFolders(String dbName, String schemaName)
  {
    List<ExplorerTreeNode> folders = new ArrayList<>();
    folders.add(makeFolder(ExplorerNodeType.TABLES_FOLDER, "Tables", dbName, schemaName, true));
    folders.add(makeFolder(ExplorerNodeType.VIEWS_FOLDER, "Views", dbName, schemaName, true));
    folders.add(makeFolder(ExplorerNodeType.FUNCTIONS_FOLDER, "Functions", dbName, schemaName, true));
    folders.add(makeFolder(ExplorerNodeType.PROCEDURES_FOLDER, "Procedures", dbName, schemaName, true));
    folders.add(makeFolder(ExplorerNodeType.TRIGGERS_FOLDER, "Triggers", dbName, schemaName, true));
    return folders;
  }

  /**
   * 创建LOADING 占位符（可懒加载）或无占位符的文件夹节点
   */
  private ExplorerTreeNode makeFolder(ExplorerNodeType type, String label, String dbName, String schemaName, boolean withLoading)
  {
    ExplorerTreeNode node = new ExplorerTreeNode(type, label);
    node.setDbName(dbName);
    node.setSchemaName(schemaName);
    if(withLoading)
      node.add(new ExplorerTreeNode(ExplorerNodeType.LOADING, "Loading..."));
    return node;
  }

  /**
   * 创建叶节点（TABLE / VIEW / FUNCTION 等）
   */
  private ExplorerTreeNode leaf(ExplorerNodeType type, String label, String dbName, String schemaName)
  {
    ExplorerTreeNode node = new ExplorerTreeNode(type, label);
    node.setDbName(dbName);
    node.setSchemaName(schemaName);
    return node;
  }

  private ExplorerTreeNode errorNode(String msg)
  {
    return new ExplorerTreeNode(ExplorerNodeType.LOADING, "Error: " + (msg != null ? msg : "unknown"));
  }

  /**
   * 如当前连接不在目标数据库则切
   */
  private void ensureDatabase(String dbName) throws Exception
  {
    if(dbName != null && !dbName.equals(ConnectionManager.getCurrentDatabase()))
      ConnectionManager.switchDatabase(dbName);
  }

  private String buildConnectionLabel()
  {
    try
    {
      String url = ConnectionManager.getUrl(); // jdbc:postgresql://host:5432/db
      String user = ConnectionManager.getUser();
      // "//host:port" 部分
      int slashSlash = url.indexOf("//");
      if(slashSlash >= 0)
      {
        String rest = url.substring(slashSlash + 2); // "host:5432/db"
        int slash = rest.indexOf('/');
        String hostPort = (slash >= 0) ? rest.substring(0, slash) : rest;
        return hostPort + " (" + user + ")";
      }
      return url;
    }
    catch(Exception e)
    {
      return "Connected";
    }
  }

  private void installMouseBehavior()
  {
    tree.addMouseListener(new MouseAdapter()
    {
      @Override
      public void mouseClicked(MouseEvent e)
      {
        if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
        {
          TreePath path = tree.getSelectionPath();
          if(path != null && path.getLastPathComponent() instanceof ExplorerTreeNode node)
            handleDoubleClick(node);
        }
      }

      @Override
      public void mousePressed(MouseEvent e)
      {
        showPopupIfNeeded(e);
      }

      @Override
      public void mouseReleased(MouseEvent e)
      {
        showPopupIfNeeded(e);
      }
    });
  }

  /**
   * 单击选中节点时，在右侧工作区展示对应的对象列表：
   * <ul>
   *   <li>CONNECTION / DATABASES_FOLDER 数据库列/li>
   *   <li>DATABASE 该数据库的表列表</li>
   *   <li>SCHEMA Schema 的表列表</li>
   *   <li>TABLES_FOLDER 该文件夹下的表列/li>
   * </ul>
   */
  private void installSelectionBehavior()
  {
    tree.addTreeSelectionListener(new TreeSelectionListener()
    {
      @Override
      public void valueChanged(TreeSelectionEvent e)
      {
        TreePath path = e.getNewLeadSelectionPath();
        if(path == null)
        {
          return;
        }
        Object last = path.getLastPathComponent();
        if(!(last instanceof ExplorerTreeNode node))
        {
          return;
        }
        switch(node.getType())
        {
        case CONNECTION, DATABASES_FOLDER -> manager.showDatabaseList();
        case DATABASE -> manager.showTableList(node.getLabel(), null);
        case SCHEMA -> manager.showTableList(node.getDbName(), node.getSchemaName());
        case TABLES_FOLDER -> manager.showTableList(node.getDbName(), node.getSchemaName());
        default ->
        {
          // TABLE / VIEW 等叶节点：不主动打开列表，由双击处理
        }
        }
      }
    });
  }

  private void handleDoubleClick(ExplorerTreeNode node)
  {
    switch(node.getType())
    {
    case TABLE, VIEW ->
    {
      try
      {
        ensureDatabase(node.getDbName());
      }
      catch(Exception ex)
      {
        JOptionPane.showMessageDialog(this, "切换数据库失败: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        return;
      }
      manager.openTable(qualifiedName(node));
    }
    case PROCEDURE, FUNCTION, TRIGGER -> manager.openQuery(sqlTemplateForNode(node));
    default ->
    {
      // 文件夹节点由 TreeWillExpandListener 处理
    }
    }
  }

  private void showPopupIfNeeded(MouseEvent e)
  {
    if(!e.isPopupTrigger())
    {
      return;
    }
    int row = tree.getRowForLocation(e.getX(), e.getY());
    if(row < 0)
    {
      return;
    }
    tree.setSelectionRow(row);
    TreePath path = tree.getSelectionPath();
    if(path == null || !(path.getLastPathComponent() instanceof ExplorerTreeNode node))
    {
      return;
    }
    JPopupMenu menu = buildPopupMenu(node);
    if(menu != null)
      menu.show(tree, e.getX(), e.getY());
  }

  private JPopupMenu buildPopupMenu(ExplorerTreeNode node)
  {
    return switch(node.getType())
    {
      case TABLE -> ExplorerPopupMenuFactory.createTableMenu(qualifiedName(node), manager);
      case VIEW -> ExplorerPopupMenuFactory.createViewMenu(qualifiedName(node), manager);
      case PROCEDURE, FUNCTION, TRIGGER -> ExplorerPopupMenuFactory.createRoutineMenu(node.getType(), node.getLabel(), manager);
      case TABLES_FOLDER, VIEWS_FOLDER, PROCEDURES_FOLDER, FUNCTIONS_FOLDER, TRIGGERS_FOLDER ->
          ExplorerPopupMenuFactory.createTablesRootMenu(manager);
      default -> ExplorerPopupMenuFactory.createConnectionMenu(manager);
    };
  }

  /**
   * 构造限定名
   * - PostgreSQL/其他schema 时返schema.name（例public.users
   * - MySQL 环境下返catalog.name（例mydb.users），以避免在未切换到目标数据库时出现“表不存在”的错误
   * - 如果没有 schema db 信息，则返回简单表
   */
  private String qualifiedName(ExplorerTreeNode node)
  {
    String schema = node.getSchemaName();
    String name = node.getLabel();
    // 如果schema（PostgreSQL 等），优先返schema.name
    if(schema != null && !schema.isBlank())
    {
      return schema + "." + name;
    }
    // 对于 MySQL，使catalog（数据库名）作为前缀，生db.table，确SQL 在正确的 catalog 下执
    String dbName = node.getDbName();
    try
    {
      String product = ConnectionManager.getDatabaseProduct();
      if(product != null && product.toLowerCase(Locale.ROOT).contains("mysql") && dbName != null && !dbName.isBlank())
      {
        return dbName + "." + name;
      }
    }
    catch(Exception ignored)
    {
      // 忽略查询产品名时的异常，回退到默认行
    }
    return name;
  }

  private String sqlTemplateForNode(ExplorerTreeNode node)
  {
    return switch(node.getType())
    {
      case PROCEDURE -> "CALL " + node.getLabel() + "();";
      case FUNCTION -> "SELECT " + node.getLabel() + "();";
      case TRIGGER -> "-- Trigger: " + node.getLabel() + System.lineSeparator();
      default -> "";
    };
  }
}

