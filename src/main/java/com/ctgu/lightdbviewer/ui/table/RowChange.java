package com.ctgu.lightdbviewer.ui.table;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 * @date 2026-04-23 18:48
 */

import java.util.HashMap;
import java.util.Map;

public class RowChange
{
  public ChangeType type;

  /**
   * 新值（INSERT / UPDATE 用）
   */
  public Map<String, Object> newValues = new HashMap<>();

  /**
   * 原值（UPDATE / DELETE WHERE 用）
   */
  public Map<String, Object> oldValues = new HashMap<>();

  /**
   * 表格中的行索引（UI 用）
   */
  public int rowIndex;
}