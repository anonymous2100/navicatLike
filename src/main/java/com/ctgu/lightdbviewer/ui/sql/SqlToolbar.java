package com.ctgu.lightdbviewer.ui.sql;

import com.ctgu.lightdbviewer.jdbc.ConnectionManager;
import com.ctgu.lightdbviewer.metadata.MetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.GeneralPath;
import java.util.List;

/**
 * SqlToolbar Navicat 风格双行工具
 * <p>
 * 第一行：[保存] [美化SQL] [清空结果] [询问AI]
 * 第二行：[连接] [数据库] [Schema] | [运行] [停止] | [解释]
 * /**
 * SqlToolbar — Navicat 风格双行工具
 * /**
 *
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class SqlToolbar extends JPanel
{
  private static final Logger logger = LoggerFactory.getLogger(SqlToolbar.class);
  // ---- 第一行按钮 ----
  private final JButton saveButton = iconTextBtn(BtnIcon.SAVE, "保存", "保存查询 (Ctrl+S)");
  private final JButton beautifyButton = iconTextBtn(BtnIcon.BEAUTIFY, "美化SQL", "格式化 SQL 语句");
  private final JButton clearButton = iconTextBtn(BtnIcon.CLEAR, "清空结果", "清空结果面板 (Ctrl+L)");
  private final JButton aiButton = iconTextBtn(BtnIcon.AI, "询问AI", "使用 AI 辅助分析 SQL");

  // ---- 第二行：连接/数据库/Schema 选择 ----
  private final JLabel connLabel = new JLabel();
  private final JComboBox<String> dbCombo = new JComboBox<>();
  private final JComboBox<String> schemaCombo = new JComboBox<>();

  // ---- 第二行：执行控制 ----
  private final JButton runButton = iconTextBtn(BtnIcon.RUN, "运行", "执行 SQL (Ctrl+Enter)");
  private final JButton stopButton = iconTextBtn(BtnIcon.STOP, "停止", "中断执行 (Esc)");
  private final JButton explainBtn = iconTextBtn(BtnIcon.EXPLAIN, "解释", "查看执行计划 (EXPLAIN)");

  private Runnable onContextChange;
  private Runnable onAskAi;
  private Runnable onExplain;
  private volatile boolean suppressDbListener;

  public SqlToolbar(Runnable onRun, Runnable onStop, Runnable onClear)
  {
    this(onRun, onStop, onClear, null, null);
  }

  public SqlToolbar(Runnable onRun, Runnable onStop, Runnable onClear, Runnable onSave, Runnable onBeautify)
  {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
        UIManager.getColor("Separator.foreground") != null ? UIManager.getColor("Separator.foreground") : Color.LIGHT_GRAY));

    add(buildRow1(onSave, onBeautify, onClear));
    add(buildRow2(onRun, onStop));

    initShortcuts(onRun, onClear);
    stopButton.setEnabled(false);
  }

  public void setOnContextChange(Runnable r)
  {
    this.onContextChange = r;
  }

  public void setOnAskAi(Runnable r)
  {
    this.onAskAi = r;
  }

  public void setOnExplain(Runnable r)
  {
    this.onExplain = r;
  }

  private JPanel buildRow1(Runnable onSave, Runnable onBeautify, Runnable onClear)
  {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
    row.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
    saveButton.addActionListener(e -> {
      if(onSave != null)
      {
        onSave.run();
      }
    });
    beautifyButton.addActionListener(e -> {
      if(onBeautify != null)
      {
        onBeautify.run();
      }
    });
    clearButton.addActionListener(e -> onClear.run());
    aiButton.addActionListener(e -> onAskAi());
    row.add(saveButton);
    row.add(sep());
    row.add(beautifyButton);
    row.add(sep());
    row.add(clearButton);
    row.add(sep());
    row.add(aiButton);
    return row;
  }

  private JPanel buildRow2(Runnable onRun, Runnable onStop)
  {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));
    row.setBorder(BorderFactory.createEmptyBorder(0, 4, 2, 4));
    connLabel.setFont(connLabel.getFont().deriveFont(Font.BOLD, 12f));
    connLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xBDBDBD)),
        BorderFactory.createEmptyBorder(1, 6, 1, 6)));
    refreshConnLabel();
    dbCombo.setPrototypeDisplayValue("mopria_cloud    ");
    dbCombo.setToolTipText("切换数据库");
    dbCombo.addActionListener(e -> {
      if(!suppressDbListener)
      {
        onDbSelected();
      }
    });
    schemaCombo.setPrototypeDisplayValue("public    ");
    schemaCombo.setToolTipText("切换 Schema");
    // 运行按钮绿色文字
    runButton.setForeground(new Color(0x1B5E20));
    runButton.setFont(runButton.getFont().deriveFont(Font.BOLD));
    runButton.addActionListener(e -> {
      setExecuting(true);
      onRun.run();
    });
    // 停止按钮红色文字
    stopButton.setForeground(new Color(0xB71C1C));
    stopButton.setFont(stopButton.getFont().deriveFont(Font.BOLD));
    stopButton.addActionListener(e -> onStop.run());
    explainBtn.addActionListener(e -> {
      if(onExplain != null)
      {
        onExplain.run();
      }
    });
    row.add(connLabel);
    row.add(dbCombo);
    row.add(schemaCombo);
    row.add(sep());
    row.add(runButton);
    row.add(stopButton);
    row.add(sep());
    row.add(explainBtn);
    return row;
  }

  /**
   * 将外部 AI 切换按钮添加在解释按钮之后
   */
  public void addAiToggleButton(AbstractButton btn)
  {
    if(getComponentCount() >= 2 && getComponent(1) instanceof JPanel row2)
    {
      btn.setFocusable(false);
      btn.setMargin(new Insets(2, 8, 2, 8));
      row2.add(btn);
      row2.revalidate();
    }
  }

  public void refreshConnectionContext()
  {
    refreshConnLabel();
    loadDbListAsync();
  }

  public String getSelectedDatabase()
  {
    return (String)dbCombo.getSelectedItem();
  }

  public String getSelectedSchema()
  {
    return (String)schemaCombo.getSelectedItem();
  }

  public void setExecuting(boolean executing)
  {
    runButton.setEnabled(!executing);
    stopButton.setEnabled(executing);
  }

  public void setIdle()
  {
    setExecuting(false);
  }

  private void refreshConnLabel()
  {
    if(!ConnectionManager.isConnected())
    {
      connLabel.setText("未连接");
      return;
    }
    try
    {
      String url = ConnectionManager.getUrl();
      int ss = url.indexOf("//");
      if(ss >= 0)
      {
        String rest = url.substring(ss + 2);
        int sl = rest.indexOf('/');
        connLabel.setText(sl >= 0 ? rest.substring(0, sl) : rest);
      }
      else
      {
        connLabel.setText(url);
      }
    }
    catch(Exception ignored)
    {
      connLabel.setText("已连接");
    }
  }

  private void loadDbListAsync()
  {
    new SwingWorker<List<String>, Void>()
    {
      @Override
      protected List<String> doInBackground() throws Exception
      {
        return MetadataService.listDatabases();
      }

      @Override
      protected void done()
      {
        try
        {
          List<String> dbs = get();
          String current = ConnectionManager.getCurrentDatabase();
          suppressDbListener = true;
          dbCombo.removeAllItems();
          for(String db : dbs)
          {
            dbCombo.addItem(db);
          }
          if(current != null && !current.isBlank())
          {
            dbCombo.setSelectedItem(current);
          }
          suppressDbListener = false;
          loadSchemaListAsync();
        }
        catch(Exception e)
        {
          logger.warn("加载数据库列表失败", e);
        }
      }
    }.execute();
  }

  private void loadSchemaListAsync()
  {
    new SwingWorker<List<String>, Void>()
    {
      @Override
      protected List<String> doInBackground() throws Exception
      {
        return MetadataService.listSchemas();
      }

      @Override
      protected void done()
      {
        try
        {
          List<String> schemas = get();
          schemaCombo.removeAllItems();
          if(schemas.isEmpty())
          {
            schemaCombo.setVisible(false);
            return;
          }
          schemaCombo.setVisible(true);
          for(String s : schemas)
          {
            schemaCombo.addItem(s);
          }
          schemaCombo.setSelectedItem("public");
        }
        catch(Exception ignored)
        {
        }
      }
    }.execute();
  }

  private void onDbSelected()
  {
    String db = (String)dbCombo.getSelectedItem();
    if(db == null || db.equals(ConnectionManager.getCurrentDatabase()))
    {
      return;
    }
    new SwingWorker<Void, Void>()
    {
      @Override
      protected Void doInBackground() throws Exception
      {
        ConnectionManager.switchDatabase(db);
        return null;
      }

      @Override
      protected void done()
      {
        loadSchemaListAsync();
        if(onContextChange != null)
        {
          onContextChange.run();
        }
      }
    }.execute();
  }

  private void onAskAi()
  {
    if(onAskAi != null)
    {
      onAskAi.run();
    }
  }

  private void initShortcuts(Runnable onRun, Runnable onClear)
  {
    InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    ActionMap am = getActionMap();
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "RUN");
    am.put("RUN", new AbstractAction()
    {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e)
      {
        if(runButton.isEnabled())
        {
          setExecuting(true);
          onRun.run();
        }
      }
    });
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK), "CLR");
    am.put("CLR", new AbstractAction()
    {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e)
      {
        onClear.run();
      }
    });
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "STP");
    am.put("STP", new AbstractAction()
    {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e)
      {
        if(stopButton.isEnabled())
        {
          stopButton.doClick();
        }
      }
    });
  }

  private static JButton iconTextBtn(BtnIcon icon, String text, String tip)
  {
    JButton b = new JButton(text, icon);
    b.setToolTipText(tip);
    b.setFocusable(false);
    b.setHorizontalTextPosition(SwingConstants.RIGHT);
    b.setIconTextGap(4);
    b.setMargin(new Insets(2, 8, 2, 8));
    return b;
  }

  private static JComponent sep()
  {
    JSeparator s = new JSeparator(SwingConstants.VERTICAL);
    s.setPreferredSize(new Dimension(1, 18));
    return s;
  }

  private enum BtnIcon implements Icon
  {
    // 💾 保存 软盘
    SAVE
        {
          @Override
          public void paintIcon(Component c, Graphics g, int x, int y)
          {
            Graphics2D g2 = g2(g);
            g2.setColor(new Color(0x1E88E5));
            g2.fillRoundRect(x + 1, y + 1, 14, 14, 3, 3);
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 4, y + 2, 7, 5);        // 标签
            g2.setColor(new Color(0x0D47A1));
            g2.fillRect(x + 3, y + 9, 10, 6);       // 磁盘
            g2.setColor(new Color(0xBBDEFB));
            g2.fillRect(x + 6, y + 3, 2, 3);        // 写保护口
            g2.dispose();
          }
        }, // 美化SQL 魔法
    BEAUTIFY
        {
          @Override
          public void paintIcon(Component c, Graphics g, int x, int y)
          {
            Graphics2D g2 = g2(g);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // 棒体
            g2.setColor(new Color(0x7B1FA2));
            g2.drawLine(x + 3, y + 13, x + 11, y + 5);
            // 星星
            g2.setColor(new Color(0xFDD835));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int sx = x + 12, sy = y + 2;
            g2.drawLine(sx, sy - 2, sx, sy + 2);
            g2.drawLine(sx - 2, sy, sx + 2, sy);
            g2.drawLine(sx - 1, sy - 1, sx + 1, sy + 1);
            g2.drawLine(sx + 1, sy - 1, sx - 1, sy + 1);
            // 小星
            g2.setColor(new Color(0xFDD835));
            g2.fillOval(x + 5, y + 10, 3, 3);
            g2.dispose();
          }
        }, // 🗑 清空结果 橡皮
    CLEAR
        {
          @Override
          public void paintIcon(Component c, Graphics g, int x, int y)
          {
            Graphics2D g2 = g2(g);
            // 橡皮擦主体（斜角矩形
            int[] px = { x + 2, x + 10, x + 14, x + 6 };
            int[] py = { y + 13, y + 3, y + 7, y + 13 };
            g2.setColor(new Color(0xEF9A9A));
            g2.fillPolygon(px, py, 4);
            g2.setColor(new Color(0xE53935));
            g2.setStroke(new BasicStroke(1f));
            g2.drawPolygon(px, py, 4);
            // 底部白线（擦过的区域
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(x + 2, y + 13, x + 14, y + 13);
            g2.dispose();
          }
        }, // 🤖 询问AI 机器人头
    AI
        {
          @Override
          public void paintIcon(Component c, Graphics g, int x, int y)
          {
            Graphics2D g2 = g2(g);
            // 头部
            g2.setColor(new Color(0x42A5F5));
            g2.fillRoundRect(x + 2, y + 4, 12, 9, 3, 3);
            // 眼睛
            g2.setColor(Color.WHITE);
            g2.fillOval(x + 4, y + 6, 3, 3);
            g2.fillOval(x + 9, y + 6, 3, 3);
            g2.setColor(new Color(0x1565C0));
            g2.fillOval(x + 5, y + 7, 1, 1);
            g2.fillOval(x + 10, y + 7, 1, 1);
            // 天线
            g2.setColor(new Color(0x42A5F5));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(x + 8, y + 4, x + 8, y + 1);
            g2.fillOval(x + 6, y + 0, 4, 3);
            // 嘴巴
            g2.setColor(new Color(0x1565C0));
            g2.drawLine(x + 5, y + 11, x + 11, y + 11);
            g2.dispose();
          }
        }, // 运行 绿色播放三角
    RUN
        {
          @Override
          public void paintIcon(Component c, Graphics g, int x, int y)
          {
            Graphics2D g2 = g2(g);
            GeneralPath p = new GeneralPath();
            p.moveTo(x + 3, y + 2);
            p.lineTo(x + 14, y + 8);
            p.lineTo(x + 3, y + 14);
            p.closePath();
            g2.setColor(new Color(0x2E7D32));
            g2.fill(p);
            g2.setColor(new Color(0x1B5E20));
            g2.setStroke(new BasicStroke(0.8f));
            g2.draw(p);
            g2.dispose();
          }
        }, // 停止 红色圆角停止方块
    STOP
        {
          @Override
          public void paintIcon(Component c, Graphics g, int x, int y)
          {
            Graphics2D g2 = g2(g);
            g2.setColor(new Color(0xC62828));
            g2.fillRoundRect(x + 3, y + 3, 10, 10, 2, 2);
            g2.setColor(new Color(0xB71C1C));
            g2.setStroke(new BasicStroke(0.8f));
            g2.drawRoundRect(x + 3, y + 3, 10, 10, 2, 2);
            g2.dispose();
          }
        }, // 🔍 解释 放大
    EXPLAIN
        {
          @Override
          public void paintIcon(Component c, Graphics g, int x, int y)
          {
            Graphics2D g2 = g2(g);
            g2.setColor(new Color(0xE3F2FD));
            g2.fillOval(x + 1, y + 1, 11, 11);
            g2.setColor(new Color(0x1976D2));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(x + 1, y + 1, 11, 11);
            g2.setColor(new Color(0x5D4037));
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 11, y + 11, x + 15, y + 15);
            g2.dispose();
          }
        };

    private static Graphics2D g2(Graphics g)
    {
      Graphics2D g2 = (Graphics2D)g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      return g2;
    }

    @Override
    public int getIconWidth()
    {
      return 16;
    }

    @Override
    public int getIconHeight()
    {
      return 16;
    }
  }
}



