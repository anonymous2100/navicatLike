package com.ctgu.lightdbviewer.ui.ai;

import com.ctgu.lightdbviewer.ai.AiService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * NL2SQL 输入面板 — 自然语言生成 SQL，嵌入 SqlEditorPanel 底部
 */
public class Nl2SqlPanel extends JPanel
{
  private final JTextArea inputArea;
  private final JTextArea outputArea;
  private final JButton generateBtn;
  private final JButton stopBtn;
  private final JButton insertBtn;
  private final java.util.function.Consumer<String> onInsert;
  private volatile boolean stopped;

  public Nl2SqlPanel(java.util.function.Consumer<String> onInsert)
  {
    this.onInsert = onInsert;
    setLayout(new BorderLayout(6, 0));
    setBorder(new EmptyBorder(6, 4, 6, 4));

    inputArea = new JTextArea(2, 40);
    inputArea.setLineWrap(true);
    inputArea.setWrapStyleWord(true);
    inputArea.setFont(new Font("SansSerif", Font.PLAIN, 14));

    outputArea = new JTextArea(6, 40);
    outputArea.setEditable(false);
    outputArea.setLineWrap(true);
    outputArea.setWrapStyleWord(true);
    outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
    outputArea.setForeground(new Color(0, 100, 0));

    generateBtn = new JButton("AI 生成 SQL");
    stopBtn = new JButton("停止");
    stopBtn.setEnabled(false);
    insertBtn = new JButton("插入编辑器");
    insertBtn.setEnabled(false);

    JLabel inputLabel = new JLabel("自然语言描述：");
    JPanel topPanel = new JPanel(new BorderLayout(0, 2));
    topPanel.add(inputLabel, BorderLayout.NORTH);

    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
        new JScrollPane(inputArea), new JScrollPane(outputArea));
    splitPane.setResizeWeight(0.3);
    splitPane.setDividerLocation(50);
    splitPane.setBorder(null);
    topPanel.add(splitPane, BorderLayout.CENTER);
    add(topPanel, BorderLayout.CENTER);

    JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
    bottomRow.add(generateBtn);
    bottomRow.add(stopBtn);
    bottomRow.add(insertBtn);
    add(bottomRow, BorderLayout.SOUTH);

    generateBtn.addActionListener(e -> startGeneration());
    stopBtn.addActionListener(e -> { stopped = true; stopBtn.setEnabled(false); });
    insertBtn.addActionListener(e -> {
      String sql = outputArea.getText().trim();
      if(!sql.isBlank() && onInsert != null)
      {
        onInsert.accept(sql);
      }
    });
    inputArea.addKeyListener(new java.awt.event.KeyAdapter()
    {
      @Override
      public void keyPressed(java.awt.event.KeyEvent e)
      {
        if(e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER && e.isControlDown())
        {
          generateBtn.doClick();
          e.consume();
        }
      }
    });
  }

  private void startGeneration()
  {
    String input = inputArea.getText().trim();
    if(input.isBlank())
    {
      JOptionPane.showMessageDialog(this, "请输入自然语言描述", "提示", JOptionPane.WARNING_MESSAGE);
      return;
    }
    AiService ai = AiService.getInstance();
    if(!ai.isAvailable())
    {
      outputArea.setText("AI 服务未启用。请在 工具 → 选项 → AI 中配置 API Key。");
      return;
    }
    outputArea.setText("");
    generateBtn.setEnabled(false);
    stopBtn.setEnabled(true);
    insertBtn.setEnabled(false);
    stopped = false;

    String schema = ai.collectSchemaSummary();
    String systemPrompt = "你是 SQL 生成助手。根据用户描述和 Schema 生成 SQL。只输出 SQL，不要解释。生成 " + ai.getDbTypeVersion() + " 语法的 SQL。";
    String userMsg = "当前数据库：" + ai.getDbTypeVersion() + "\nSchema 信息：\n" + schema + "\n用户需求：" + input;

    ai.chatStreaming(systemPrompt, userMsg, new StreamingResponseHandler<>()
    {
      @Override
      public void onNext(String token)
      {
        if(!stopped)
        {
          SwingUtilities.invokeLater(() -> outputArea.append(token));
        }
      }

      @Override
      public void onComplete(dev.langchain4j.model.output.Response<AiMessage> response)
      {
        SwingUtilities.invokeLater(() -> {
          generateBtn.setEnabled(true);
          stopBtn.setEnabled(false);
          insertBtn.setEnabled(true);
        });
      }

      @Override
      public void onError(Throwable error)
      {
        SwingUtilities.invokeLater(() -> {
          outputArea.append("\n[错误] " + error.getMessage());
          generateBtn.setEnabled(true);
          stopBtn.setEnabled(false);
        });
      }
    });
  }

  @Override
  public void setVisible(boolean visible)
  {
    super.setVisible(visible);
    if(visible)
    {
      inputArea.requestFocusInWindow();
    }
  }
}
