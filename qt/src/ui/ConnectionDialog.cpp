#include "ConnectionDialog.h"
#include "config/AppSettings.h"
#include "pool/ConnectionPool.h"
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QFormLayout>
#include <QGroupBox>
#include <QLabel>
#include <QMessageBox>
#include <QFrame>
#include <QDialogButtonBox>
#include "common/Logging.h"

ConnectionDialog::ConnectionDialog(QWidget* parent)
    : QDialog(parent)
{
    setWindowTitle(tr("Connection Manager"));
    setMinimumSize(680, 420);

    auto* mainLayout = new QHBoxLayout(this);

    // --- Left panel: Saved connections ---
    auto* leftPanel = new QWidget(this);
    auto* leftLayout = new QVBoxLayout(leftPanel);
    leftLayout->setContentsMargins(12, 12, 12, 12);

    auto* leftTitle = new QLabel(tr("Saved Connections"), leftPanel);
    QFont boldFont = leftTitle->font();
    boldFont.setBold(true);
    leftTitle->setFont(boldFont);

    m_connectionList = new QListWidget(leftPanel);
    m_connectionList->setMinimumWidth(180);

    m_deleteBtn = new QPushButton(tr("Delete"), leftPanel);
    m_deleteBtn->setEnabled(false);
    connect(m_deleteBtn, &QPushButton::clicked, this, &ConnectionDialog::onDeleteConnection);

    leftLayout->addWidget(leftTitle);
    leftLayout->addWidget(m_connectionList);
    leftLayout->addWidget(m_deleteBtn);

    // --- Right panel: Form ---
    auto* rightPanel = new QWidget(this);
    auto* rightLayout = new QVBoxLayout(rightPanel);
    rightLayout->setContentsMargins(16, 12, 16, 12);

    auto* formGroup = new QGroupBox(tr("Connection Properties"), rightPanel);
    auto* formLayout = new QFormLayout(formGroup);

    m_nameField = new QLineEdit(formGroup);
    m_nameField->setPlaceholderText(tr("My Connection"));
    formLayout->addRow(tr("Name:"), m_nameField);

    m_typeCombo = new QComboBox(formGroup);
    m_typeCombo->addItem("MySQL", "MYSQL");
    m_typeCombo->addItem("PostgreSQL", "POSTGRESQL");
    formLayout->addRow(tr("Type:"), m_typeCombo);

    m_hostField = new QLineEdit("localhost", formGroup);
    formLayout->addRow(tr("Host:"), m_hostField);

    m_portField = new QLineEdit("5432", formGroup);
    formLayout->addRow(tr("Port:"), m_portField);

    m_dbField = new QLineEdit(formGroup);
    formLayout->addRow(tr("Database:"), m_dbField);

    m_userField = new QLineEdit(formGroup);
    formLayout->addRow(tr("Username:"), m_userField);

    m_pwdField = new QLineEdit(formGroup);
    m_pwdField->setEchoMode(QLineEdit::Password);
    formLayout->addRow(tr("Password:"), m_pwdField);

    rightLayout->addWidget(formGroup);

    // --- Buttons ---
    m_testBtn = new QPushButton(tr("Test Connection"), rightPanel);
    m_connectBtn = new QPushButton(tr("Connect"), rightPanel);
    m_connectBtn->setDefault(true);
    auto* cancelBtn = new QPushButton(tr("Cancel"), rightPanel);

    auto* btnLayout = new QHBoxLayout();
    btnLayout->addWidget(m_testBtn);
    btnLayout->addStretch();
    btnLayout->addWidget(m_connectBtn);
    btnLayout->addWidget(cancelBtn);
    rightLayout->addLayout(btnLayout);

    // --- Assemble ---
    QFrame* separator = new QFrame(this);
    separator->setFrameShape(QFrame::VLine);
    separator->setFrameShadow(QFrame::Sunken);

    mainLayout->addWidget(leftPanel);
    mainLayout->addWidget(separator);
    mainLayout->addWidget(rightPanel, 1);

    // --- Connections ---
    connect(m_connectionList, &QListWidget::currentRowChanged,
            this, &ConnectionDialog::onConnectionSelected);
    connect(m_typeCombo, &QComboBox::currentIndexChanged,
            this, &ConnectionDialog::onTypeChanged);
    connect(m_testBtn, &QPushButton::clicked, this, &ConnectionDialog::onTestConnection);
    connect(m_connectBtn, &QPushButton::clicked, this, &ConnectionDialog::onAccept);
    connect(cancelBtn, &QPushButton::clicked, this, &QDialog::reject);

    loadSavedConnections();
}

