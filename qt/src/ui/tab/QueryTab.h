#pragma once
#include "ui/workspace/AbstractTab.h"

class SqlEditorWidget;
class SqlResultPanel;
class QSplitter;

class QueryTab : public AbstractTab
{
    Q_OBJECT

public:
    explicit QueryTab(QWidget* parent = nullptr);

    QString tabTitle() const override { return tr("Query"); }
    void setSql(const QString& sql);

public slots:
    void refresh() override;
    void executeQuery();

private:
    SqlEditorWidget* m_editor = nullptr;
    SqlResultPanel* m_resultPanel = nullptr;
    QSplitter* m_splitter = nullptr;
};
