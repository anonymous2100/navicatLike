package com.ctgu.lightdbviewer.util;

import com.ctgu.lightdbviewer.config.AppConfig;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 *
 * ThemeManager — 主题 & 背景色管理
 * <p>
 * 职责：
 * - 维护全部可用主题列表（FlatLaf IntelliJ Themes Pack）
 * - 动态切换主题（安装 LaF → 重新应用字体 → 重新应用背景色 → 刷新窗口）
 * - 管理全局背景色（护眼绿等），支持只影响面板和编辑器区域
 */
public final class ThemeManager
{
  public record ThemeEntry(String name, String className)
  {
  }

  public static final ThemeEntry[] FLAT_THEMES =
      { new ThemeEntry("FlatLight", "com.formdev.flatlaf.FlatLightLaf"), new ThemeEntry("FlatDark", "com.formdev.flatlaf.FlatDarkLaf"),
          new ThemeEntry("FlatDarcula", "com.formdev.flatlaf.FlatDarculaLaf"), };

  public static final ThemeEntry[] IJ_THEMES = { new ThemeEntry("Arc", "com.formdev.flatlaf.intellijthemes.FlatArcIJTheme"),
      new ThemeEntry("Arc - Orange", "com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme"),
      new ThemeEntry("Arc Dark", "com.formdev.flatlaf.intellijthemes.FlatArcDarkIJTheme"),
      new ThemeEntry("Arc Dark - Orange", "com.formdev.flatlaf.intellijthemes.FlatArcDarkOrangeIJTheme"),
      new ThemeEntry("Carbon", "com.formdev.flatlaf.intellijthemes.FlatCarbonIJTheme"),
      new ThemeEntry("Cobalt 2", "com.formdev.flatlaf.intellijthemes.FlatCobalt2IJTheme"),
      new ThemeEntry("Cyan Light", "com.formdev.flatlaf.intellijthemes.FlatCyanLightIJTheme"),
      new ThemeEntry("Dark Flat", "com.formdev.flatlaf.intellijthemes.FlatDarkFlatIJTheme"),
      new ThemeEntry("Dark Purple", "com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme"),
      new ThemeEntry("Dracula", "com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme"),
      new ThemeEntry("Gradianto Dark Fuchsia", "com.formdev.flatlaf.intellijthemes.FlatGradiantoDarkFuchsiaIJTheme"),
      new ThemeEntry("Gradianto Deep Ocean", "com.formdev.flatlaf.intellijthemes.FlatGradiantoDeepOceanIJTheme"),
      new ThemeEntry("Gradianto Midnight Blue", "com.formdev.flatlaf.intellijthemes.FlatGradiantoMidnightBlueIJTheme"),
      new ThemeEntry("Gradianto Nature Green", "com.formdev.flatlaf.intellijthemes.FlatGradiantoNatureGreenIJTheme"),
      new ThemeEntry("Gray", "com.formdev.flatlaf.intellijthemes.FlatGrayIJTheme"),
      new ThemeEntry("Gruvbox Dark Hard", "com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkHardIJTheme"),
      new ThemeEntry("Hiberbee Dark", "com.formdev.flatlaf.intellijthemes.FlatHiberbeeDarkIJTheme"),
      new ThemeEntry("High Contrast", "com.formdev.flatlaf.intellijthemes.FlatHighContrastIJTheme"),
      new ThemeEntry("Light Flat", "com.formdev.flatlaf.intellijthemes.FlatLightFlatIJTheme"),
      new ThemeEntry("Material Design Dark", "com.formdev.flatlaf.intellijthemes.FlatMaterialDesignDarkIJTheme"),
      new ThemeEntry("Monocai", "com.formdev.flatlaf.intellijthemes.FlatMonocaiIJTheme"),
      new ThemeEntry("Monokai Pro", "com.formdev.flatlaf.intellijthemes.FlatMonokaiProIJTheme"),
      new ThemeEntry("Nord", "com.formdev.flatlaf.intellijthemes.FlatNordIJTheme"),
      new ThemeEntry("One Dark", "com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme"),
      new ThemeEntry("Solarized Dark", "com.formdev.flatlaf.intellijthemes.FlatSolarizedDarkIJTheme"),
      new ThemeEntry("Solarized Light", "com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme"),
      new ThemeEntry("Spacegray", "com.formdev.flatlaf.intellijthemes.FlatSpacegrayIJTheme"),
      new ThemeEntry("Vuesion", "com.formdev.flatlaf.intellijthemes.FlatVuesionIJTheme"),
      new ThemeEntry("Xcode-Dark", "com.formdev.flatlaf.intellijthemes.FlatXcodeDarkIJTheme"), };

