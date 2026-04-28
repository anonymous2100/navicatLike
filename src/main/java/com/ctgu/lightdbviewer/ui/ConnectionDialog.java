package com.ctgu.lightdbviewer.ui;

import com.ctgu.lightdbviewer.config.AppConfig;
import com.ctgu.lightdbviewer.config.SavedConnection;
import com.ctgu.lightdbviewer.db.DbType;
import com.ctgu.lightdbviewer.model.DbConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 *
 * ConnectionDialog — 数据库连接管理对话框
 * <p>
 * 布局：左侧已保存连接列表 + 右侧带边距的连接表单
 * 功能：新建/编辑/删除连接，密码由 AppConfig 加密保存
 */
public class ConnectionDialog extends JDialog
{
  public interface OnConnect
  {
    void accept(DbConfig cfg);
  }

  private static final int PADDING = 16;
  private static final int FIELD_WIDTH = 260;

  private final OnConnect onConnect;

  // 左侧列表
  private final DefaultListModel<SavedConnection> listModel = new DefaultListModel<>();
  private final JList<SavedConnection> connectionList = new JList<>(listModel);

  // 右侧表单字段
  private final JTextField nameField = new JTextField();
  private final JComboBox<DbType> typeCombo = new JComboBox<>(DbType.values());
  private final JTextField hostField = new JTextField("localhost");
  private final JTextField portField = new JTextField("5432");
  private final JTextField dbField = new JTextField();
  private final JTextField userField = new JTextField();
  private final JPasswordField pwdField = new JPasswordField();

  public ConnectionDialog(OnConnect onConnect)
  {
    this.onConnect = onConnect;
    setTitle("连接管理器");
    setModal(true);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    JPanel content = new JPanel(new BorderLayout(0, 0));
    content.add(buildLeftPanel(), BorderLayout.WEST);
    content.add(buildFormPanel(), BorderLayout.CENTER);
    setContentPane(content);

    loadSavedConnections();
    pack();
    setMinimumSize(new Dimension(680, 380));
  }


