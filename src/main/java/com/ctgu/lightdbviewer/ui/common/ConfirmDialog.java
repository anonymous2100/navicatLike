package com.ctgu.lightdbviewer.ui.common;

import javax.swing.*;
import java.awt.*;

/**
 * @author lihuahui
 * @version 1.0
 * @description: ConfirmDialog
 * <p>
 * 统一的确认对话框封装，用于：
 * - Revert
 * - Refresh with unsaved changes
 * - Delete row
 * - Truncate table
 * - Drop table
 * <p>
 * 设计目标：
 * 不散落 JOptionPane
 * UI 语义统一
 * 升级 Navicat 级体验
 */
public class ConfirmDialog
{
  /**
   * 确认级别
   */
  public enum Level
  {
    INFO, WARNING, DANGER
  }

  /**
   * 显示确认窗口
   *
   * @return true = 用户确认；false = 取消
   */
  public static boolean confirm(Component parent, String title, String message, Level level)
  {
    int messageType = switch(level)
    {
      case INFO -> JOptionPane.INFORMATION_MESSAGE;
      case WARNING -> JOptionPane.WARNING_MESSAGE;
      case DANGER -> JOptionPane.ERROR_MESSAGE;
    };
    Object[] options = new Object[] { "OK", "Cancel" };
    int result =
        JOptionPane.showOptionDialog(parent, createMessagePanel(message, level), title, JOptionPane.OK_CANCEL_OPTION, messageType, null,
            options, options[0]);
    return result == JOptionPane.OK_OPTION;
  }

  /**
   * 快捷：普通确认
   */
  public static boolean confirm(Component parent, String title, String message)
  {
    return confirm(parent, title, message, Level.WARNING);
  }

  public static boolean confirmDangerousTwice(Component parent, String title, String message, String expectedToken)
  {
    boolean first = confirm(parent, title, message, Level.DANGER);
    if(!first)
    {
      return false;
    }
    JTextField tokenField = new JTextField();
    JPanel panel = new JPanel(new BorderLayout(8, 8));
    JLabel label = new JLabel("Type \"" + expectedToken + "\" to confirm:");
    label.setForeground(new Color(180, 0, 0));
    panel.add(label, BorderLayout.NORTH);
    panel.add(tokenField, BorderLayout.CENTER);
    int second =
        JOptionPane.showConfirmDialog(parent, panel, title + " - Confirm Again", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);
    return second == JOptionPane.OK_OPTION && expectedToken.equals(tokenField.getText().trim());
  }

  /**
   * 信息面板（支持强调危险）
   */
  private static JComponent createMessagePanel(String message, Level level)
  {
    JPanel panel = new JPanel(new BorderLayout(8, 8));
    JLabel icon = new JLabel();
    icon.setVerticalAlignment(SwingConstants.TOP);
    switch(level)
    {
    case INFO -> icon.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
    case WARNING -> icon.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
    case DANGER -> icon.setIcon(UIManager.getIcon("OptionPane.errorIcon"));
    }
    JTextArea text = new JTextArea(message);
    text.setEditable(false);
    text.setOpaque(false);
    text.setLineWrap(true);
    text.setWrapStyleWord(true);
    text.setFont(UIManager.getFont("Label.font"));
    panel.add(icon, BorderLayout.WEST);
    panel.add(text, BorderLayout.CENTER);
    return panel;
  }
}