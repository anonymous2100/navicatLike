package com.ctgu.lightdbviewer.ui.table;



import com.ctgu.lightdbviewer.ui.status.StatusBarPanel;

import javax.swing.*;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public class DataToolbar extends JPanel
{
  public DataToolbar(EditableResultTableModel model, StatusBarPanel status)
  {
    JButton add = new JButton("+");
    JButton del = new JButton("-");
    JButton save = new JButton("保存");
    JButton revert = new JButton("撤销");

    add.addActionListener(e -> model.addInsertRow());
    del.addActionListener(e -> { /* 需要外部提供选中行索引，此处无上下文，暂不实现 */ });
    save.addActionListener(e -> {
      model.commit();
      status.setMessage("修改已保存");
    });
    revert.addActionListener(e -> model.revert());
    add(add);
    add(del);
    add(save);
    add(revert);
  }

  public DataToolbar(Runnable onAdd, Runnable onDelete, Runnable onSave, Runnable onRevert, Runnable onRefresh)
  {
    JButton add = new JButton("+");
    JButton del = new JButton("-");
    JButton save = new JButton("保存");
    JButton revert = new JButton("撤销");
    JButton refresh = new JButton("刷新");
    add.addActionListener(e -> onAdd.run());
    del.addActionListener(e -> onDelete.run());
    save.addActionListener(e -> onSave.run());
    revert.addActionListener(e -> onRevert.run());
    refresh.addActionListener(e -> onRefresh.run());
    add(add);
    add(del);
    add(save);
    add(revert);
    add(refresh);
  }
}