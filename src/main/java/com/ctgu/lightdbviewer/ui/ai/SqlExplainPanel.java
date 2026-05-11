package com.ctgu.lightdbviewer.ui.ai;

import com.ctgu.lightdbviewer.ai.AiService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * SQL 解释结果弹出面板 — 选中 SQL → 右键 → AI 解释
 */
public class SqlExplainPanel extends JPanel
{
  private final JTextArea resultArea;
  private final JButton closeBtn;
  private final JLabel statusLabel;

  public SqlExplainPanel()
  {
    setLayout(new BorderLayout());
    setBorder(new EmptyBorder(8, 8, 8, 8));

    JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    statusLabel = new JLabel("SQL 解释");
    statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
    topPanel.add(statusLabel);

    resultArea = new JTextArea(8, 50);
    resultArea.setEditable(false);
    resultArea.setLineWrap(true);
    resultArea.setWrapStyleWord(true);
    resultArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
    resultArea.setBorder(new EmptyBorder(6, 6, 6, 6));

    JScrollPane scrollPane = new JScrollPane(resultArea);

    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    closeBtn = new JButton("关闭");
    bottomPanel.add(closeBtn);

    add(topPanel, BorderLayout.NORTH);
    add(scrollPane, BorderLayout.CENTER);
    add(bottomPanel, BorderLayout.SOUTH);
  }

  /**
   * 执行 SQL 解释
   *
   * @param sql            待解释的 SQL
   * @param onDispose      面板使用完毕后回调（如关闭弹出窗口）
   */
  public void explain(String sql, Runnable onDispose)
  {
    resultArea.setText("正在分析 ...");
    statusLabel.setText("SQL 解释 — 分析中...");

    AiService ai = AiService.getInstance();
    if(!ai.isAvailable())
    {
      resultArea.setText("AI 服务未启用。请在 工具 → 选项 → AI 中配置 API Key。");
      statusLabel.setText("SQL 解释");
      return;
    }

    closeBtn.addActionListener(e -> {
      if(onDispose != null)
      {
        onDispose.run();
      }
    });

    new SwingWorker<Void, Void>()
    {
      @Override
      protected Void doInBackground()
      {
        try
        {
          String systemPrompt = "用中文简洁解释以下 SQL 语句的作用、涉及的表、可能的性能影响。";
          String answer = ai.chat(systemPrompt, sql);
          SwingUtilities.invokeLater(() -> {
            resultArea.setText(answer);
            statusLabel.setText("SQL 解释");
          });
        }
        catch(Exception e)
        {
          SwingUtilities.invokeLater(() -> {
            resultArea.setText("AI 请求失败: " + e.getMessage());
            statusLabel.setText("SQL 解释");
          });
        }
        return null;
      }
    }.execute();
  }
}
