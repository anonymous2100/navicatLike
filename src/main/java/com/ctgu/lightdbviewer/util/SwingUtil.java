package com.ctgu.lightdbviewer.util;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 * @date 2026-04-23 18:33
 */
public final class SwingUtil
{

  private static final Logger logger = LoggerFactory.getLogger(SwingUtil.class);

  private SwingUtil()
  {
  }

  /**
   * 在 EDT 中执行
   */
  public static void invokeLater(Runnable r)
  {
    if(SwingUtilities.isEventDispatchThread())
    {
      r.run();
    }
    else
    {
      SwingUtilities.invokeLater(r);
    }
  }

  /**
   * 安全弹错误框
   */
  public static void showError(java.awt.Component parent, String title, Throwable t)
  {
    JOptionPane.showMessageDialog(parent, t.getMessage(), title, JOptionPane.ERROR_MESSAGE);
  }

  /**
   * 设置系统 LookAndFeel
   */
  public static void setupSystemLookAndFeel()
  {
    try
    {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch(Exception e)
    {
      logger.debug("系统 LookAndFeel 设置失败，使用默认", e);
    }
  }
}
