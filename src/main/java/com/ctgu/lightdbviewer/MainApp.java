package com.ctgu.lightdbviewer;

import com.ctgu.lightdbviewer.config.AppConfig;
import com.ctgu.lightdbviewer.ui.frame.MainFrame;
import com.ctgu.lightdbviewer.util.FontManager;
import com.ctgu.lightdbviewer.util.ThemeManager;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class MainApp
{
  public static void main(String[] args)
  {
    // 1. 加载配置文件（在任何 UI 初始化之前）
    AppConfig.getInstance().load();

    SwingUtilities.invokeLater(() -> {
      try
      {
        // 2. 安装配置文件中保存的主题（失败则回退FlatLightLaf）
        String savedTheme = AppConfig.getInstance().getThemeName();
        try
        {
          LookAndFeel laf = (LookAndFeel)Class.forName(savedTheme).getDeclaredConstructor().newInstance();
          UIManager.setLookAndFeel(laf);
        }
        catch(Exception ex)
        {
          FlatLightLaf.setup();
        }
        // 3. 应用全局 UI 字体并刷新窗口，同时应用编辑器字体（编辑器为等宽字体）
        FontManager.applyUiFont(AppConfig.getInstance().getFontFamily(), AppConfig.getInstance().getFontSize());
        FontManager.applyEditorFont(AppConfig.getInstance().getEditorFontFamily(), AppConfig.getInstance().getEditorFontSize());
        // 4. 应用全局背景色（写入 UIManager，后续新建组件自动继承）
        Color bgColor = ThemeManager.parseBgColor(AppConfig.getInstance().getBgColorHex());
        ThemeManager.applyBgColorToUiManager(bgColor);
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", true);
      }
      catch(Exception e)
      {
        try
        {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch(Exception ignored)
        {
          ignored.printStackTrace();
        }
      }
      // 5. 创建并显示主窗口
      MainFrame window = new MainFrame();
      window.setVisible(true);
      // 6. 窗口创建完成后，递归应用背景色到所有已创建的组
      Color bgColor = ThemeManager.parseBgColor(AppConfig.getInstance().getBgColorHex());
      ThemeManager.applyBgColor(bgColor);
      // 7. 弹出数据库连接对话框（不再自动连接第一个已保存连接
      SwingUtilities.invokeLater(window::openConnectionDialog);
    });
  }
}

