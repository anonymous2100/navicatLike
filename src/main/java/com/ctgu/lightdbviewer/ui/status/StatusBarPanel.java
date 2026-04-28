package com.ctgu.lightdbviewer.ui.status;

import javax.swing.*;
import java.awt.*;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class StatusBarPanel extends JPanel
{
  private final JLabel messageLabel = new JLabel("就绪");
  private final JLabel elapsedLabel = new JLabel("耗时: -");
  private final JLabel contextLabel = new JLabel("未连接");
  /**
   * SQL 编辑器光标位置（ x,  y），仅在 QueryTab 激活时显示
   */
  private final JLabel editorPosLabel = new JLabel("");

  public StatusBarPanel()
  {
    setLayout(new BorderLayout(8, 0));
    setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")),
        BorderFactory.createEmptyBorder(3, 8, 3, 8)));
    JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
    rightGroup.setOpaque(false);
    rightGroup.add(editorPosLabel);
    rightGroup.add(elapsedLabel);
    rightGroup.add(contextLabel);
    add(messageLabel, BorderLayout.WEST);
    add(rightGroup, BorderLayout.EAST);
  }

  public void setMessage(String msg)
  {
    messageLabel.setText(msg);
  }

  public void setContext(String context)
  {
    contextLabel.setText(context);
  }

  public void setElapsedMs(long elapsedMs)
  {
    elapsedLabel.setText("耗时: " + elapsedMs + " ms");
  }

  public void clearElapsed()
  {
    elapsedLabel.setText("耗时: -");
  }

  /**
   * 显示 SQL 编辑器光标位置（QueryTab 激活时调用）
   *
   * @param line 行号-based
   * @param col  列号-based
   */
  public void setEditorPosition(int line, int col)
  {
    editorPosLabel.setText(" " + line + "   " + col);
  }

  /**
   * 清除编辑器位置显示（切换离开 QueryTab 时调用）
   */
  public void clearEditorPosition()
  {
    editorPosLabel.setText("");
  }
}

