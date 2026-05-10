#pragma once
#include <QToolBar>

class DataToolbar : public QToolBar
{
    Q_OBJECT

public:
    explicit DataToolbar(QWidget* parent = nullptr);

    void setPage(int page);
    void setHasNext(bool hasNext);

signals:
    void prevPage();
    void nextPage();
    void save();
    void revert();
    void addRow();
    void deleteRow();
    void refresh();

private:
    QAction* m_pageLabel = nullptr;
    QAction* m_nextAction = nullptr;
};
