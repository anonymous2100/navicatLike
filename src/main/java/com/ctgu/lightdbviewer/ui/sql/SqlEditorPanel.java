package com.ctgu.lightdbviewer.ui.sql;

import com.ctgu.lightdbviewer.util.FontManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.function.BiConsumer;

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
    add(scrollPane, BorderLayout.CENTER);
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