  private JPanel buildLeftPanel()
  {
    JPanel panel = new JPanel(new BorderLayout(0, 8));
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(200, 200, 200)),
        new EmptyBorder(PADDING, PADDING, PADDING, PADDING)));
    panel.setPreferredSize(new Dimension(200, 0));

    JLabel title = new JLabel("已保存连接");
    title.setFont(title.getFont().deriveFont(Font.BOLD));
    title.setBorder(new EmptyBorder(0, 0, 6, 0));

    connectionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    connectionList.addListSelectionListener(e -> {
      if(!e.getValueIsAdjusting())
        loadIntoForm(connectionList.getSelectedValue());
    });

    JScrollPane scroll = new JScrollPane(connectionList);
    scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

    JButton newBtn = new JButton("新建");
    JButton delBtn = new JButton("删除");
    newBtn.addActionListener(e -> clearForm());
    delBtn.addActionListener(e -> deleteSelected());

    JPanel btnRow = new JPanel(new GridLayout(1, 2, 6, 0));
    btnRow.setBorder(new EmptyBorder(6, 0, 0, 0));
    btnRow.add(newBtn);
    btnRow.add(delBtn);

    panel.add(title, BorderLayout.NORTH);
    panel.add(scroll, BorderLayout.CENTER);
    panel.add(btnRow, BorderLayout.SOUTH);
    return panel;
  }

  private JPanel buildFormPanel()
  {
    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.setBorder(new EmptyBorder(PADDING, PADDING + 4, PADDING, PADDING));

    JLabel title = new JLabel("连接详情");
    title.setFont(title.getFont().deriveFont(Font.BOLD));
    title.setBorder(new EmptyBorder(0, 0, 12, 0));

    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints left = formGbc(0);
    GridBagConstraints right = formGbc(1);
    right.fill = GridBagConstraints.HORIZONTAL;
    right.weightx = 1.0;

    String[] labels  = { "连接名称", "数据库类型", "主机地址", "端口", "数据库", "用户名", "密码" };
    Component[] fields = { nameField, typeCombo, hostField, portField, dbField, userField, pwdField };

    for(int i = 0; i < labels.length; i++)
    {
      left.gridy = i;
      right.gridy = i;
      JLabel lbl = new JLabel(labels[i] + "  ");
      lbl.setHorizontalAlignment(SwingConstants.RIGHT);
      form.add(lbl, left);
      if(fields[i] instanceof JTextField tf)
        tf.setPreferredSize(new Dimension(FIELD_WIDTH, tf.getPreferredSize().height));
      form.add(fields[i], right);
    }

    // 按钮行
    left.gridy = labels.length;
    right.gridy = labels.length;
    left.gridwidth = 2;
    right.gridwidth = 0;
    form.add(buildButtonRow(), left);

    wrapper.add(title, BorderLayout.NORTH);
    wrapper.add(form, BorderLayout.CENTER);
    return wrapper;
  }

  private JPanel buildButtonRow()
  {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
    JButton saveBtn    = new JButton("保存连接");
    JButton connectBtn = new JButton("连 接");
    connectBtn.setFont(connectBtn.getFont().deriveFont(Font.BOLD));

    saveBtn.addActionListener(e -> saveCurrentConnection());
    connectBtn.addActionListener(e -> doConnect());

    row.add(saveBtn);
    row.add(connectBtn);
    return row;
  }

  private static GridBagConstraints formGbc(int col)
  {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = col;
    gbc.insets = new Insets(6, 4, 6, 4);
    gbc.anchor = GridBagConstraints.WEST;
    return gbc;
  }

  private void loadSavedConnections()
  {
    listModel.clear();
    for(SavedConnection sc : AppConfig.getInstance().getConnections())
      listModel.addElement(sc);
  }

  private void loadIntoForm(SavedConnection sc)
  {
    if(sc == null) return;
    nameField.setText(sc.name);
    typeCombo.setSelectedItem(sc.type);
    hostField.setText(sc.host);
    portField.setText(String.valueOf(sc.port));
    dbField.setText(sc.database);
    userField.setText(sc.username);
    pwdField.setText(sc.password);
  }

  private void clearForm()
  {
    connectionList.clearSelection();
    nameField.setText("");
    typeCombo.setSelectedIndex(0);
    hostField.setText("localhost");
    portField.setText("5432");
    dbField.setText("");
    userField.setText("");
    pwdField.setText("");
    nameField.requestFocus();
  }

  private void deleteSelected()
  {
    SavedConnection sc = connectionList.getSelectedValue();
    if(sc == null) return;
    int confirm = JOptionPane.showConfirmDialog(this,
        "删除连接 \"" + sc + "\" ？", "删除连接", JOptionPane.YES_NO_OPTION);
    if(confirm != JOptionPane.YES_OPTION) return;
    AppConfig.getInstance().removeConnection(sc);
    AppConfig.getInstance().save();
    loadSavedConnections();
    clearForm();
  }

  private void saveCurrentConnection()
  {
    SavedConnection sc = buildFromForm();
    if(sc.name.isBlank())
    {
      JOptionPane.showMessageDialog(this, "请填写连接名称后再保存。", "保存失败", JOptionPane.WARNING_MESSAGE);
      nameField.requestFocus();
      return;
    }
    AppConfig.getInstance().addOrUpdateConnection(sc);
    AppConfig.getInstance().save();
    loadSavedConnections();
    // 重选刚保存的项
    for(int i = 0; i < listModel.size(); i++)
    {
      if(listModel.get(i).name.equals(sc.name))
      {
        connectionList.setSelectedIndex(i);
        break;
      }
    }
    JOptionPane.showMessageDialog(this, "连接 \"" + sc.name + "\" 已保存。", "保存成功", JOptionPane.INFORMATION_MESSAGE);
  }

  private void doConnect()
  {
    SavedConnection sc = buildFromForm();
    // 有名称则自动保存
    if(!sc.name.isBlank())
    {
      AppConfig.getInstance().addOrUpdateConnection(sc);
      AppConfig.getInstance().save();
    }
    onConnect.accept(sc.toDbConfig());
    dispose();
  }

  private SavedConnection buildFromForm()
  {
    SavedConnection sc = new SavedConnection();
    sc.name = nameField.getText().trim();
    sc.type = (DbType) typeCombo.getSelectedItem();
    sc.host = hostField.getText().trim();
    try { sc.port = Integer.parseInt(portField.getText().trim()); }
    catch(Exception e) { sc.port = 5432; }
    sc.database = dbField.getText().trim();
    sc.username = userField.getText().trim();
    sc.password = new String(pwdField.getPassword());
    return sc;
  }
}