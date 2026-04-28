package com.ctgu.lightdbviewer.ui.sql;

import com.ctgu.lightdbviewer.ui.common.UiConstants;
import com.ctgu.lightdbviewer.util.FontManager;
import com.ctgu.lightdbviewer.util.SqlValueUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * SqlResultPanel
 * <p>
 * QueryTab 中用于展SQL 执行结果
 * - SELECT JTable（只读）
 * - DML Message
 * - ERROR Error Message
 * <p>
 * 设计原则
 * TableDataTab 完全隔离
 * 不可编辑
 * 结果覆盖（无历史堆积
 */
/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class SqlResultPanel extends JPanel
{
  private final CardLayout layout = new CardLayout();
  private final JPanel emptyPanel = new JPanel();
  private final JPanel tablePanel = new JPanel(new BorderLayout());
  private final JPanel messagePanel = new JPanel(new BorderLayout());
  private final JTable table = new JTable();
  private final JTextArea messageArea = new JTextArea();
  private final JButton prevPageButton = new JButton("上一页");
  private final JButton nextPageButton = new JButton("下一页");
  private final JLabel pageInfoLabel = new JLabel("第 1 页");
  private final JLabel guardLabel = new JLabel();

  public SqlResultPanel()
  {
    setLayout(layout);
    initEmpty();
    initTable();
    initMessage();
    add(emptyPanel, "EMPTY");
    add(tablePanel, "TABLE");
    add(messagePanel, "MESSAGE");
    layout.show(this, "EMPTY");
  }

  private void initEmpty()
  {
    emptyPanel.setLayout(new GridBagLayout());
    JLabel label = new JLabel("暂无结果");
    label.setForeground(Color.GRAY);
    emptyPanel.add(label);
  }

  private void initTable()
  {
    table.setFillsViewportHeight(true);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    Font uiFont = FontManager.getCurrentFont();
    table.setFont(uiFont);
    table.getTableHeader().setFont(uiFont.deriveFont(Font.BOLD));
    table.setRowHeight(uiFont.getSize() + 6);
    // 不再 setEnabled(false)：disabled 状态会吃掉鼠标事件导致右键菜单无法触发
    // 只读DefaultTableModel.isCellEditable 返回 false 保证
    table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

    // 支持多行选择
    table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    installContextMenu();

    JPanel pager = new JPanel(new BorderLayout(8, 0));
    JPanel pagerButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    pagerButtons.add(prevPageButton);
    pagerButtons.add(nextPageButton);
    pagerButtons.add(pageInfoLabel);

    guardLabel.setForeground(UiConstants.COLOR_ERROR_TEXT);

    prevPageButton.setEnabled(false);
    nextPageButton.setEnabled(false);

    pager.add(pagerButtons, BorderLayout.WEST);
    pager.add(guardLabel, BorderLayout.EAST);

    tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
    tablePanel.add(pager, BorderLayout.SOUTH);
  }

  private void initMessage()
  {
    messageArea.setEditable(false);
    messageArea.setFont(FontManager.getCurrentEditorFont());
    messageArea.setOpaque(false);
    messageArea.setLineWrap(true);
    messageArea.setWrapStyleWord(true);
    messageArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

    messagePanel.add(messageArea, BorderLayout.CENTER);
  }

  private void installContextMenu()
  {
    JPopupMenu popup = new JPopupMenu();

    JMenuItem copyInsert = new JMenuItem("复制选中行为 INSERT SQL");
    JMenuItem copyUpdate = new JMenuItem("复制选中行为 UPDATE SQL（首列为主键）");
    JMenuItem copyCsv = new JMenuItem("复制选中行为 CSV");
    JMenuItem copyTab = new JMenuItem("复制选中行（制表符分隔）");
    JMenuItem selectAll = new JMenuItem("全选");

    copyInsert.addActionListener(e -> copySelectedAsInsertSql());
    copyUpdate.addActionListener(e -> copySelectedAsUpdateSql());
    copyCsv.addActionListener(e -> copySelectedAsCsv());
    copyTab.addActionListener(e -> copySelectedAsTabSeparated());
    selectAll.addActionListener(e -> table.selectAll());

    popup.add(copyInsert);
    popup.add(copyUpdate);
    popup.addSeparator();
    popup.add(copyCsv);
    popup.add(copyTab);
    popup.addSeparator();
    popup.add(selectAll);

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
        // 右键点中某行但不在已有选区内时，单独选中该行
        int row = table.rowAtPoint(e.getPoint());
        if(row >= 0 && !table.isRowSelected(row))
          table.setRowSelectionInterval(row, row);

        boolean hasSel = table.getSelectedRowCount() > 0;
        copyInsert.setEnabled(hasSel);
        copyUpdate.setEnabled(hasSel);
        copyCsv.setEnabled(hasSel);
        copyTab.setEnabled(hasSel);

        // 更新菜单标题显示选中行数
        int cnt = table.getSelectedRowCount();
        String suffix = cnt > 1 ? "（" + cnt + " 行）" : "";
        copyInsert.setText("复制选中行为 INSERT SQL" + suffix);
        copyUpdate.setText("复制选中行为 UPDATE SQL" + suffix + "（首列为主键）");
        copyCsv.setText("复制选中行为 CSV" + suffix);
        copyTab.setText("复制选中行（制表符分隔）" + suffix);

        popup.show(table, e.getX(), e.getY());
      }
    });
  }

  private void copySelectedAsInsertSql()
  {
    int[] viewRows = table.getSelectedRows();
    if(viewRows.length == 0)
    {
      return;
    }
    TableModel m = table.getModel();
    int cols = m.getColumnCount();
    StringBuilder colPart = new StringBuilder();
    for(int c = 0; c < cols; c++)
    {
      if(c > 0)
      {
        colPart.append(", ");
      }
      colPart.append(quoteId(m.getColumnName(c)));
    }
    StringBuilder sb = new StringBuilder();
    for(int vr : viewRows)
    {
      int mr = table.convertRowIndexToModel(vr);
      sb.append("INSERT INTO <table_name> (").append(colPart).append(")\nVALUES (");
      for(int c = 0; c < cols; c++)
      {
        if(c > 0)
        {
          sb.append(", ");
        }
        sb.append(SqlValueUtil.toSqlLiteral(m.getValueAt(mr, c)));
      }
      sb.append(");\n");
    }
    copyToClipboard(sb.toString().trim());
    showCopied("INSERT SQL（" + viewRows.length + " 行）已复制到剪贴板");
  }

  private void copySelectedAsUpdateSql()
  {
    int[] viewRows = table.getSelectedRows();
    if(viewRows.length == 0)
    {
      return;
    }
    TableModel m = table.getModel();
    int cols = m.getColumnCount();
    StringBuilder sb = new StringBuilder();
    for(int vr : viewRows)
    {
      int mr = table.convertRowIndexToModel(vr);
      sb.append("UPDATE <table_name> SET\n");
      for(int c = 1; c < cols; c++)
      {
        if(c > 1)
        {
          sb.append(",\n");
        }
        sb.append("  ").append(quoteId(m.getColumnName(c))).append(" = ").append(SqlValueUtil.toSqlLiteral(m.getValueAt(mr, c)));
      }
      sb.append("\nWHERE ").append(quoteId(m.getColumnName(0))).append(" = ").append(SqlValueUtil.toSqlLiteral(m.getValueAt(mr, 0)))
          .append(";\n\n");
    }
    copyToClipboard(sb.toString().trim());
    showCopied("UPDATE SQL（" + viewRows.length + " 行）已复制到剪贴板");
  }

  private void copySelectedAsCsv()
  {
    int[] viewRows = table.getSelectedRows();
    if(viewRows.length == 0)
    {
      return;
    }
    TableModel m = table.getModel();
    int cols = m.getColumnCount();
    StringBuilder sb = new StringBuilder();
    // 表头
    for(int c = 0; c < cols; c++)
    {
      if(c > 0)
      {
        sb.append(',');
      }
      sb.append(csvEscape(m.getColumnName(c)));
    }
    sb.append('\n');
    // 数据
    for(int vr : viewRows)
    {
      int mr = table.convertRowIndexToModel(vr);
      for(int c = 0; c < cols; c++)
      {
        if(c > 0)
        {
          sb.append(',');
        }
        Object val = m.getValueAt(mr, c);
        sb.append(csvEscape(val != null ? val.toString() : ""));
      }
      sb.append('\n');
    }
    copyToClipboard(sb.toString());
    showCopied("CSV（" + viewRows.length + " 行）已复制到剪贴板");
  }

  private void copySelectedAsTabSeparated()
  {
    int[] viewRows = table.getSelectedRows();
    if(viewRows.length == 0)
    {
      return;
    }
    TableModel m = table.getModel();
    int cols = m.getColumnCount();
    StringBuilder sb = new StringBuilder();
    for(int vr : viewRows)
    {
      int mr = table.convertRowIndexToModel(vr);
      for(int c = 0; c < cols; c++)
      {
        if(c > 0)
        {
          sb.append('\t');
        }
        Object val = m.getValueAt(mr, c);
        sb.append(val != null ? val : "");
      }
      sb.append('\n');
    }
    copyToClipboard(sb.toString());
    showCopied("制表符数据（" + viewRows.length + " 行）已复制到剪贴板");
  }

  /**
   * 用双引号引用标识符（标准 SQL，兼容 MySQL / PostgreSQL）
   */
  private static String quoteId(String name)
  {
    if(name == null || name.isBlank())
    {
      return name;
    }
    return "\"" + name.replace("\"", "\"\"") + "\"";
  }

  private static String csvEscape(String val)
  {
    if(val == null)
    {
      return "";
    }
    if(val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r"))
    {
      return "\"" + val.replace("\"", "\"\"") + "\"";
    }
    return val;
  }

  private void copyToClipboard(String text)
  {
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
  }

  private void showCopied(String msg)
  {
    JOptionPane.showMessageDialog(this, msg, "复制成功", JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * 显示 SELECT 查询结果
   */
  public void showResultSet(DefaultTableModel model, int pageIndex, int pageSize, boolean hasNextPage, boolean truncated, int maxRows)
  {
    Font uiFont = FontManager.getCurrentFont();
    table.setFont(uiFont);
    table.getTableHeader().setFont(uiFont.deriveFont(Font.BOLD));
    table.setRowHeight(uiFont.getSize() + 6);
    table.setModel(model);
    autoFitColumnWidths();
    prevPageButton.setEnabled(pageIndex > 0);
    nextPageButton.setEnabled(hasNextPage && !truncated);
    pageInfoLabel.setText("第 " + (pageIndex + 1) + " 页（本页 " + model.getRowCount() + " 行）");
    guardLabel.setText(truncated ? "已达最大行数限制 " + maxRows : "");
    layout.show(this, "TABLE");
  }

  /**
   * 显示 DML 影响行数
   */
  public void showUpdateCount(int count, long elapsedMs)
  {
    messageArea.setForeground(Color.BLACK);
    messageArea.setText(String.format("执行成功，影响 %d 行（耗时 %d ms）", count, elapsedMs));
    layout.show(this, "MESSAGE");
    guardLabel.setText("");
  }

  /**
   * 显示错误
   */
  public void showError(String title, Exception e)
  {
    messageArea.setForeground(UiConstants.COLOR_ERROR_TEXT);
    messageArea.setText(title + ":\n\n" + e.getMessage());
    layout.show(this, "MESSAGE");
    guardLabel.setText("");
  }

  public void showMessage(String message)
  {
    messageArea.setForeground(Color.BLACK);
    messageArea.setText(message);
    layout.show(this, "MESSAGE");
    guardLabel.setText("");
  }

  /**
   * 清空结果
   */
  public void clear()
  {
    table.setModel(new DefaultTableModel());
    messageArea.setText("");
    prevPageButton.setEnabled(false);
    nextPageButton.setEnabled(false);
    pageInfoLabel.setText("第 1 页");
    guardLabel.setText("");
    layout.show(this, "EMPTY");
  }

  public void setPageActions(Runnable prevAction, Runnable nextAction)
  {
    for(java.awt.event.ActionListener l : prevPageButton.getActionListeners())
      prevPageButton.removeActionListener(l);
    for(java.awt.event.ActionListener l : nextPageButton.getActionListeners())
      nextPageButton.removeActionListener(l);
    prevPageButton.addActionListener(e -> prevAction.run());
    nextPageButton.addActionListener(e -> nextAction.run());
  }

  /**
   * 根据表头文字和数据内容自动调整列
   */
  private void autoFitColumnWidths()
  {
    TableModel m = table.getModel();
    TableColumnModel cm = table.getColumnModel();
    FontMetrics headerFm = table.getTableHeader().getFontMetrics(table.getTableHeader().getFont());
    FontMetrics cellFm = table.getFontMetrics(table.getFont());
    int padding = 16;
    int maxWidth = 400;
    int sampleRows = Math.min(m.getRowCount(), 50);
    for(int col = 0; col < cm.getColumnCount(); col++)
    {
      TableColumn tc = cm.getColumn(col);
      String headerVal = String.valueOf(m.getColumnName(col));
      int width = headerFm.stringWidth(headerVal) + padding;
      for(int row = 0; row < sampleRows; row++)
      {
        Object val = m.getValueAt(row, col);
        if(val != null)
        {
          int cellWidth = cellFm.stringWidth(val.toString()) + padding;
          width = Math.max(width, cellWidth);
        }
      }
      tc.setPreferredWidth(Math.min(width, maxWidth));
    }
  }
}



