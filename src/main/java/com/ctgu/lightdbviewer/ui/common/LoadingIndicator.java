package com.ctgu.lightdbviewer.ui.common;

import javax.swing.*;
import java.awt.*;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 *
 * 加载状态指示器
 * <p>
 * 显示加载动画和状态文本
 */
public class LoadingIndicator extends JPanel
{
  private final JProgressBar progressBar;
  private final JLabel statusLabel;
  private final Timer animationTimer;
  private int progress = 0;

  public LoadingIndicator()
  {
    setLayout(new BorderLayout(10, 10));
    setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    setPreferredSize(new Dimension(300, 80));
    // 进度条
    progressBar = new JProgressBar();
    progressBar.setIndeterminate(true);
    progressBar.setStringPainted(false);
    // 状态标签
    statusLabel = new JLabel("加载中...");
    statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
    add(progressBar, BorderLayout.CENTER);
    add(statusLabel, BorderLayout.SOUTH);
    // 创建动画效果
    animationTimer = new Timer(100, e -> {
      progress = (progress + 1) % 100;
      if(!progressBar.isIndeterminate())
      {
        progressBar.setValue(progress);
      }
    });
  }

  /**
   * 设置状态文本
   *
   * @param text 状态文本
   */
  public void setStatusText(String text)
  {
    statusLabel.setText(text != null ? text : "加载中...");
  }

  /**
   * 设置进度
   *
   * @param value 进度值 (0-100)
   */
  public void setProgress(int value)
  {
    progress = value;
    progressBar.setIndeterminate(false);
    progressBar.setValue(value);
    progressBar.setStringPainted(true);
  }

  /**
   * 设置为不确定进度模式
   */
  public void setIndeterminate(boolean indeterminate)
  {
    progressBar.setIndeterminate(indeterminate);
    progressBar.setStringPainted(!indeterminate);
  }

  /**
   * 开始加载动画
   */
  public void startLoading()
  {
    progress = 0;
    progressBar.setIndeterminate(true);
    progressBar.setStringPainted(false);
    animationTimer.start();
  }

  /**
   * 停止加载动画
   */
  public void stopLoading()
  {
    animationTimer.stop();
    progressBar.setValue(100);
    progressBar.setIndeterminate(false);
  }

  /**
   * 显示在对话框中
   *
   * @param parent 父组件
   * @param title  对话框标题
   * @param message 消息文本
   * @return 创建的对话框
   */
  public static JDialog showLoadingDialog(Component parent, String title, String message)
  {
    JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), title, true);
    LoadingIndicator indicator = new LoadingIndicator();
    indicator.setStatusText(message);
    indicator.startLoading();
    dialog.add(indicator);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    return dialog;
  }
}
