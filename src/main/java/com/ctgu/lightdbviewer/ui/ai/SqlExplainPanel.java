package com.ctgu.lightdbviewer.ui.ai;

import com.ctgu.lightdbviewer.ai.AiService;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.Link;
import org.commonmark.node.ListItem;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * SQL 解释结果弹出面板 — 选中 SQL → 右键 → AI 解释
 */
public class SqlExplainPanel extends JPanel
{
  private final JEditorPane resultPane;
  private final JButton closeBtn;
  private final JButton copyBtn;
  private final JButton exportBtn;
  private final JLabel statusLabel;
  private String rawMarkdown;
  private String plainText;
  private final Parser mdParser = Parser.builder().build();
  private final HtmlRenderer mdHtmlRenderer = HtmlRenderer.builder().build();

  public SqlExplainPanel()
  {
    setLayout(new BorderLayout());
    setBorder(new EmptyBorder(8, 8, 8, 8));

    JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    statusLabel = new JLabel("SQL 解释");
    statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
    topPanel.add(statusLabel);

    resultPane = new JEditorPane();
    resultPane.setEditable(false);
    resultPane.setContentType("text/html");
    resultPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
    resultPane.setFont(new Font("SansSerif", Font.PLAIN, 14));
    resultPane.setBorder(new EmptyBorder(6, 6, 6, 6));
    ((HTMLEditorKit)resultPane.getEditorKit()).setAutoFormSubmission(false);

    JScrollPane scrollPane = new JScrollPane(resultPane);

    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    copyBtn = new JButton("复制");
    copyBtn.addActionListener(e -> copyContent());
    exportBtn = new JButton("导出");
    exportBtn.addActionListener(e -> exportContent());
    closeBtn = new JButton("关闭");
    bottomPanel.add(copyBtn);
    bottomPanel.add(exportBtn);
    bottomPanel.add(closeBtn);

    add(topPanel, BorderLayout.NORTH);
    add(scrollPane, BorderLayout.CENTER);
    add(bottomPanel, BorderLayout.SOUTH);
  }

  private void copyContent()
  {
    if(plainText == null || plainText.isEmpty() || plainText.startsWith("正在分析"))
    {
      return;
    }
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(plainText), null);
    copyBtn.setText("已复制");
    Timer t = new Timer(2000, e -> copyBtn.setText("复制"));
    t.setRepeats(false);
    t.start();
  }

  private void exportContent()
  {
    if(rawMarkdown == null || rawMarkdown.isEmpty() || rawMarkdown.startsWith("正在分析"))
    {
      return;
    }
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("导出 AI 解释");
    chooser.setSelectedFile(new File("sql_explain.md"));
    if(chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
    {
      return;
    }
    File file = chooser.getSelectedFile();
    if(file == null)
    {
      return;
    }
    try
    {
      String content = "# SQL 解释\n\n" + rawMarkdown;
      try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
      {
        w.write(content);
      }
      exportBtn.setText("已导出");
      Timer t = new Timer(2000, e -> exportBtn.setText("导出"));
      t.setRepeats(false);
      t.start();
    }
    catch(Exception ex)
    {
      JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
    }
  }

  private String renderHtml(String md)
  {
    String body = mdHtmlRenderer.render(mdParser.parse(md));
    return "<html><body style=\"font-family:SansSerif;font-size:14pt;padding:6px\">" + body + "</body></html>";
  }

  private String extractPlainText(String md)
  {
    StringBuilder sb = new StringBuilder();
    mdParser.parse(md).accept(new AbstractVisitor()
    {
      @Override
      public void visit(Text text)
      {
        sb.append(text.getLiteral());
      }

      @Override
      public void visit(SoftLineBreak softLineBreak)
      {
        sb.append('\n');
      }

      @Override
      public void visit(HardLineBreak hardLineBreak)
      {
        sb.append('\n');
      }

      @Override
      public void visit(Link link)
      {
        sb.append(link.getTitle());
      }

      @Override
      public void visit(Heading heading)
      {
        visitChildren(heading);
        sb.append("\n\n");
      }

      @Override
      public void visit(Paragraph paragraph)
      {
        visitChildren(paragraph);
        sb.append("\n\n");
      }

      @Override
      public void visit(ListItem listItem)
      {
        sb.append("  ");
        visitChildren(listItem);
        sb.append('\n');
      }
    });
    return sb.toString().strip();
  }

  /**
   * 执行 SQL 解释
   *
   * @param sql            待解释的 SQL
   * @param onDispose      面板使用完毕后回调（如关闭弹出窗口）
   */
  public void explain(String sql, Runnable onDispose)
  {
    rawMarkdown = "";
    plainText = "";
    resultPane.setText("<html><body style=\"font-family:SansSerif;font-size:14pt;padding:6px\"><i>正在分析 ...</i></body></html>");
    statusLabel.setText("SQL 解释 — 分析中...");

    AiService ai = AiService.getInstance();
    if(!ai.isAvailable())
    {
      resultPane.setText("<html><body style=\"font-family:SansSerif;font-size:14pt;padding:6px;color:#B40000\">"
          + "AI 服务未启用。请在 工具 → 选项 → AI 中配置 API Key。</body></html>");
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
            rawMarkdown = answer;
            plainText = extractPlainText(answer);
            resultPane.setText(renderHtml(answer));
            resultPane.setCaretPosition(0);
            statusLabel.setText("SQL 解释");
          });
        }
        catch(Exception e)
        {
          SwingUtilities.invokeLater(() -> {
            resultPane.setText("<html><body style=\"font-family:SansSerif;font-size:14pt;padding:6px;color:#B40000\">"
                + "AI 请求失败: " + e.getMessage() + "</body></html>");
            statusLabel.setText("SQL 解释");
          });
        }
        return null;
      }
    }.execute();
  }
}
