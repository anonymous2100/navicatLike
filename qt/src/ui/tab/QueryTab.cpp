#include "QueryTab.h"
#include "ui/sql/SqlEditorWidget.h"
#include "ui/sql/SqlResultPanel.h"
#include "service/QueryService.h"
#include "pool/ConnectionPool.h"
#include <QSplitter>
#include <QVBoxLayout>
#include <QMessageBox>
#include "common/Logging.h"

QueryTab::QueryTab(QWidget* parent)
    : AbstractTab(parent)
{
    auto* layout = new QVBoxLayout(this);
    layout->setContentsMargins(0, 0, 0, 0);

    m_splitter = new QSplitter(Qt::Vertical, this);

    m_editor = new SqlEditorWidget(m_splitter);
    m_resultPanel = new SqlResultPanel(m_splitter);

    m_splitter->addWidget(m_editor);
    m_splitter->addWidget(m_resultPanel);
    m_splitter->setStretchFactor(0, 1);
    m_splitter->setStretchFactor(1, 1);
    m_splitter->setSizes({400, 400});

    layout->addWidget(m_splitter);

    connect(m_editor, &SqlEditorWidget::executeRequested, this, &QueryTab::executeQuery);
}

void QueryTab::setSql(const QString& sql)
{
    m_editor->setPlainText(sql);
}

void QueryTab::refresh()
{
    executeQuery();
}

void QueryTab::executeQuery()
{
    QString sql = m_editor->selectedOrAllText();
    if (sql.trimmed().isEmpty()) return;

    try {
        QSqlDatabase db = ConnectionPool::instance().getConnection();
        QueryResult result = QueryService::execute(db, sql);

        if (result.hasResultSet) {
            m_resultPanel->showResult(result);
        } else {
            m_resultPanel->showUpdateCount(result.updateCount, result.elapsedMs);
        }
        qCDebug(lcLightDB) << "Query executed successfully";
    } catch (const std::exception& e) {
        m_resultPanel->showError(e.what());
    }
}
