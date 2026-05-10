#pragma once
#include <QWidget>
#include <QTreeView>

class ExplorerTreeModel;
class TabManager;

class ObjectExplorerWidget : public QWidget
{
    Q_OBJECT

public:
    explicit ObjectExplorerWidget(TabManager* tabManager, QWidget* parent = nullptr);

    void refreshTree();

private slots:
    void onItemDoubleClicked(const QModelIndex& index);
    void onItemExpanded(const QModelIndex& index);
    void onContextMenu(const QPoint& pos);

private:
    void loadChildren(const QModelIndex& parentIndex);

    QTreeView* m_treeView = nullptr;
    ExplorerTreeModel* m_treeModel = nullptr;
    TabManager* m_tabManager = nullptr;
};
