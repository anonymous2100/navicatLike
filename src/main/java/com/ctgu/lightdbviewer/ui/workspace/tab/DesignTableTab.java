package com.ctgu.lightdbviewer.ui.workspace.tab;

import com.ctgu.lightdbviewer.jdbc.ConnectionManager;
import com.ctgu.lightdbviewer.metadata.ColumnInfo;
import com.ctgu.lightdbviewer.metadata.MetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ctgu.lightdbviewer.ui.status.StatusBarPanel;
import com.ctgu.lightdbviewer.util.ThemeManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;

/**
 * DesignTableTab - 表结构设计视图（多标签页）
 * <p>
 * 标签页：字段、索引、外键、唯一键、检查、排除、规则、触发器、选项、注释、SQL预览
 * <p>
 * 字段标签页支持编辑：字段名、类型、长度、小数位、允许空、主键、注释
 */
/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class DesignTableTab extends AbstractTab
{
  private static final Logger logger = LoggerFactory.getLogger(DesignTableTab.class);
  private final String tableName;
  private final StatusBarPanel statusBar;
  private boolean dirty = false;
  // 字段编辑表格
  private DefaultTableModel columnsModel;
  private JTable columnsTable;
  /**
   * 保存加载时的原始列数据，用于与编辑后数据对比生成正确 ALTER SQL
   */
  private List<Object[]> originalRows = new ArrayList<>();
  // 其他只读标签页
  private DefaultTableModel indexModel;
  private DefaultTableModel fkModel;
  private DefaultTableModel uniqueModel;
  private DefaultTableModel checkModel;
  private DefaultTableModel excludeModel;
  private DefaultTableModel ruleModel;
  private DefaultTableModel triggerModel;
  private JTextArea optionsArea;
  private JTextArea commentArea;
  private JTextArea sqlPreviewArea;
  private JTabbedPane innerTabs;

  public DesignTableTab(String tableName, StatusBarPanel statusBar)
  {
    this.tableName = tableName;
    this.statusBar = statusBar;
    setLayout(new BorderLayout());
    initUi();
    ThemeManager.applyBgColorToComponent(this);
    loadAllAsync();
  }

  public String getTableName()
  {
    return tableName;
  }

  @Override
  public boolean isDirty()
  {
    return dirty;
  }

  private void initUi()
  {
    // ---- 顶部工具栏----
    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);

    JButton saveBtn = new JButton("保存");
    saveBtn.setToolTipText("保存变更到数据库 (ALTER TABLE)");
    saveBtn.addActionListener(e -> onSave());

    JButton addBtn = new JButton("添加字段");
    addBtn.setToolTipText("在末尾添加新列");
    addBtn.addActionListener(e -> onAddColumn());

    JButton delBtn = new JButton("删除字段");
    delBtn.setToolTipText("删除选中列");
    delBtn.addActionListener(e -> onDeleteColumn());

    JButton refreshBtn = new JButton("刷新");
    refreshBtn.setToolTipText("重新加载表结构");
    refreshBtn.addActionListener(e -> loadAllAsync());

    toolbar.add(saveBtn);
    toolbar.addSeparator();
    toolbar.add(addBtn);
    toolbar.add(delBtn);
    toolbar.addSeparator();
    toolbar.add(refreshBtn);
    toolbar.addSeparator();
    JLabel titleLabel = new JLabel("Design: " + tableName);
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
    toolbar.add(titleLabel);

    // ---- 内部标签页----
    innerTabs = new JTabbedPane(JTabbedPane.TOP);

    // 1. 字段
    innerTabs.addTab("字段", buildColumnsPanel());
    // 2. 索引
    innerTabs.addTab("索引", buildReadOnlyTableTab(indexModel = roModel("索引名", "列", "类型", "唯一")));
    // 3. 外键
    innerTabs.addTab("外键", buildReadOnlyTableTab(fkModel = roModel("约束名", "列", "引用表", "引用列", "ON UPDATE", "ON DELETE")));
    // 4. 唯一
    innerTabs.addTab("唯一", buildReadOnlyTableTab(uniqueModel = roModel("约束名", "列")));
    // 5. 检查
    innerTabs.addTab("检查", buildReadOnlyTableTab(checkModel = roModel("约束名", "表达式")));
    // 6. 排除
    innerTabs.addTab("排除", buildReadOnlyTableTab(excludeModel = roModel("约束名", "定义")));
    // 7. 规则
    innerTabs.addTab("规则", buildReadOnlyTableTab(ruleModel = roModel("规则名", "事件", "定义")));
    // 8. 触发器
    innerTabs.addTab("触发器", buildReadOnlyTableTab(triggerModel = roModel("触发器名", "事件", "时机", "类型", "定义")));
    // 9. 选项
    innerTabs.addTab("选项", buildTextAreaTab(optionsArea = new JTextArea()));
    // 10. 注释
    innerTabs.addTab("注释", buildTextAreaTab(commentArea = new JTextArea()));
    // 11. SQL预览
    innerTabs.addTab("SQL预览", buildTextAreaTab(sqlPreviewArea = new JTextArea()));

    add(toolbar, BorderLayout.NORTH);
    add(innerTabs, BorderLayout.CENTER);
  }

  private JPanel buildColumnsPanel()
  {
    String[] headers = { "#", "字段名", "类型", "长度/精度", "小数位", "允许空", "主键", "自增", "默认值", "注释" };
    columnsModel = new DefaultTableModel(headers, 0)
    {
      @Override
      public boolean isCellEditable(int row, int col)
      {
        // # 列和自增列不可编辑
        return col != 0 && col != 7;
      }
      @Override
      public Class<?> getColumnClass(int col)
      {
        if(col == 5 || col == 6)
          return Boolean.class; // 允许空、主键用 checkbox
        return Object.class;
      }
    };

    columnsTable = new JTable(columnsModel);
    columnsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    columnsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    columnsTable.getTableHeader().setReorderingAllowed(false);
    columnsTable.setRowHeight(columnsTable.getFont() != null ? columnsTable.getFont().getSize() + 8 : 24);

    // 监听修改标记 dirty
    columnsModel.addTableModelListener(e -> {
      if(!dirty)
      {
        dirty = true;
        updateTabTitle();
      }
    });

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(new JScrollPane(columnsTable), BorderLayout.CENTER);
    return panel;
  }

  // ---- 只读表格标签页----

  private DefaultTableModel roModel(String... headers)
  {
    return new DefaultTableModel(headers, 0)
    {
      @Override
      public boolean isCellEditable(int row, int col)
      {
        return false;
      }
    };
  }

  private JScrollPane buildReadOnlyTableTab(DefaultTableModel model)
  {
    JTable t = new JTable(model);
    t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    t.getTableHeader().setReorderingAllowed(false);
    t.setRowHeight(t.getFont() != null ? t.getFont().getSize() + 8 : 24);
    return new JScrollPane(t);
  }

  private JScrollPane buildTextAreaTab(JTextArea area)
  {
    area.setEditable(false);
    area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    return new JScrollPane(area);
  }

  private void loadAllAsync()
  {
    statusBar.setMessage("加载表结构 " + tableName + " ...");
    new SwingWorker<DesignData, Void>()
    {
      @Override
      protected DesignData doInBackground() throws Exception
      {
        String bare = stripSchema(tableName);
        String schema = extractSchema(tableName);
        Connection conn = ConnectionManager.get();
        try
        {
          DatabaseMetaData meta = conn.getMetaData();
          String product = meta.getDatabaseProductName().toLowerCase(Locale.ROOT);
          boolean isPg = product.contains("postgres");
          String resolvedSchema = schema != null ? schema : (isPg ? "public" : null);
          DesignData data = new DesignData();
          // 1. 列
          data.columns = MetadataService.columns(bare);
          // 2~8: 每项查询使用 SAVEPOINT 保护，防止单项失败导致整个事务终止
          data.indexes = safeLoad(conn, () -> loadIndexes(meta, resolvedSchema, bare));
          data.foreignKeys = safeLoad(conn, () -> loadForeignKeys(meta, resolvedSchema, bare));
          data.uniqueKeys = safeLoad(conn, () -> loadUniqueConstraints(conn, resolvedSchema, bare, isPg));
          data.checks = safeLoad(conn, () -> loadCheckConstraints(conn, resolvedSchema, bare, isPg));
          data.exclusions = isPg ? safeLoad(conn, () -> loadExclusionConstraints(conn, resolvedSchema, bare)) : List.of();
          data.rules = isPg ? safeLoad(conn, () -> loadRules(conn, resolvedSchema, bare)) : List.of();
          data.triggers = safeLoad(conn, () -> loadTriggers(conn, resolvedSchema, bare, isPg));
          // 9~11: 文本类信息
          data.options = safeLoadStr(conn, () -> loadTableOptions(conn, resolvedSchema, bare, isPg));
          data.comment = safeLoadStr(conn, () -> loadTableComment(conn, resolvedSchema, bare, isPg));
          data.ddl = generateCreateTableDDL(conn, resolvedSchema, bare, isPg, data.columns);
          return data;
        }
        finally
        {
          conn.close();
        }
      }

      @Override
      protected void done()
      {
        try
        {
          DesignData data = get();
          populateAll(data);
          dirty = false;
          statusBar.setMessage("已加载表结构: " + tableName);
        }
        catch(Exception ex)
        {
          statusBar.setMessage("加载失败: " + ex.getMessage());
          JOptionPane.showMessageDialog(DesignTableTab.this, "加载表结构失败\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
      }
    }.execute();
  }

  private void populateAll(DesignData data)
  {
    // 字段
    columnsModel.setRowCount(0);
    for(int i = 0; i < data.columns.size(); i++)
    {
      ColumnInfo c = data.columns.get(i);
      columnsModel.addRow(new Object[] { i + 1, c.name, c.typeName, c.columnSize > 0 ? String.valueOf(c.columnSize) : "",
          c.decimalDigits > 0 ? String.valueOf(c.decimalDigits) : "", c.nullable, c.pk, c.autoIncrement ? "YES" : "",
          c.defaultValue != null ? c.defaultValue : "", c.remarks != null ? c.remarks : "" });
    }
    autoFitColumns(columnsTable);
    // 保存原始列数据快照
    originalRows.clear();
    for(int i = 0; i < columnsModel.getRowCount(); i++)
    {
      Object[] row = new Object[columnsModel.getColumnCount()];
      for(int c = 0; c < row.length; c++)
        row[c] = columnsModel.getValueAt(i, c);
      originalRows.add(row);
    }
    // 索引
    indexModel.setRowCount(0);
    for(String[] row : data.indexes)
      indexModel.addRow(row);
    // 外键
    fkModel.setRowCount(0);
    for(String[] row : data.foreignKeys)
      fkModel.addRow(row);
    // 唯一
    uniqueModel.setRowCount(0);
    for(String[] row : data.uniqueKeys)
      uniqueModel.addRow(row);
    // 检查
    checkModel.setRowCount(0);
    for(String[] row : data.checks)
      checkModel.addRow(row);
    // 排除
    excludeModel.setRowCount(0);
    for(String[] row : data.exclusions)
      excludeModel.addRow(row);
    // 规则
    ruleModel.setRowCount(0);
    for(String[] row : data.rules)
      ruleModel.addRow(row);
    // 触发器
    triggerModel.setRowCount(0);
    for(String[] row : data.triggers)
      triggerModel.addRow(row);
    // 选项
    optionsArea.setText(data.options);
    // 注释
    commentArea.setText(data.comment != null ? data.comment : "");
    // SQL预览
    sqlPreviewArea.setText(data.ddl);
    // 自适应所有只读表格列宽
    for(int i = 1; i <= 7; i++)
    {
      Component comp = innerTabs.getComponentAt(i);
      if(comp instanceof JScrollPane sp && sp.getViewport().getView() instanceof JTable t)
        autoFitColumns(t);
    }
    dirty = false;
  }

  private void onAddColumn()
  {
    int rowCount = columnsModel.getRowCount();
    columnsModel.addRow(new Object[] { rowCount + 1, "new_column", "varchar", "255", "", true, false, "", "", "" });
    columnsTable.setRowSelectionInterval(rowCount, rowCount);
    columnsTable.scrollRectToVisible(columnsTable.getCellRect(rowCount, 0, true));
  }

  private void onDeleteColumn()
  {
    int row = columnsTable.getSelectedRow();
    if(row < 0)
    {
      JOptionPane.showMessageDialog(this, "请先选中要删除的字段", "提示", JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    String colName = String.valueOf(columnsModel.getValueAt(row, 1));
    int confirm =
        JOptionPane.showConfirmDialog(this, "确定删除字段 \"" + colName + "\"？\n保存后将执行 ALTER TABLE DROP COLUMN", "删除字段",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    if(confirm == JOptionPane.YES_OPTION)
    {
      columnsModel.removeRow(row);
      // 重新编号
      for(int i = 0; i < columnsModel.getRowCount(); i++)
        columnsModel.setValueAt(i + 1, i, 0);
    }
  }

  private void onSave()
  {
    // 先提交正在编辑的单元格
    if(columnsTable.isEditing())
    {
      columnsTable.getCellEditor().stopCellEditing();
    }
      if(!dirty)
      {
        statusBar.setMessage("无变更需要保存");
        return;
      }
    // 生成并展示 ALTER SQL 让用户确认
    String alterSql = generateAlterSql();
    if(alterSql == null || alterSql.isBlank())
    {
      statusBar.setMessage("未检测到需要执行的变更");
      return;
    }
    int confirm =
        JOptionPane.showConfirmDialog(this, "将执行以下 SQL:\n\n" + alterSql + "\n\n确认执行吗?", "保存表结构", JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
    if(confirm != JOptionPane.YES_OPTION)
      return;
    try
    {
      try (Connection conn = ConnectionManager.get())
      {
        try (Statement stmt = conn.createStatement())
        {
          for(String sql : alterSql.split(";"))
          {
            String trimmed = sql.trim();
            if(!trimmed.isEmpty() && !trimmed.startsWith("--"))
              stmt.execute(trimmed);
          }
          conn.commit();
        }
      }
      dirty = false;
      updateTabTitle();
      statusBar.setMessage("表结构已保存: " + tableName);
      // 重新加载
      loadAllAsync();
    }
    catch(Exception ex)
    {
      statusBar.setMessage("保存失败: " + ex.getMessage());
      JOptionPane.showMessageDialog(this, "执行 ALTER TABLE 失败:\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * 根据当前编辑表中的数据与原始数据对比，生成 ALTER TABLE SQL
   */
  private String generateAlterSql()
  {
    StringBuilder sb = new StringBuilder();
    String qTable = tableName;
    // 构建原始列名 -> 原始行数据的映射
    Map<String, Object[]> origMap = new LinkedHashMap<>();
    for(Object[] row : originalRows)
    {
      String origName = str(row[1]);
      origMap.put(origName, row);
    }
    // 当前列名集合
    Set<String> currentNames = new LinkedHashSet<>();
    for(int i = 0; i < columnsModel.getRowCount(); i++)
    {
      currentNames.add(str(columnsModel.getValueAt(i, 1)));
    }
    // 1. 处理已删除的列（原始有、当前无）
    for(String origName : origMap.keySet())
    {
      if(!currentNames.contains(origName))
      {
        sb.append("ALTER TABLE ").append(qTable).append(" DROP COLUMN ").append(quote(origName)).append(";\n");
      }
    }
    // 2. 处理新增和修改的列
    for(int i = 0; i < columnsModel.getRowCount(); i++)
    {
      String name = str(columnsModel.getValueAt(i, 1));
      String type = str(columnsModel.getValueAt(i, 2));
      String len = str(columnsModel.getValueAt(i, 3));
      String dec = str(columnsModel.getValueAt(i, 4));
      Object nullableObj = columnsModel.getValueAt(i, 5);
      boolean nullable = nullableObj instanceof Boolean ? (Boolean)nullableObj : "true".equalsIgnoreCase(str(nullableObj));
      String defVal = str(columnsModel.getValueAt(i, 8));
      String comment = str(columnsModel.getValueAt(i, 9));
      // 构造完整类型表达式
      String fullType = type;
      if(!len.isEmpty())
      {
        fullType += "(" + len;
        if(!dec.isEmpty())
          fullType += "," + dec;
        fullType += ")";
      }
      Object[] orig = origMap.get(name);
      if(orig == null)
      {
        // ---- 新增列 ----
        sb.append("-- 新增列 ").append(name).append("\n");
        sb.append("ALTER TABLE ").append(qTable).append(" ADD COLUMN ").append(quote(name)).append(" ").append(fullType);
        if(!nullable)
          sb.append(" NOT NULL");
        if(!defVal.isEmpty())
          sb.append(" DEFAULT ").append(validateDefault(defVal));
        sb.append(";\n");
      }
      else
      {
        // ---- 修改既有列 ----
        String origType = str(orig[2]);
        String origLen = str(orig[3]);
        String origDec = str(orig[4]);
        Object origNullObj = orig[5];
        boolean origNullable = origNullObj instanceof Boolean ? (Boolean)origNullObj : "true".equalsIgnoreCase(str(origNullObj));
        String origDef = str(orig[8]);
        String origFullType = origType;
        if(!origLen.isEmpty())
        {
          origFullType += "(" + origLen;
          if(!origDec.isEmpty())
            origFullType += "," + origDec;
          origFullType += ")";
        }
        // 类型变更
        if(!fullType.equalsIgnoreCase(origFullType))
        {
          sb.append("-- 修改列类型 ").append(name).append("\n");
          sb.append("ALTER TABLE ").append(qTable).append(" ALTER COLUMN ").append(quote(name)).append(" TYPE ").append(fullType)
              .append(";\n");
        }
        // 可空性变更
        if(nullable != origNullable)
        {
          sb.append("ALTER TABLE ").append(qTable).append(" ALTER COLUMN ").append(quote(name))
              .append(nullable ? " DROP NOT NULL" : " SET NOT NULL").append(";\n");
        }
        // 默认值变更
        if(!defVal.equals(origDef))
        {
          if(defVal.isEmpty())
          {
            sb.append("ALTER TABLE ").append(qTable).append(" ALTER COLUMN ").append(quote(name)).append(" DROP DEFAULT;\n");
          }
          else
          {
            sb.append("ALTER TABLE ").append(qTable).append(" ALTER COLUMN ").append(quote(name)).append(" SET DEFAULT ")
                .append(validateDefault(defVal)).append(";\n");
          }
        }
      }
      // 注释（无论新增还是修改）
      String origComment = orig != null ? str(orig[9]) : "";
      if(!comment.equals(origComment))
      {
        sb.append("COMMENT ON COLUMN ").append(qTable).append(".").append(quote(name)).append(" IS '").append(comment.replace("'", "''"))
            .append("';\n");
      }
    }
    return sb.toString();
  }

  /**
   * 使用 SAVEPOINT 保护的安全加载：如果查询失败，回滚到 savepoint 后返回空列表
   * 不会导致 PostgreSQL 事务进入 aborted 状态
   */
  @FunctionalInterface
  private interface SqlListSupplier
  {
    List<String[]> get() throws Exception;
  }

  @FunctionalInterface
  private interface SqlStringSupplier
  {
    String get() throws Exception;
  }

  private static List<String[]> safeLoad(Connection conn, SqlListSupplier supplier)
  {
    try
    {
      Savepoint sp = conn.setSavepoint();
      try
      {
        List<String[]> result = supplier.get();
        conn.releaseSavepoint(sp);
        return result;
      }
      catch(Exception e)
      {
        conn.rollback(sp);
        return List.of();
      }
    }
    catch(SQLException ex)
    {
      return List.of();
    }
  }

  private static String safeLoadStr(Connection conn, SqlStringSupplier supplier)
  {
    try
    {
      Savepoint sp = conn.setSavepoint();
      try
      {
        String result = supplier.get();
        conn.releaseSavepoint(sp);
        return result != null ? result : "";
      }
      catch(Exception e)
      {
        conn.rollback(sp);
        return "";
      }
    }
    catch(SQLException ex)
    {
      return "";
    }
  }

  private static List<String[]> loadIndexes(DatabaseMetaData meta, String schema, String table) throws SQLException
  {
    List<String[]> result = new ArrayList<>();
    Map<String, List<String>> indexCols = new LinkedHashMap<>();
    Map<String, String> indexType = new LinkedHashMap<>();
    Map<String, Boolean> indexUnique = new LinkedHashMap<>();

    try (ResultSet rs = meta.getIndexInfo(null, schema, table, false, false))
    {
      while(rs.next())
      {
        String idxName = rs.getString("INDEX_NAME");
        if(idxName == null)
          continue;
        String col = rs.getString("COLUMN_NAME");
        boolean nonUnique = rs.getBoolean("NON_UNIQUE");
        String type = switch(rs.getShort("TYPE"))
        {
          case DatabaseMetaData.tableIndexStatistic -> "统计";
          case DatabaseMetaData.tableIndexClustered -> "聚簇";
          case DatabaseMetaData.tableIndexHashed -> "哈希";
          default -> "B-Tree";
        };
        indexCols.computeIfAbsent(idxName, k -> new ArrayList<>()).add(col);
        indexType.put(idxName, type);
        indexUnique.put(idxName, !nonUnique);
      }
    }
    for(var entry : indexCols.entrySet())
    {
      String name = entry.getKey();
      result.add(new String[] { name, String.join(", ", entry.getValue()), indexType.getOrDefault(name, ""),
          indexUnique.getOrDefault(name, false) ? "YES" : "NO" });
    }
    return result;
  }

  private static List<String[]> loadForeignKeys(DatabaseMetaData meta, String schema, String table) throws SQLException
  {
    List<String[]> result = new ArrayList<>();
    try (ResultSet rs = meta.getImportedKeys(null, schema, table))
    {
      while(rs.next())
      {
        result.add(new String[] { rs.getString("FK_NAME"), rs.getString("FKCOLUMN_NAME"), rs.getString("PKTABLE_NAME"),
            rs.getString("PKCOLUMN_NAME"), ruleStr(rs.getInt("UPDATE_RULE")), ruleStr(rs.getInt("DELETE_RULE")) });
      }
    }
    return result;
  }

  private static String ruleStr(int rule)
  {
    return switch(rule)
    {
      case DatabaseMetaData.importedKeyCascade -> "CASCADE";
      case DatabaseMetaData.importedKeySetNull -> "SET NULL";
      case DatabaseMetaData.importedKeySetDefault -> "SET DEFAULT";
      case DatabaseMetaData.importedKeyRestrict -> "RESTRICT";
      default -> "NO ACTION";
    };
  }

  private static List<String[]> loadUniqueConstraints(Connection conn, String schema, String table, boolean isPg) throws SQLException
  {
    List<String[]> result = new ArrayList<>();
    if(isPg)
    {
      String sql = """
          SELECT con.conname, array_to_string(array_agg(a.attname ORDER BY u.ord), ', ')
          FROM pg_constraint con
          CROSS JOIN LATERAL unnest(con.conkey) WITH ORDINALITY AS u(attnum, ord)
          JOIN pg_attribute a ON a.attrelid = con.conrelid AND a.attnum = u.attnum
          JOIN pg_class c ON c.oid = con.conrelid
          JOIN pg_namespace n ON n.oid = c.relnamespace
          WHERE con.contype = 'u' AND c.relname = ? AND n.nspname = ?
          GROUP BY con.conname ORDER BY con.conname
          """;
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
        ps.setString(1, table);
        ps.setString(2, schema != null ? schema : "public");
        try (ResultSet rs = ps.executeQuery())
        {
          while(rs.next())
            result.add(new String[] { rs.getString(1), rs.getString(2) });
        }
      }
    }
    else
    {
      // MySQL: unique constraints shown as unique indexes
      DatabaseMetaData meta = conn.getMetaData();
      Map<String, List<String>> uniq = new LinkedHashMap<>();
      try (ResultSet rs = meta.getIndexInfo(null, schema, table, true, false))
      {
        while(rs.next())
        {
          String idxName = rs.getString("INDEX_NAME");
          String col = rs.getString("COLUMN_NAME");
          if(idxName != null && !"PRIMARY".equalsIgnoreCase(idxName))
            uniq.computeIfAbsent(idxName, k -> new ArrayList<>()).add(col);
        }
      }
      for(var e : uniq.entrySet())
        result.add(new String[] { e.getKey(), String.join(", ", e.getValue()) });
    }
    return result;
  }

  private static List<String[]> loadCheckConstraints(Connection conn, String schema, String table, boolean isPg) throws SQLException
  {
    List<String[]> result = new ArrayList<>();
    if(isPg)
    {
      String sql = """
          SELECT con.conname, pg_get_constraintdef(con.oid)
          FROM pg_constraint con
          JOIN pg_class c ON c.oid = con.conrelid
          JOIN pg_namespace n ON n.oid = c.relnamespace
          WHERE con.contype = 'c' AND c.relname = ? AND n.nspname = ?
          ORDER BY con.conname
          """;
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
        ps.setString(1, table);
        ps.setString(2, schema != null ? schema : "public");
        try (ResultSet rs = ps.executeQuery())
        {
          while(rs.next())
            result.add(new String[] { rs.getString(1), rs.getString(2) });
        }
      }
    }
    else
    {
      String sql = "SELECT CONSTRAINT_NAME, CHECK_CLAUSE FROM information_schema.CHECK_CONSTRAINTS "
          + "WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = ?";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
        ps.setString(1, table);
        try (ResultSet rs = ps.executeQuery())
        {
          while(rs.next())
            result.add(new String[] { rs.getString(1), rs.getString(2) });
        }
      }
      catch(SQLException ignored)
      {
      } // older MySQL
    }
    return result;
  }

  private static List<String[]> loadExclusionConstraints(Connection conn, String schema, String table) throws SQLException
  {
    List<String[]> result = new ArrayList<>();
    String sql = """
        SELECT con.conname, pg_get_constraintdef(con.oid)
        FROM pg_constraint con
        JOIN pg_class c ON c.oid = con.conrelid
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE con.contype = 'x' AND c.relname = ? AND n.nspname = ?
        ORDER BY con.conname
        """;
    try (PreparedStatement ps = conn.prepareStatement(sql))
    {
      ps.setString(1, table);
      ps.setString(2, schema != null ? schema : "public");
      try (ResultSet rs = ps.executeQuery())
      {
        while(rs.next())
          result.add(new String[] { rs.getString(1), rs.getString(2) });
      }
    }
    return result;
  }

  private static List<String[]> loadRules(Connection conn, String schema, String table) throws SQLException
  {
    List<String[]> result = new ArrayList<>();
    String sql = """
        SELECT r.rulename,
               CASE r.ev_type
                 WHEN '1' THEN 'SELECT'
                 WHEN '2' THEN 'UPDATE'
                 WHEN '3' THEN 'INSERT'
                 WHEN '4' THEN 'DELETE'
                 ELSE r.ev_type::text
               END AS event,
               pg_get_ruledef(r.oid) AS definition
        FROM pg_rewrite r
        JOIN pg_class c ON c.oid = r.ev_class
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = ? AND n.nspname = ?
          AND r.rulename <> '_RETURN'
        ORDER BY r.rulename
        """;
    try (PreparedStatement ps = conn.prepareStatement(sql))
    {
      ps.setString(1, table);
      ps.setString(2, schema != null ? schema : "public");
      try (ResultSet rs = ps.executeQuery())
      {
        while(rs.next())
          result.add(new String[] { rs.getString(1), rs.getString(2), rs.getString(3) });
      }
    }
    catch(SQLException e)
    {
      logger.debug("加载规则失败（仅 PostgreSQL 支持）", e);
    }
    return result;
  }

  private static List<String[]> loadTriggers(Connection conn, String schema, String table, boolean isPg) throws SQLException
  {
    List<String[]> result = new ArrayList<>();
    if(isPg)
    {
      String sql = "SELECT trigger_name, event_manipulation, action_timing, action_orientation, event_object_table "
          + "FROM information_schema.triggers WHERE trigger_schema = ? AND event_object_table = ? " + "ORDER BY trigger_name";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
        ps.setString(1, schema != null ? schema : "public");
        ps.setString(2, table);
        try (ResultSet rs = ps.executeQuery())
        {
          while(rs.next())
            result.add(new String[] { rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5) });
        }
      }
    }
    else
    {
      String sql = "SELECT trigger_name, event_manipulation, action_timing, action_orientation, event_object_table "
          + "FROM information_schema.triggers WHERE trigger_schema = DATABASE() AND event_object_table = ? " + "ORDER BY trigger_name";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
        ps.setString(1, table);
        try (ResultSet rs = ps.executeQuery())
        {
          while(rs.next())
            result.add(new String[] { rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5) });
        }
      }
    }
    return result;
  }

  private static String loadTableOptions(Connection conn, String schema, String table, boolean isPg) throws SQLException
  {
    StringBuilder sb = new StringBuilder();
    if(isPg)
    {
      String sql = """
          SELECT c.relkind, c.relpersistence,
                 pg_size_pretty(pg_total_relation_size(c.oid)) AS total_size,
                 pg_size_pretty(pg_relation_size(c.oid)) AS table_size,
                 pg_size_pretty(pg_indexes_size(c.oid)) AS index_size,
                 c.reltuples::bigint AS est_rows,
                 c.reloptions
          FROM pg_class c
          JOIN pg_namespace n ON n.oid = c.relnamespace
          WHERE c.relname = ? AND n.nspname = ?
          """;
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
        ps.setString(1, table);
        ps.setString(2, schema != null ? schema : "public");
        try (ResultSet rs = ps.executeQuery())
        {
          if(rs.next())
          {
            sb.append("类型: ").append(kindStr(rs.getString(1))).append("\n");
            sb.append("持久性: ").append(persistStr(rs.getString(2))).append("\n");
            sb.append("总大小: ").append(rs.getString(3)).append("\n");
            sb.append("表大小: ").append(rs.getString(4)).append("\n");
            sb.append("索引大小: ").append(rs.getString(5)).append("\n");
            sb.append("估计行数: ").append(rs.getLong(6)).append("\n");
            String opts = rs.getString(7);
            if(opts != null)
              sb.append("存储选项: ").append(opts).append("\n");
          }
        }
      }
    }
    else
    {
      String sql = "SELECT ENGINE, TABLE_COLLATION, TABLE_ROWS, DATA_LENGTH, INDEX_LENGTH, AUTO_INCREMENT "
          + "FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
        ps.setString(1, table);
        try (ResultSet rs = ps.executeQuery())
        {
          if(rs.next())
          {
            sb.append("引擎: ").append(rs.getString(1)).append("\n");
            sb.append("排序规则: ").append(rs.getString(2)).append("\n");
            sb.append("估计行数: ").append(rs.getLong(3)).append("\n");
            sb.append("数据大小: ").append(rs.getLong(4)).append(" bytes\n");
            sb.append("索引大小: ").append(rs.getLong(5)).append(" bytes\n");
            long ai = rs.getLong(6);
            if(!rs.wasNull())
              sb.append("自增值: ").append(ai).append("\n");
          }
        }
      }
    }
    return sb.toString();
  }

  private static String loadTableComment(Connection conn, String schema, String table, boolean isPg) throws SQLException
  {
    if(isPg)
    {
      String sql = """
          SELECT obj_description(c.oid)
          FROM pg_class c
          JOIN pg_namespace n ON n.oid = c.relnamespace
          WHERE c.relname = ? AND n.nspname = ?
          """;
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
        ps.setString(1, table);
        ps.setString(2, schema != null ? schema : "public");
        try (ResultSet rs = ps.executeQuery())
        {
          return rs.next() ? rs.getString(1) : "";
        }
      }
    }
    else
    {
      String sql = "SELECT TABLE_COMMENT FROM information_schema.TABLES " + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
        ps.setString(1, table);
        try (ResultSet rs = ps.executeQuery())
        {
          return rs.next() ? rs.getString(1) : "";
        }
      }
    }
  }

  private static String generateCreateTableDDL(Connection conn, String schema, String table, boolean isPg, List<ColumnInfo> columns)
  {
    StringBuilder sb = new StringBuilder();
    String qTable = (schema != null ? schema + "." : "") + table;
    sb.append("CREATE TABLE ").append(qTable).append(" (\n");

    List<String> pkCols = new ArrayList<>();
    for(int i = 0; i < columns.size(); i++)
    {
      ColumnInfo c = columns.get(i);
      sb.append("  ").append(c.name).append(" ").append(c.typeName);
      if(c.columnSize > 0)
      {
        String tn = c.typeName.toLowerCase(Locale.ROOT);
        if(!tn.contains("int") && !tn.equals("serial") && !tn.equals("bigserial") && !tn.equals("boolean") && !tn.equals("bool")
            && !tn.equals("date") && !tn.equals("text") && !tn.equals("bytea") && !tn.contains("timestamp") && !tn.equals("uuid")
            && !tn.equals("json") && !tn.equals("jsonb"))
        {
          sb.append("(").append(c.columnSize);
          if(c.decimalDigits > 0)
            sb.append(",").append(c.decimalDigits);
          sb.append(")");
        }
      }
      if(!c.nullable)
        sb.append(" NOT NULL");
      if(c.defaultValue != null && !c.defaultValue.isEmpty())
        sb.append(" DEFAULT ").append(validateDefault(c.defaultValue));
      if(c.pk)
        pkCols.add(c.name);
      if(i < columns.size() - 1 || !pkCols.isEmpty())
        sb.append(",");
      if(c.remarks != null && !c.remarks.isEmpty())
        sb.append(" -- ").append(c.remarks);
      sb.append("\n");
    }
    if(!pkCols.isEmpty())
      sb.append("  PRIMARY KEY (").append(String.join(", ", pkCols)).append(")\n");
    sb.append(");\n");

    // 列注释
    for(ColumnInfo c : columns)
    {
      if(c.remarks != null && !c.remarks.isEmpty())
      {
        if(isPg)
          sb.append("COMMENT ON COLUMN ").append(qTable).append(".").append(c.name).append(" IS '").append(c.remarks.replace("'", "''"))
              .append("';\n");
      }
    }
    return sb.toString();
  }

  private static String kindStr(String k)
  {
    if(k == null)
      return "";
    return switch(k)
    {
      case "r" -> "普通表";
      case "i" -> "索引";
      case "S" -> "序列";
      case "v" -> "视图";
      case "m" -> "物化视图";
      case "c" -> "复合类型";
      case "t" -> "TOAST表";
      case "f" -> "外部表";
      case "p" -> "分区表";
      default -> k;
    };
  }

  private static String persistStr(String p)
  {
    if(p == null)
      return "";
    return switch(p)
    {
      case "p" -> "永久";
      case "u" -> "无日志";
      case "t" -> "临时";
      default -> p;
    };
  }

  private static String stripSchema(String qualifiedName)
  {
    if(qualifiedName == null)
      return "";
    int dot = qualifiedName.indexOf('.');
    return dot >= 0 ? qualifiedName.substring(dot + 1) : qualifiedName;
  }

  private static String extractSchema(String qualifiedName)
  {
    if(qualifiedName == null)
      return null;
    int dot = qualifiedName.indexOf('.');
    return dot >= 0 ? qualifiedName.substring(0, dot) : null;
  }

  private static String quote(String name)
  {
    if(name == null)
      return "\"\"";
    return "\"" + name.replace("\"", "\"\"") + "\"";
  }

  /**
   * 校验默认值表达式：只允许单表达式，禁止多语句/注释注入
   */
  private static String validateDefault(String val)
  {
    if(val == null || val.isEmpty())
      return val;
    String trimmed = val.trim();
    if(trimmed.contains(";") || trimmed.contains("--") || trimmed.contains("/*"))
      throw new IllegalArgumentException("默认值包含非法字符: " + trimmed);
    return trimmed;
  }

  private static String str(Object obj)
  {
    return obj != null ? obj.toString().trim() : "";
  }

  private void updateTabTitle()
  {
    Container p = getParent();
    if(!(p instanceof JTabbedPane tabs))
      return;
    int idx = tabs.indexOfComponent(this);
    if(idx >= 0)
    {
      String base = "Design: " + stripSchema(tableName);
      tabs.setTitleAt(idx, base + (dirty ? " *" : ""));
    }
  }

  /**
   * 自适应列宽
   */
  private static void autoFitColumns(JTable table)
  {
    TableModel m = table.getModel();
    TableColumnModel cm = table.getColumnModel();
    FontMetrics headerFm = table.getTableHeader().getFontMetrics(table.getTableHeader().getFont());
    FontMetrics cellFm = table.getFontMetrics(table.getFont());
    int padding = 20;
    int maxWidth = 400;
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
          width = Math.max(width, cellFm.stringWidth(val.toString()) + padding);
      }
      tc.setPreferredWidth(Math.min(width, maxWidth));
    }
  }

  private static class DesignData
  {
    List<ColumnInfo> columns = List.of();
    List<String[]> indexes = List.of();
    List<String[]> foreignKeys = List.of();
    List<String[]> uniqueKeys = List.of();
    List<String[]> checks = List.of();
    List<String[]> exclusions = List.of();
    List<String[]> rules = List.of();
    List<String[]> triggers = List.of();
    String options = "";
    String comment = "";
    String ddl = "";
  }
}


