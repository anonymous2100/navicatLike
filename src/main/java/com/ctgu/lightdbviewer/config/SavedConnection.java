package com.ctgu.lightdbviewer.config;

import com.ctgu.lightdbviewer.db.DbType;
import com.ctgu.lightdbviewer.model.DbConfig;

/**
 * @author lihuahui
 * @version 1.0
 * @description: 已保存的数据库连接（含名称，用于左侧列表展示和持久化）
 */
public class SavedConnection
{
  public String name = "";
  public DbType type = DbType.POSTGRESQL;
  public String host = "localhost";
  public int port = 5432;
  public String database = "";
  public String username = "";
  public String password = "";

  @Override
  public String toString()
  {
    return (name != null && !name.isBlank()) ? name : host + ":" + port;
  }

  /**
   * 转为 DbConfig 传递给连接回调
   */
  public DbConfig toDbConfig()
  {
    DbConfig cfg = new DbConfig();
    cfg.name = name;
    cfg.type = type;
    cfg.host = host;
    cfg.port = port;
    cfg.database = database;
    cfg.username = username;
    cfg.password = password;
    return cfg;
  }

  /**
   * DbConfig 构建（用于连接成功后自动保存）
   */
  public static SavedConnection fromDbConfig(DbConfig cfg)
  {
    SavedConnection sc = new SavedConnection();
    sc.name = cfg.name;
    sc.type = cfg.type;
    sc.host = cfg.host;
    sc.port = cfg.port;
    sc.database = cfg.database;
    sc.username = cfg.username;
    sc.password = cfg.password;
    return sc;
  }
}



