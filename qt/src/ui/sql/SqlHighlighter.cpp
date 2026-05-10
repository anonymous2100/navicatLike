#include "SqlHighlighter.h"

SqlHighlighter::SqlHighlighter(QTextDocument* parent)
    : QSyntaxHighlighter(parent)
{
    // Keyword format
    m_keywordFormat.setForeground(QColor("#0000FF"));
    m_keywordFormat.setFontWeight(QFont::Bold);

    // Number format
    m_numberFormat.setForeground(QColor("#098658"));

    // String format
    m_stringFormat.setForeground(QColor("#A31515"));

    // Comment format
    m_commentFormat.setForeground(QColor("#008000"));
    m_commentFormat.setFontItalic(true);

    // Function format
    m_functionFormat.setForeground(QColor("#795E26"));

    addKeywordRules();
    addStringRules();
    addNumberRules();
    addCommentRules();
}

void SqlHighlighter::addKeywordRules()
{
    QStringList keywords = {
        // DML
        "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
        "DELETE", "MERGE", "REPLACE",
        // DDL
        "CREATE", "ALTER", "DROP", "TABLE", "INDEX", "VIEW", "TRIGGER",
        "PROCEDURE", "FUNCTION", "DATABASE", "SCHEMA", "SEQUENCE",
        // Clauses
        "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "CROSS", "FULL",
        "ON", "AS", "AND", "OR", "NOT", "IN", "IS", "NULL",
        "LIKE", "BETWEEN", "EXISTS", "ALL", "ANY", "SOME",
        "GROUP", "BY", "ORDER", "ASC", "DESC", "HAVING",
        "LIMIT", "OFFSET", "FETCH", "NEXT", "ROWS", "ONLY",
        "UNION", "INTERSECT", "EXCEPT",
        // Constraints
        "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "UNIQUE", "CHECK",
        "DEFAULT", "AUTO_INCREMENT", "SERIAL", "IDENTITY",
        // Types
        "INT", "INTEGER", "BIGINT", "SMALLINT", "TINYINT",
        "VARCHAR", "CHAR", "TEXT", "BOOLEAN", "BOOL",
        "FLOAT", "DOUBLE", "DECIMAL", "NUMERIC", "REAL",
        "DATE", "TIME", "TIMESTAMP", "DATETIME", "INTERVAL",
        "BYTEA", "BLOB", "JSON", "JSONB", "XML",
        "ENUM", "SET",
        // Transaction
        "BEGIN", "COMMIT", "ROLLBACK", "SAVEPOINT", "TRANSACTION",
        // Other
        "CASE", "WHEN", "THEN", "ELSE", "END", "COALESCE",
        "IF", "ELSE", "ENDIF",
        "EXPLAIN", "ANALYZE", "WITH", "RECURSIVE",
        "RETURNING", "CASCADE", "RESTRICT", "DISTINCT",
    };

    for (const auto& kw : keywords) {
        HighlightRule rule;
        rule.pattern = QRegularExpression(
            "\\b" + kw + "\\b",
            QRegularExpression::CaseInsensitiveOption);
        rule.format = m_keywordFormat;
        m_rules.append(rule);
    }

    // SQL functions
    QStringList functions = {
        "COUNT", "SUM", "AVG", "MIN", "MAX", "COALESCE", "NULLIF",
        "NOW", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP",
        "EXTRACT", "CAST", "CONVERT", "SUBSTRING", "TRIM", "UPPER", "LOWER",
        "LENGTH", "REPLACE", "CONCAT", "GROUP_CONCAT", "STRING_AGG",
        "ROW_NUMBER", "RANK", "DENSE_RANK", "LAG", "LEAD",
        "FIRST_VALUE", "LAST_VALUE", "NTH_VALUE",
    };
    for (const auto& fn : functions) {
        HighlightRule rule;
        rule.pattern = QRegularExpression(
            "\\b" + fn + "\\s*\\(",
            QRegularExpression::CaseInsensitiveOption);
        rule.format = m_functionFormat;
        m_rules.append(rule);
    }
}

void SqlHighlighter::addStringRules()
{
    // Single-quoted strings
    {
        HighlightRule rule;
        rule.pattern = QRegularExpression("'[^']*'");
        rule.format = m_stringFormat;
        m_rules.append(rule);
    }
}

void SqlHighlighter::addNumberRules()
{
    {
        HighlightRule rule;
        rule.pattern = QRegularExpression("\\b\\d+(\\.\\d+)?\\b");
        rule.format = m_numberFormat;
        m_rules.append(rule);
    }
}

void SqlHighlighter::addCommentRules()
{
    // Single-line comments
    {
        HighlightRule rule;
        rule.pattern = QRegularExpression("--[^\n]*");
        rule.format = m_commentFormat;
        m_rules.append(rule);
    }

    // Block comments
    {
        HighlightRule rule;
        rule.pattern = QRegularExpression("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/");
        rule.format = m_commentFormat;
        m_rules.append(rule);
    }
}

void SqlHighlighter::highlightBlock(const QString& text)
{
    for (const auto& rule : m_rules) {
        auto it = rule.pattern.globalMatch(text);
        while (it.hasNext()) {
            auto match = it.next();
            setFormat(static_cast<int>(match.capturedStart()),
                      static_cast<int>(match.capturedLength()),
                      rule.format);
        }
    }
}
