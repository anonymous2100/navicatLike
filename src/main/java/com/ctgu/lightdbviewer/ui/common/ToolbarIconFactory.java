package com.ctgu.lightdbviewer.ui.common;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 *
 * 工具栏图标工厂 — 生成 Navicat 风格的 32×32 彩色图标
 */
public class ToolbarIconFactory
{
  private static final int S = 32;

  private static Graphics2D aa(Graphics g)
  {
    Graphics2D g2 = (Graphics2D)g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    return g2;
  }

  /**
   * 创建 Navicat 风格的工具栏按钮：上方图标 + 下方文字
   */
  public static JButton createToolbarButton(String text, Icon icon, String tooltip)
  {
    JButton btn = new JButton();
    btn.setLayout(new BoxLayout(btn, BoxLayout.Y_AXIS));
    btn.setToolTipText(tooltip);
    btn.setFocusable(false);
    // 图标区域 — 固定高度，确保不被压缩
    JLabel iconLabel = new JLabel(icon);
    iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    int iconH = icon.getIconHeight();
    int iconW = icon.getIconWidth();
    Dimension iconDim = new Dimension(iconW, iconH);
    iconLabel.setPreferredSize(iconDim);
    iconLabel.setMinimumSize(iconDim);
    iconLabel.setMaximumSize(iconDim);
    // 文字区域
    JLabel textLabel = new JLabel(text);
    textLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    textLabel.setFont(textLabel.getFont().deriveFont(11f));
    int textH = textLabel.getFontMetrics(textLabel.getFont()).getHeight();
    btn.add(Box.createVerticalStrut(2));
    btn.add(iconLabel);
    btn.add(Box.createVerticalStrut(1));
    btn.add(textLabel);
    btn.add(Box.createVerticalStrut(2));
    // 按钮总尺寸
    int totalH = 2 + iconH + 1 + textH + 4; // struts + icon + gap + text + bottom padding
    Dimension pref = new Dimension(60, Math.max(totalH, 54));
    btn.setPreferredSize(pref);
    btn.setMinimumSize(pref);
    btn.setMaximumSize(pref);
    btn.setMargin(new Insets(0, 2, 0, 2));
    return btn;
  }

