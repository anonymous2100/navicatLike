package com.ctgu.lightdbviewer.service;

import com.ctgu.lightdbviewer.jdbc.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 *
 * TransactionService
 * <p>
 * 职责：
 * - 管理 JDBC 事务边界
 * - 提供统一的 commit / rollback
 * - 改进异常处理和日志记录
 * <p>
 * 设计原则：
 *  UI 不直接操作 Connection
 *  所有事务行为集中在这里
 *  详细的错误日志记录
 */
public class TransactionService
{
  private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

  /**
   * 提交事务
   */
  public static void commit() throws SQLException
  {
    try(Connection conn = ConnectionManager.get())
    {
      conn.commit();
      logger.info("事务提交成功");
    }
    catch(SQLException e)
    {
      logger.error("事务提交失败", e);
      throw e;
    }
  }

  /**
   * 回滚事务
   */
  public static void rollback() throws SQLException
  {
    try(Connection conn = ConnectionManager.get())
    {
      conn.rollback();
      logger.info("事务回滚成功");
    }
    catch(SQLException e)
    {
      logger.error("事务回滚失败", e);
      throw e;
    }
  }

  /**
   * 设置自动提交
   */
  public static void setAutoCommit(boolean autoCommit) throws SQLException
  {
    try(Connection conn = ConnectionManager.get())
    {
      conn.setAutoCommit(autoCommit);
      logger.debug("设置自动提交模式: {}", autoCommit);
    }
    catch(SQLException e)
    {
      logger.error("设置自动提交模式失败: {}", autoCommit, e);
      throw e;
    }
  }

  /**
   * 判断当前是否自动提交
   */
  public static boolean isAutoCommit() throws SQLException
  {
    try(Connection conn = ConnectionManager.get())
    {
      return conn.getAutoCommit();
    }
    catch(SQLException e)
    {
      logger.error("获取自动提交状态失败", e);
      throw e;
    }
  }
}
