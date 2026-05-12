package com.ctgu.lightdbviewer.ui.workspace.tab;

import com.ctgu.lightdbviewer.jdbc.ConnectionManager;
import com.ctgu.lightdbviewer.jdbc.JdbcRowWriter;
import com.ctgu.lightdbviewer.metadata.MetadataService;
import com.ctgu.lightdbviewer.service.TableDataService;
import com.ctgu.lightdbviewer.ui.status.StatusBarPanel;
import com.ctgu.lightdbviewer.ui.table.DataGridPanel;
import com.ctgu.lightdbviewer.ui.table.EditableResultTableModel;
import com.ctgu.lightdbviewer.util.ThemeManager;
import com.ctgu.lightdbviewer.util.SqlValueUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * TableDataTab - 表格数据查看/编辑页签
 * <p>
 * 支持分页：首页 | 上一页 | 第N/M页 共X行 | 下一页 | 末页 | 每页 [100]
 */
/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class TableDataTab extends AbstractTab
{
  private static final Logger logger = LoggerFactory.getLogger(TableDataTab.class);
  private final String tableName;
  private final StatusBarPanel statusBar;
  private EditableResultTableModel model;
  private DataGridPanel gridPanel;
  // 分页状态
  private int currentPage = 0;
  private int pageSize = 100;
  private int totalRows = -1; // -1 = 尚未统计
  // 分页控件
  private JButton firstBtn;
  private JButton prevBtn;
  private JButton nextBtn;
  private JButton lastBtn;
  private JLabel pageInfoLabel;
  private JComboBox<Integer> pageSizeCombo;
  private boolean dirty = false;
  /**
   * 主键列名缓存（异步加载后刷新渲染器）
   */
  private java.util.List<String> pkColumns = new java.util.ArrayList<>();

  public TableDataTab(String tableName, StatusBarPanel statusBar)
  {
    this.tableName = tableName;
    this.statusBar = statusBar;

    setLayout(new BorderLayout());
    initUi();
    // 将当前配置的背景色应用到新创建的组件
    ThemeManager.applyBgColorToComponent(this);
    loadCurrentPage();
    loadTotalRowsAsync();
    loadPkColumnsAsync();
  }

  /**
   * 返回本 Tab 对应的表名（供 TabManager 自动生成 SQL 用）
   */
  public String getTableName()
  {
    return tableName;
  }

  private void initUi()
  {
    model = new EditableResultTableModel();
    model.addTableModelListener(e -> syncDirtyFromModel());
    gridPanel = new DataGridPanel(model);
    gridPanel.setTableName(tableName);   // 注入表名，右键菜单生成 SQL 时使用

    add(gridPanel, BorderLayout.CENTER);
    add(buildBottomBar(), BorderLayout.SOUTH);
  }

  /**
   * 构建底部栏：左侧操作按钮（图标） + 中间分隔 + 右侧分页控件
   */
  private JPanel buildBottomBar()
  {
    JPanel bar = new JPanel(new BorderLayout());
    bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
    // ---- 左侧：操作按钮（图标）----
    JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 3));
    actionPanel.add(iconBtn(ActionIcon.ADD, "添加行", this::onAddRow));
    actionPanel.add(iconBtn(ActionIcon.DELETE, "删除行", this::onDeleteRow));
    actionPanel.add(iconBtn(ActionIcon.SAVE, "保存修改", this::onSave));
    actionPanel.add(iconBtn(ActionIcon.REVERT, "撤销修改", this::onRevert));
    actionPanel.add(iconBtn(ActionIcon.REFRESH, "刷新数据", this::onRefresh));
    actionPanel.add(new JSeparator(SwingConstants.VERTICAL));
    actionPanel.add(iconBtn(ActionIcon.EXPORT, "导出数据", this::onExport));
    actionPanel.add(iconBtn(ActionIcon.IMPORT, "导入数据", this::onImport));
    // ---- 右侧：分页控件----
    JPanel pagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 3));
    firstBtn = navBtn("|<", "第一页", () -> goToPage(0));
    prevBtn = navBtn("<", "上一页", () -> goToPage(currentPage - 1));
    nextBtn = navBtn(">", "下一页", () -> goToPage(currentPage + 1));
    lastBtn = navBtn(">|", "最后一页", () -> goToPage(getLastPage()));
    pageInfoLabel = new JLabel("第1 页");
    pageInfoLabel.setPreferredSize(new Dimension(220, pageInfoLabel.getPreferredSize().height));
    pageInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);
    pageSizeCombo = new JComboBox<>(new Integer[] { 50, 100, 200, 500, 1000 });
    pageSizeCombo.setSelectedItem(pageSize);
    pageSizeCombo.setToolTipText("每页显示行数");
    pageSizeCombo.addActionListener(e -> {
      Integer sel = (Integer)pageSizeCombo.getSelectedItem();
      if(sel != null && sel != pageSize)
      {
        pageSize = sel;
        currentPage = 0;
        totalRows = -1;
        loadCurrentPage();
        loadTotalRowsAsync();
      }
    });
    pagePanel.add(firstBtn);
    pagePanel.add(prevBtn);
    pagePanel.add(pageInfoLabel);
    pagePanel.add(nextBtn);
    pagePanel.add(lastBtn);
    pagePanel.add(new JSeparator(SwingConstants.VERTICAL));
    pagePanel.add(new JLabel("每页:"));
    pagePanel.add(pageSizeCombo);
    bar.add(actionPanel, BorderLayout.WEST);
    bar.add(pagePanel, BorderLayout.CENTER);
    return bar;
  }

  private JButton iconBtn(ActionIcon type, String tooltip, Runnable action)
  {
    JButton btn = new JButton(type.icon);
    btn.setToolTipText(tooltip);
    btn.setFocusable(false);
    btn.setMargin(new Insets(2, 6, 2, 6));
    btn.addActionListener(e -> action.run());
    return btn;
  }

  private JButton navBtn(String text, String tip, Runnable action)
  {
    JButton btn = new JButton(text);
    btn.setToolTipText(tip);
    btn.setFocusable(false);
    btn.setMargin(new Insets(1, 6, 1, 6));
    btn.addActionListener(e -> action.run());
    return btn;
  }

  private void loadCurrentPage()
  {
    try
    {
      model.clearAll();
      EditableResultTableModel loaded = TableDataService.loadPage(tableName, currentPage, pageSize);
      model.copyFrom(loaded);
      dirty = false;
      updateTabTitle();
      updatePaginationControls();
      statusBar.setMessage(tableName + " 第" + (currentPage + 1) + " 页，" + model.getRowCount() + " 行");
      // 恢复 PK 列高亮（列可能在 clearAll 后重建）
      if(!pkColumns.isEmpty())
      {
        gridPanel.setPkColumns(pkColumns);
      }
    }
    catch(Exception e)
    {
      showError("Load failed", e);
    }
  }

  /**
   * 后台异步查询总行数，完成后更新分页信息
   */
  private void loadTotalRowsAsync()
  {
    new SwingWorker<Integer, Void>()
    {
      @Override
      protected Integer doInBackground() throws Exception
      {
        return TableDataService.countRows(tableName);
      }

      @Override
      protected void done()
      {
        try
        {
          totalRows = get();
          updatePaginationControls();
        }
        catch(Exception e)
        {
          logger.warn("获取总行数失败", e);
        }
      }
    }.execute();
  }

  /**
   * 后台异步加载主键列名，完成后刷新渲染器（PK 列粗体）
   */
  private void loadPkColumnsAsync()
  {
    new SwingWorker<java.util.List<String>, Void>()
    {
      @Override
      protected java.util.List<String> doInBackground() throws Exception
      {
        return MetadataService.primaryKeys(tableName);
      }

      @Override
      protected void done()
      {
        try
        {
          pkColumns = get();
          gridPanel.setPkColumns(pkColumns);
        }
        catch(Exception e)
        {
          logger.warn("加载主键列失败: {}", e.getMessage());
        }
      }
    }.execute();
  }

  private void goToPage(int page)
  {
    if(page < 0) {
      page = 0;
    }
    if(totalRows > 0)
    {
      int maxPage = (totalRows - 1) / pageSize;
      if(page > maxPage) {
        page = maxPage;
      }
    }
    currentPage = page;
    loadCurrentPage();
  }

  private int getLastPage()
  {
    if(totalRows <= 0) {
      return currentPage;
    }
    return Math.max(0, (totalRows - 1) / pageSize);
  }

  private void updatePaginationControls()
  {
    boolean hasPrev = currentPage > 0;
    boolean hasNext = model.getRowCount() >= pageSize || (totalRows > 0 && (long)(currentPage + 1) * pageSize < totalRows);
    boolean hasLast = totalRows > 0 && currentPage < getLastPage();

    firstBtn.setEnabled(hasPrev);
    prevBtn.setEnabled(hasPrev);
    nextBtn.setEnabled(hasNext);
    lastBtn.setEnabled(hasLast);

    if(totalRows < 0)
    {
      pageInfoLabel.setText("第" + (currentPage + 1) + " 页（统计中…）");
    }
    else
    {
      int totalPages = Math.max(1, (totalRows + pageSize - 1) / pageSize);
      pageInfoLabel.setText("第" + (currentPage + 1) + " / " + totalPages + " 页，共 " + totalRows + " 行");
    }
  }

  private void onAddRow()
  {
    model.addInsertRow();
    markDirty();
  }

  private void onDeleteRow()
  {
    int row = gridPanel.getSelectedRow();
    if(row < 0) {
      return;
    }
    model.markDeleteRow(row);
    markDirty();
  }

  private void onSave()
  {
    if(!model.isDirty()) {
      return;
    }
    try
    {
      try (Connection conn = ConnectionManager.get())
      {
        // 优先使用已缓存的 PK 列，否则实时查询
        List<String> pks = pkColumns.isEmpty() ? MetadataService.primaryKeys(tableName) : pkColumns;
        JdbcRowWriter writer = new JdbcRowWriter(conn, tableName, pks);
        writer.applyChanges(model.getChanges());
      }
      statusBar.setMessage("已保存修改: " + tableName);
      // 保存后重新从 DB 加载，刷新软删除行（移除），状态颜色复位
      totalRows = -1;
      loadCurrentPage();
      loadTotalRowsAsync();
      }
      catch(Exception e)
      {
        showError("保存失败", e);
      }
  }

  private void onRevert()
  {
    int confirm = JOptionPane.showConfirmDialog(this, "放弃所有未保存的修改？", "撤销修改", JOptionPane.YES_NO_OPTION);
    if(confirm == JOptionPane.YES_OPTION)
    {
      loadCurrentPage();
      statusBar.setMessage("已撤销修改: " + tableName);
    }
  }

  private void onRefresh()
  {
    if(model.isDirty())
    {
      int confirm = JOptionPane.showConfirmDialog(this, "有未保存的修改，刷新后将丢失，继续？", "刷新", JOptionPane.YES_NO_OPTION);
      if(confirm != JOptionPane.YES_OPTION) {
        return;
      }
    }
    totalRows = -1;
    loadCurrentPage();
    loadTotalRowsAsync();
  }

  private void markDirty()
  {
    if(!dirty)
    {
      dirty = true;
      updateTabTitle();
    }
  }

  private void syncDirtyFromModel()
  {
    if(model.isDirty())
      markDirty();
  }

  private void updateTabTitle()
  {
    Container p = getParent();
    if(!(p instanceof JTabbedPane tabs)) {
      return;
    }
    int idx = tabs.indexOfComponent(this);
    if(idx >= 0) {
      tabs.setTitleAt(idx, tableName + (dirty ? " *" : ""));
    }
  }

  @Override
  public boolean isDirty()
  {
    return dirty;
  }

  private void showError(String title, Exception e)
  {
    statusBar.setMessage(title + ": " + e.getMessage());
    JOptionPane.showMessageDialog(this, e.getMessage(), title, JOptionPane.ERROR_MESSAGE);
  }

  private void onExport()
  {
    String[] options = { "导出为 CSV", "导出为 SQL INSERT", "取消" };
    int choice = JOptionPane.showOptionDialog(this, "选择导出格式（当前页共 " + model.getRowCount() + " 行）", "导出数据 - " + tableName,
        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

    if(choice == 0) {
      exportCsv();
    } else if(choice == 1) {
      exportSql();
    }
  }

  private void exportCsv()
  {
    JFileChooser fc = new JFileChooser();
    fc.setDialogTitle("导出 CSV");
    fc.setSelectedFile(new File(tableName + ".csv"));
    fc.setFileFilter(new FileNameExtensionFilter("CSV 文件 (*.csv)", "csv"));
    if(fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    File file = fc.getSelectedFile();
    if(!file.getName().endsWith(".csv")) {
      file = new File(file.getPath() + ".csv");
    }
    File finalFile = file;

    new SwingWorker<Void, Void>()
    {
      @Override
      protected Void doInBackground() throws Exception
      {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(finalFile), StandardCharsets.UTF_8)))
        {
          // BOM for Excel UTF-8 recognition
          bw.write('\uFEFF');
          int cols = model.getColumnCount();
          // 表头
          for(int c = 0; c < cols; c++)
          {
            if(c > 0) {
              bw.write(',');
            }
            bw.write(csvEscape(model.getColumnName(c)));
          }
          bw.newLine();
          // 数据行
          for(int r = 0; r < model.getRowCount(); r++)
          {
            for(int c = 0; c < cols; c++)
            {
              if(c > 0) {
                bw.write(',');
              }
              Object val = model.getValueAt(r, c);
              bw.write(csvEscape(val != null ? val.toString() : ""));
            }
            bw.newLine();
          }
        }
        return null;
      }

      @Override
      protected void done()
      {
        try
        {
          get();
          statusBar.setMessage("已导出 CSV: " + finalFile.getName());
          JOptionPane.showMessageDialog(TableDataTab.this, "导出成功: " + finalFile.getAbsolutePath(), "导出完成",
              JOptionPane.INFORMATION_MESSAGE);
        }
        catch(Exception ex)
        {
          showError("导出失败", ex);
        }
      }
    }.execute();
  }

  private void exportSql()
  {
    JFileChooser fc = new JFileChooser();
    fc.setDialogTitle("导出 SQL");
    fc.setSelectedFile(new File(tableName + ".sql"));
    fc.setFileFilter(new FileNameExtensionFilter("SQL 文件 (*.sql)", "sql"));
    if(fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    File file = fc.getSelectedFile();
    if(!file.getName().endsWith(".sql")) {
      file = new File(file.getPath() + ".sql");
    }
    File finalFile = file;

    new SwingWorker<Void, Void>()
    {
      @Override
      protected Void doInBackground() throws Exception
      {
        int cols = model.getColumnCount();
        // 预生成列名部分
        StringBuilder colPart = new StringBuilder();
        for(int c = 0; c < cols; c++)
        {
          if(c > 0) {
            colPart.append(", ");
          }
          colPart.append(quoteIdSql(model.getColumnName(c)));
        }

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(finalFile), StandardCharsets.UTF_8)))
        {
          bw.write("-- Exported from LightDB Viewer: " + tableName);
          bw.newLine();
          for(int r = 0; r < model.getRowCount(); r++)
          {
            bw.write("INSERT INTO " + quoteIdSql(tableName) + " (" + colPart + ") VALUES (");
            for(int c = 0; c < cols; c++)
            {
              if(c > 0) {
                bw.write(", ");
              }
              bw.write(SqlValueUtil.toSqlLiteral(model.getValueAt(r, c)));
            }
            bw.write(");");
            bw.newLine();
          }
        }
        return null;
      }

      @Override
      protected void done()
      {
        try
        {
          get();
          statusBar.setMessage("已导出 SQL: " + finalFile.getName());
          JOptionPane.showMessageDialog(TableDataTab.this, "导出成功: " + finalFile.getAbsolutePath(), "导出完成",
              JOptionPane.INFORMATION_MESSAGE);
        }
        catch(Exception ex)
        {
          showError("导出失败", ex);
        }
      }
    }.execute();
  }

  private void onImport()
  {
    JFileChooser fc = new JFileChooser();
    fc.setDialogTitle("导入 CSV 文件");
    fc.setFileFilter(new FileNameExtensionFilter("CSV 文件 (*.csv)", "csv"));
    if(fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    File file = fc.getSelectedFile();
    int confirm = JOptionPane.showConfirmDialog(this,
        "将从文件导入数据到表 " + tableName + "。\n" + "CSV 第一行必须为列名（与表列名对应），数据将直接写入数据库。\n\n继续吗?", "导入确认",
        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    if(confirm != JOptionPane.YES_OPTION) {
      return;
    }

    new SwingWorker<int[], Void>()
    {
      @Override
      protected int[] doInBackground() throws Exception
      {
        List<String[]> rows = parseCsv(file);
        if(rows.isEmpty())
        {
          return new int[] { 0, 0 };
        }
        String[] headers = rows.get(0);
        int success = 0, fail = 0;
        try (Connection conn = ConnectionManager.get())
        {
          for(int r = 1; r < rows.size(); r++)
          {
            String[] rowData = rows.get(r);
            try
            {
              // 构造 INSERT SQL
              StringBuilder sb = new StringBuilder("INSERT INTO ");
              sb.append(quoteIdSql(tableName)).append(" (");
              for(int c = 0; c < headers.length; c++)
              {
                if(c > 0) {
                  sb.append(", ");
                }
                sb.append(quoteIdSql(headers[c]));
              }
              sb.append(") VALUES (");
              for(int c = 0; c < headers.length; c++)
              {
                if(c > 0) {
                  sb.append(", ");
                }
                String val = c < rowData.length ? rowData[c] : "";
                sb.append("NULL".equalsIgnoreCase(val) || val.isEmpty() ? "NULL" : "'" + val.replace("'", "''") + "'");
              }
              sb.append(")");
              try (var stmt = conn.createStatement())
              {
                stmt.executeUpdate(sb.toString());
              }
              conn.commit();
              success++;
            }
            catch(Exception ex)
            {
              logger.warn("导入第 {} 行失败: {}", r, ex.getMessage());
              fail++;
            }
          }
        }
        return new int[] { success, fail };
      }

      @Override
      protected void done()
      {
        try
        {
          int[] result = get();
           statusBar.setMessage("导入完成：成功 " + result[0] + " 行，失败 " + result[1] + " 行");
           JOptionPane.showMessageDialog(TableDataTab.this, "导入完成\n成功 " + result[0] + " 行\n失败 " + result[1] + " 行", "导入结果",
               JOptionPane.INFORMATION_MESSAGE);
          totalRows = -1;
          loadCurrentPage();
          loadTotalRowsAsync();
        }
        catch(Exception ex)
        {
          showError("导入失败", ex);
        }
      }
    }.execute();
  }

  private static String csvEscape(String val)
  {
    if(val == null) {
      return "";
    }
    if(val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
      return "\"" + val.replace("\"", "\"\"") + "\"";
    }
    return val;
  }

  /**
   * 简易 CSV 解析（支持双引号转义）
   */
  private static List<String[]> parseCsv(File file) throws IOException
  {
    List<String[]> result = new ArrayList<>();
    // 尝试先用 UTF-8（含 BOM），失败再退回 GBK
    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)))
    {
      String line;
      while((line = br.readLine()) != null)
      {
        // 跳过 BOM
        if(!result.isEmpty() || !line.startsWith("\uFEFF")) {
          result.add(splitCsvLine(line));
        } else {
          result.add(splitCsvLine(line.substring(1)));
        }
      }
    }
    return result;
  }

  private static String[] splitCsvLine(String line)
  {
    List<String> fields = new ArrayList<>();
    StringBuilder cur = new StringBuilder();
    boolean inQuote = false;
    for(int i = 0; i < line.length(); i++)
    {
      char ch = line.charAt(i);
      if(inQuote)
      {
        if(ch == '"')
        {
          if(i + 1 < line.length() && line.charAt(i + 1) == '"')
          {
            cur.append('"');
            i++;
          }
          else {
            inQuote = false;
          }
        }
        else {
          cur.append(ch);
        }
      }
      else
      {
        if(ch == '"') {
          inQuote = true;
        }
        else if(ch == ',')
        {
          fields.add(cur.toString());
          cur.setLength(0);
        }
        else {
          cur.append(ch);
        }
      }
    }
    fields.add(cur.toString());
    return fields.toArray(new String[0]);
  }

  private String quoteIdSql(String name)
  {
    if(name == null || name.isBlank()) {
      return name;
    }
    try
    {
      String product = ConnectionManager.getDatabaseProduct().toLowerCase();
      if(product.contains("mysql")) {
        return "`" + name.replace("`", "``") + "`";
      }
    }
    catch(Exception ignored)
    {
    }
    return "\"" + name.replace("\"", "\"\"") + "\"";
  }

  private enum ActionIcon
  {
    ADD(new Icon()
    {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y)
      {
        Graphics2D g2 = init(g);
        g2.setColor(new Color(0x4CAF50));
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x + 8, y + 3, x + 8, y + 13);
        g2.drawLine(x + 3, y + 8, x + 13, y + 8);
        g2.dispose();
      }

      @Override
      public int getIconWidth()
      {
        return 16;
      }

      @Override
      public int getIconHeight()
      {
        return 16;
      }
    }),

    DELETE(new Icon()
    {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y)
      {
        Graphics2D g2 = init(g);
        g2.setColor(new Color(0xE53935));
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x + 3, y + 8, x + 13, y + 8);
        g2.dispose();
      }

      @Override
      public int getIconWidth()
      {
        return 16;
      }

      @Override
      public int getIconHeight()
      {
        return 16;
      }
    }),

    SAVE(new Icon()
    {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y)
      {
        Graphics2D g2 = init(g);
        // 软盘外壳
        g2.setColor(new Color(0x42A5F5));
        g2.fillRoundRect(x + 1, y + 1, 14, 14, 3, 3);
        // 标签区（上部白色）
        g2.setColor(Color.WHITE);
        g2.fillRect(x + 4, y + 1, 8, 5);
        // 磁盘窗口（下部深色）
        g2.setColor(new Color(0x1565C0));
        g2.fillRect(x + 3, y + 9, 10, 5);
        // 边框
        g2.setColor(new Color(0x1E88E5));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x + 1, y + 1, 14, 14, 3, 3);
        g2.dispose();
      }

      @Override
      public int getIconWidth()
      {
        return 16;
      }

      @Override
      public int getIconHeight()
      {
        return 16;
      }
    }),

    REVERT(new Icon()
    {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y)
      {
        Graphics2D g2 = init(g);
        g2.setColor(new Color(0xFFA000));
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // 弧线
        g2.drawArc(x + 3, y + 3, 10, 10, 60, 240);
        // 箭头
        g2.drawLine(x + 4, y + 3, x + 4, y + 7);
        g2.drawLine(x + 4, y + 3, x + 8, y + 3);
        g2.dispose();
      }

      @Override
      public int getIconWidth()
      {
        return 16;
      }

      @Override
      public int getIconHeight()
      {
        return 16;
      }
    }),

    REFRESH(new Icon()
    {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y)
      {
        Graphics2D g2 = init(g);
        g2.setColor(new Color(0x26A69A));
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // 双弧线
        g2.drawArc(x + 2, y + 2, 12, 12, 45, 200);
        g2.drawArc(x + 2, y + 2, 12, 12, 225, 200);
        // 上箭头
        g2.drawLine(x + 11, y + 2, x + 11, y + 6);
        g2.drawLine(x + 11, y + 2, x + 14, y + 4);
        // 下箭头
        g2.drawLine(x + 5, y + 14, x + 5, y + 10);
        g2.drawLine(x + 5, y + 14, x + 2, y + 12);
        g2.dispose();
      }

      @Override
      public int getIconWidth()
      {
        return 16;
      }

      @Override
      public int getIconHeight()
      {
        return 16;
      }
    }),

    // 导出：向下箭头 + 托盘
    EXPORT(new Icon()
    {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y)
      {
        Graphics2D g2 = init(g);
        g2.setColor(new Color(0x7B1FA2));
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // 竖线
        g2.drawLine(x + 8, y + 2, x + 8, y + 10);
        // 箭头头部
        g2.drawLine(x + 5, y + 7, x + 8, y + 11);
        g2.drawLine(x + 11, y + 7, x + 8, y + 11);
        // 托盘底部
        g2.drawLine(x + 2, y + 13, x + 14, y + 13);
        g2.dispose();
      }

      @Override
      public int getIconWidth()
      {
        return 16;
      }

      @Override
      public int getIconHeight()
      {
        return 16;
      }
    }),

    // 导入：向上箭头 + 托盘
    IMPORT(new Icon()
    {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y)
      {
        Graphics2D g2 = init(g);
        g2.setColor(new Color(0x00897B));
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // 竖线
        g2.drawLine(x + 8, y + 4, x + 8, y + 12);
        // 箭头头部（向上）
        g2.drawLine(x + 5, y + 7, x + 8, y + 3);
        g2.drawLine(x + 11, y + 7, x + 8, y + 3);
        // 托盘底部
        g2.drawLine(x + 2, y + 13, x + 14, y + 13);
        g2.dispose();
      }

      @Override
      public int getIconWidth()
      {
        return 16;
      }

      @Override
      public int getIconHeight()
      {
        return 16;
      }
    });

    final Icon icon;

    ActionIcon(Icon icon)
    {
      this.icon = icon;
    }

    private static Graphics2D init(Graphics g)
    {
      Graphics2D g2 = (Graphics2D)g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      return g2;
    }
  }
}

