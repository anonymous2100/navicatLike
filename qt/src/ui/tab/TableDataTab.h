#pragma once
#include "ui/workspace/AbstractTab.h"
#include <QString>
#include <QVariant>
#include <QVector>

class QTableView;
class QLabel;
class QToolBar;
class EditableTableModel;
class PagedTableModel;
class DataGridWidget;

class TableDataTab : public AbstractTab
{
    Q_OBJECT

public:
    explicit TableDataTab(const QString& tableName, QWidget* parent = nullptr);

    QString tabTitle() const override { return m_tableName; }

public slots:
    void refresh() override;

private slots:
    void onPageChanged(int page);
    void onSave();
    void onRevert();
    void onAddRow();
    void onDeleteRow();
    void onRefresh();

private:
    void loadPage(int page);

    QString m_tableName;
    DataGridWidget* m_dataGrid = nullptr;
    int m_currentPage = 0;
    int m_pageSize = 200;

    QVector<QVector<QVariant>> m_originalData;
};
