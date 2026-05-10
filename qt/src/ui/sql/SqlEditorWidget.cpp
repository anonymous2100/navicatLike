#include "SqlEditorWidget.h"
#include "SqlHighlighter.h"
#include "SqlToolbar.h"
#include <QVBoxLayout>
#include <QFont>
#include <QTextCursor>

SqlEditorWidget::SqlEditorWidget(QWidget* parent)
    : QWidget(parent)
{
    auto* layout = new QVBoxLayout(this);
    layout->setContentsMargins(0, 0, 0, 0);

    auto* toolbar = new SqlToolbar(this);
    layout->addWidget(toolbar);

    m_editor = new QPlainTextEdit(this);
    m_editor->setFont(QFont("Consolas", 14));
    m_editor->setTabStopDistance(32);
    m_editor->setLineWrapMode(QPlainTextEdit::NoWrap);
    m_editor->setPlaceholderText(tr("Enter SQL here..."));
    layout->addWidget(m_editor);

    m_highlighter = new SqlHighlighter(m_editor->document());

    connect(toolbar, &SqlToolbar::execute, this, &SqlEditorWidget::executeRequested);
    connect(m_editor, &QPlainTextEdit::textChanged, this, &SqlEditorWidget::textChanged);
}

void SqlEditorWidget::setPlainText(const QString& text)
{
    m_editor->setPlainText(text);
}

QString SqlEditorWidget::selectedOrAllText() const
{
    QTextCursor cursor = m_editor->textCursor();
    if (cursor.hasSelection())
        return cursor.selectedText().trimmed();
    return m_editor->toPlainText().trimmed();
}

QString SqlEditorWidget::toPlainText() const
{
    return m_editor->toPlainText();
}
