#pragma once
#include <QWidget>
#include <QTableView>
#include <QLabel>
#include "service/QueryService.h"

class SqlResultPanel : public QWidget
{
    Q_OBJECT

public:
    explicit SqlResultPanel(QWidget* parent = nullptr);

    void showResult(const QueryResult& result);
    void showUpdateCount(int count, qint64 elapsedMs);
    void showError(const QString& error);

private:
    QTableView* m_tableView = nullptr;
    QLabel* m_statusLabel = nullptr;
    QLabel* m_errorLabel = nullptr;
};
