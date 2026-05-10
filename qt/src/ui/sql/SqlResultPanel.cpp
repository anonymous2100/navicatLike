#include "SqlResultPanel.h"
#include "ui/table/PagedTableModel.h"
#include <QVBoxLayout>
#include <QHeaderView>

SqlResultPanel::SqlResultPanel(QWidget* parent)
    : QWidget(parent)
{
    auto* layout = new QVBoxLayout(this);
    layout->setContentsMargins(0, 0, 0, 0);

    m_statusLabel = new QLabel(tr("Ready"), this);
    m_statusLabel->setStyleSheet("padding: 4px; background-color: #F0F0F0;");

    m_errorLabel = new QLabel(this);
    m_errorLabel->setStyleSheet("padding: 4px; color: red; font-weight: bold;");
    m_errorLabel->hide();

    m_tableView = new QTableView(this);
    m_tableView->setSelectionBehavior(QAbstractItemView::SelectRows);
    m_tableView->horizontalHeader()->setStretchLastSection(true);
    m_tableView->setEditTriggers(QAbstractItemView::NoEditTriggers);

    layout->addWidget(m_statusLabel);
    layout->addWidget(m_errorLabel);
    layout->addWidget(m_tableView);
}

void SqlResultPanel::showResult(const QueryResult& result)
{
    m_errorLabel->hide();

    auto* model = new PagedTableModel(this);
    model->setData(result.columnNames, result.rows);
    m_tableView->setModel(model);
    m_tableView->resizeColumnsToContents();

    QString msg = tr("%1 rows, %2 ms").arg(result.rows.size()).arg(result.elapsedMs);
    if (result.truncated) msg += tr(" (truncated)");
    if (result.hasNextPage) msg += tr(" [more pages available]");
    m_statusLabel->setText(msg);
}

void SqlResultPanel::showUpdateCount(int count, qint64 elapsedMs)
{
    m_errorLabel->hide();
    m_tableView->setModel(new PagedTableModel(this));

    m_statusLabel->setText(
        tr("Query OK, %1 rows affected (%2 ms)").arg(count).arg(elapsedMs));
}

void SqlResultPanel::showError(const QString& error)
{
    m_errorLabel->setText(error);
    m_errorLabel->show();
    m_statusLabel->setText(tr("Error"));
}
