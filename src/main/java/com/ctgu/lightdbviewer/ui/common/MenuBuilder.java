package com.ctgu.lightdbviewer.ui.common;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 *
 * 菜单构建器 - 减少菜单创建的重复代码
 * <p>
 * 提供流畅的API来构建菜单结构
 */
public class MenuBuilder
{
  private final JMenu menu;

  private MenuBuilder(String name)
  {
    this.menu = new JMenu(name);
  }

  /**
   * 创建新的菜单构建器
   *
   * @param name 菜单名称
   * @return 菜单构建器
   */
  public static MenuBuilder create(String name)
  {
    return new MenuBuilder(name);
  }

  /**
   * 添加菜单项
   *
   * @param text   菜单项文本
   * @param action 动作监听器
   * @return 菜单构建器
   */
  public MenuBuilder addItem(String text, ActionListener action)
  {
    JMenuItem item = new JMenuItem(text);
    item.addActionListener(action);
    menu.add(item);
    return this;
  }

  /**
   * 添加带快捷键的菜单项
   *
   * @param text      菜单项文本
   * @param keyCode   键码
   * @param modifiers 修饰键（如 KeyEvent.CTRL_DOWN_MASK）
   * @param action    动作监听器
   * @return 菜单构建器
   */
  public MenuBuilder addItem(String text, int keyCode, int modifiers, ActionListener action)
  {
    JMenuItem item = new JMenuItem(text);
    item.setAccelerator(KeyStroke.getKeyStroke(keyCode, modifiers));
    item.addActionListener(action);
    menu.add(item);
    return this;
  }

  /**
   * 添加分隔符
   *
   * @return 菜单构建器
   */
  public MenuBuilder addSeparator()
  {
    menu.addSeparator();
    return this;
  }

  /**
   * 添加复选框菜单项
   *
   * @param text     菜单项文本
   * @param selected 是否选中
   * @param action   动作监听器
   * @return 菜单构建器
   */
  public MenuBuilder addCheckBoxItem(String text, boolean selected, ActionListener action)
  {
    JCheckBoxMenuItem item = new JCheckBoxMenuItem(text, selected);
    item.addActionListener(action);
    menu.add(item);
    return this;
  }

  /**
   * 添加子菜单
   *
   * @param subMenu 子菜单
   * @return 菜单构建器
   */
  public MenuBuilder addSubMenu(JMenu subMenu)
  {
    menu.add(subMenu);
    return this;
  }

  /**
   * 构建并返回菜单
   *
   * @return 构建的菜单
   */
  public JMenu build()
  {
    return menu;
  }

  /**
   * 创建标准的编辑菜单
   *
   * @return 编辑菜单
   */
  public static JMenu createEditMenu()
  {
    return MenuBuilder.create("编辑").addItem("复制", KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK, e -> copyAction())
        .addItem("粘贴", KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK, e -> pasteAction())
        .addItem("全选", KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK, e -> selectAllAction()).build();
  }

  /**
   * 创建标准的文件菜单
   *
   * @param newConnectionAction   新建连接动作
   * @param closeConnectionAction 关闭连接动作
   * @param exitAction            退出动作
   * @return 文件菜单
   */
  public static JMenu createFileMenu(ActionListener newConnectionAction, ActionListener closeConnectionAction, ActionListener exitAction)
  {
    return MenuBuilder.create("文件").addItem("新建连接", newConnectionAction).addItem("关闭连接", closeConnectionAction).addSeparator()
        .addItem("退出", KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK, exitAction).build();
  }

  private static void copyAction()
  {
    java.awt.Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if(focused instanceof javax.swing.text.JTextComponent tc)
    {
      tc.copy();
    }
  }

  private static void pasteAction()
  {
    java.awt.Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if(focused instanceof javax.swing.text.JTextComponent tc)
    {
      tc.paste();
    }
  }

  private static void selectAllAction()
  {
    java.awt.Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if(focused instanceof javax.swing.text.JTextComponent tc)
    {
      tc.selectAll();
    }
  }
}
