#include "ConfirmDialog.h"
#include <QMessageBox>

bool ConfirmDialog::confirm(QWidget* parent, const QString& title, const QString& message)
{
    return QMessageBox::question(parent, title, message,
                                  QMessageBox::Yes | QMessageBox::No) == QMessageBox::Yes;
}

bool ConfirmDialog::confirmDelete(QWidget* parent, const QString& objectName)
{
    return QMessageBox::warning(parent, "Confirm Delete",
                                 QString("Are you sure you want to delete '%1'?\nThis action cannot be undone.").arg(objectName),
                                 QMessageBox::Yes | QMessageBox::No,
                                 QMessageBox::No) == QMessageBox::Yes;
}

void ConfirmDialog::info(QWidget* parent, const QString& title, const QString& message)
{
    QMessageBox::information(parent, title, message);
}

void ConfirmDialog::warn(QWidget* parent, const QString& title, const QString& message)
{
    QMessageBox::warning(parent, title, message);
}
