package com.ctgu.lightdbviewer.ui.table;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class EditableResultTableModel extends DefaultTableModel
{
  private boolean dirty = false;
  private final List<RowChange> changes = new ArrayList<>();

  /**
   * 行状态映射：key = model 行索引，value = 当前未提交状态（INSERT / UPDATE / DELETE）
   * 普通行（无变更）不在此 Map 中
   */
  private final Map<Integer, ChangeType> rowStates = new HashMap<>();

  public static EditableResultTableModel mock(String table)
  {
    EditableResultTableModel m = new EditableResultTableModel();
    m.addColumn("id");
    m.addColumn("name");
    m.addRow(new Object[] { 1, table + "_row" });
    return m;
  }

  @Override
  public boolean isCellEditable(int r, int c)
  {
    // DELETE 标记行不可编辑
    if(rowStates.get(r) == ChangeType.DELETE) {
      return false;
    }
    return c != 0;
  }

  @Override
  public void setValueAt(Object a, int r, int c)
  {
    Object old = getValueAt(r, c);
    super.setValueAt(a, r, c);
    dirty = true;

    // INSERT 行编辑时不降级为 UPDATE（保持 INSERT 状态）
    if(rowStates.get(r) != ChangeType.INSERT)
    {
      rowStates.put(r, ChangeType.UPDATE);
    }

    RowChange change = new RowChange();
    change.type = ChangeType.UPDATE;
    change.rowIndex = r;
    String col = getColumnName(c);
    change.oldValues.put(col, old);
    change.newValues.put(col, a);
    changes.add(change);
  }

  public void addInsertRow()
  {
    int cols = getColumnCount();
    if(cols == 0) {
      return;
    }
    Object[] row = new Object[cols];
    super.addRow(row);

    int newRowIndex = getRowCount() - 1;

    RowChange change = new RowChange();
    change.type = ChangeType.INSERT;
    change.rowIndex = newRowIndex;
    for(int i = 0; i < cols; i++)
    {
      change.newValues.put(getColumnName(i), null);
    }
    changes.add(change);
    rowStates.put(newRowIndex, ChangeType.INSERT);
    dirty = true;
  }

  /**
   * 标记删除指定行（Navicat 延迟提交模式）：
   * <ul>
   * <li>INSERT 状态行：直接从模型移除（未入库，无需 DELETE SQL）</li>
   * <li>其他行：标记为 DELETE（保留在模型中，显示红色）；提交时执行 DELETE SQL</li>
   * </ul>
   */
  public void markDeleteRow(int row)
  {
    if(row < 0 || row >= getRowCount()) {
      return;
    }

    ChangeType currentState = rowStates.get(row);

    if(currentState == ChangeType.INSERT)
    {
      // INSERT 行尚未入库，直接移除，不需要 DELETE SQL
      rowStates.remove(row);
      super.removeRow(row);
      // 移除该行的所有 INSERT / UPDATE 变更记录
      final int targetRow = row;
      changes.removeIf(c -> c.rowIndex == targetRow && (c.type == ChangeType.INSERT || c.type == ChangeType.UPDATE));
      // 重新索引：移除行之后的行号都 -1
      reindexAfterRemoval(row);
      dirty = !changes.isEmpty();
    }
    else if(currentState == ChangeType.DELETE)
    {
      // 已标记，幂等
    }
    else
    {
      // 正常行：软删除（Navicat 延迟提交）
      RowChange change = new RowChange();
      change.type = ChangeType.DELETE;
      change.rowIndex = row;
      for(int i = 0; i < getColumnCount(); i++)
      {
        change.oldValues.put(getColumnName(i), getValueAt(row, i));
      }
      changes.add(change);
      rowStates.put(row, ChangeType.DELETE);
      dirty = true;
      fireTableRowsUpdated(row, row); // 触发重绘（红色背景）
    }
  }

  /**
   * 移除某行后，将 rowStates 中索引 > removedRow 的条目下移一位。
   */
  private void reindexAfterRemoval(int removedRow)
  {
    Map<Integer, ChangeType> updated = new HashMap<>();
    for(Map.Entry<Integer, ChangeType> e : rowStates.entrySet())
    {
      int key = e.getKey();
      updated.put(key > removedRow ? key - 1 : key, e.getValue());
    }
    rowStates.clear();
    rowStates.putAll(updated);
  }

  /**
   * 查询某 model 行的当前状态，正常行返回 null。
   */
  public ChangeType getRowState(int row)
  {
    return rowStates.get(row);
  }

  public boolean isDirty()
  {
    return dirty;
  }

  public void commit()
  {
    dirty = false;
  }

  public void revert()
  {
    setRowCount(0);
    clearChanges();
  }

  public void clearChanges()
  {
    changes.clear();
    rowStates.clear();
    dirty = false;
  }

  public List<RowChange> getChanges()
  {
    return new ArrayList<>(changes);
  }

  public void clearAll()
  {
    setRowCount(0);
    setColumnCount(0);
    clearChanges();
  }

  public void copyFrom(EditableResultTableModel other)
  {
    clearAll();

    for(int i = 0; i < other.getColumnCount(); i++)
    {
      addColumn(other.getColumnName(i));
    }

    for(int r = 0; r < other.getRowCount(); r++)
    {
      Object[] row = new Object[other.getColumnCount()];
      for(int c = 0; c < other.getColumnCount(); c++)
      {
        row[c] = other.getValueAt(r, c);
      }
      super.addRow(row);
    }

    clearChanges(); // rowStates 已由 clearAll() → clearChanges() 清理
  }
}