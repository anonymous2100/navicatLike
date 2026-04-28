package com.ctgu.lightdbviewer.ui.workspace;

import com.ctgu.lightdbviewer.ui.common.ConfirmDialog;
import com.ctgu.lightdbviewer.ui.workspace.tab.AbstractTab;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class WorkspaceTabs extends JTabbedPane
{

  public WorkspaceTabs()
  {
    setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
  }

  @Override
  public void addTab(String title, Component component)
  {
    super.addTab(title, component);
    int index = indexOfComponent(component);
    if(index >= 0)
    {
      setTabComponentAt(index, createTabHeader(component));
    }
  }

  @Override
  public void removeTabAt(int index)
  {
    java.awt.Component comp = getComponentAt(index);

    if(comp instanceof AbstractTab tab && tab.isDirty())
    {
      boolean ok = ConfirmDialog.confirm(this, "关闭标签页", "该标签页有未保存的修改，确定关闭？");
      if(!ok)
      {
        return;
      }
    }
    super.removeTabAt(index);
  }

  /**
   * 关闭除 keepTab 之外的所有标签页
   */
  public void closeOtherTabs(Component keepTab)
  {
    for(int i = getTabCount() - 1; i >= 0; i--)
    {
      if(getComponentAt(i) != keepTab)
        removeTabAt(i);
    }
  }

  /**
   * 关闭所有标签页
   */
  public void closeAllTabs()
  {
    for(int i = getTabCount() - 1; i >= 0; i--)
      removeTabAt(i);
  }

  private JComponent createTabHeader(Component tabComponent)
  {
    JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    header.setOpaque(false);

    JLabel label = new JLabel()
    {
      @Override
      public String getText()
      {
        int idx = indexOfComponent(tabComponent);
        return idx >= 0 ? WorkspaceTabs.this.getTitleAt(idx) : "";
      }
    };

    JButton close = new JButton("x");
    close.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
    close.setFocusable(false);
    close.setMargin(new Insets(0, 0, 0, 0));
    close.addActionListener(e -> {
      int idx = indexOfComponent(tabComponent);
      if(idx >= 0)
      {
        removeTabAt(idx);
      }
    });

    header.add(label);
    header.add(close);

    // 统一鼠标处理：左键选中标签，右键弹出菜单
    // 注意：setTabComponentAt 后子组件会消费鼠标事件，必须手动调用 setSelectedIndex
    MouseAdapter headerAdapter = new MouseAdapter()
    {
      private void selectTab()
      {
        int idx = indexOfComponent(tabComponent);
        if(idx >= 0)
          setSelectedIndex(idx);
      }

      @Override
      public void mousePressed(MouseEvent e)
      {
        if(SwingUtilities.isLeftMouseButton(e))
          selectTab();
        if(e.isPopupTrigger())
          showTabPopup(e, tabComponent);
      }

      @Override
      public void mouseReleased(MouseEvent e)
      {
        if(e.isPopupTrigger())
          showTabPopup(e, tabComponent);
      }
    };

    header.addMouseListener(headerAdapter);
    label.addMouseListener(headerAdapter);

    return header;
  }

  private void showTabPopup(MouseEvent e, Component tabComponent)
  {
    // 先选中被右键的标签
    int idx = indexOfComponent(tabComponent);
    if(idx >= 0)
      setSelectedIndex(idx);

    JPopupMenu popup = new JPopupMenu();

    JMenuItem closeCurrent = new JMenuItem("关闭当前标签页");
    closeCurrent.addActionListener(ev -> {
      int i = indexOfComponent(tabComponent);
      if(i >= 0)
        removeTabAt(i);
    });

    JMenuItem closeOthers = new JMenuItem("关闭其他标签页");
    closeOthers.addActionListener(ev -> closeOtherTabs(tabComponent));

    JMenuItem closeAll = new JMenuItem("关闭所有标签页");
    closeAll.addActionListener(ev -> closeAllTabs());

    popup.add(closeCurrent);
    popup.add(closeOthers);
    popup.addSeparator();
    popup.add(closeAll);

    popup.show((Component)e.getSource(), e.getX(), e.getY());
  }
}