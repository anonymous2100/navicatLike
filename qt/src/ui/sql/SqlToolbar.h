#pragma once
#include <QToolBar>

class SqlToolbar : public QToolBar
{
    Q_OBJECT

public:
    explicit SqlToolbar(QWidget* parent = nullptr);

signals:
    void execute();
    void stop();
    void format();
};
