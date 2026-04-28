package com.ctgu.lightdbviewer.ui.common;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 *
 * 用户操作反馈工具
 * <p>
 * 提供操作的成功/失败反馈，包括Toast消息、状态栏更新等
 */
public final class UserFeedback
{
  private UserFeedback()
  {
  }

  /**
   * 显示Toast消息（临时通知）
   *
   * @param parent  父组件
   * @param message 消息内容
   * @param type    消息类型
   */
  public static void showToast(Component parent, String message, MessageType type)
  {
    SwingUtilities.invokeLater(() -> {
      JWindow toast = new JWindow();
      JPanel panel = new JPanel();
      panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(getTypeColor(type), 2),
          BorderFactory.createEmptyBorder(10, 20, 10, 20)));
      JLabel label = new JLabel(message);
      label.setFont(label.getFont().deriveFont(Font.BOLD));
      label.setForeground(getTypeColor(type));
      panel.add(label);
      toast.add(panel);
      toast.pack();
      // 定位到右下角
      Point location = getLocationForToast(parent);
      toast.setLocation(location);
      // 显示后自动消失
      toast.setVisible(true);
      Timer timer = new Timer(3000, e -> toast.dispose());
      timer.setRepeats(false);
      timer.start();
    });
  }

  /**
   * 显示成功消息
   *
   * @param parent  父组件
   * @param message 消息内容
   */
  public static void showSuccess(Component parent, String message)
  {
    showToast(parent, message, MessageType.SUCCESS);
  }

  /**
   * 显示错误消息
   *
   * @param parent  父组件
   * @param message 消息内容
   */
  public static void showError(Component parent, String message)
  {
    showToast(parent, message, MessageType.ERROR);
  }

  /**
   * 显示警告消息
   *
   * @param parent  父组件
   * @param message 消息内容
   */
  public static void showWarning(Component parent, String message)
  {
    showToast(parent, message, MessageType.WARNING);
  }

  /**
   * 显示信息消息
   *
   * @param parent  父组件
   * @param message 消息内容
   */
  public static void showInfo(Component parent, String message)
  {
    showToast(parent, message, MessageType.INFO);
  }

  /**
   * 在状态栏显示消息
   *
   * @param statusBar 状态栏组件
   * @param message   消息内容
   * @param type      消息类型
   */
  public static void showStatusMessage(JComponent statusBar, String message, MessageType type)
  {
    if(statusBar instanceof com.ctgu.lightdbviewer.ui.status.StatusBarPanel)
    {
      com.ctgu.lightdbviewer.ui.status.StatusBarPanel panel = (com.ctgu.lightdbviewer.ui.status.StatusBarPanel)statusBar;
      panel.setMessage(message);
      // 根据消息类型设置不同的前景色
      Color color = getTypeColor(type);
      panel.setForeground(color);
    }
  }

  /**
   * 带加载反馈的操作执行
   *
   * @param parent      父组件
   * @param loadingText 加载文本
   * @param operation   要执行的操作
   * @param callback    完成回调
   */
  public static void executeWithLoading(Component parent, String loadingText, Runnable operation, Runnable callback)
  {
    AtomicReference<JDialog> dialogRef = new AtomicReference<>();
    SwingWorker<Void, Void> worker = new SwingWorker<>()
    {
      @Override
      protected Void doInBackground() throws Exception
      {
        try
        {
          operation.run();
        }
        catch(Exception e)
        {
          SwingUtilities.invokeLater(() -> {
            UserFeedback.showError(parent, "操作失败: " + e.getMessage());
          });
          throw e;
        }
        return null;
      }

      @Override
      protected void done()
      {
        try
        {
          get(); // 检查是否有异常
          SwingUtilities.invokeLater(() -> {
            JDialog dialog = dialogRef.get();
            if(dialog != null)
            {
              dialog.dispose();
            }
            if(callback != null)
            {
              callback.run();
            }
          });
        }
        catch(Exception e)
        {
          SwingUtilities.invokeLater(() -> {
            JDialog dialog = dialogRef.get();
            if(dialog != null)
            {
              dialog.dispose();
            }
          });
        }
      }
    };

    SwingUtilities.invokeLater(() -> {
      JDialog dialog = LoadingIndicator.showLoadingDialog(parent, "请稍候", loadingText);
      dialogRef.set(dialog);
      worker.execute();
    });
  }

  private static Point getLocationForToast(Component parent)
  {
    if(parent == null)
    {
      GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
      Rectangle bounds = gc.getBounds();
      return new Point(bounds.width - 320, bounds.height - 100);
    }
    Window window = SwingUtilities.getWindowAncestor(parent);
    if(window != null)
    {
      Point location = window.getLocation();
      return new Point(location.x + window.getWidth() - 320, location.y + window.getHeight() - 100);
    }
    return new Point(100, 100);
  }

  private static Color getTypeColor(MessageType type)
  {
    return switch(type)
    {
      case SUCCESS -> new Color(46, 139, 87);   // 绿色
      case ERROR -> new Color(220, 20, 60);     // 红色
      case WARNING -> new Color(255, 140, 0);   // 橙色
      case INFO -> new Color(30, 144, 255);     // 蓝色
    };
  }

  /**
   * 消息类型枚举
   */
  public enum MessageType
  {
    SUCCESS, ERROR, WARNING, INFO
  }
}
