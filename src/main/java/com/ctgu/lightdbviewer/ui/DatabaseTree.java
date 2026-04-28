package com.ctgu.lightdbviewer.ui;

import com.ctgu.lightdbviewer.metadata.MetadataService;
import com.ctgu.lightdbviewer.ui.explorer.ExplorerPopupMenuFactory;
import com.ctgu.lightdbviewer.ui.workspace.TabManager;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 * @date 2026-04-23 18:11
 */

public class DatabaseTree extends JPanel
{
  private final JTree tree;
  private final TabManager tabManager;

  public DatabaseTree(TabManager tabManager)
  {
    this.tabManager = tabManager;
    setLayout(new BorderLayout());

    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Database");

    DefaultMutableTreeNode tablesNode = new DefaultMutableTreeNode("Tables");

    root.add(tablesNode);

    try
    {
      List<String> tables = MetadataService.listTables();
      for(String t : tables)
      {
        tablesNode.add(new DefaultMutableTreeNode(t));
      }
    }
    catch(SQLException e)
    {
      e.printStackTrace();
    }

    tree = new JTree(root);

    tree.addMouseListener(new MouseAdapter()
    {
      @Override
      public void mousePressed(MouseEvent e)
      {
        handleMouse(e);
      }
    });

    add(new JScrollPane(tree), BorderLayout.CENTER);
  }

  private void handleMouse(MouseEvent e)
  {
    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
    if(path == null)
      return;

    tree.setSelectionPath(path);
    Object node = path.getLastPathComponent();

    if(e.getClickCount() == 2 && path.getPathCount() == 3)
    {

      tabManager.openTable(node.toString());
      return;
    }

    if(SwingUtilities.isRightMouseButton(e))
    {
      JPopupMenu menu;

      if(path.getPathCount() == 3)
      {
        menu = ExplorerPopupMenuFactory.createTableMenu(node.toString(), tabManager);
      }
      else
      {
        menu = ExplorerPopupMenuFactory.createConnectionMenu(tabManager);
      }
      menu.show(tree, e.getX(), e.getY());
    }
  }
}