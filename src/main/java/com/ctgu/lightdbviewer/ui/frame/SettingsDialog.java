package com.ctgu.lightdbviewer.ui.frame;

import com.ctgu.lightdbviewer.config.AppConfig;
import com.ctgu.lightdbviewer.util.FontManager;
import com.ctgu.lightdbviewer.util.ThemeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 选项设置窗口 —左侧列表导航，右侧卡片面
 *
 */

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class SettingsDialog extends JDialog
{
  private static final Logger logger = LoggerFactory.getLogger(SettingsDialog.class);
  private final CardLayout cardLayout = new CardLayout();
  private final JPanel cardPanel = new JPanel(cardLayout);
  private final DefaultListModel<String> listModel = new DefaultListModel<>();
  private final JList<String> categoryList = new JList<>(listModel);
  /**
   * UI 字体候选列
   */
  private static final String[] UI_FONT_CANDIDATES =
      { "微软雅黑", "Microsoft YaHei UI", "宋体", "新宋", "黑体", "楷体", "仿宋", "华文楷体", "华文宋体", "华文黑体", "华文细黑",
          "华文仿宋", "华文中宋", "方正姚体", "方正舒体", "思源黑体", "思源宋体", "Noto Sans CJK SC", "Noto Serif CJK SC", "Segoe UI",
          "Segoe UI Variable", "Arial", "Calibri", "Tahoma", "Verdana", "Georgia", "Cambria", "Times New Roman", "Helvetica",
          "Helvetica Neue", "Ubuntu", "Roboto", "Open Sans", "Lato", "Noto Sans", "Noto Serif", "SansSerif", "Serif", "Dialog",
          "DialogInput", };
  /**
   * 面板注册表（保持插入顺序
   */
  private final Map<String, JPanel> panels = new LinkedHashMap<>();
  private JComboBox<String> themeCombo;
  private JComboBox<String> languageCombo;
  private JComboBox<String> uiFontCombo;
  private JSpinner uiFontSizeSpinner;
  private JCheckBox showLineNumbers;
  private JCheckBox codeFolding;
  private JCheckBox bracketHighlight;
  private JCheckBox syntaxHighlight;
  private JCheckBox autoWrap;
  private JSpinner tabWidthSpinner;
  private JComboBox<String> editorFontCombo;
  private JSpinner editorFontSizeSpinner;

  public SettingsDialog(Frame owner)
  {
    super(owner, "选项", true);
    setSize(820, 560);
    setMinimumSize(new Dimension(700, 480));
    setLocationRelativeTo(owner);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    initPanels();
    initLayout();
  }

  private void initPanels()
  {
    panels.put("常规", createGeneralPanel());
    panels.put("选项", createPlaceholderPanel("选项卡设置（待实现）"));
    panels.put("代码补全", createPlaceholderPanel("代码补全设置（待实现)"));
    panels.put("编辑", createEditorPanel());
    panels.put("记录", createPlaceholderPanel("记录设置（待实现)"));
    panels.put("AI", createAiPanel());
    panels.put("自动恢复", createPlaceholderPanel("自动恢复设置（待实现)"));
    panels.put("文件位置", createFileLocationPanel());
    panels.put("连接", createPlaceholderPanel("连接性设置（待实现）"));
    panels.put("环境", createPlaceholderPanel("环境设置（待实现)"));
    panels.put("高级", createPlaceholderPanel("高级设置（待实现)"));
  }

  private void initLayout()
  {
    // —左侧列表 —
    for(String name : panels.keySet())
    {
      listModel.addElement(name);
      cardPanel.add(panels.get(name), name);
    }
    categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    categoryList.setSelectedIndex(0);
    categoryList.setFixedCellHeight(32);
    categoryList.setBorder(new EmptyBorder(4, 8, 4, 8));
    categoryList.addListSelectionListener(e -> {
      if(!e.getValueIsAdjusting())
      {
        String selected = categoryList.getSelectedValue();
        if(selected != null)
          cardLayout.show(cardPanel, selected);
      }
    });
    JScrollPane listScroll = new JScrollPane(categoryList);
    listScroll.setPreferredSize(new Dimension(160, 0));
    listScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIManager.getColor("Separator.foreground")));

    // —右侧卡片 —
    cardPanel.setBorder(new EmptyBorder(12, 16, 12, 16));

    // —底部按钮 —
    JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
    JButton okBtn = new JButton("确定");
    JButton cancelBtn = new JButton("取消");
    JButton applyBtn = new JButton("应用");
    okBtn.addActionListener(e -> {
      applyAll();
      dispose();
    });
    cancelBtn.addActionListener(e -> dispose());
    applyBtn.addActionListener(e -> applyAll());
    bottomBar.add(okBtn);
    bottomBar.add(cancelBtn);
    bottomBar.add(applyBtn);

    // —组装 —
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, cardPanel);
    split.setDividerLocation(160);
    split.setResizeWeight(0);
    split.setBorder(BorderFactory.createEmptyBorder());
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(split, BorderLayout.CENTER);
    getContentPane().add(bottomBar, BorderLayout.SOUTH);
  }

  private JPanel createGeneralPanel()
  {
    JPanel p = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = gbc();
    int row = 0;
    // 主题
    gbc.gridx = 0;
    gbc.gridy = row;
    p.add(new JLabel("主题"), gbc);
    gbc.gridx = 1;
    themeCombo = new JComboBox<>();
    String currentTheme = AppConfig.getInstance().getThemeName();
    int selectedIdx = -1;
    int idx = 0;
    for(ThemeManager.ThemeEntry t : ThemeManager.FLAT_THEMES)
    {
      themeCombo.addItem(t.name());
      if(t.className().equals(currentTheme))
      {
        selectedIdx = idx;
      }
      idx++;
    }
    for(ThemeManager.ThemeEntry t : ThemeManager.IJ_THEMES)
    {
      themeCombo.addItem(t.name());
      if(t.className().equals(currentTheme))
      {
        selectedIdx = idx;
      }
      idx++;
    }
    for(ThemeManager.ThemeEntry t : ThemeManager.MATERIAL_THEMES)
    {
      themeCombo.addItem(t.name());
      if(t.className().equals(currentTheme))
      {
        selectedIdx = idx;
      }
      idx++;
    }
    if(selectedIdx >= 0)
      themeCombo.setSelectedIndex(selectedIdx);
    p.add(themeCombo, gbc);
    row++;
    // 语言
    gbc.gridx = 0;
    gbc.gridy = row;
    p.add(new JLabel("语言"), gbc);
    gbc.gridx = 1;
    languageCombo = new JComboBox<>(new String[] { "中文（简体）", "English" });
    languageCombo.setSelectedIndex(0);
    p.add(languageCombo, gbc);
    row++;

    // 分隔
    gbc.gridx = 0;
    gbc.gridy = row;
    gbc.gridwidth = 2;
    p.add(new JSeparator(), gbc);
    gbc.gridwidth = 1;
    row++;

    // UI 字体
    gbc.gridx = 0;
    gbc.gridy = row;
    p.add(new JLabel("UI 字体"), gbc);
    gbc.gridx = 1;
    uiFontCombo = new JComboBox<>();
    String currentUiFont = AppConfig.getInstance().getFontFamily();
    java.util.Set<String> availFonts =
        new java.util.HashSet<>(java.util.Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
    java.util.Set<String> logicalFonts = java.util.Set.of("SansSerif", "Serif", "Monospaced", "Dialog", "DialogInput");
    for(String f : UI_FONT_CANDIDATES)
    {
      if(availFonts.contains(f) || logicalFonts.contains(f))
      {
        uiFontCombo.addItem(f);
      }
    }
    uiFontCombo.setSelectedItem(currentUiFont);
    p.add(uiFontCombo, gbc);
    row++;

    // UI 字号
    gbc.gridx = 0;
    gbc.gridy = row;
    p.add(new JLabel("UI 字号"), gbc);
    gbc.gridx = 1;
    uiFontSizeSpinner = new JSpinner(new SpinnerNumberModel(AppConfig.getInstance().getFontSize(), 10, 28, 1));
    p.add(uiFontSizeSpinner, gbc);
    row++;

    // 填充
    gbc.gridx = 0;
    gbc.gridy = row;
    gbc.weighty = 1;
    gbc.gridwidth = 2;
    p.add(Box.createVerticalGlue(), gbc);

    return p;
  }

  private JPanel createEditorPanel()
  {
    JPanel p = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = gbc();

    int row = 0;

    showLineNumbers = new JCheckBox("显示行号", true);
    gbc.gridx = 0;
    gbc.gridy = row++;
    gbc.gridwidth = 2;
    p.add(showLineNumbers, gbc);

    codeFolding = new JCheckBox("使用代码折叠", true);
    gbc.gridy = row++;
    p.add(codeFolding, gbc);

    bracketHighlight = new JCheckBox("使用括号高亮显示", true);
    gbc.gridy = row++;
    p.add(bracketHighlight, gbc);

    syntaxHighlight = new JCheckBox("使用语法突出显示", true);
    gbc.gridy = row++;
    p.add(syntaxHighlight, gbc);

    autoWrap = new JCheckBox("使用自动换行", false);
    gbc.gridy = row++;
    p.add(autoWrap, gbc);

    // 制表符宽
    gbc.gridwidth = 1;
    gbc.gridx = 0;
    gbc.gridy = row;
    p.add(new JLabel("制表符宽度："), gbc);
    gbc.gridx = 1;
    tabWidthSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 16, 1));
    p.add(tabWidthSpinner, gbc);
    row++;

    // 字体
    gbc.gridx = 0;
    gbc.gridy = row;
    p.add(new JLabel("编辑器字体："), gbc);
    gbc.gridx = 1;
    editorFontCombo = new JComboBox<>();
    String currentFont = AppConfig.getInstance().getEditorFontFamily();
    String[] monoFonts =
        { "微软雅黑", "宋体", "黑体", "仿宋", "楷体", "新宋", "Consolas", "Courier New", "JetBrains Mono", "Fira Code", "Source Code Pro",
            "Cascadia Code", "DejaVu Sans Mono", "Liberation Mono", "Lucida Console", "Monaco", "Menlo", "Monospaced" };
    java.util.Set<String> avail =
        new java.util.HashSet<>(java.util.Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
    for(String f : monoFonts)
    {
      if(avail.contains(f) || "Monospaced".equals(f))
        editorFontCombo.addItem(f);
    }
    editorFontCombo.setSelectedItem(currentFont);
    p.add(editorFontCombo, gbc);
    row++;

    // 字号
    gbc.gridx = 0;
    gbc.gridy = row;
    p.add(new JLabel("编辑器字号："), gbc);
    gbc.gridx = 1;
    editorFontSizeSpinner = new JSpinner(new SpinnerNumberModel(AppConfig.getInstance().getEditorFontSize(), 8, 36, 1));
    p.add(editorFontSizeSpinner, gbc);
    row++;

    // 填充
    gbc.gridx = 0;
    gbc.gridy = row;
    gbc.weighty = 1;
    gbc.gridwidth = 2;
    p.add(Box.createVerticalGlue(), gbc);

    return p;
  }

  // ================================================================
  // AI 面板
  // ================================================================

  private JTextField aiModelName;
  private JTextField aiEndpoint;
  private JTextField aiApiKey;

  private JPanel createAiPanel()
  {
    JPanel p = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = gbc();

    int row = 0;
    gbc.gridx = 0;
    gbc.gridy = row;
    p.add(new JLabel("大模型名称："), gbc);
    gbc.gridx = 1;
    aiModelName = new JTextField(readProp("ai.model.name", ""), 24);
    p.add(aiModelName, gbc);
    row++;

    gbc.gridx = 0;
    gbc.gridy = row;
    p.add(new JLabel("链接地址"), gbc);
    gbc.gridx = 1;
    aiEndpoint = new JTextField(readProp("ai.endpoint", ""), 24);
    p.add(aiEndpoint, gbc);
    row++;

    gbc.gridx = 0;
    gbc.gridy = row;
    p.add(new JLabel("API Key"), gbc);
    gbc.gridx = 1;
    aiApiKey = new JTextField(readProp("ai.api.key", ""), 24);
    p.add(aiApiKey, gbc);
    row++;

    // 填充
    gbc.gridx = 0;
    gbc.gridy = row;
    gbc.weighty = 1;
    gbc.gridwidth = 2;
    p.add(Box.createVerticalGlue(), gbc);

    return p;
  }

  // ================================================================
  // 文件位置面板
  // ================================================================

  private JTextField configFilePath;
  private JTextField logFilePath;

  private JPanel createFileLocationPanel()
  {
    JPanel p = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = gbc();

    int row = 0;
    gbc.gridx = 0;
    gbc.gridy = row;
    p.add(new JLabel("配置文件路径"), gbc);
    gbc.gridx = 1;
    configFilePath = new JTextField(readProp("file.config.path", System.getProperty("user.dir") + "/lightdbviewer.properties"), 28);
    p.add(configFilePath, gbc);
    gbc.gridx = 2;
    JButton browseConfig = new JButton("浏览...");
    browseConfig.addActionListener(e -> browseFile(configFilePath));
    p.add(browseConfig, gbc);
    row++;

    gbc.gridx = 0;
    gbc.gridy = row;
    p.add(new JLabel("日志文件路径"), gbc);
    gbc.gridx = 1;
    logFilePath = new JTextField(readProp("file.log.path", ""), 28);
    p.add(logFilePath, gbc);
    gbc.gridx = 2;
    JButton browseLog = new JButton("浏览...");
    browseLog.addActionListener(e -> browseFile(logFilePath));
    p.add(browseLog, gbc);
    row++;

    // 填充
    gbc.gridx = 0;
    gbc.gridy = row;
    gbc.weighty = 1;
    gbc.gridwidth = 3;
    p.add(Box.createVerticalGlue(), gbc);

    return p;
  }

  // ================================================================
  // 占位面板
  // ================================================================

  private JPanel createPlaceholderPanel(String text)
  {
    JPanel p = new JPanel(new BorderLayout());
    JLabel label = new JLabel(text, SwingConstants.CENTER);
    label.setForeground(UIManager.getColor("Label.disabledForeground"));
    p.add(label, BorderLayout.CENTER);
    return p;
  }

  // ================================================================
  // 应用设置
  // ================================================================

  private void applyAll()
  {
    AppConfig cfg = AppConfig.getInstance();

    // 常规 - 主题
    if(themeCombo.getSelectedIndex() >= 0)
    {
      String selectedName = (String)themeCombo.getSelectedItem();
      String className = resolveThemeClassName(selectedName);
      if(className != null)
      {
        cfg.setThemeName(className);
        ThemeManager.applyTheme(className);
      }
    }

    // 常规 - UI 字体
    String uiFont = (String)uiFontCombo.getSelectedItem();
    int uiSize = (Integer)uiFontSizeSpinner.getValue();
    if(uiFont != null)
    {
      cfg.setFontFamily(uiFont);
      cfg.setFontSize(uiSize);
      FontManager.applyUiFont(uiFont, uiSize);
    }

    // 编辑器字
    String edFont = (String)editorFontCombo.getSelectedItem();
    int edSize = (Integer)editorFontSizeSpinner.getValue();
    if(edFont != null)
    {
      cfg.setEditorFontFamily(edFont);
      cfg.setEditorFontSize(edSize);
      FontManager.applyEditorFont(edFont, edSize);
    }

    // AI 配置（写properties
    writeProp("ai.model.name", aiModelName.getText());
    writeProp("ai.endpoint", aiEndpoint.getText());
    writeProp("ai.api.key", aiApiKey.getText());

    // 文件位置
    writeProp("file.config.path", configFilePath.getText());
    writeProp("file.log.path", logFilePath.getText());

    cfg.save();
  }

  private String resolveThemeClassName(String displayName)
  {
    for(ThemeManager.ThemeEntry t : ThemeManager.FLAT_THEMES)
    {
      if(t.name().equals(displayName))
      {
        return t.className();
      }
    }
    for(ThemeManager.ThemeEntry t : ThemeManager.IJ_THEMES)
    {
      if(t.name().equals(displayName))
      {
        return t.className();
      }
    }
    for(ThemeManager.ThemeEntry t : ThemeManager.MATERIAL_THEMES)
    {
      if(t.name().equals(displayName))
      {
        return t.className();
      }
    }
    return null;
  }

  private GridBagConstraints gbc()
  {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(6, 6, 6, 6);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    return gbc;
  }

  private void browseFile(JTextField target)
  {
    JFileChooser chooser = new JFileChooser(target.getText());
    chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
    {
      target.setText(chooser.getSelectedFile().getAbsolutePath());
    }
  }

  private String readProp(String key, String def)
  {
    // 通过反射读取 AppConfig 内部 Properties（简化实现）
    try
    {
      var field = AppConfig.class.getDeclaredField("props");
      field.setAccessible(true);
      java.util.Properties props = (java.util.Properties)field.get(AppConfig.getInstance());
      return props.getProperty(key, def);
    }
    catch(Exception e)
    {
      return def;
    }
  }

  private void writeProp(String key, String value)
  {
    try
    {
      var field = AppConfig.class.getDeclaredField("props");
      field.setAccessible(true);
      java.util.Properties props = (java.util.Properties)field.get(AppConfig.getInstance());
      props.setProperty(key, value != null ? value : "");
    }
    catch(Exception e)
    {
      logger.warn("通过反射读取配置失败: {}", key, e);
    }
  }
}



