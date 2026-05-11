package com.ctgu.lightdbviewer.ui.ai;

import com.ctgu.lightdbviewer.ai.AiService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * AI 错误诊断对话框 — 流式展示分析结果
 */
public class AiErrorDialog extends JDialog
{
  private final JTextArea outputArea;
  private final JButton stopBtn;
  private final JButton closeBtn;
  private final JButton copySqlBtn;
  private final JTextField askField;
  private final JButton askBtn;
  private volatile boolean stopped;
  private String lastAnswer = "";
  private dev.langchain4j.memory.ChatMemory memory;

  public AiErrorDialog(Frame owner, String sql, String errorMessage)
  {
    super(owner, "AI 错误分析", false);
    setSize(650, 500);
    setMinimumSize(new Dimension(500, 350));
    setLocationRelativeTo(owner);

    outputArea = new JTextArea();
    outputArea.setEditable(false);
    outputArea.setLineWrap(true);
    outputArea.setWrapStyleWord(true);
    outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
    outputArea.setBorder(new EmptyBorder(8, 8, 8, 8));

    JScrollPane scrollPane = new JScrollPane(outputArea);

    // 顶部信息栏
    JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    topPanel.add(new JLabel("正在分析 SQL 错误 ..."));

    // 底部按钮栏
    JPanel bottomPanel = new JPanel(new BorderLayout(8, 4));
    JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
    copySqlBtn = new JButton("复制修正 SQL");
    copySqlBtn.setEnabled(false);
    stopBtn = new JButton("停止");
    closeBtn = new JButton("关闭");
    btnRow.add(copySqlBtn);
    btnRow.add(stopBtn);
    btnRow.add(closeBtn);

    // 追问栏
    JPanel askPanel = new JPanel(new BorderLayout(6, 0));
    askPanel.setBorder(new EmptyBorder(4, 8, 4, 8));
    askField = new JTextField();
    askBtn = new JButton("追问");
    askBtn.setEnabled(false);
    askPanel.add(askField, BorderLayout.CENTER);
    askPanel.add(askBtn, BorderLayout.EAST);
    bottomPanel.add(askPanel, BorderLayout.NORTH);
    bottomPanel.add(btnRow, BorderLayout.SOUTH);

    stopBtn.addActionListener(e -> { stopped = true; stopBtn.setEnabled(false); });
    closeBtn.addActionListener(e -> dispose());
    copySqlBtn.addActionListener(e -> {
      if(!lastAnswer.isBlank())
      {
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new java.awt.datatransfer.StringSelection(lastAnswer), null);
      }
    });

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(topPanel, BorderLayout.NORTH);
    getContentPane().add(scrollPane, BorderLayout.CENTER);
    getContentPane().add(bottomPanel, BorderLayout.SOUTH);

    startAnalysis(sql, errorMessage);

    askBtn.addActionListener(e -> {
      String q = askField.getText().trim();
      if(q.isBlank() || memory == null)
      {
        return;
      }
      askField.setText("");
      stopped = false;
      stopBtn.setEnabled(true);
      askBtn.setEnabled(false);
      outputArea.append("\n\n--- 追问 ---\n");
      new SwingWorker<Void, Void>()
      {
        @Override
        protected Void doInBackground()
        {
          try
          {
            memory.add(dev.langchain4j.data.message.UserMessage.from(q));
            String answer = AiService.getInstance().chatWithMemory(memory);
            memory.add(dev.langchain4j.data.message.AiMessage.from(answer));
            if(!stopped)
            {
              SwingUtilities.invokeLater(() -> {
                outputArea.append(answer);
                askBtn.setEnabled(true);
                stopBtn.setEnabled(false);
              });
            }
          }
          catch(Exception ex)
          {
            SwingUtilities.invokeLater(() -> outputArea.append("\n错误: " + ex.getMessage()));
          }
          return null;
        }
      }.execute();
    });

    askField.addActionListener(e -> askBtn.doClick());
  }

  private void startAnalysis(String sql, String errorMessage)
  {
    AiService ai = AiService.getInstance();
    String dbType = ai.getDbTypeVersion();
    memory = ai.createMemory();
    String systemPrompt = "你是数据库专家，分析 SQL 错误并给出修正方案。用中文回答。";
    memory.add(dev.langchain4j.data.message.SystemMessage.from(systemPrompt));
    String userMsg = "数据库类型：" + dbType + "\n执行 SQL：\n" + sql + "\n错误信息：\n" + errorMessage;
    memory.add(dev.langchain4j.data.message.UserMessage.from(userMsg));

    new SwingWorker<Void, Void>()
    {
      @Override
      protected Void doInBackground()
      {
        try
        {
          String answer = ai.chatWithMemory(memory);
          if(!stopped)
          {
            lastAnswer = answer;
            SwingUtilities.invokeLater(() -> {
              outputArea.setText(answer);
              copySqlBtn.setEnabled(true);
              askBtn.setEnabled(true);
              stopBtn.setEnabled(false);
            });
            if(memory != null)
            {
              memory.add(dev.langchain4j.data.message.AiMessage.from(answer));
            }
          }
        }
        catch(Exception e)
        {
          SwingUtilities.invokeLater(() -> {
            outputArea.setText("AI 请求失败: " + e.getMessage());
            stopBtn.setEnabled(false);
          });
        }
        return null;
      }
    }.execute();
  }
}
