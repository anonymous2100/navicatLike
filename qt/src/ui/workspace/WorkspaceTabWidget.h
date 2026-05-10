#pragma once
#include <QTabWidget>
#include "AbstractTab.h"

class WorkspaceTabWidget : public QTabWidget
{
    Q_OBJECT

public:
    explicit WorkspaceTabWidget(QWidget* parent = nullptr);

    int addTab(AbstractTab* tab, const QString& title);
    void removeTab(int index);

public slots:
    void closeCurrentTab();
    void closeOtherTabs(int index);

signals:
    void tabCountChanged(int count);

private:
    void onTabTitleChanged(const QString& title);
    void onTabDirtyChanged(bool dirty);
    void updateTabLabel(int index);
};
