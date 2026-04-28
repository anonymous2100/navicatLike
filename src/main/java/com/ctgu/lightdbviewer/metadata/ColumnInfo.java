package com.ctgu.lightdbviewer.metadata;

import lombok.Data;

import java.util.List;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 * @date 2026-04-23 18:32
 */
@Data
public class ColumnInfo
{
  public String name;

  // JDBC
  public int jdbcType;
  public String typeName;
  /** 列长度/精度（如 varchar(255) 的 255） */
  public int columnSize;
  /** 小数位数（数值类型） */
  public int decimalDigits;

  // 属性
  public boolean nullable;
  public boolean pk;
  // Backward-compatible alias used by older code paths.
  public boolean primaryKey;
  public boolean autoIncrement;
  /** 默认值 */
  public String defaultValue;
  /** 注释（REMARKS） */
  public String remarks;

  // 外键
  public boolean foreignKey;
  public String fkTable;
  public String fkColumn;

  // ENUM
  public boolean enumType;
  public List<String> enumValues;
}