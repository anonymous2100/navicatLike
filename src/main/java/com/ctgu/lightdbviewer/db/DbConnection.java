package com.ctgu.lightdbviewer.db;

import com.ctgu.lightdbviewer.model.DbConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 * @date 2026-04-23 18:11
 */
public class DbConnection
{
  public static Connection connect(DbConfig cfg) throws SQLException
  {
    String url;
    if(cfg.type == DbType.MYSQL)
    {
      url = "jdbc:mysql://" + cfg.host + ":" + cfg.port + "/" + cfg.database;
    }
    else
    {
      url = "jdbc:postgresql://" + cfg.host + ":" + cfg.port + "/" + cfg.database;
    }
    return DriverManager.getConnection(url, cfg.username, cfg.password);
  }
}