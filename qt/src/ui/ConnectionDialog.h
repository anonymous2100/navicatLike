#pragma once
#include <QDialog>
#include <QListWidget>
#include <QLineEdit>
#include <QComboBox>
#include <QPushButton>
#include <QStringList>
#include "common/Types.h"

class ConnectionDialog : public QDialog
{
    Q_OBJECT

public:
    explicit ConnectionDialog(QWidget* parent = nullptr);

    ConnectionInfo connectionInfo() const;

private slots:
    void onTestConnection();
    void onAccept();
    void onConnectionSelected(int index);
    void onTypeChanged(int idx);
    void onDeleteConnection();

private:
    void loadSavedConnections();
    void loadIntoForm(const ConnectionInfo& info);
    void clearForm();

    QListWidget* m_connectionList = nullptr;
    QLineEdit* m_nameField = nullptr;
    QComboBox* m_typeCombo = nullptr;
    QLineEdit* m_hostField = nullptr;
    QLineEdit* m_portField = nullptr;
    QLineEdit* m_dbField = nullptr;
    QLineEdit* m_userField = nullptr;
    QLineEdit* m_pwdField = nullptr;
    QPushButton* m_testBtn = nullptr;
    QPushButton* m_connectBtn = nullptr;
    QPushButton* m_deleteBtn = nullptr;

    ConnectionInfo m_currentInfo;
};
