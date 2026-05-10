#include "WorkspaceTabWidget.h"
#include <QMenu>
#include <QAction>
#include <QTabBar>

WorkspaceTabWidget::WorkspaceTabWidget(QWidget* parent)
    : QTabWidget(parent)
{
    setTabsClosable(true);
    setMovable(true);
    setDocumentMode(true);
    setContextMenuPolicy(Qt::CustomContextMenu);

    connect(this, &QTabWidget::tabCloseRequested, this, &WorkspaceTabWidget::removeTab);
    connect(this, &QTabWidget::customContextMenuRequested, this, [this](const QPoint& pos) {
        int idx = tabBar()->tabAt(pos);
        if (idx < 0) return;

        QMenu menu(this);
        menu.addAction(tr("Close Tab"), this, [this, idx]() { removeTab(idx); });
        menu.addAction(tr("Close Others"), this, [this, idx]() { closeOtherTabs(idx); });
        menu.addAction(tr("Close All"), this, [this]() {
            while (count() > 0) removeTab(0);
        });
        menu.exec(tabBar()->mapToGlobal(pos));
    });
}

int WorkspaceTabWidget::addTab(AbstractTab* tab, const QString& title)
{
    int idx = QTabWidget::addTab(tab, title);
    setCurrentIndex(idx);

    connect(tab, &AbstractTab::titleChanged, this, &WorkspaceTabWidget::onTabTitleChanged);
    connect(tab, &AbstractTab::dirtyChanged, this, &WorkspaceTabWidget::onTabDirtyChanged);

    emit tabCountChanged(count());
    return idx;
}

void WorkspaceTabWidget::removeTab(int index)
{
    if (index < 0 || index >= count()) return;

    QWidget* w = widget(index);
    bool canClose = true;
    if (auto* tab = qobject_cast<AbstractTab*>(w)) {
        if (tab->isDirty()) {
            // TODO: ask user to save
        }
    }

    if (canClose) {
        QTabWidget::removeTab(index);
        w->deleteLater();
        emit tabCountChanged(count());
    }
}

void WorkspaceTabWidget::closeCurrentTab()
{
    removeTab(currentIndex());
}

void WorkspaceTabWidget::closeOtherTabs(int keepIndex)
{
    for (int i = count() - 1; i >= 0; --i) {
        if (i != keepIndex)
            removeTab(i);
    }
}

void WorkspaceTabWidget::onTabTitleChanged(const QString& title)
{
    auto* tab = qobject_cast<AbstractTab*>(sender());
    if (!tab) return;
    int idx = indexOf(tab);
    if (idx >= 0) updateTabLabel(idx);
}

void WorkspaceTabWidget::onTabDirtyChanged(bool dirty)
{
    auto* tab = qobject_cast<AbstractTab*>(sender());
    if (!tab) return;
    int idx = indexOf(tab);
    if (idx >= 0) updateTabLabel(idx);
}

void WorkspaceTabWidget::updateTabLabel(int index)
{
    auto* tab = qobject_cast<AbstractTab*>(widget(index));
    if (!tab) return;
    QString title = tab->isDirty() ? tab->tabTitle() + " *" : tab->tabTitle();
    setTabText(index, title);
}
