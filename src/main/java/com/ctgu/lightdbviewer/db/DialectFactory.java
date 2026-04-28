package com.ctgu.lightdbviewer.db;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库方言工厂
 * <p>
 * 根据数据库类型提供对应的方言实例
 */
/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public final class DialectFactory
{
  private static final Map<DbType, Dialect> DIALECT_CACHE = new ConcurrentHashMap<>();

  static
  {
    // 预注册已知方言
    registerDialect(DbType.MYSQL, new MySQLDialect());
    registerDialect(DbType.POSTGRESQL, new PostgreSQLDialect());
  }

  private DialectFactory()
  {
  }

  /**
   * 获取指定数据库类型的方言
   *
   * @param dbType 数据库类   * @return 数据库方言
   * @throws IllegalArgumentException 不支持的数据库类   */
  public static Dialect getDialect(DbType dbType)
  {
    Dialect dialect = DIALECT_CACHE.get(dbType);
    if(dialect == null)
    {
      throw new IllegalArgumentException("不支持的数据库类 " + dbType);
    }
    return dialect;
  }

  /**
   * 根据数据库名称获取方言
   *
   * @param databaseProductName 数据库产品名称（MySQL"PostgreSQL"   * @return 数据库方言
   * @throws IllegalArgumentException 无法识别的数据库
   */
  public static Dialect getDialect(String databaseProductName)
  {
    if(databaseProductName == null)
    {
      throw new IllegalArgumentException("数据库名称不能为null");
    }

    String dbName = databaseProductName.toLowerCase();

    if(dbName.contains("mysql"))
    {
      return getDialect(DbType.MYSQL);
    }
    else if(dbName.contains("postgresql"))
    {
      return getDialect(DbType.POSTGRESQL);
    }
    else
    {
      throw new IllegalArgumentException("无法识别的数据库: " + databaseProductName);
    }
  }

  /**
   * 注册自定义方言
   *
   * @param dbType  数据库类   * @param dialect 方言实例
   */
  public static void registerDialect(DbType dbType, Dialect dialect)
  {
    if(dbType == null || dialect == null)
    {
      throw new IllegalArgumentException("数据库类型和方言实例不能为null");
    }
    DIALECT_CACHE.put(dbType, dialect);
  }

  /**
   * 检查是否支持指定的数据库类   *
   * @param dbType 数据库类   * @return 是否支持
   */
  public static boolean isSupported(DbType dbType)
  {
    if(dbType == null)
    {
      return false;
    }
    return DIALECT_CACHE.containsKey(dbType);
  }

  /**
   * 获取所有支持的数据库类   *
   * @return 支持的数据库类型数组
   */
  public static DbType[] getSupportedTypes()
  {
    return DIALECT_CACHE.keySet().toArray(new DbType[0]);
  }
}


