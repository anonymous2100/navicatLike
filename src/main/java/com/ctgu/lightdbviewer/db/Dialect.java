package com.ctgu.lightdbviewer.db;

/**
 * 数据库方言接口
 * <p>
 * 为不同数据库提供特定的SQL语法和功能支 */
/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public interface Dialect
{
  /**
   * 获取数据库类   *
   * @return 数据库类   */
  DbType getDbType();

  /**
   * 获取分页SQL
   *
   * @param sql       原始SQL
   * @param offset    偏移   * @param limit     限制数量
   * @return 分页SQL
   */
  String getLimitSql(String sql, int offset, int limit);

  /**
   * 获取当前时间函数
   *
   * @return 时间函数SQL
   */
  String getCurrentTimeFunction();

  /**
   * 获取表列表SQL
   *
   * @param schema 模式名（可为null   * @return 查询SQL
   */
  String getTablesSql(String schema);

  /**
   * 获取视图列表SQL
   *
   * @param schema 模式名（可为null   * @return 查询SQL
   */
  String getViewsSql(String schema);

  /**
   * 获取列列表SQL
   *
   * @param schema 模式名（可为null   * @param table  表名
   * @return 查询SQL
   */
  String getColumnsSql(String schema, String table);

  /**
   * 获取主键SQL
   *
   * @param schema 模式名（可为null   * @param table  表名
   * @return 查询SQL
   */
  String getPrimaryKeysSql(String schema, String table);

  /**
   * 获取外键SQL
   *
   * @param schema 模式名（可为null   * @param table  表名
   * @return 查询SQL
   */
  String getForeignKeysSql(String schema, String table);

  /**
   * 获取索引列表SQL
   *
   * @param schema 模式名（可为null   * @param table  表名
   * @return 查询SQL
   */
  String getIndexesSql(String schema, String table);

  /**
   * 是否支持模式
   *
   * @return 是否支持
   */
  boolean supportsSchema();

  /**
   * 获取默认模式   *
   * @return 默认模式   */
  String getDefaultSchema();

  /**
   * 转义标识   *
   * @param identifier 标识   * @return 转义后的标识   */
  String escapeIdentifier(String identifier);

  /**
   * 转义字符串字面量
   *
   * @param literal 字符   * @return 转义后的字符   */
  String escapeStringLiteral(String literal);

  /**
   * 是否支持CUBE/ROLLUP
   *
   * @return 是否支持
   */
  boolean supportsCubeRollup();

  /**
   * 获取序列列表SQL
   *
   * @param schema 模式名（可为null   * @return 查询SQL
   */
  String getSequencesSql(String schema);
}


