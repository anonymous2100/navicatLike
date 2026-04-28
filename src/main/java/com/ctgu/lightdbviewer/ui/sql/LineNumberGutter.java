package com.ctgu.lightdbviewer.ui.sql;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Rectangle2D;

/**
 * SQL 编辑器行号边栏（作为 JScrollPane rowHeader 使用，随编辑器同步滚动）
 * <p>
 * 特性：
 * <ul>
 *   <li>行号与编辑器字体同步</li>
 *   <li>自适应总行数宽度（行数增加时自动扩宽）</li>
 *   <li>Navicat 风格：灰色背+ 浅色分隔/li>
 * </ul>
 */

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class LineNumberGutter extends JPanel
{
  private static final int RIGHT_PADDING = 6;
  private static final Color BG_COLOR = new Color(0xF0F0F0);
  private static final Color FG_COLOR = new Color(0x888888);
  private static final Color BORDER_COLOR = new Color(0xD0D0D0);
  private final JTextPane textPane;

  public LineNumberGutter(JTextPane textPane)
  {
    this.textPane = textPane;
    setBackground(BG_COLOR);
    setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR));
    // 文档变更 重新计算宽度并重
    textPane.getDocument().addDocumentListener(new DocumentListener()
    {
      @Override
      public void insertUpdate(DocumentEvent e)
      {
        SwingUtilities.invokeLater(LineNumberGutter.this::updateWidthAndRepaint);
      }

      @Override
      public void removeUpdate(DocumentEvent e)
      {
        SwingUtilities.invokeLater(LineNumberGutter.this::updateWidthAndRepaint);
      }

      @Override
      public void changedUpdate(DocumentEvent e)
      {
        SwingUtilities.invokeLater(LineNumberGutter.this::updateWidthAndRepaint);
      }
    });
    // 编辑器大小改重绘
    textPane.addComponentListener(new ComponentAdapter()
    {
      @Override
      public void componentResized(ComponentEvent e)
      {
        repaint();
      }
    });
    updateWidthAndRepaint();
  }

  private void updateWidthAndRepaint()
  {
    int lineCount = textPane.getDocument().getDefaultRootElement().getElementCount();
    FontMetrics fm = getFontMetrics(getLineFont());
    int digits = String.valueOf(Math.max(lineCount, 99)).length();
    int newWidth = fm.charWidth('0') * digits + RIGHT_PADDING * 2;
    Dimension current = getPreferredSize();
    if(current.width != newWidth)
    {
      setPreferredSize(new Dimension(newWidth, current.height));
      revalidate();
    }
    repaint();
  }

  @Override
  public Dimension getPreferredSize()
  {
    // 高度跟随编辑器实际高度，确保 row header 不截
    Dimension d = super.getPreferredSize();
    return new Dimension(d.width, textPane.getHeight());
  }

  @Override
  protected void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D)g.create();
    try
    {
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      Font lineFont = getLineFont();
      g2.setFont(lineFont);
      g2.setColor(FG_COLOR);

      FontMetrics fm = g2.getFontMetrics(lineFont);
      Element root = textPane.getDocument().getDefaultRootElement();
      int lineCount = root.getElementCount();
      int width = getWidth();

      for(int i = 0; i < lineCount; i++)
      {
        Element lineElem = root.getElement(i);
        try
        {
          Rectangle2D r = textPane.modelToView2D(lineElem.getStartOffset());
          if(r == null)
            continue;

          int y = (int)r.getY() + fm.getAscent();
          String num = String.valueOf(i + 1);
          int x = width - fm.stringWidth(num) - RIGHT_PADDING;
          g2.drawString(num, x, y);
        }
        catch(BadLocationException ex)
        {
          break;
        }
      }
    }
    finally
    {
      g2.dispose();
    }
  }

  /**
   * 行号字体：与编辑器字体同族、同大小但略1pt
   */
  private Font getLineFont()
  {
    Font editorFont = textPane.getFont();
    if(editorFont == null)
    {
      return new Font("Monospaced", Font.PLAIN, 11);
    }
    return editorFont.deriveFont(Math.max(9f, editorFont.getSize2D() - 1f));
  }
}



