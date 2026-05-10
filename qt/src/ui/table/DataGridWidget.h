#pragma once
#include <QWidget>
#include <QTableView>

class EditableTableModel;
class DataToolbar;

class DataGridWidget : public QWidget
{
    Q_OBJECT

public:
    explicit DataGridWidget(QWidget* parent = nullptr);

    void setModel(EditableTableModel* model);
    EditableTableModel* editableModel() const;

    void setHasMore(bool hasMore);
    void setCurrentPage(int page);
    int currentPage() const;
    int currentRow() const;

signals:
    void pageChanged(int page);
    void saveRequested();
    void revertRequested();
    void addRowRequested();
    void deleteRowRequested();
    void refreshRequested();

private:
    QTableView* m_tableView = nullptr;
    DataToolbar* m_toolbar = nullptr;
    EditableTableModel* m_model = nullptr;
    bool m_hasMore = false;
    int m_currentPage = 0;
};
