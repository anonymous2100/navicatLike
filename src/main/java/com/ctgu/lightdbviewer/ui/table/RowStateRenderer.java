package com.ctgu.lightdbviewer.ui.table;

import com.ctgu.lightdbviewer.ui.common.UiConstants;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @version 1.0
 * @description: Navicat 风格行状态渲染器
 * <ul>
 *   <li>INSERT 浅绿背景</li>
 *   <li>UPDATE 浅黄背景</li>
 *   <li>DELETE 浅红背景 + Tooltip 提示</li>
 *   <li>NULL   灰色斜体 "NULL" 文字</li>
 *   <li>PK     粗体</li>
 *   <li>选中   状态色与选中色融合，不完全覆/li>
 * </ul>
 * @date 2026-04-23 18:31
 */
/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class RowStateRenderer extends DefaultTableCellRenderer
{
  private final EditableResultTableModel tableModel;

  /**
   * model 列索引（view 列）中属于主键的集合
   */
  private final Set<Integer> pkColumnModelIndices = new HashSet<>();

  public RowStateRenderer(EditableResultTableModel model)
  {
    this.tableModel = model;
    setOpaque(true);
  }

  /**
   * 更新 PK 列的 model 列索引集合，调用后触发重绘。
   */
  public void setPkColumnIndices(Set<Integer> indices)
  {
    pkColumnModelIndices.clear();
    pkColumnModelIndices.addAll(indices);
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int viewRow, int viewCol)
  {
    // 先调用父类以应用默认文本/颜色
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, viewRow, viewCol);
    int modelRow = table.convertRowIndexToModel(viewRow);
    int modelCol = table.convertColumnIndexToModel(viewCol);
    ChangeType state = tableModel.getRowState(modelRow);
    // ── 1. 确定行状态底──────────────────────────────────────
    Color stateColor = null;
    if(state == ChangeType.INSERT) {
      stateColor = UiConstants.COLOR_ROW_INSERT;
    } else if(state == ChangeType.UPDATE) {
      stateColor = UiConstants.COLOR_ROW_UPDATE;
    } else if(state == ChangeType.DELETE) {
      stateColor = UiConstants.COLOR_ROW_DELETE;
    }
    // ── 2. 应用背景─────────────────────────────────────────
    if(isSelected)
    {
      Color selBg = table.getSelectionBackground();
      // 选中时：状态色 40% + 选中60%，既保留状态感知又有选中标识
      setBackground(stateColor != null ? blend(stateColor, selBg, 0.40f) : selBg);
      setForeground(table.getSelectionForeground());
    }
    else
    {
      setBackground(stateColor != null ? stateColor : table.getBackground());
      setForeground(table.getForeground());
    }
    // ── 3. NULL 值：灰色斜体 "NULL" ──────────────────────────
    boolean isNull = (value == null);
    if(isNull)
    {
      setText("NULL");
      setForeground(isSelected ? table.getSelectionForeground() : UiConstants.COLOR_NULL_TEXT);
    }
    // ── 4. 字体：PK = 粗体；NULL = 斜体；两者叠────────────
    boolean isPk = pkColumnModelIndices.contains(modelCol);
    int fontStyle = Font.PLAIN;
    if(isPk) {
      fontStyle |= Font.BOLD;
    }
    if(isNull) {
      fontStyle |= Font.ITALIC;
    }
    // ── 4b. PK 背景：无状态色也不选中时，显示 PK 专有背景 ──
    if(isPk && stateColor == null && !isSelected)
    {
      setBackground(UiConstants.COLOR_PK_BG);
    }
    if(fontStyle != Font.PLAIN)
    {
      setFont(table.getFont().deriveFont(fontStyle));
    }
    else
    {
      setFont(table.getFont());
    }
    // ── 5. Tooltip：DELETE 行提醒用────────────────────────
    if(state == ChangeType.DELETE)
    {
      setToolTipText("该行已标记删除，点击「保存」提交，或点击「撤销」恢复。");
    }
    else
    {
      setToolTipText(null);
    }
    // 移除焦点边框（保持干净）
    setBorder(noFocusBorder);
    return this;
  }

  /**
   * 颜色混合：ratioA a 颜色 + (1 - ratioA) b 颜色
   */
  private static Color blend(Color a, Color b, float ratioA)
  {
    float ratioB = 1f - ratioA;
    int r = Math.round(a.getRed() * ratioA + b.getRed() * ratioB);
    int g = Math.round(a.getGreen() * ratioA + b.getGreen() * ratioB);
    int bl = Math.round(a.getBlue() * ratioA + b.getBlue() * ratioB);
    return new Color(Math.min(255, r), Math.min(255, g), Math.min(255, bl));
  }
}