  public static final ThemeEntry[] MATERIAL_THEMES =
      { new ThemeEntry("Arc Dark (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTArcDarkIJTheme"),
          new ThemeEntry("Atom One Dark (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTAtomOneDarkIJTheme"),
          new ThemeEntry("Atom One Light (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTAtomOneLightIJTheme"),
          new ThemeEntry("Dracula (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTDraculaIJTheme"),
          new ThemeEntry("GitHub (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTGitHubIJTheme"),
          new ThemeEntry("GitHub Dark (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTGitHubDarkIJTheme"),
          new ThemeEntry("Light Owl (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTLightOwlIJTheme"),
          new ThemeEntry("Material Darker (Material)",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialDarkerIJTheme"),
          new ThemeEntry("Material Deep Ocean (Material)",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialDeepOceanIJTheme"),
          new ThemeEntry("Material Lighter (Material)",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialLighterIJTheme"),
          new ThemeEntry("Material Oceanic (Material)",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialOceanicIJTheme"),
          new ThemeEntry("Material Palenight (Material)",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialPalenightIJTheme"),
          new ThemeEntry("Monokai Pro (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMonokaiProIJTheme"),
          new ThemeEntry("Moonlight (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMoonlightIJTheme"),
          new ThemeEntry("Night Owl (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTNightOwlIJTheme"),
          new ThemeEntry("Solarized Dark (Material)", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTSolarizedDarkIJTheme"),
          new ThemeEntry("Solarized Light (Material)",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTSolarizedLightIJTheme"), };

  // 护眼预设色
  public static final String[][] BG_PRESETS =
      { { "绿豆沙      #C7EDCC", "#C7EDCC" }, { "银河白      #FFFFFF", "#FFFFFF" }, { "杏仁黄      #FAF9DE", "#FAF9DE" },
          { "秋叶褐      #FFF2E2", "#FFF2E2" }, { "胭脂红      #FDE6E0", "#FDE6E0" }, { "海天蓝      #DCE2F1", "#DCE2F1" },
          { "葛巾紫      #E9EBFE", "#E9EBFE" }, { "极光灰      #EAEAEF", "#EAEAEF" }, { "青草绿      #E3EDCD", "#E3EDCD" },
          { "电脑管家    #CCE8CF", "#CCE8CF" }, { "WPS护眼色  #6E7B6C", "#6E7B6C" }, };

  private ThemeManager()
  {
  }

  /**
   * 安装指定主题；切换后自动重新应用字体和背景色，刷新所有已打开窗口。
   */
  public static void applyTheme(String className)
  {
    String safeClass = (className != null && !className.isBlank()) ? className : "com.formdev.flatlaf.FlatLightLaf";
    try
    {
      LookAndFeel laf = (LookAndFeel)Class.forName(safeClass).getDeclaredConstructor().newInstance();
      UIManager.setLookAndFeel(laf);
    }
    catch(Exception e)
    {
      try
      {
        FlatLightLaf.setup();
      }
      catch(Exception ignored)
      {
      }
    }
    // LaF 变更后重新覆盖字体和背景色（LaF.setup 会重置 UIManager 默认值）
    AppConfig cfg = AppConfig.getInstance();
    FontManager.applyGlobalFont(cfg.getFontFamily(), cfg.getFontSize());
    applyBgColorToUiManager(parseBgColor(cfg.getBgColorHex()));
    // 刷新所有已打开窗口
    for(Window w : Window.getWindows())
    {
      if(w.isDisplayable())
        SwingUtilities.updateComponentTreeUI(w);
    }
    // 再做递归 font + bgColor 精细覆盖
    FontManager.refreshAllWindows();
    applyBgColorToAllWindows(parseBgColor(cfg.getBgColorHex()));
  }

  /**
   * 将背景色写入 UIManager 相关键，影响后续新建的组件。
   * color == null 表示不覆盖（使用主题默认）。
   */
  public static void applyBgColorToUiManager(Color color)
  {
    if(color == null)
      return;
    ColorUIResource cr = new ColorUIResource(color);
    // 面板 & 滚动容器
    UIManager.put("Panel.background", cr);
    UIManager.put("ScrollPane.background", cr);
    UIManager.put("Viewport.background", cr);
    // 文本编辑区（SQL编辑器、消息区）
    UIManager.put("TextArea.background", cr);
    UIManager.put("TextPane.background", cr);
    UIManager.put("EditorPane.background", cr);
    // 表格 & 树保留主题配色，不在此处覆盖
  }

  /**
   * 将背景色递归应用到所有已打开窗口的目标组件。
   */
  public static void applyBgColor(Color color)
  {
    applyBgColorToUiManager(color);
    applyBgColorToAllWindows(color);
    for(Window w : Window.getWindows())
    {
      if(w.isDisplayable())
      {
        w.revalidate();
        w.repaint();
      }
    }
  }

  private static void applyBgColorToAllWindows(Color color)
  {
    if(color == null)
      return;
    for(Window w : Window.getWindows())
    {
      if(w.isDisplayable())
        applyBgColorRecursive(w, color);
    }
  }

  /**
   * 递归设置背景色：
   * - JPanel、JScrollPane viewport → 目标色
   * - JTextArea / JTextPane / JEditorPane → 目标色（护眼核心）
   * - JTable、JTree、JToolBar、Buttons、Menus → 保留主题色，跳过
   */
  private static void applyBgColorRecursive(Component c, Color color)
  {
    if(c instanceof JTableHeader || c instanceof JTree || c instanceof JToolBar || c instanceof AbstractButton || c instanceof JMenuBar
        || c instanceof JMenu || c instanceof JMenuItem || c instanceof JPopupMenu || c instanceof JLabel || c instanceof JComboBox)
    {
      // 保留主题配色，不递归覆盖（控件由 LaF 管理配色）
    }
    else if(c instanceof JScrollPane sp)
    {
      sp.setBackground(color);
      if(sp.getViewport() != null)
        sp.getViewport().setBackground(color);
    }
    else if(c instanceof JTable table)
    {
      // 表格单元格默认背景色设为护眼色，行状态渲染器可在此之上覆盖
      table.setBackground(color);
    }
    else if(c instanceof JTextArea || c instanceof JTextPane || c instanceof JEditorPane)
    {
      c.setBackground(color);
      ((JComponent)c).setOpaque(true);
    }
    else if(c instanceof JPanel)
    {
      c.setBackground(color);
    }

    if(c instanceof Container container)
    {
      for(Component child : container.getComponents())
        applyBgColorRecursive(child, color);
    }
  }

  /**
   * 解析十六进制颜色字符串，"" / "none" / "default" 返回 null
   */
  public static Color parseBgColor(String hex)
  {
    if(hex == null || hex.isBlank() || "none".equalsIgnoreCase(hex) || "default".equalsIgnoreCase(hex))
      return null;
    try
    {
      return Color.decode(hex);
    }
    catch(Exception e)
    {
      return null;
    }
  }

  public static String colorToHex(Color c)
  {
    return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
  }

  /**
   * 将当前配置的背景色应用到指定组件（及其子组件）。
   * 适用于动态创建的新组件（如新打开的 Tab）在创建后立即调用。
   */
  public static void applyBgColorToComponent(Component c)
  {
    Color color = parseBgColor(AppConfig.getInstance().getBgColorHex());
    if(color != null)
      applyBgColorRecursive(c, color);
  }
}

