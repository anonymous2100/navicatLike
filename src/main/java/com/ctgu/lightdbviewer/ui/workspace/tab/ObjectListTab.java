package com.ctgu.lightdbviewer.ui.workspace.tab;

import com.ctgu.lightdbviewer.jdbc.ConnectionManager;
import com.ctgu.lightdbviewer.metadata.MetadataService;
import com.ctgu.lightdbviewer.ui.status.StatusBarPanel;
import com.ctgu.lightdbviewer.ui.workspace.TabManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.sql.Connection;

/**
 * ObjectListTab - 显示数据库对象列表（表、视图、函数、角色等）
 *
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class ObjectListTab extends AbstractTab
{
  private final ObjectType objectType;
  private final StatusBarPanel statusBar;
  private final TabManager tabManager;
  /**
   * 目标数据库名，null 表示使用当前数据库
   */
  private final String dbName;
  /**
   * 目标 Schema 名（PostgreSQL），null 表示默认
   */
  private final String schemaName;

  /**
   * 兼容旧调用：不指定 db/schema 上下文
   */
  public ObjectListTab(ObjectType objectType, StatusBarPanel statusBar, TabManager tabManager)
  {
    this(objectType, statusBar, tabManager, null, null);
  }

  /**
   * 带 db/schema 上下文的构造（用于"点击数据库节点"/"点击 Tables 文件"场景）
   */
  public ObjectListTab(ObjectType objectType, StatusBarPanel statusBar, TabManager tabManager, String dbName, String schemaName)
  {
    this.objectType = objectType;
    this.statusBar = statusBar;
    this.tabManager = tabManager;
    this.dbName = dbName;
    this.schemaName = schemaName;
    setLayout(new BorderLayout());
    loadDataAsync();
  }

  @Override
  public boolean isDirty()
  {
    return false;
  }

  private void loadDataAsync()
  {
    String contextDb = dbName != null ? dbName : ConnectionManager.getCurrentDatabase();
    statusBar.setMessage("加载 " + objectType.label + " 列表 (" + contextDb + ") ...");

    new SwingWorker<DefaultTableModel, Void>()
    {
      @Override
      protected DefaultTableModel doInBackground() throws Exception
      {
        // 如果指定了目标数据库，先切换
        if(dbName != null && !dbName.equals(ConnectionManager.getCurrentDatabase()))
        {
          ConnectionManager.switchDatabase(dbName);
        }

        DefaultTableModel model = new DefaultTableModel(objectType.headers, 0)
        {
          @Override
          public boolean isCellEditable(int row, int col)
          {
            return false;
          }
        };

        switch(objectType)
        {
        case DATABASES ->
        {
          for(String db : MetadataService.listDatabases())
          {
            model.addRow(new Object[] { db });
          }
        }
        case TABLES ->
        {
          for(String t : MetadataService.listTables(schemaName))
          {
            model.addRow(new Object[] { t });
          }
        }
        case VIEWS ->
        {
          for(String v : MetadataService.listViews(schemaName))
          {
            model.addRow(new Object[] { v });
          }
        }
        case FUNCTIONS ->
        {
          for(String f : MetadataService.listFunctions(schemaName))
          {
            model.addRow(new Object[] { f });
          }
        }
        case ROLES ->
        {
          for(String[] r : MetadataService.listRoles())
          {
            model.addRow(r);
          }
        }
        case OTHER ->
        {
          for(String p : MetadataService.listProcedures())
          {
            model.addRow(new Object[] { "存储过程", p });
          }
          for(String t : MetadataService.listTriggers())
          {
            model.addRow(new Object[] { "触发器", t });
          }
        }
        case QUERIES ->
        {
          try
          {
            try (Connection conn = ConnectionManager.get())
            {
              String product = conn.getMetaData().getDatabaseProductName().toLowerCase();
              String sql;
              if(product.contains("postgres"))
              {
                sql = "SELECT pid::text, state, query FROM pg_stat_activity " + "WHERE datname = current_database() AND state IS NOT NULL "
                    + "ORDER BY backend_start DESC";
              }
              else
              {
                sql = "SELECT ID, COMMAND, INFO FROM information_schema.PROCESSLIST " + "WHERE DB = DATABASE() ORDER BY ID";
              }
              try (var stmt = conn.createStatement(); var rs = stmt.executeQuery(sql))
              {
                while(rs.next())
                {
                  model.addRow(new Object[] { rs.getString(1), rs.getString(2), rs.getString(3) });
                }
              }
            }
          }
          catch(Exception e)
          {
            model.addRow(new Object[] { "", "错误", e.getMessage() });
          }
        }
        case BACKUP ->
        {
          model.addRow(new Object[] { "备份功能需配合 pg_dump / mysqldump 外部工具使用" });
          model.addRow(new Object[] { "提示: 可在「查询」标签页中执行 COPY 命令进行数据导出" });
        }
        }
        return model;
      }

      @Override
      protected void done()
      {
        try
        {
          DefaultTableModel model = get();
          JTable table = new JTable(model);
          table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
          table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
          table.setRowHeight(table.getFont() != null ? table.getFont().getSize() + 8 : 24);
          autoFitColumns(table);

          // 双击：DATABASES -> 切换数据库并打开表列表；TABLES/VIEWS -> 打开表
          if(objectType == ObjectType.DATABASES)
          {
            table.addMouseListener(new MouseAdapter()
            {
              @Override
              public void mouseClicked(MouseEvent e)
              {
                if(e.getClickCount() == 2 && tabManager != null)
                {
                  int row = table.getSelectedRow();
                  if(row >= 0)
                  {
                    String name = String.valueOf(model.getValueAt(row, 0));
                    tabManager.showTableList(name, null);
                  }
                }
              }
            });
          }
          else if(objectType == ObjectType.TABLES || objectType == ObjectType.VIEWS)
          {
            table.addMouseListener(new MouseAdapter()
            {
              @Override
              public void mouseClicked(MouseEvent e)
              {
                if(e.getClickCount() == 2 && tabManager != null)
                {
                  int row = table.getSelectedRow();
                  if(row >= 0)
                  {
                    String name = String.valueOf(model.getValueAt(row, 0));
                    // 拼上 schema 前缀（如有）
                    String qualified = (schemaName != null && !schemaName.isBlank()) ? schemaName + "." + name : name;
                    tabManager.openTable(qualified);
                  }
                }
              }
            });
          }

          // 顶部信息
          String contextDb = dbName != null ? dbName : ConnectionManager.getCurrentDatabase();
          String contextLabel = schemaName != null ? contextDb + "." + schemaName : contextDb;
          if(contextLabel == null || contextLabel.isBlank())
          {
            contextLabel = "(当前连接)";
          }
          JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
          topPanel.add(new JLabel(objectType.label + " " + contextLabel + " (" + model.getRowCount() + " 条)"));
          JButton refreshBtn = new JButton("刷新");
          refreshBtn.addActionListener(ev -> {
            removeAll();
            loadDataAsync();
            revalidate();
            repaint();
          });
          topPanel.add(refreshBtn);

          add(topPanel, BorderLayout.NORTH);
          add(new JScrollPane(table), BorderLayout.CENTER);
          revalidate();
          repaint();
          statusBar.setMessage("已加载 " + objectType.label + ": " + model.getRowCount() + " 条");
        }
        catch(Exception ex)
        {
          statusBar.setMessage("加载失败: " + ex.getMessage());
          add(new JLabel("加载失败: " + ex.getMessage()), BorderLayout.CENTER);
          revalidate();
        }
      }
    }.execute();
  }

  private static void autoFitColumns(JTable table)
  {
    TableModel m = table.getModel();
    TableColumnModel cm = table.getColumnModel();
    FontMetrics headerFm = table.getTableHeader().getFontMetrics(table.getTableHeader().getFont());
    FontMetrics cellFm = table.getFontMetrics(table.getFont());
    int padding = 20;
    int maxWidth = 600;
    int sampleRows = Math.min(m.getRowCount(), 100);

    for(int col = 0; col < cm.getColumnCount(); col++)
    {
      TableColumn tc = cm.getColumn(col);
      String hdr = String.valueOf(m.getColumnName(col));
      int width = headerFm.stringWidth(hdr) + padding;
      for(int row = 0; row < sampleRows; row++)
      {
        Object val = m.getValueAt(row, col);
        if(val != null)
        {
          width = Math.max(width, cellFm.stringWidth(val.toString()) + padding);
        }
      }
      tc.setPreferredWidth(Math.min(width, maxWidth));
    }
  }

  /**
   * @author lihuahui
   * @version 1.0
   * @description:
   */
  public enum ObjectType
  {
    DATABASES("数据库", new String[] { "名称" }), TABLES("表", new String[] { "名称" }), VIEWS("视图", new String[] { "名称" }), FUNCTIONS(
      "函数", new String[] { "名称" }), ROLES("角色", new String[] { "名称", "超级用户", "可创建DB", "可登录" }), OTHER("其他",
      new String[] { "类型", "名称" }), QUERIES("查询", new String[] { "查询编号", "状态", "SQL" }), BACKUP("备份",
      new String[] { "信息" });

    public final String label;
    public final String[] headers;

    ObjectType(String label, String[] headers)
    {
      this.label = label;
      this.headers = headers;
    }
  }
}





