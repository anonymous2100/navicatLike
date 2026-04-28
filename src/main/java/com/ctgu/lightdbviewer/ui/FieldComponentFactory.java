package com.ctgu.lightdbviewer.ui;

import com.ctgu.lightdbviewer.metadata.ColumnInfo;

import javax.swing.*;
import java.sql.Types;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 * @date 2026-04-23 18:28
 */
public class FieldComponentFactory
{
  public static JComponent create(ColumnInfo col)
  {
    switch(col.jdbcType)
    {
    // ===== 字符串 =====
    case Types.VARCHAR:
    case Types.LONGVARCHAR:
    case Types.CHAR:
      return new JTextField();

    // ===== 数字 =====
    case Types.INTEGER:
    case Types.BIGINT:
    case Types.SMALLINT:
    case Types.TINYINT:
      return new JFormattedTextField(NumberFormat.getIntegerInstance());

    case Types.DECIMAL:
    case Types.NUMERIC:
    case Types.FLOAT:
    case Types.DOUBLE:
      return new JFormattedTextField(NumberFormat.getNumberInstance());

    // ===== 布尔 =====
    case Types.BOOLEAN:
    case Types.BIT:
      return new JCheckBox();

    // ===== 日期 =====
    case Types.DATE:
      return new JFormattedTextField(new SimpleDateFormat("yyyy-MM-dd"));

    case Types.TIMESTAMP:
    case Types.TIMESTAMP_WITH_TIMEZONE:
      return new JFormattedTextField(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    // ===== fallback =====
    default:
      return new JTextField();
    }
  }
}