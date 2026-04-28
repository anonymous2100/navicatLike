package com.ctgu.lightdbviewer.ui.common;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 * @date 2026-04-23 18:32
 */

import java.awt.*;

/**
 * UiConstants
 * <p>
 * UI 层的统一常量定义：
 * - 字体
 * - 颜色
 * - 间距
 * - 行状态配色
 * <p>
 * 设计原则：
 * 所有 UI 魔法值集中
 * Navicat 风格（工具感、稳、冷）
 * <p>
 * 规则：
 * UI 代码中不允许直接 new Color / new Font
 * 一律使用此类
 */
public final class UiConstants
{
  /**
   * 默认 UI 字体
   */
  public static final Font FONT_DEFAULT = new Font(getDefaultUIFontFamily(), Font.PLAIN, 12);
  /**
   * 表头字体
   */
  public static final Font FONT_TABLE_HEADER = new Font(getDefaultUIFontFamily(), Font.BOLD, 12);
  /**
   * SQL 编辑器字体
   */
  public static final Font FONT_MONO = new Font("Monospaced", Font.PLAIN, 12);
  /**
   * 主键列 / 重要字段
   */
  public static final Font FONT_BOLD = new Font(getDefaultUIFontFamily(), Font.BOLD, 12);
  /**
   * 普通背景
   */
  public static final Color COLOR_BG = Color.WHITE;
  /**
   * 表格边框
   */
  public static final Color COLOR_BORDER = new Color(220, 220, 220);
  /**
   * NULL 值显示色
   */
  public static final Color COLOR_NULL_TEXT = new Color(150, 150, 150);
  /**
   * 错误文本
   */
  public static final Color COLOR_ERROR_TEXT = new Color(180, 0, 0);
  /**
   * 主键列背景
   */
  public static final Color COLOR_PK_BG = new Color(245, 245, 245);
  /**
   * INSERT 行
   */
  public static final Color COLOR_ROW_INSERT = new Color(230, 247, 230); // 浅绿
  /**
   * UPDATE 行
   */
  public static final Color COLOR_ROW_UPDATE = new Color(255, 247, 204); // 浅黄
  /**
   * DELETE 行
   */
  public static final Color COLOR_ROW_DELETE = new Color(255, 230, 230); // 浅红
  /**
   * 选中行（不覆盖状态色）
   */
  public static final Color COLOR_ROW_SELECTED = new Color(200, 220, 245);
  /**
   * Tab 内边距
   */
  public static final int PADDING_TAB = 6;
  /**
   * Toolbar 间距
   */
  public static final int PADDING_TOOLBAR = 4;
  /**
   * 表单间距
   */
  public static final int PADDING_FORM = 8;
  /**
   * Cell 内边距（用于 renderer）
   */
  public static final int PADDING_CELL = 3;
  /**
   * Toolbar 按钮高度
   */
  public static final int TOOLBAR_BUTTON_HEIGHT = 22;
  /**
   * 默认 Row 高度
   */
  public static final int TABLE_ROW_HEIGHT = 22;

  private UiConstants()
  {
    // utility class
  }

  /**
   * 根据系统选择合适的 UI 字体
   */
  private static String getDefaultUIFontFamily()
  {
    String os = System.getProperty("os.name").toLowerCase();
    if(os.contains("win"))
    {
      return "Segoe UI";
    }
    if(os.contains("mac"))
    {
      return "San Francisco";
    }
    return "SansSerif";
  }
}