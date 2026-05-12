package com.ctgu.lightdbviewer.ui.frame;

import com.ctgu.lightdbviewer.ai.AiService;
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
        {
          cardLayout.show(cardPanel, selected);
        }
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
    {
      themeCombo.setSelectedIndex(selectedIdx);
    }
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
      {
        editorFontCombo.addItem(f);
      }
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

  private JCheckBox aiShow;
  private JCheckBox aiEnabled;
  private DefaultListModel<com.ctgu.lightdbviewer.ai.AiModelItem> aiListModel;
  private JList<com.ctgu.lightdbviewer.ai.AiModelItem> aiList;
  private JTextField aiNameField;
  private JTextField aiProviderField;
  private JTextField aiHostField;
  private JTextField aiEndpointField;
  private JTextField aiApiKeyField;
  private JTextField aiModelField;
  private JSpinner aiTemperatureSpinner;
  private JTextArea aiDescArea;
  private JComboBox<String> aiEnterActionCombo;
  private JCheckBox aiCompareMode;
  private JComboBox<String> aiLanguageCombo;
  private JLabel aiTestResult;

  private JPanel createAiPanel()
  {
    JPanel p = new JPanel(new BorderLayout(0, 8));
    p.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

    // ── 顶部：复选框行 ──
    JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
    topRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
    aiShow = new JCheckBox("显示AI功能");
    aiShow.setSelected("true".equals(readProp("ai.show", "true")));
    aiEnabled = new JCheckBox("启用AI助手");
    aiEnabled.setSelected("true".equals(readProp("ai.enabled", "true")));
    topRow.add(aiShow);
    topRow.add(aiEnabled);

    // ── AI助手 区域 ──
    JPanel assistantPanel = new JPanel(new BorderLayout(6, 6));
    assistantPanel.setBorder(BorderFactory.createTitledBorder("AI助手"));
    aiListModel = new DefaultListModel<>();
    loadAiModelsFromConfig();
    aiList = new JList<>(aiListModel);
    aiList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    aiList.setFixedCellWidth(140);
    aiList.addListSelectionListener(e -> {
      if(!e.getValueIsAdjusting())
      {
        populateModelFields(aiList.getSelectedValue());
      }
    });
    JScrollPane listScroll = new JScrollPane(aiList);
    listScroll.setPreferredSize(new Dimension(150, 0));

    // +/- 按钮
    JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
    JButton addBtn = new JButton("+");
    addBtn.setToolTipText("添加大模型");
    addBtn.addActionListener(e -> {
      com.ctgu.lightdbviewer.ai.AiModelItem item = new com.ctgu.lightdbviewer.ai.AiModelItem();
      item.setName("新模型");
      aiListModel.addElement(item);
      aiList.setSelectedValue(item, true);
    });
    JButton delBtn = new JButton("-");
    delBtn.setToolTipText("删除大模型");
    delBtn.addActionListener(e -> {
      int idx = aiList.getSelectedIndex();
      if(idx >= 0)
      {
        aiListModel.remove(idx);
        if(aiListModel.size() > 0)
        {
          aiList.setSelectedIndex(Math.min(idx, aiListModel.size() - 1));
        }
      }
    });
    btnPanel.add(addBtn);
    btnPanel.add(delBtn);

    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.add(listScroll, BorderLayout.CENTER);
    leftPanel.add(btnPanel, BorderLayout.SOUTH);

    // 右侧配置表单
    JPanel formPanel = new JPanel(new GridBagLayout());
    GridBagConstraints g = new GridBagConstraints();
    g.insets = new Insets(3, 4, 3, 4);
    g.anchor = GridBagConstraints.WEST;
    g.fill = GridBagConstraints.HORIZONTAL;

    g.gridx = 0;
    g.gridy = 0;
    formPanel.add(new JLabel("AI助手名称："), g);
    g.gridx = 1;
    aiNameField = new JTextField(20);
    formPanel.add(aiNameField, g);

    g.gridx = 0;
    g.gridy = 1;
    formPanel.add(new JLabel("AI提供商："), g);
    g.gridx = 1;
    aiProviderField = new JTextField(20);
    formPanel.add(aiProviderField, g);

    g.gridx = 0;
    g.gridy = 2;
    formPanel.add(new JLabel("API主机："), g);
    g.gridx = 1;
    aiHostField = new JTextField(20);
    formPanel.add(aiHostField, g);

    g.gridx = 0;
    g.gridy = 3;
    formPanel.add(new JLabel("API端点："), g);
    g.gridx = 1;
    aiEndpointField = new JTextField(20);
    formPanel.add(aiEndpointField, g);

    g.gridx = 0;
    g.gridy = 4;
    formPanel.add(new JLabel("API密钥："), g);
    g.gridx = 1;
    aiApiKeyField = new JTextField(20);
    formPanel.add(aiApiKeyField, g);

    g.gridx = 0;
    g.gridy = 5;
    formPanel.add(new JLabel("模型："), g);
    g.gridx = 1;
    aiModelField = new JTextField(20);
    formPanel.add(aiModelField, g);

    g.gridx = 0;
    g.gridy = 6;
    formPanel.add(new JLabel("温度："), g);
    g.gridx = 1;
    aiTemperatureSpinner = new JSpinner(new SpinnerNumberModel(0.7, 0.0, 2.0, 0.1));
    formPanel.add(aiTemperatureSpinner, g);

    g.gridx = 0;
    g.gridy = 7;
    g.anchor = GridBagConstraints.NORTHWEST;
    formPanel.add(new JLabel("说明："), g);
    g.gridx = 1;
    g.anchor = GridBagConstraints.WEST;
    aiDescArea = new JTextArea(3, 20);
    aiDescArea.setLineWrap(true);
    aiDescArea.setWrapStyleWord(true);
    formPanel.add(new JScrollPane(aiDescArea), g);

    // 测试连接按钮
    g.gridx = 1;
    g.gridy = 8;
    g.anchor = GridBagConstraints.EAST;
    g.fill = GridBagConstraints.NONE;
    JButton testBtn = new JButton("测试连接");
    testBtn.addActionListener(e -> testAiConnection());
    JPanel testRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
    testRow.add(testBtn);
    formPanel.add(testRow, g);

    // 测试结果
    g.gridx = 0;
    g.gridy = 9;
    g.gridwidth = 2;
    g.anchor = GridBagConstraints.WEST;
    g.fill = GridBagConstraints.HORIZONTAL;
    g.weightx = 1;
    aiTestResult = new JLabel(" ");
    aiTestResult.setFont(aiTestResult.getFont().deriveFont(Font.PLAIN, 12));
    formPanel.add(aiTestResult, g);

    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, formPanel);
    split.setDividerLocation(160);
    split.setResizeWeight(0);
    split.setBorder(BorderFactory.createEmptyBorder());
    assistantPanel.add(split, BorderLayout.CENTER);

    // ── AI助手UI 区域 ──
    JPanel uiPanel = new JPanel(new GridBagLayout());
    uiPanel.setBorder(BorderFactory.createTitledBorder("AI助手UI"));
    GridBagConstraints g2 = new GridBagConstraints();
    g2.insets = new Insets(3, 4, 3, 4);
    g2.anchor = GridBagConstraints.WEST;
    g2.fill = GridBagConstraints.HORIZONTAL;

    g2.gridx = 0;
    g2.gridy = 0;
    uiPanel.add(new JLabel("按下回车键时执行的操作："), g2);
    g2.gridx = 1;
    aiEnterActionCombo = new JComboBox<>(new String[] { "发送消息", "另起一行" });
    aiEnterActionCombo.setSelectedIndex("newline".equals(readProp("ai.enter.action", "send")) ? 1 : 0);
    uiPanel.add(aiEnterActionCombo, g2);

    g2.gridx = 0;
    g2.gridy = 1;
    g2.gridwidth = 2;
    aiCompareMode = new JCheckBox("与其他助手比较");
    aiCompareMode.setSelected("true".equals(readProp("ai.compare.mode", "false")));
    uiPanel.add(aiCompareMode, g2);

    // ── 询问AI 区域 ──
    JPanel askPanel = new JPanel(new GridBagLayout());
    askPanel.setBorder(BorderFactory.createTitledBorder("询问AI"));
    GridBagConstraints g3 = new GridBagConstraints();
    g3.insets = new Insets(3, 4, 3, 4);
    g3.anchor = GridBagConstraints.WEST;
    g3.fill = GridBagConstraints.HORIZONTAL;

    g3.gridx = 0;
    g3.gridy = 0;
    askPanel.add(new JLabel("语言："), g3);
    g3.gridx = 1;
    aiLanguageCombo = new JComboBox<>(new String[] { "简体中文", "English" });
    aiLanguageCombo.setSelectedIndex("en".equals(readProp("ai.language", "zh")) ? 1 : 0);
    askPanel.add(aiLanguageCombo, g3);

    // 填充底部空间
    g3.gridx = 0;
    g3.gridy = 1;
    g3.weighty = 1;
    g3.gridwidth = 2;
    askPanel.add(Box.createVerticalGlue(), g3);

    // ── 组装 ──
    JPanel centerPanel = new JPanel();
    centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
    centerPanel.add(assistantPanel);
    centerPanel.add(Box.createVerticalStrut(6));
    centerPanel.add(uiPanel);
    centerPanel.add(Box.createVerticalStrut(6));
    centerPanel.add(askPanel);

    p.add(topRow, BorderLayout.NORTH);
    p.add(centerPanel, BorderLayout.CENTER);

    // 选中第一个模型
    if(aiListModel.size() > 0 && aiList.getSelectedIndex() < 0)
    {
      aiList.setSelectedIndex(0);
    }

    return p;
  }

  private void loadAiModelsFromConfig()
  {
    aiListModel.clear();
    int count = Integer.parseInt(readProp("ai.models.count", "0"));
    if(count == 0)
    {
      com.ctgu.lightdbviewer.ai.AiModelItem legacy = new com.ctgu.lightdbviewer.ai.AiModelItem();
      legacy.setName(readProp("ai.model", "deepseek-chat"));
      legacy.setModel(readProp("ai.model", "deepseek-chat"));
      legacy.setEndpoint(readProp("ai.endpoint", "https://api.deepseek.com/v1"));
      legacy.setApiKey(readProp("ai.api.key", ""));
      legacy.setProvider("DeepSeek");
      legacy.setTemperature(Double.parseDouble(readProp("ai.temperature", "0.7")));
      aiListModel.addElement(legacy);
      return;
    }
    for(int i = 0; i < count; i++)
    {
      String pfx = "ai.model." + i + ".";
      com.ctgu.lightdbviewer.ai.AiModelItem item = new com.ctgu.lightdbviewer.ai.AiModelItem();
      item.setName(readProp(pfx + "name", ""));
      item.setProvider(readProp(pfx + "provider", ""));
      item.setHost(readProp(pfx + "host", ""));
      item.setEndpoint(readProp(pfx + "endpoint", ""));
      item.setApiKey(readProp(pfx + "apiKey", ""));
      item.setModel(readProp(pfx + "model", ""));
      item.setTemperature(Double.parseDouble(readProp(pfx + "temperature", "0.7")));
      item.setDescription(readProp(pfx + "description", ""));
      aiListModel.addElement(item);
    }
  }

  private void populateModelFields(com.ctgu.lightdbviewer.ai.AiModelItem item)
  {
    if(item == null)
    {
      aiNameField.setText("");
      aiProviderField.setText("");
      aiHostField.setText("");
      aiEndpointField.setText("");
      aiApiKeyField.setText("");
      aiModelField.setText("");
      aiTemperatureSpinner.setValue(0.7);
      aiDescArea.setText("");
      return;
    }
    aiNameField.setText(item.getName());
    aiProviderField.setText(item.getProvider());
    aiHostField.setText(item.getHost());
    aiEndpointField.setText(item.getEndpoint());
    aiApiKeyField.setText(item.getApiKey());
    aiModelField.setText(item.getModel());
    aiTemperatureSpinner.setValue(item.getTemperature());
    aiDescArea.setText(item.getDescription());
  }

  private com.ctgu.lightdbviewer.ai.AiModelItem getCurrentAiModelItem()
  {
    com.ctgu.lightdbviewer.ai.AiModelItem item = aiList.getSelectedValue();
    if(item == null)
    {
      return null;
    }
    item.setName(aiNameField.getText());
    item.setProvider(aiProviderField.getText());
    item.setHost(aiHostField.getText());
    item.setEndpoint(aiEndpointField.getText());
    item.setApiKey(aiApiKeyField.getText());
    item.setModel(aiModelField.getText());
    item.setTemperature((Double)aiTemperatureSpinner.getValue());
    item.setDescription(aiDescArea.getText());
    return item;
  }

  private void testAiConnection()
  {
    aiTestResult.setForeground(Color.BLACK);
    aiTestResult.setText("正在测试连接，请稍候...");
    String apiKey = aiApiKeyField.getText().trim();
    String endpoint = aiEndpointField.getText().trim();
    String model = aiModelField.getText().trim();
    if(apiKey.isBlank())
    {
      aiTestResult.setForeground(Color.RED);
      aiTestResult.setText("请先填写 API Key");
      return;
    }
    if(model.isEmpty())
    {
      aiTestResult.setForeground(Color.RED);
      aiTestResult.setText("请填写模型名称");
      return;
    }
    new SwingWorker<String, Void>()
    {
      @Override
      protected String doInBackground()
      {
        StringBuilder result = new StringBuilder();
        try
        {
          java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
              .connectTimeout(java.time.Duration.ofSeconds(10))
              .build();

          String body = """
              {"model":"%s","messages":[{"role":"user","content":"hi"}],"max_tokens":5}
              """.formatted(model);
          java.net.http.HttpRequest chatReq = java.net.http.HttpRequest.newBuilder()
              .uri(java.net.URI.create(endpoint + "/chat/completions"))
              .header("Authorization", "Bearer " + apiKey)
              .header("Content-Type", "application/json")
              .timeout(java.time.Duration.ofSeconds(15))
              .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
              .build();
          java.net.http.HttpResponse<String> chatResp = client.send(chatReq,
              java.net.http.HttpResponse.BodyHandlers.ofString());

          if(chatResp.statusCode() == 200)
          {
            result.append("[API 连接] 成功 — 模型 ").append(model).append(" 可用");
          }
          else if(chatResp.statusCode() == 401)
          {
            return "[API 连接] 失败 — API Key 无效（401 Unauthorized）";
          }
          else if(chatResp.statusCode() == 403)
          {
            return "[API 连接] 失败 — 无权限访问（403 Forbidden）";
          }
          else if(chatResp.statusCode() == 404)
          {
            return "[API 连接] 失败 — 接口不存在（404），请检查 API 地址和模型名";
          }
          else
          {
            result.append("[API 连接] 失败 — HTTP ").append(chatResp.statusCode()).append(": ")
                .append(chatResp.body().length() > 200 ? chatResp.body().substring(0, 200) : chatResp.body());
            return result.toString();
          }

          try
          {
            java.net.http.HttpRequest balanceReq = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(endpoint + "/user/balance"))
                .header("Authorization", "Bearer " + apiKey)
                .timeout(java.time.Duration.ofSeconds(10))
                .GET()
                .build();
            java.net.http.HttpResponse<String> balanceResp = client.send(balanceReq,
                java.net.http.HttpResponse.BodyHandlers.ofString());
            if(balanceResp.statusCode() == 200 && !balanceResp.body().isBlank())
            {
              result.append("\n[余额信息] ").append(balanceResp.body());
            }
          }
          catch(Exception ignored)
          {
          }
        }
        catch(java.net.ConnectException e)
        {
          return "[网络错误] 无法连接到 " + endpoint + " — 请检查 API 地址和网络";
        }
        catch(java.net.http.HttpTimeoutException e)
        {
          return "[超时] 连接超时 — 请检查网络或 API 地址";
        }
        catch(Exception e)
        {
          return "[网络错误] " + e.getMessage();
        }
        return result.toString();
      }

      @Override
      protected void done()
      {
        try
        {
          String msg = get();
          if(msg.contains("失败") || msg.contains("错误") || msg.contains("超时"))
          {
            aiTestResult.setForeground(Color.RED);
          }
          else
          {
            aiTestResult.setForeground(new Color(0, 128, 0));
          }
          aiTestResult.setText("<html>" + msg.replace("\n", "<br>") + "</html>");
        }
        catch(Exception e)
        {
          aiTestResult.setForeground(Color.RED);
          aiTestResult.setText("测试异常: " + e.getMessage());
        }
      }
    }.execute();
  }

  // ================================================================
  // 文件位置面板
  // ================================================================

  private JTextField configFilePath;
  private JTextField logFilePath;

  private void saveAiModelsToConfig()
  {
    // 先同步当前编辑的字段回选中项
    getCurrentAiModelItem();
    // 先清空旧的模型配置键
    int oldCount = Integer.parseInt(readProp("ai.models.count", "0"));
    for(int i = 0; i < oldCount; i++)
    {
      String pfx = "ai.model." + i + ".";
      writeProp(pfx + "name", "");
      writeProp(pfx + "provider", "");
      writeProp(pfx + "host", "");
      writeProp(pfx + "endpoint", "");
      writeProp(pfx + "apiKey", "");
      writeProp(pfx + "model", "");
      writeProp(pfx + "temperature", "");
      writeProp(pfx + "description", "");
    }
    // 写入当前列表
    writeProp("ai.models.count", String.valueOf(aiListModel.size()));
    for(int i = 0; i < aiListModel.size(); i++)
    {
      com.ctgu.lightdbviewer.ai.AiModelItem item = aiListModel.get(i);
      String pfx = "ai.model." + i + ".";
      writeProp(pfx + "name", item.getName());
      writeProp(pfx + "provider", item.getProvider());
      writeProp(pfx + "host", item.getHost());
      writeProp(pfx + "endpoint", item.getEndpoint());
      writeProp(pfx + "apiKey", item.getApiKey());
      writeProp(pfx + "model", item.getModel());
      writeProp(pfx + "temperature", String.valueOf(item.getTemperature()));
      writeProp(pfx + "description", item.getDescription());
    }
    // 向后兼容：第一个模型也写入旧格式键
    if(aiListModel.size() > 0)
    {
      com.ctgu.lightdbviewer.ai.AiModelItem first = aiListModel.get(0);
      writeProp("ai.model", first.getModel());
      writeProp("ai.endpoint", first.getEndpoint());
      writeProp("ai.api.key", first.getApiKey());
    }
  }

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

    // AI 配置
    writeProp("ai.show", String.valueOf(aiShow.isSelected()));
    writeProp("ai.enabled", String.valueOf(aiEnabled.isSelected()));
    saveAiModelsToConfig();
    writeProp("ai.enter.action", aiEnterActionCombo.getSelectedIndex() == 0 ? "send" : "newline");
    writeProp("ai.compare.mode", String.valueOf(aiCompareMode.isSelected()));
    writeProp("ai.language", aiLanguageCombo.getSelectedIndex() == 0 ? "zh" : "en");

    // 文件位置
    writeProp("file.config.path", configFilePath.getText());
    writeProp("file.log.path", logFilePath.getText());

    cfg.save();
    // 重新初始化 AI 服务以应用新配置
    AiService.getInstance().initFromAppConfig();
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



