#pragma once
#include <QObject>

class WorkspaceTabWidget;
class StatusBarWidget;
class AbstractTab;

class TabManager : public QObject
{
    Q_OBJECT

public:
    explicit TabManager(WorkspaceTabWidget* workspace,
                         StatusBarWidget* statusBar,
                         QObject* parent = nullptr);

    void openTable(const QString& tableName);
    void openQuery(const QString& initialSql = {});
    void openDesignTable(const QString& tableName);
    void openObjectList(const QString& objectName, const QString& listType);

    void executeCurrentQuery();

private:
    WorkspaceTabWidget* m_workspace;
    StatusBarWidget* m_statusBar;
};
