#pragma once
#include <QWidget>
#include <QPlainTextEdit>

class SqlHighlighter;

class SqlEditorWidget : public QWidget
{
    Q_OBJECT

public:
    explicit SqlEditorWidget(QWidget* parent = nullptr);

    void setPlainText(const QString& text);
    QString selectedOrAllText() const;
    QString toPlainText() const;

signals:
    void executeRequested();
    void textChanged();

private:
    QPlainTextEdit* m_editor = nullptr;
    SqlHighlighter* m_highlighter = nullptr;
};
