package com.ctgu.lightdbviewer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.Component;
import java.sql.SQLException;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 *
 * 统一的异常处理工具类
 * <p>
 * 职责：
 * - 提供用户友好的错误提示
 * - 记录详细的错误日志
 * - 区分不同类型的异常
 */
public final class ErrorHandler
{
  private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

  private ErrorHandler()
  {
  }

  /**
   * 处理异常并显示用户友好的错误消息
   *
   * @param parent      父组件
   * @param e           异常
   * @param userMessage 用户友好的错误描述
   */
  public static void handleError(Component parent, Exception e, String userMessage)
  {
    String displayMessage = buildUserMessage(e, userMessage);
    logError(e, userMessage);

    JOptionPane.showMessageDialog(parent, displayMessage, "错误", JOptionPane.ERROR_MESSAGE);
  }

  /**
   * 处理异常并显示用户友好的错误消息（无父组件）
   *
   * @param e           异常
   * @param userMessage 用户友好的错误描述
   */
  public static void handleError(Exception e, String userMessage)
  {
    handleError(null, e, userMessage);
  }

  /**
   * 处理异常并返回用户友好的错误消息（不显示对话框）
   *
   * @param e           异常
   * @param userMessage 用户友好的错误描述
   * @return 用户友好的错误消息
   */
  public static String buildUserMessage(Exception e, String userMessage)
  {
    if(userMessage == null || userMessage.isEmpty())
    {
      userMessage = "操作失败";
    }

    String detailMessage = extractDetailMessage(e);
    if(detailMessage != null && !detailMessage.isEmpty())
    {
      return userMessage + ":\n" + detailMessage;
    }
    return userMessage;
  }

  /**
   * 记录错误日志
   *
   * @param e           异常
   * @param userMessage 用户友好的错误描述
   */
  public static void logError(Exception e, String userMessage)
  {
    if(e instanceof SQLException)
    {
      logger.error("{}: SQLState={}, ErrorCode={}", userMessage, ((SQLException)e).getSQLState(), ((SQLException)e).getErrorCode(), e);
    }
    else
    {
      logger.error(userMessage, e);
    }
  }

  /**
   * 从异常中提取详细的错误消息
   *
   * @param e 异常
   * @return 详细的错误消息
   */
  private static String extractDetailMessage(Exception e)
  {
    if(e == null)
    {
      return "";
    }

    // 处理SQL异常
    if(e instanceof SQLException)
    {
      SQLException sqlEx = (SQLException)e;
      String message = sqlEx.getMessage();

      // 常见数据库错误的友好提示
      if(message != null)
      {
        if(message.contains("connection") || message.contains("Connection"))
        {
          return "数据库连接失败，请检查网络和连接配置";
        }
        else if(message.contains("timeout") || message.contains("Timeout"))
        {
          return "操作超时，请重试";
        }
        else if(message.contains("syntax") || message.contains("Syntax"))
        {
          return "SQL语法错误，请检查SQL语句";
        }
        else if(message.contains("duplicate") || message.contains("Duplicate") || message.contains("unique") || message.contains("Unique"))
        {
          return "数据重复，违反唯一约束";
        }
        else if(message.contains("foreign key") || message.contains("Foreign key"))
        {
          return "外键约束冲突";
        }
        else if(message.contains("permission") || message.contains("Permission") || message.contains("access") || message.contains(
            "Access"))
        {
          return "权限不足，请联系管理员";
        }
      }

      return message != null ? message : "未知数据库错误";
    }

    // 处理其他异常
    String message = e.getMessage();
    if(message != null && !message.isEmpty())
    {
      return message;
    }

    return e.getClass().getSimpleName();
  }

  /**
   * 显示信息消息
   *
   * @param parent  父组件
   * @param message 消息内容
   * @param title   标题
   */
  public static void showInfo(Component parent, String message, String title)
  {
    logger.info("显示信息: {}", message);
    JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * 显示警告消息
   *
   * @param parent  父组件
   * @param message 消息内容
   * @param title   标题
   */
  public static void showWarning(Component parent, String message, String title)
  {
    logger.warn("显示警告: {}", message);
    JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
  }

  /**
   * 显示确认对话框
   *
   * @param parent  父组件
   * @param message 消息内容
   * @param title   标题
   * @return 用户选择（true=确认，false=取消）
   */
  public static boolean showConfirm(Component parent, String message, String title)
  {
    logger.debug("显示确认对话框: {}", message);
    int result = JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION);
    return result == JOptionPane.YES_OPTION;
  }
}
