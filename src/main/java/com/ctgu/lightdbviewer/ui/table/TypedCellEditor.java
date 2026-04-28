package com.ctgu.lightdbviewer.ui.table;


import com.ctgu.lightdbviewer.metadata.ColumnInfo;
import com.ctgu.lightdbviewer.ui.common.UiConstants;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 *
 * TypedCellEditor
 * <p>
 * JTable 单元格编辑器（类型感知）
 * Phase 2 最终版
 */
public class TypedCellEditor extends AbstractCellEditor implements TableCellEditor
{
  private final ColumnInfo column;
  private JComponent editor;

  public TypedCellEditor(ColumnInfo column)
  {
    this.column = column;
  }

  @Override
  public Object getCellEditorValue()
  {
    if(editor instanceof JCheckBox cb)
    {
      return cb.isSelected();
    }
    if(editor instanceof JComboBox<?> box)
    {
      return box.getSelectedItem();
    }
    if(editor instanceof JTextArea ta)
    {
      return ta.getText();
    }
    if(editor instanceof JFormattedTextField tf)
    {
      return tf.getValue();
    }
    if(editor instanceof JTextField tf)
    {
      return tf.getText();
    }
    return null;
  }

  @Override
  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int columnIndex)
  {

    editor = createEditorComponent();

    if(value != null)
    {
      if(editor instanceof JCheckBox cb)
      {
        cb.setSelected(Boolean.TRUE.equals(value));
      }
      else if(editor instanceof JTextField tf)
      {
        tf.setText(value.toString());
      }
      else if(editor instanceof JTextArea ta)
      {
        ta.setText(value.toString());
      }
      else if(editor instanceof JComboBox<?> box)
      {
        box.setSelectedItem(value);
      }
      else if(editor instanceof JFormattedTextField tf)
      {
        tf.setValue(value);
      }
    }
    return editor;
  }

  private JComponent createEditorComponent()
  {
    // ENUM
    if(column.enumType && column.enumValues != null)
    {
      JComboBox<String> box = new JComboBox<>(column.enumValues.toArray(String[]::new));
      box.setFont(UiConstants.FONT_DEFAULT);
      return box;
    }
    // Boolean
    if(column.jdbcType == java.sql.Types.BOOLEAN)
    {
      JCheckBox checkBox = new JCheckBox();
      checkBox.setHorizontalAlignment(SwingConstants.CENTER);
      return checkBox;
    }
    // JSON
    if(column.typeName != null && column.typeName.toLowerCase().contains("json"))
    {
      JTextArea ta = new JTextArea();
      ta.setFont(UiConstants.FONT_MONO);
      ta.setLineWrap(true);
      ta.setWrapStyleWord(true);
      return ta;
    }
    // 数值
    if(column.jdbcType == java.sql.Types.INTEGER || column.jdbcType == java.sql.Types.BIGINT || column.jdbcType == java.sql.Types.DECIMAL)
    {

      JFormattedTextField tf = new JFormattedTextField();
      tf.setFont(UiConstants.FONT_DEFAULT);
      return tf;
    }
    // 默认：文本
    JTextField tf = new JTextField();
    tf.setFont(UiConstants.FONT_DEFAULT);
    return tf;
  }
}