  public static Icon connectIcon()
  {
    return new Icon()
    {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y)
      {
        Graphics2D g2 = aa(g);
        // 地球
        int d = 22, ox = x + 5, oy = y + 3;
        g2.setColor(new Color(0x4A90D9));
        g2.fillOval(ox, oy, d, d);
        g2.setColor(new Color(0x3670B0));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(ox, oy, d, d);
        // 经纬线
        g2.setColor(new Color(255, 255, 255, 120));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(ox + d / 2, oy + 1, ox + d / 2, oy + d - 1);
        g2.drawLine(ox + 2, oy + d / 2, ox + d - 2, oy + d / 2);
        g2.drawArc(ox + 3, oy, d - 8, d, 90, -180);
        g2.drawArc(ox + 5, oy, d + 1, d, 90, 180);
        // 绿色加号（右下角）
        g2.setColor(new Color(0x4CAF50));
        g2.fillOval(x + 19, y + 18, 12, 12);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(x + 22, y + 24, x + 28, y + 24);
        g2.drawLine(x + 25, y + 21, x + 25, y + 27);
        g2.dispose();
      }

      @Override
      public int getIconWidth()
      {
        return S;
      }

      @Override
      public int getIconHeight()
      {
        return S;
      }
    };
  }

  public static Icon newQueryIcon()
  {
    return new Icon()
    {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y)
      {
        Graphics2D g2 = aa(g);
        // 文档
        int ox = x + 6, oy = y + 2, w = 18, h = 24;
        g2.setColor(new Color(0xE8F0FE));
        g2.fillRoundRect(ox, oy, w, h, 3, 3);
        g2.setColor(new Color(0x4A90D9));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(ox, oy, w, h, 3, 3);
        // SQL 文字
        g2.setColor(new Color(0x4A90D9));
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        g2.drawString("SQL", ox + 2, oy + 13);
        // 横线
        g2.setColor(new Color(0xB0CCE8));
        g2.drawLine(ox + 3, oy + 17, ox + w - 3, oy + 17);
        g2.drawLine(ox + 3, oy + 20, ox + w - 6, oy + 20);
        // 绿色加号
        g2.setColor(new Color(0x4CAF50));
        g2.fillOval(x + 19, y + 18, 12, 12);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(x + 22, y + 24, x + 28, y + 24);
        g2.drawLine(x + 25, y + 21, x + 25, y + 27);
        g2.dispose();
      }

      @Override
      public int getIconWidth()
      {
        return S;
      }

      @Override
      public int getIconHeight()
      {
        return S;
      }
    };
  }

  public static Icon tableIcon()
  {
    return new Icon()
    {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y)
      {
        Graphics2D g2 = aa(g);
        int ox = x + 4, oy = y + 3, w = 24, h = 22;
        // 表头
        g2.setColor(new Color(0x4A90D9));
        g2.fillRect(ox, oy, w, 6);
        // 表体
        g2.setColor(new Color(0xD6E9F8));
        g2.fillRect(ox, oy + 6, w, h - 6);
        // 边框
        g2.setColor(new Color(0x3B7DD8));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect(ox, oy, w, h);
        // 横线
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(ox, oy + 6, ox + w, oy + 6);
        g2.drawLine(ox, oy + 11, ox + w, oy + 11);
        g2.drawLine(ox, oy + 16, ox + w, oy + 16);
        // 竖线
        g2.drawLine(ox + 8, oy + 6, ox + 8, oy + h);
        g2.drawLine(ox + 16, oy + 6, ox + 16, oy + h);
        g2.dispose();
      }

      @Override
      public int getIconWidth()
      {
        return S;
      }

      @Override
      public int getIconHeight()
      {
        return S;
      }
    };
  }

  public static Icon viewIcon()
  {
    return new Icon()
    {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y)
      {
        Graphics2D g2 = aa(g);
        g2.setStroke(new BasicStroke(2.5f));
        // 左圆
        g2.setColor(new Color(0x4A90D9));
        g2.drawOval(x + 2, y + 6, 16, 16);
        // 右圆
        g2.setColor(new Color(0x4A90D9));
        g2.drawOval(x + 13, y + 6, 16, 16);
        // 交叉区域填充
        g2.setColor(new Color(0x4A90D9, true));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
        g2.fillOval(x + 2, y + 6, 16, 16);
        g2.fillOval(x + 13, y + 6, 16, 16);
        g2.dispose();
      }

      @Override
      public int getIconWidth()
      {
        return S;
      }

      @Override
      public int getIconHeight()
      {
        return S;
      }
    };
  }

  public static Icon functionIcon()
  {
    return new Icon()
    {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y)
      {
        Graphics2D g2 = aa(g);
        // 背景圆角矩形
        g2.setColor(new Color(0xFFF3E0));
        g2.fillRoundRect(x + 2, y + 2, 28, 26, 6, 6);
        g2.setColor(new Color(0xE65100));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x + 2, y + 2, 28, 26, 6, 6);
        // f(x) 文字
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC, 16));
        FontMetrics fm = g2.getFontMetrics();
        String t = "f(x)";
        g2.drawString(t, x + (S - fm.stringWidth(t)) / 2, y + (S + fm.getAscent() - fm.getDescent()) / 2);
        g2.dispose();
      }

      @Override
      public int getIconWidth()
      {
        return S;
      }

      @Override
      public int getIconHeight()
      {
        return S;
      }
    };
  }

  public static Icon roleIcon()
  {
    return new Icon()
    {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y)
      {
        Graphics2D g2 = aa(g);
        int cx = x + S / 2;
        // 头
        g2.setColor(new Color(0x5B9BD5));
        g2.fillOval(cx - 5, y + 3, 11, 11);
        // 身体
        g2.fillArc(cx - 11, y + 13, 22, 18, 0, 180);
        // 轮廓
        g2.setColor(new Color(0x3A78BF));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawOval(cx - 5, y + 3, 11, 11);
        g2.drawArc(cx - 11, y + 13, 22, 18, 0, 180);
        g2.dispose();
      }

      @Override
      public int getIconWidth()
      {
        return S;
      }

      @Override
      public int getIconHeight()
      {
        return S;
      }
    };
  }

  public static Icon otherIcon()
  {
    return new Icon()
    {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y)
      {
        Graphics2D g2 = aa(g);
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // 扳手（左下到右上）
        g2.setColor(new Color(0x78909C));
        g2.drawLine(x + 6, y + 24, x + 20, y + 8);
        g2.drawOval(x + 17, y + 4, 8, 8);
        // 螺丝刀（右下到左上）
        g2.setColor(new Color(0xEF6C00));
        g2.drawLine(x + 24, y + 24, x + 12, y + 8);
        g2.fillRect(x + 9, y + 4, 5, 8);
        g2.dispose();
      }

      @Override
      public int getIconWidth()
      {
        return S;
      }

      @Override
      public int getIconHeight()
      {
        return S;
      }
    };
  }

  public static Icon queryIcon()
  {
    return new Icon()
    {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y)
      {
        Graphics2D g2 = aa(g);
        // 镜片
        g2.setColor(new Color(0xE3F2FD));
        g2.fillOval(x + 3, y + 3, 18, 18);
        g2.setColor(new Color(0x42A5F5));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawOval(x + 3, y + 3, 18, 18);
        // 手柄
        g2.setColor(new Color(0x795548));
        g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x + 19, y + 20, x + 28, y + 28);
        g2.dispose();
      }

      @Override
      public int getIconWidth()
      {
        return S;
      }

      @Override
      public int getIconHeight()
      {
        return S;
      }
    };
  }

  public static Icon backupIcon()
  {
    return new Icon()
    {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y)
      {
        Graphics2D g2 = aa(g);
        // 圆柱
        int ox = x + 4, oy = y + 3, w = 16, h = 16, ex = 4;
        g2.setColor(new Color(0xFFB74D));
        g2.fillRect(ox, oy + ex / 2, w, h);
        g2.setColor(new Color(0xEF6C00));
        g2.fillOval(ox, oy + h - ex / 2, w, ex + 1);
        g2.setColor(new Color(0xFFE0B2));
        g2.fillOval(ox, oy, w, ex + 1);
        g2.setColor(new Color(0xEF6C00));
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(ox, oy, w, ex + 1);
        g2.drawLine(ox, oy + ex / 2, ox, oy + h);
        g2.drawLine(ox + w, oy + ex / 2, ox + w, oy + h);
        g2.drawArc(ox, oy + h - ex / 2, w, ex + 1, 180, 180);
        // 箭头（向右）
        g2.setColor(new Color(0x4CAF50));
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x + 22, y + 15, x + 30, y + 15);
        g2.drawLine(x + 27, y + 11, x + 30, y + 15);
        g2.drawLine(x + 27, y + 19, x + 30, y + 15);
        g2.dispose();
      }

      @Override
      public int getIconWidth()
      {
        return S;
      }

      @Override
      public int getIconHeight()
      {
        return S;
      }
    };
  }
}

