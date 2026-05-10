#pragma once
#include "ui/workspace/AbstractTab.h"
#include <QTableView>
#include <QStringList>

class ObjectListTab : public AbstractTab
{
    Q_OBJECT

public:
    explicit ObjectListTab(const QString& objectName, const QString& listType,
                            QWidget* parent = nullptr);

    QString tabTitle() const override { return m_objectName; }

public slots:
    void refresh() override;

private:
    QString m_objectName;
    QString m_listType;
    QTableView* m_tableView = nullptr;
};
