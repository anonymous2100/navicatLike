package com.ctgu.lightdbviewer.ui.sql;

import com.ctgu.lightdbviewer.ai.AiService;
import com.ctgu.lightdbviewer.ui.ai.Nl2SqlPanel;
import com.ctgu.lightdbviewer.ui.ai.SqlExplainPanel;
import com.ctgu.lightdbviewer.util.FontManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * SQL 编辑器面基于 JTextPane，支持语法高+ 行号边栏
 */

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class SqlEditorPanel extends JPanel
{
  private final JTextPane sqlPane;
  private final Timer highlightTimer;
  private final Nl2SqlPanel nl2SqlPanel;
  private final JToggleButton aiToggleBtn;

  public SqlEditorPanel()
  {
    setLayout(new BorderLayout());
    sqlPane = new JTextPane();
    sqlPane.setFont(FontManager.getCurrentEditorFont());
    // 语法高亮延时触发00ms 防抖
    highlightTimer = new Timer(300, e -> doHighlight());
    highlightTimer.setRepeats(false);
    sqlPane.getDocument().addDocumentListener(new DocumentListener()
    {
      @Override
      public void insertUpdate(DocumentEvent e)
      {
        highlightTimer.restart();
      }

      @Override
      public void removeUpdate(DocumentEvent e)
      {
        highlightTimer.restart();
      }

      @Override
      public void changedUpdate(DocumentEvent e)
      {
        highlightTimer.restart();
      }
    });
    // ── 行号边栏（作JScrollPane rowHeader 随编辑器同步滚动
    JScrollPane scrollPane = new JScrollPane(sqlPane);
    scrollPane.setRowHeaderView(new LineNumberGutter(sqlPane));
    scrollPane.setBorder(BorderFactory.createEmptyBorder());

    // AI 生成 SQL 面板
    nl2SqlPanel = new Nl2SqlPanel(sql -> {
      sqlPane.setText(sql);
      doHighlight();
    });
    nl2SqlPanel.setVisible(false);

    // AI 切换按钮
    aiToggleBtn = new JToggleButton("AI");
    aiToggleBtn.setToolTipText("AI 助手：自然语言生成 SQL");
    aiToggleBtn.setFocusable(false);
    aiToggleBtn.addActionListener(e -> nl2SqlPanel.setVisible(aiToggleBtn.isSelected()));

    // 右键菜单 — AI 解释
    sqlPane.addMouseListener(new java.awt.event.MouseAdapter()
    {
      @Override
      public void mousePressed(java.awt.event.MouseEvent e)
      {
        if(e.isPopupTrigger())
        {
          showPopup(e);
        }
      }

      @Override
      public void mouseReleased(java.awt.event.MouseEvent e)
      {
        if(e.isPopupTrigger())
        {
          showPopup(e);
        }
      }

      private void showPopup(java.awt.event.MouseEvent e)
      {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem explainItem = new JMenuItem("AI 解释 SQL");
        explainItem.setEnabled(AiService.getInstance().isAvailable());
        explainItem.addActionListener(ev -> {
          String selectedSql = getSelectedSqlOrAll();
          if(!selectedSql.isBlank())
          {
            SqlExplainPanel explainPanel = new SqlExplainPanel();
            JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(SqlEditorPanel.this), "AI SQL 解释", Dialog.ModalityType.MODELESS);
            dlg.setContentPane(explainPanel);
            dlg.setSize(550, 350);
            dlg.setLocationRelativeTo(SqlEditorPanel.this);
            explainPanel.explain(selectedSql, dlg::dispose);
            dlg.setVisible(true);
          }
        });
        menu.add(explainItem);
        menu.show(sqlPane, e.getX(), e.getY());
      }
    });

    JPanel centerArea = new JPanel(new BorderLayout());
    centerArea.add(scrollPane, BorderLayout.CENTER);
    centerArea.add(nl2SqlPanel, BorderLayout.SOUTH);

    add(centerArea, BorderLayout.CENTER);
  }

  /**
   * 获取 AI 切换按钮（供工具栏或调用方使用）
   */
  public JToggleButton getAiToggleBtn()
  {
    return aiToggleBtn;
  }

  public void setSql(String sql)
  {
    sqlPane.setText(sql);
    // 立即高亮（无需等待防抖定时器）
    SwingUtilities.invokeLater(this::doHighlight);
  }

  public String getSqlText()
  {
    return sqlPane.getText();
  }

  public String getSelectedSqlOrAll()
  {
    String selected = sqlPane.getSelectedText();
    if(selected != null && !selected.isBlank())
    {
      return selected;
    }
    return getSqlText();
  }

  public void addSqlChangeListener(Runnable onChange)
  {
    sqlPane.getDocument().addDocumentListener(new DocumentListener()
    {
      @Override
      public void insertUpdate(DocumentEvent e)
      {
        onChange.run();
      }

      @Override
      public void removeUpdate(DocumentEvent e)
      {
        onChange.run();
      }

      @Override
      public void changedUpdate(DocumentEvent e)
      {
        onChange.run();
      }
    });
  }

  /**
   * 注册光标位置变化回调，参数为 (行号, 列号)，均1 开始计数   * 典型用途：将行/列号显示在状态栏
   */
  public void addCaretPositionListener(BiConsumer<Integer, Integer> listener)
  {
    sqlPane.addCaretListener(e -> {
      try
      {
        int pos = sqlPane.getCaretPosition();
        Element root = sqlPane.getDocument().getDefaultRootElement();
        int lineIndex = root.getElementIndex(pos);
        Element lineElem = root.getElement(lineIndex);
        int col = pos - lineElem.getStartOffset() + 1;
        listener.accept(lineIndex + 1, col);
      }
      catch(Exception ex)
      {
        // 忽略布局未完成时的异
      }
    });
  }

  public void focusEditor()
  {
    sqlPane.requestFocusInWindow();
  }

  /**
   * 对当前文档执行语法高
   */
  private void doHighlight()
  {
    SqlSyntaxHighlighter.highlight((StyledDocument)sqlPane.getDocument());
  }
}


