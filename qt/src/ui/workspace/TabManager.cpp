#include "TabManager.h"
#include "WorkspaceTabWidget.h"
#include "ui/status/StatusBarWidget.h"
#include "ui/tab/TableDataTab.h"
#include "ui/tab/QueryTab.h"
#include "ui/tab/DesignTableTab.h"
#include "ui/tab/ObjectListTab.h"

TabManager::TabManager(WorkspaceTabWidget* workspace,
                         StatusBarWidget* statusBar,
                         QObject* parent)
    : QObject(parent)
    , m_workspace(workspace)
    , m_statusBar(statusBar)
{
}

void TabManager::openTable(const QString& tableName)
{
    auto* tab = new TableDataTab(tableName, m_workspace);
    m_workspace->addTab(tab, tableName);
    tab->refresh();
}

void TabManager::openQuery(const QString& initialSql)
{
    auto* tab = new QueryTab(m_workspace);
    if (!initialSql.isEmpty()) {
        tab->setSql(initialSql);
    }
    m_workspace->addTab(tab, tr("Query"));
}

void TabManager::openDesignTable(const QString& tableName)
{
    auto* tab = new DesignTableTab(tableName, m_workspace);
    m_workspace->addTab(tab, tr("Design: %1").arg(tableName));
    tab->refresh();
}

void TabManager::openObjectList(const QString& objectName, const QString& listType)
{
    auto* tab = new ObjectListTab(objectName, listType, m_workspace);
    m_workspace->addTab(tab, objectName);
    tab->refresh();
}

void TabManager::executeCurrentQuery()
{
    auto* tab = qobject_cast<QueryTab*>(m_workspace->currentWidget());
    if (tab)
        tab->executeQuery();
}
