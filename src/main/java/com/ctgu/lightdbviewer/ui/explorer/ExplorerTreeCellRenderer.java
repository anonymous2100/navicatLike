package com.ctgu.lightdbviewer.ui.explorer;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.EnumMap;
import java.util.Map;

/**
 * Navicat 风格的树节点图标渲染
 */
/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class ExplorerTreeCellRenderer extends DefaultTreeCellRenderer
{
  private static final Map<ExplorerNodeType, Icon> ICON_CACHE = new EnumMap<>(ExplorerNodeType.class);
  private static final int S = 16;

  @Override
  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row,
      boolean hasFocus)
  {
    JLabel label = (JLabel)super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
    if(value instanceof ExplorerTreeNode node)
    {
      label.setIcon(ICON_CACHE.computeIfAbsent(node.getType(), ExplorerTreeCellRenderer::createIcon));
    }
    return label;
  }

  private static Icon createIcon(ExplorerNodeType type)
  {
    return switch(type)
    {
      // ---- 连接 / 数据----
      case CONNECTION -> new ConnectionIcon();
      case DATABASES_FOLDER -> new FolderIcon(new Color(0x6C8EBF));
      case DATABASE -> new CylinderIcon(new Color(0xE8A33D), new Color(0xD4872A));
      case SCHEMA -> new SchemaIcon();
      // ---- 文件----
      case TABLES_FOLDER -> new FolderIcon(new Color(0x4CAF50));
      case VIEWS_FOLDER -> new FolderIcon(new Color(0x9C27B0));
      case PROCEDURES_FOLDER -> new FolderIcon(new Color(0xE67E22));
      case FUNCTIONS_FOLDER -> new FolderIcon(new Color(0xE65100));
      case TRIGGERS_FOLDER -> new FolderIcon(new Color(0xC62828));
      // ---- 叶子节点 ----
      case TABLE -> new TableIcon();
      case VIEW -> new ViewIcon();
      case PROCEDURE -> new GearIcon(new Color(0xE67E22));
      case FUNCTION -> new FuncIcon();
      case TRIGGER -> new TriggerIcon();

      case LOADING -> new LoadingIcon();
    };
  }

  private static Graphics2D aa(Graphics g)
  {
    Graphics2D g2 = (Graphics2D)g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    return g2;
  }

  private static class ConnectionIcon implements Icon
  {
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
      Graphics2D g2 = aa(g);
      int d = 13;
      int ox = x + 1, oy = y + 1;
      // 地球
      g2.setColor(new Color(0x4A90D9));
      g2.fillOval(ox, oy, d, d);
      g2.setColor(new Color(0x3A78BF));
      g2.drawOval(ox, oy, d, d);
      // 经纬
      g2.setColor(new Color(0xFFFFFF, true));
      g2.setStroke(new BasicStroke(0.8f));
      g2.drawLine(ox + d / 2, oy, ox + d / 2, oy + d);
      g2.drawLine(ox, oy + d / 2, ox + d, oy + d / 2);
      g2.drawArc(ox + 2, oy, d - 5, d, 90, -180);
      g2.drawArc(ox + 3, oy, d - 1, d, 90, 180);
      // 绿色在线指示
      g2.setColor(new Color(0x4CAF50));
      g2.fillOval(x + 11, y + 11, 5, 5);
      g2.setColor(new Color(0x388E3C));
      g2.drawOval(x + 11, y + 11, 5, 5);
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
  }

  private static class CylinderIcon implements Icon
  {
    private final Color fill, border;

    CylinderIcon(Color fill, Color border)
    {
      this.fill = fill;
      this.border = border;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
      Graphics2D g2 = aa(g);
      int w = 12, h = 12, ex = 3; // 椭圆高度
      int ox = x + 2, oy = y + 1;
      // 柱身
      g2.setColor(fill);
      g2.fillRect(ox, oy + ex / 2, w, h);
      // 底部椭圆
      g2.setColor(fill.darker());
      g2.fillOval(ox, oy + h - ex / 2, w, ex + 1);
      // 顶部椭圆（亮色）
      g2.setColor(fill.brighter());
      g2.fillOval(ox, oy, w, ex + 1);
      // 边框
      g2.setColor(border);
      g2.setStroke(new BasicStroke(1f));
      g2.drawOval(ox, oy, w, ex + 1);
      g2.drawLine(ox, oy + ex / 2, ox, oy + h);
      g2.drawLine(ox + w, oy + ex / 2, ox + w, oy + h);
      g2.drawArc(ox, oy + h - ex / 2, w, ex + 1, 180, 180);
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
  }

  private static class SchemaIcon implements Icon
  {
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
      Graphics2D g2 = aa(g);
      int cx = x + S / 2, cy = y + S / 2;
      int r = 6;
      GeneralPath p = new GeneralPath();
      p.moveTo(cx, cy - r);
      p.lineTo(cx + r, cy);
      p.lineTo(cx, cy + r);
      p.lineTo(cx - r, cy);
      p.closePath();
      g2.setColor(new Color(0x5C9BD5));
      g2.fill(p);
      g2.setColor(new Color(0x3B7DD8));
      g2.setStroke(new BasicStroke(1.2f));
      g2.draw(p);
      // S 字母
      g2.setColor(Color.WHITE);
      g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
      FontMetrics fm = g2.getFontMetrics();
      g2.drawString("S", cx - fm.stringWidth("S") / 2, cy + fm.getAscent() / 2 - 1);
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
  }

  private static class FolderIcon implements Icon
  {
    private final Color color;

    FolderIcon(Color color)
    {
      this.color = color;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
      Graphics2D g2 = aa(g);
      // 文件夹选项
      g2.setColor(color);
      g2.fillRoundRect(x + 1, y + 2, 6, 3, 2, 2);
      // 文件夹主
      g2.fillRoundRect(x + 1, y + 4, 14, 10, 2, 2);
      // 高光
      g2.setColor(new Color(255, 255, 255, 60));
      g2.fillRect(x + 2, y + 5, 12, 4);
      // 边框
      g2.setColor(color.darker());
      g2.setStroke(new BasicStroke(0.8f));
      g2.drawRoundRect(x + 1, y + 4, 14, 10, 2, 2);
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
  }

  private static class TableIcon implements Icon
  {
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
      Graphics2D g2 = aa(g);
      int ox = x + 1, oy = y + 2, w = 13, h = 12;
      // 表头（深蓝）
      g2.setColor(new Color(0x4A90D9));
      g2.fillRect(ox, oy, w, 4);
      // 表体（浅蓝）
      g2.setColor(new Color(0xD6E9F8));
      g2.fillRect(ox, oy + 4, w, h - 4);
      // 边框
      g2.setColor(new Color(0x3B7DD8));
      g2.setStroke(new BasicStroke(1f));
      g2.drawRect(ox, oy, w, h);
      // 横线
      g2.drawLine(ox, oy + 4, ox + w, oy + 4);
      g2.drawLine(ox, oy + 8, ox + w, oy + 8);
      // 竖线
      g2.drawLine(ox + w / 3, oy + 4, ox + w / 3, oy + h);
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
  }

  private static class ViewIcon implements Icon
  {
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
      Graphics2D g2 = aa(g);
      // 后层（偏移）
      g2.setColor(new Color(0xB0D4F1));
      g2.fillRect(x + 4, y + 1, 11, 9);
      g2.setColor(new Color(0x6CA6CD));
      g2.drawRect(x + 4, y + 1, 11, 9);
      // 前层
      g2.setColor(new Color(0xD5E8D4));
      g2.fillRect(x + 1, y + 4, 11, 9);
      g2.setColor(new Color(0x82B366));
      g2.setStroke(new BasicStroke(1f));
      g2.drawRect(x + 1, y + 4, 11, 9);
      // 前层表头
      g2.setColor(new Color(0x82B366));
      g2.fillRect(x + 1, y + 4, 11, 3);
      // 横线
      g2.drawLine(x + 1, y + 7, x + 12, y + 7);
      g2.drawLine(x + 1, y + 10, x + 12, y + 10);
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
  }

  private static class FuncIcon implements Icon
  {
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
      Graphics2D g2 = aa(g);
      // 背景
      g2.setColor(new Color(0xFFF3E0));
      g2.fillRoundRect(x + 1, y + 1, 14, 14, 4, 4);
      g2.setColor(new Color(0xE65100));
      g2.setStroke(new BasicStroke(1f));
      g2.drawRoundRect(x + 1, y + 1, 14, 14, 4, 4);
      // ƒ(x) 文字
      g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC, 11));
      FontMetrics fm = g2.getFontMetrics();
      String t = "ƒx";
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
  }

  private static class GearIcon implements Icon
  {
    private final Color color;

    GearIcon(Color color)
    {
      this.color = color;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
      Graphics2D g2 = aa(g);
      int cx = x + S / 2, cy = y + S / 2;
      // 齿轮外圈（简化为 8 齿方+ 内圆
      g2.setColor(color);
      int r = 7;
      for(int i = 0; i < 8; i++)
      {
        double angle = Math.PI * i / 4;
        int tx = (int)(cx + r * Math.cos(angle)) - 1;
        int ty = (int)(cy + r * Math.sin(angle)) - 1;
        g2.fillRect(tx, ty, 3, 3);
      }
      g2.fillOval(cx - 5, cy - 5, 10, 10);
      // 内圆（空心）
      g2.setColor(g2.getBackground() != null ? UIManager.getColor("Tree.background") : Color.WHITE);
      if(g2.getColor() == null)
      {
        g2.setColor(Color.WHITE);
      }
      g2.fillOval(cx - 2, cy - 2, 5, 5);
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
  }

  private static class TriggerIcon implements Icon
  {
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
      Graphics2D g2 = aa(g);
      GeneralPath p = new GeneralPath();
      p.moveTo(x + 9, y + 1);
      p.lineTo(x + 4, y + 8);
      p.lineTo(x + 8, y + 8);
      p.lineTo(x + 6, y + 15);
      p.lineTo(x + 12, y + 7);
      p.lineTo(x + 8, y + 7);
      p.lineTo(x + 10, y + 1);
      p.closePath();
      g2.setColor(new Color(0xFFC107));
      g2.fill(p);
      g2.setColor(new Color(0xE6A000));
      g2.setStroke(new BasicStroke(0.8f));
      g2.draw(p);
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
  }

  private static class LoadingIcon implements Icon
  {
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
      Graphics2D g2 = aa(g);
      g2.setColor(new Color(0xBDBDBD));
      g2.setStroke(new BasicStroke(2f));
      g2.drawArc(x + 3, y + 3, 10, 10, 30, 300);
      // 箭头
      g2.fillPolygon(new int[] { x + 11, x + 14, x + 12 }, new int[] { y + 3, y + 5, y + 7 }, 3);
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
  }
}


