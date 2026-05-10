#pragma once
#include <QSyntaxHighlighter>
#include <QRegularExpression>

class SqlHighlighter : public QSyntaxHighlighter
{
    Q_OBJECT

public:
    explicit SqlHighlighter(QTextDocument* parent = nullptr);

protected:
    void highlightBlock(const QString& text) override;

private:
    struct HighlightRule
    {
        QRegularExpression pattern;
        QTextCharFormat format;
    };
    QVector<HighlightRule> m_rules;

    void addKeywordRules();
    void addStringRules();
    void addNumberRules();
    void addCommentRules();

    QTextCharFormat m_keywordFormat;
    QTextCharFormat m_numberFormat;
    QTextCharFormat m_stringFormat;
    QTextCharFormat m_commentFormat;
    QTextCharFormat m_functionFormat;
};