void ConnectionDialog::loadSavedConnections()
{
    m_connectionList->clear();
    auto conns = AppSettings::instance().connections();
    for (const auto& c : conns) {
        m_connectionList->addItem(c.displayName());
    }
    if (!conns.empty()) {
        m_connectionList->setCurrentRow(0);
    }
}

void ConnectionDialog::onConnectionSelected(int index)
{
    if (index < 0) return;
    auto conns = AppSettings::instance().connections();
    if (index < static_cast<int>(conns.size())) {
        loadIntoForm(conns[index]);
    }
    m_deleteBtn->setEnabled(true);
}

void ConnectionDialog::onTypeChanged(int idx)
{
    QString typeStr = m_typeCombo->itemData(idx).toString();
    if (typeStr == "MYSQL") {
        m_portField->setText("3306");
    } else {
        m_portField->setText("5432");
    }
}

void ConnectionDialog::onDeleteConnection()
{
    int idx = m_connectionList->currentRow();
    if (idx < 0) return;
    auto conns = AppSettings::instance().connections();
    if (idx < static_cast<int>(conns.size())) {
        AppSettings::instance().removeConnection(conns[idx]);
        AppSettings::instance().save();
        loadSavedConnections();
        clearForm();
        m_deleteBtn->setEnabled(false);
    }
}

void ConnectionDialog::loadIntoForm(const ConnectionInfo& info)
{
    m_nameField->setText(info.name);
    int typeIdx = m_typeCombo->findData(dbTypeToString(info.type));
    if (typeIdx >= 0) m_typeCombo->setCurrentIndex(typeIdx);
    m_hostField->setText(info.host);
    m_portField->setText(QString::number(info.port));
    m_dbField->setText(info.database);
    m_userField->setText(info.username);
    m_pwdField->setText(info.password);
}

void ConnectionDialog::clearForm()
{
    m_nameField->clear();
    m_hostField->setText("localhost");
    m_portField->setText("5432");
    m_dbField->clear();
    m_userField->clear();
    m_pwdField->clear();
}

void ConnectionDialog::onTestConnection()
{
    ConnectionInfo info;
    info.name = m_nameField->text();
    info.type = dbTypeFromString(m_typeCombo->currentData().toString());
    info.host = m_hostField->text();
    info.port = m_portField->text().toInt();
    info.database = m_dbField->text();
    info.username = m_userField->text();
    info.password = m_pwdField->text();

    try {
        ConnectionPool::instance().connect(
            info.host, info.port, info.database,
            info.username, info.password, info.type);
        ConnectionPool::instance().closeCurrent();
        QMessageBox::information(this, tr("Success"),
            tr("Connection test successful!"));
    } catch (const std::exception& e) {
        QMessageBox::critical(this, tr("Test Failed"), e.what());
    }
}

void ConnectionDialog::onAccept()
{
    m_currentInfo.name = m_nameField->text();
    m_currentInfo.type = dbTypeFromString(m_typeCombo->currentData().toString());
    m_currentInfo.host = m_hostField->text();
    m_currentInfo.port = m_portField->text().toInt();
    m_currentInfo.database = m_dbField->text();
    m_currentInfo.username = m_userField->text();
    m_currentInfo.password = m_pwdField->text();

    if (m_currentInfo.name.isEmpty())
        m_currentInfo.name = m_currentInfo.displayName();

    accept();
}

ConnectionInfo ConnectionDialog::connectionInfo() const
{
    return m_currentInfo;
}
