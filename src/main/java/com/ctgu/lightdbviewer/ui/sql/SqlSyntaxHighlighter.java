package com.ctgu.lightdbviewer.ui.sql;

import javax.swing.text.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 语法高亮引擎 基于正则分词，为 StyledDocument 设置字符属性 * <p>
 * 支持token 类型 * - 关键字（蓝色加粗 * - 字符串（绿色 * - 数字（暗橙色 * - 单行 / 多行注释（灰色斜体）
 */
/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public final class SqlSyntaxHighlighter
{
  // ---- 颜色常量 ----
  private static final Color COLOR_KEYWORD = new Color(0x0033B3);
  private static final Color COLOR_STRING = new Color(0x067D17);
  private static final Color COLOR_NUMBER = new Color(0x1750EB);
  private static final Color COLOR_COMMENT = new Color(0x8C8C8C);

  // ---- 属性集（缓存避免重复分配） ----
  private static final SimpleAttributeSet ATTR_DEFAULT;
  private static final SimpleAttributeSet ATTR_KEYWORD;
  private static final SimpleAttributeSet ATTR_STRING;
  private static final SimpleAttributeSet ATTR_NUMBER;
  private static final SimpleAttributeSet ATTR_COMMENT;

  static
  {
    ATTR_DEFAULT = new SimpleAttributeSet();
    StyleConstants.setForeground(ATTR_DEFAULT, Color.BLACK);

    ATTR_KEYWORD = new SimpleAttributeSet();
    StyleConstants.setForeground(ATTR_KEYWORD, COLOR_KEYWORD);
    StyleConstants.setBold(ATTR_KEYWORD, true);

    ATTR_STRING = new SimpleAttributeSet();
    StyleConstants.setForeground(ATTR_STRING, COLOR_STRING);

    ATTR_NUMBER = new SimpleAttributeSet();
    StyleConstants.setForeground(ATTR_NUMBER, COLOR_NUMBER);

    ATTR_COMMENT = new SimpleAttributeSet();
    StyleConstants.setForeground(ATTR_COMMENT, COLOR_COMMENT);
    StyleConstants.setItalic(ATTR_COMMENT, true);
  }

  // ---- SQL 关键词（按长度降序排列，保证最长匹配优先） ----
  private static final String[] KEYWORDS =
      { "AUTO_INCREMENT", "IF NOT EXISTS", "OR REPLACE", "NO ACTION", "SET NULL", "INNER JOIN", "LEFT JOIN", "RIGHT JOIN", "OUTER JOIN",
          "CROSS JOIN", "FULL JOIN", "NATURAL JOIN", "GROUP BY", "ORDER BY", "PARTITION BY", "UNION ALL", "UNION DISTINCT", "INSERT INTO",
          "DELETE FROM", "DROP TABLE", "DROP INDEX", "DROP VIEW", "CREATE TABLE", "CREATE INDEX", "CREATE VIEW", "CREATE SEQUENCE",
          "CREATE SCHEMA", "ALTER TABLE", "TRUNCATE TABLE", "PRECEDING", "FOLLOWING", "UNBOUNDED", "SELECT", "FROM", "WHERE", "AND", "OR",
          "NOT", "IN", "IS", "NULL", "LIKE", "BETWEEN", "EXISTS", "ILIKE", "AS", "ON", "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "CROSS",
          "FULL", "NATURAL", "USING", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE", "CREATE", "ALTER", "DROP", "TABLE", "INDEX",
          "VIEW", "SEQUENCE", "TRIGGER", "FUNCTION", "PROCEDURE", "SCHEMA", "DATABASE", "GROUP", "BY", "ORDER", "ASC", "DESC", "HAVING",
          "LIMIT", "OFFSET", "UNION", "ALL", "DISTINCT", "CASE", "WHEN", "THEN", "ELSE", "END", "BEGIN", "COMMIT", "ROLLBACK",
          "TRANSACTION", "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "CONSTRAINT", "DEFAULT", "CHECK", "UNIQUE", "IF", "FOR", "DO",
          "DECLARE", "RETURN", "EXCEPTION", "RAISE", "TRUE", "FALSE", "EXPLAIN", "ANALYZE", "EXECUTE", "CALL", "GRANT", "REVOKE", "ROLE",
          "USER", "WITH", "RECURSIVE", "WINDOW", "OVER", "PARTITION", "ROW", "ROWS", "RANGE", "CAST", "COALESCE", "NULLIF", "COUNT", "SUM",
          "AVG", "MIN", "MAX", "TRUNCATE", "REPLACE", "MERGE", "TEMPORARY", "TEMP", "CASCADE", "RESTRICT", "SERIAL", "BIGSERIAL",
          "SMALLSERIAL", "VARCHAR", "CHAR", "TEXT", "INTEGER", "INT", "BIGINT", "SMALLINT", "TINYINT", "BYTEA", "BOOLEAN", "BOOL", "FLOAT",
          "DOUBLE", "REAL", "DECIMAL", "NUMERIC", "DATE", "TIME", "TIMESTAMP", "INTERVAL", "JSON", "JSONB", "ENUM", "NOT NULL",
          "SET DEFAULT" };

  // ---- 正则 ----
  // 注释、字符串、数字、关键词依次匹配（顺序重要，注释/字符串优先于关键词）
  private static final Pattern SQL_PATTERN = Pattern.compile(
      "(?<COMMENT>--[^\n]*|/\\*[\\s\\S]*?\\*/)" + "|(?<STRING>'[^']*'|\"[^\"]*\")" + "|(?<NUMBER>\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b)"
          + "|(?<KEYWORD>\\b(?:" + joinKeywords(KEYWORDS) + ")\\b)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

  private SqlSyntaxHighlighter()
  {
  }

  /**
   * 将关键词数组合并为正则分支，较长的词排在前面
   */
  private static String joinKeywords(String[] kw)
  {
    // 按长度降序排列，保证最长匹配优先（"NOT NULL" 优先于 "NOT"）
    java.util.Arrays.sort(kw, (a, b) -> b.length() - a.length());
    StringBuilder sb = new StringBuilder();
    for(int i = 0; i < kw.length; i++)
    {
      if(i > 0) {
        sb.append('|');
      }
      sb.append(Pattern.quote(kw[i]));
    }
    return sb.toString();
  }

  /**
   * 对文档全文执行语法高亮。
   * <p>
   * 建议在 EDT 上调用。
   */
  public static void highlight(StyledDocument doc)
  {
    int len = doc.getLength();
    if(len == 0)
    {
      return;
    }
    String text;
    try
    {
      text = doc.getText(0, len);
    }
    catch(BadLocationException e)
    {
      return;
    }
    // 1. 重置全文为默认样式
    doc.setCharacterAttributes(0, len, ATTR_DEFAULT, true);
    // 2. 按 token 着色
    Matcher m = SQL_PATTERN.matcher(text);
    while(m.find())
    {
      SimpleAttributeSet attrs = null;
      if(m.group("KEYWORD") != null)
      {
        attrs = ATTR_KEYWORD;
      }
      else if(m.group("STRING") != null)
      {
        attrs = ATTR_STRING;
      }
      else if(m.group("NUMBER") != null)
      {
        attrs = ATTR_NUMBER;
      }
      else if(m.group("COMMENT") != null)
      {
        attrs = ATTR_COMMENT;
      }
      if(attrs != null)
      {
        doc.setCharacterAttributes(m.start(), m.end() - m.start(), attrs, true);
      }
    }
  }
}


