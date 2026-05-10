#pragma once
#include "ui/workspace/AbstractTab.h"
#include "common/Types.h"
#include <QTabWidget>
#include <QTableView>
#include <QLabel>

class DesignTableTab : public AbstractTab
{
    Q_OBJECT

public:
    explicit DesignTableTab(const QString& tableName, QWidget* parent = nullptr);

    QString tabTitle() const override { return m_tableName; }

public slots:
    void refresh() override;

private:
    void loadColumns();
    void loadIndexes();
    void loadForeignKeys();

    QString m_tableName;
    QTabWidget* m_tabs = nullptr;
    QTableView* m_columnsTable = nullptr;
    QTableView* m_indexesTable = nullptr;
    QTableView* m_fksTable = nullptr;
    std::vector<ColumnInfo> m_columns;
};
