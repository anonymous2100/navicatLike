#pragma once
#include <QDialog>
#include <QLabel>

class ConfirmDialog
{
public:
    static bool confirm(QWidget* parent, const QString& title, const QString& message);
    static bool confirmDelete(QWidget* parent, const QString& objectName);
    static void info(QWidget* parent, const QString& title, const QString& message);
    static void warn(QWidget* parent, const QString& title, const QString& message);
};
