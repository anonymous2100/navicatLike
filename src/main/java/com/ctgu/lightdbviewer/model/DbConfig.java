package com.ctgu.lightdbviewer.model;

import com.ctgu.lightdbviewer.db.DbType;
import lombok.Data;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
@Data
public class DbConfig
{
  /**
   * 连接别名（可选，保存时使用）
   */
  public String name;
  public DbType type;
  public String host;
  public int port;
  public String database;
  public String username;
  public String password;
}