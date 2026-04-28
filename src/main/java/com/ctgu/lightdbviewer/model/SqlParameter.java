package com.ctgu.lightdbviewer.model;

import lombok.Data;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
@Data
public class SqlParameter
{
  public String name;
  public boolean primaryKey;
  public boolean autoIncrement;
}