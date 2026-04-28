package com.ctgu.lightdbviewer.util;

import com.ctgu.lightdbviewer.config.AppConfig;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.Enumeration;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 *
 * FontManager — 全局字体管理
 * <p>
 * - UI 主字体（树、按钮、标签、结果表格等）
 * - 编辑器字体（SQL编辑器、消息区域等等宽区域）
 * - 运行时递归刷新所有已打开窗口
 */
public final class FontManager
{
  private static final String[] MONO_FAMILIES =
      { "Consolas", "Courier New", "Courier", "Lucida Console", "Lucida Sans Typewriter", "JetBrains Mono", "Fira Code", "Fira Mono",
          "Source Code Pro", "Cascadia Code", "Cascadia Mono", "Cascadia Code PL", "Cascadia Mono PL", "DejaVu Sans Mono",
          "Liberation Mono", "Hack", "Inconsolata", "Andale Mono", "Menlo", "Monaco", "SF Mono", "Ubuntu Mono", "Noto Sans Mono",
          "Noto Mono", "Roboto Mono", "IBM Plex Mono", "Droid Sans Mono", "Monospaced", };

  private FontManager()
  {
  }

  /**
   * 切换 UI 主字体：更新 UIManager 默认字体，然后刷新所有窗口
   */
  public static void applyUiFont(String family, int size)
  {
    applyGlobalFont(family, size);
    refreshAllWindows();
  }

  /**
   * 切换编辑器字体：直接刷新所有窗口（读取 AppConfig 中已保存的配置）
   */
  public static void applyEditorFont(String family, int size)
  {
    // 参数 family/size 已由调用方写入 AppConfig，刷新时会通过 getCurrentEditorFont() 读取
    refreshAllWindows();
  }

  public static void applyGlobalFont(String family, int size)
  {
    Font resolved = resolveFont(family, Font.PLAIN, size);
    FontUIResource fontResource = new FontUIResource(resolved);
    UIManager.put("defaultFont", fontResource);
    Enumeration<Object> keys = UIManager.getDefaults().keys();
    while(keys.hasMoreElements())
    {
      Object key = keys.nextElement();
      if(UIManager.get(key) instanceof FontUIResource)
        UIManager.put(key, fontResource);
    }
  }

  public static void refreshAllWindows()
  {
    Font uiFont = getCurrentFont();
    Font editorFont = getCurrentEditorFont();
    for(Window w : Window.getWindows())
    {
      if(w.isDisplayable())
      {
        applyFontRecursive(w, uiFont, editorFont);
        w.revalidate();
        w.repaint();
      }
    }
  }

  public static Font getCurrentFont()
  {
    AppConfig cfg = AppConfig.getInstance();
    return resolveFont(cfg.getFontFamily(), Font.PLAIN, cfg.getFontSize());
  }

  public static Font getCurrentEditorFont()
  {
    AppConfig cfg = AppConfig.getInstance();
    return resolveMonoFont(cfg.getEditorFontFamily(), Font.PLAIN, cfg.getEditorFontSize());
  }

  private static void applyFontRecursive(Component c, Font uiFont, Font editorFont)
  {
    if(c instanceof javax.swing.text.JTextComponent tc && (tc instanceof javax.swing.JTextPane || tc instanceof javax.swing.JEditorPane))
    {
      // 编辑器组件（JTextPane / JEditorPane）优先使用编辑器（等宽）字体
      tc.setFont(editorFont);
    }
    else if(c instanceof JTextArea ta)
    {
      // 普通 JTextArea：保持原有策略——如果当前字体是等宽则视为编辑器/代码区，使用编辑器字体；否则使用 UI 字体
      Font cur = ta.getFont();
      ta.setFont(isMonospace(cur) ? editorFont : uiFont);
    }
    else if(c instanceof JTable table)
    {
      // JTable 需要同时更新行高和表头
      table.setFont(uiFont);
      table.setRowHeight(uiFont.getSize() + 6);
      JTableHeader header = table.getTableHeader();
      if(header != null)
        header.setFont(uiFont.deriveFont(Font.BOLD));
    }
    else if(c instanceof JTree tree)
    {
      // JTree 行高跟随字体
      tree.setFont(uiFont);
      tree.setRowHeight(uiFont.getSize() + 8);
    }
    else
    {
      // 其余普通组件
      Font cur = c.getFont();
      if(cur == null || !isMonospace(cur))
        c.setFont(uiFont);
    }

    if(c instanceof Container container)
    {
      for(Component child : container.getComponents())
        applyFontRecursive(child, uiFont, editorFont);
    }
  }

  public static Font resolveFont(String family, int style, int size)
  {
    if(family != null && isFontAvailable(family))
      return new Font(family, style, size);
    for(String fb : new String[] { "Segoe UI", "SansSerif", "Dialog" })
    {
      if(isFontAvailable(fb))
        return new Font(fb, style, size);
    }
    return new Font(Font.SANS_SERIF, style, size);
  }

  private static Font resolveMonoFont(String preferred, int style, int size)
  {
    if(preferred != null && isFontAvailable(preferred))
      return new Font(preferred, style, size);
    for(String m : MONO_FAMILIES)
    {
      if(isFontAvailable(m))
        return new Font(m, style, size);
    }
    return new Font(Font.MONOSPACED, style, size);
  }

  private static boolean isMonospace(Font f)
  {
    if(f == null)
      return false;
    String family = f.getFamily();
    for(String m : MONO_FAMILIES)
    {
      if(m.equalsIgnoreCase(family))
        return true;
    }
    return false;
  }

  private static boolean isFontAvailable(String name)
  {
    for(String f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
    {
      if(f.equalsIgnoreCase(name))
        return true;
    }
    return false;
  }
}
