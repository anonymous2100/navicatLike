package com.ctgu.lightdbviewer.ui.table;

import com.ctgu.lightdbviewer.jdbc.ConnectionManager;
import com.ctgu.lightdbviewer.util.SqlValueUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class DataGridPanel extends JScrollPane
{
  private final JTable table;
  private final RowStateRenderer rowStateRenderer;
  private String tableName = "";

  public DataGridPanel(EditableResultTableModel model)
  {
    table = new JTable(model);
    table.setRowSorter(new TableRowSorter<>(model));
    // 关闭自动缩放，让每列按实际内容宽度展示，支持水平滚动
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    // ── Navicat 风格行状态渲染器（绿/黄/红 + NULL 灰斜体 + PK 粗体）
    rowStateRenderer = new RowStateRenderer(model);
    table.setDefaultRenderer(Object.class, rowStateRenderer);

    // ── 列头变更指示器（行有未提交修改时显示橙色下划线）
    table.getTableHeader().setDefaultRenderer(new ChangeAwareHeaderRenderer(table));

    setViewportView(table);

    // 当模型数据变化时自动调整列宽
    model.addTableModelListener(e -> SwingUtilities.invokeLater(this::autoFitColumnWidths));

    installContextMenu();
  }

  /**
   * 设置当前表名（用于生成 INSERT SQL）
   */
  public void setTableName(String name)
  {
    this.tableName = name != null ? name : "";
  }

  public int getSelectedRow()
  {
    int viewRow = table.getSelectedRow();
    return viewRow < 0 ? -1 : table.convertRowIndexToModel(viewRow);
  }

  public JTable getTable()
  {
    return table;
  }

  private void installContextMenu()
  {
    JPopupMenu popup = new JPopupMenu();
    JMenuItem copyInsertSql = new JMenuItem("复制为 INSERT SQL");
    JMenuItem copyUpdateSql = new JMenuItem("复制为 UPDATE SQL");
    JMenuItem copyRowData = new JMenuItem("复制行数据（制表符分隔）");
    copyInsertSql.addActionListener(e -> copyRowAsInsertSql());
    copyUpdateSql.addActionListener(e -> copyRowAsUpdateSql());
    copyRowData.addActionListener(e -> copyRowData());
    popup.add(copyInsertSql);
    popup.add(copyUpdateSql);
    popup.addSeparator();
    popup.add(copyRowData);

    table.addMouseListener(new MouseAdapter()
    {
      @Override
      public void mousePressed(MouseEvent e)
      {
        maybeShowPopup(e);
      }

      @Override
      public void mouseReleased(MouseEvent e)
      {
        maybeShowPopup(e);
      }

      private void maybeShowPopup(MouseEvent e)
      {
        if(!e.isPopupTrigger())
          return;
        int row = table.rowAtPoint(e.getPoint());
        if(row >= 0 && table.getSelectedRow() != row)
        {
          table.setRowSelectionInterval(row, row);
        }
        boolean hasSelection = table.getSelectedRow() >= 0;
        copyInsertSql.setEnabled(hasSelection);
        copyUpdateSql.setEnabled(hasSelection);
        copyRowData.setEnabled(hasSelection);
        popup.show(table, e.getX(), e.getY());
      }
    });
  }

  private void copyRowAsInsertSql()
  {
    int modelRow = getSelectedRow();
    if(modelRow < 0) {
      return;
    }
    TableModel m = table.getModel();
    int cols = m.getColumnCount();

    StringBuilder sb = new StringBuilder("INSERT INTO ");
    sb.append(quoteId(tableName)).append(" (");
    for(int i = 0; i < cols; i++)
    {
      if(i > 0) {
        sb.append(", ");
      }
      sb.append(quoteId(m.getColumnName(i)));
    }
    sb.append(")\nVALUES (");
    for(int i = 0; i < cols; i++)
    {
      if(i > 0) {
        sb.append(", ");
      }
      sb.append(SqlValueUtil.toSqlLiteral(m.getValueAt(modelRow, i)));
    }
    sb.append(");");

    copyToClipboard(sb.toString());
    JOptionPane.showMessageDialog(this, "INSERT SQL 已复制到剪贴板", "复制成功", JOptionPane.INFORMATION_MESSAGE);
  }

  private void copyRowAsUpdateSql()
  {
    int modelRow = getSelectedRow();
    if(modelRow < 0) {
      return;
    }
    TableModel m = table.getModel();
    int cols = m.getColumnCount();

    // 第一列默认视为主键（回退方案）
    StringBuilder sb = new StringBuilder("UPDATE ");
    sb.append(quoteId(tableName)).append(" SET\n");
    for(int i = 1; i < cols; i++)
    {
      if(i > 1) {
        sb.append(",\n");
      }
      sb.append("  ").append(quoteId(m.getColumnName(i))).append(" = ").append(SqlValueUtil.toSqlLiteral(m.getValueAt(modelRow, i)));
    }
    sb.append("\nWHERE ");
    sb.append(quoteId(m.getColumnName(0))).append(" = ").append(SqlValueUtil.toSqlLiteral(m.getValueAt(modelRow, 0))).append(";");

    copyToClipboard(sb.toString());
    JOptionPane.showMessageDialog(this, "UPDATE SQL 已复制到剪贴板", "复制成功", JOptionPane.INFORMATION_MESSAGE);
  }

  private void copyRowData()
  {
    int modelRow = getSelectedRow();
    if(modelRow < 0) {
      return;
    }
    TableModel m = table.getModel();
    StringBuilder sb = new StringBuilder();
    for(int i = 0; i < m.getColumnCount(); i++)
    {
      if(i > 0) {
        sb.append('\t');
      }
      Object val = m.getValueAt(modelRow, i);
      sb.append(val != null ? val : "");
    }
    copyToClipboard(sb.toString());
  }

  /**
   * 根据当前连接的数据库类型选择标识符引号
   */
  private String quoteId(String name)
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

  private void copyToClipboard(String text)
  {
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
  }

  /**
   * 设置主键列名列表，渲染器将对这些 model 列显示粗体。
   * 应在模型装载完列信息后调用。
   */
  public void setPkColumns(List<String> pkColumns)
  {
    if(pkColumns == null || pkColumns.isEmpty())
    {
      rowStateRenderer.setPkColumnIndices(new HashSet<>());
      table.repaint();
      return;
    }
    TableModel m = table.getModel();
    Set<Integer> indices = new HashSet<>();
    for(int i = 0; i < m.getColumnCount(); i++)
    {
      if(pkColumns.contains(m.getColumnName(i))) {
        indices.add(i);
      }
    }
    rowStateRenderer.setPkColumnIndices(indices);
    table.repaint();
  }

  /**
   * 根据表头文字和前若干行数据内容自动调整每列宽度。
   * - 最小宽度 = 表头文字宽度 + padding
   * - 采样前 50 行取最大值
   * - 上限 400px 防止单列太宽
   */
  public void autoFitColumnWidths()
  {
    TableModel m = table.getModel();
    TableColumnModel cm = table.getColumnModel();
    FontMetrics headerFm = table.getTableHeader().getFontMetrics(table.getTableHeader().getFont());
    FontMetrics cellFm = table.getFontMetrics(table.getFont());
    int padding = 16; // 左右各 8px 内边距
    int maxWidth = 400;
    int sampleRows = Math.min(m.getRowCount(), 50);

    for(int col = 0; col < cm.getColumnCount(); col++)
    {
      TableColumn tc = cm.getColumn(col);
      // 表头宽度
      String headerVal = String.valueOf(m.getColumnName(col));
      int width = headerFm.stringWidth(headerVal) + padding;
      // 采样数据行取最大宽度
      for(int row = 0; row < sampleRows; row++)
      {
        Object val = m.getValueAt(row, col);
        if(val != null)
        {
          int cellWidth = cellFm.stringWidth(val.toString()) + padding;
          width = Math.max(width, cellWidth);
        }
      }
      width = Math.min(width, maxWidth);
      tc.setPreferredWidth(width);
    }
  }

  private static class ChangeAwareHeaderRenderer extends DefaultTableCellRenderer
  {
    private final JTable table;

    ChangeAwareHeaderRenderer(JTable table)
    {
      this.table = table;
    }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col)
    {
      super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
      if(table.getModel() instanceof EditableResultTableModel model && model.isDirty())
      {
        setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(255, 180, 0)));
      }
      return this;
    }
  }
}