package com.ctgu.lightdbviewer.util;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 *
 * SqlValueUtil — SQL 字面量转换工具
 */
public final class SqlValueUtil
{
  private SqlValueUtil()
  {
  }

  //  /**
  //   * 将 Java 对象转换为 SQL 字面量。
  //   * <ul>
  //   *   <li>{@code null} → {@code NULL}</li>
  //   *   <li>{@link Number} / {@link Boolean} → 原始值（不加引号）</li>
  //   *   <li>二进制占位符（{@code <binary...>}）→ {@code NULL /*binary*/}</li>
  //   *   <li>其他 → 单引号字符串，内部单引号加倍转义</li>
  //   * </ul>
  //   */
  public static String toSqlLiteral(Object val)
  {
    if(val == null) {
      return "NULL";
    }
    if(val instanceof Number || val instanceof Boolean) {
      return val.toString();
    }
    String s = val.toString();
    if(s.startsWith("<binary")) {
      return "NULL /*binary*/";
    }
    return "'" + s.replace("'", "''") + "'";
  }
}

