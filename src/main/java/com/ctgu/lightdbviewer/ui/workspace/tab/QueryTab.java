package com.ctgu.lightdbviewer.ui.workspace.tab;

import com.ctgu.lightdbviewer.service.QueryService;
import com.ctgu.lightdbviewer.ui.common.ConfirmDialog;
import com.ctgu.lightdbviewer.ui.sql.SqlEditorPanel;
import com.ctgu.lightdbviewer.ui.sql.SqlResultPanel;
import com.ctgu.lightdbviewer.ui.sql.SqlToolbar;
import com.ctgu.lightdbviewer.ui.status.StatusBarPanel;
import com.ctgu.lightdbviewer.util.ErrorHandler;
import com.ctgu.lightdbviewer.util.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.CancellationException;

/**
 * @author lihuahui
 * @version 1.0
 * @description:
 * @date 2026-04-23 18:30
 */
public class QueryTab extends AbstractTab
{
  private final String baseTitle;
  private final StatusBarPanel status;
  private final SqlEditorPanel editorPanel;
  private final SqlResultPanel resultPanel;
  private final SqlToolbar toolbar;

  private static final int HISTORY_LIMIT = 15;
  private static final int PAGE_SIZE = 200;
  private static final int MAX_ROWS = 1000;
  private boolean dirty = false;
  private SwingWorker<QueryService.QueryResult, Void> currentWorker;
  private volatile Statement runningStatement;
  private final Deque<String> sqlHistory = new ArrayDeque<>();
  private int currentPage = 0;
  private String lastPagedSql;

  public QueryTab(String baseTitle, String initialSql, StatusBarPanel status)
  {
    this.baseTitle = baseTitle;
    this.status = status;
    setLayout(new BorderLayout());
    editorPanel = new SqlEditorPanel();
    resultPanel = new SqlResultPanel();
    toolbar = new SqlToolbar(this::runSql, this::stopSql, this::clearResults, this::onSave, this::onBeautifySql);
    toolbar.setOnAskAi(editorPanel.getAiToggleBtn()::doClick);
    toolbar.setOnExplain(this::runExplain);
    toolbar.addAiToggleButton(editorPanel.getAiToggleBtn());
    JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorPanel, resultPanel);
    split.setResizeWeight(0.45);
    split.setDividerLocation(260);
    add(toolbar, BorderLayout.NORTH);
    add(split, BorderLayout.CENTER);
    // 将当前配置的背景色应用到新创建的组件
    ThemeManager.applyBgColorToComponent(this);
    resultPanel.setPageActions(this::loadPrevPage, this::loadNextPage);
    if(initialSql != null && !initialSql.isBlank())
    {
      editorPanel.setSql(initialSql);
      dirty = true;
      updateTabTitle();
    }
    editorPanel.addSqlChangeListener(this::markDirty);
    // 光标位置 → 状态栏行/列显示
    editorPanel.addCaretPositionListener((line, col) -> status.setEditorPosition(line, col));
    // 异步加载数据库/Schema 列表
    toolbar.refreshConnectionContext();
  }

  public void runCurrentSql()
  {
    if(currentWorker != null && !currentWorker.isDone())
    {
      status.setMessage("已有 SQL 正在执行中");
      return;
    }
    String raw = editorPanel.getSelectedSqlOrAll().trim();
    String sql = normalizeSql(raw);
    if(sql.isEmpty())
    {
      status.setMessage("SQL 为空");
      toolbar.setIdle();
      return;
    }
    if(isDangerousSql(sql))
    {
      boolean ok =
          ConfirmDialog.confirmDangerousTwice(null, "高危 SQL", "检测到高危 SQL 语句：\n\n" + sql + "\n\n此操作可能导致数据不可逆丢失。",
              "EXECUTE");
      if(!ok)
      {
        return;
      }
    }
    currentPage = 0;
    lastPagedSql = sql;
    executeSqlPage(sql, true);
  }

  private void executeSqlPage(String sql, boolean addToHistory)
  {
    toolbar.setExecuting(true);
    status.setMessage("正在执行 SQL...");
    status.clearElapsed();

    currentWorker = new SwingWorker<>()
    {
      @Override
      protected QueryService.QueryResult doInBackground() throws Exception
      {
        return QueryService.executePaged(sql, currentPage, PAGE_SIZE, MAX_ROWS, stmt -> runningStatement = stmt);
      }

      @Override
      protected void done()
      {
        runningStatement = null;
        toolbar.setIdle();
        try
        {
          if(isCancelled())
          {
            resultPanel.showMessage("执行已取消");
            status.setMessage("已取消");
            status.clearElapsed();
            return;
          }
          QueryService.QueryResult result = get();
          status.setElapsedMs(result.timeMs);
          if(result.hasResultSet)
          {
            resultPanel.showResultSet(result.tableModel, result.pageIndex, result.pageSize, result.hasNextPage, result.truncated, MAX_ROWS);
            status.setMessage("查询返回结果集");
          }
          else
          {
            resultPanel.showUpdateCount(result.updateCount, result.timeMs);
            status.setMessage("执行成功，影响 " + result.updateCount + " 行");
          }
          dirty = false;
          updateTabTitle();
          if(addToHistory)
          {
            addHistory(sql);
          }
        }
        catch(CancellationException e)
        {
          resultPanel.showMessage("执行已取消");
          status.setMessage("已取消");
          status.clearElapsed();
        }
        catch(Exception e)
        {
          resultPanel.showError("执行失败", e);
          status.setMessage("执行失败: " + e.getMessage());
          status.clearElapsed();
          ErrorHandler.handleSqlError((java.awt.Frame)SwingUtilities.getWindowAncestor(QueryTab.this), sql, e.getMessage());
        }
      }
    };
    currentWorker.execute();
  }

  public void cancelCurrentSql()
  {
    if(currentWorker == null || currentWorker.isDone())
    {
      return;
    }
    QueryService.cancel(runningStatement);
    currentWorker.cancel(true);
    resultPanel.showMessage("执行已取消");
    status.setMessage("已取消");
    status.clearElapsed();
    toolbar.setIdle();
  }

  private void clearResults()
  {
    resultPanel.clear();
    status.setMessage("结果已清空");
    status.clearElapsed();
    currentPage = 0;
    lastPagedSql = null;
  }

  private void onSave()
  {
    // 简单保存：将当前 SQL 标记为已保存（未来可接入文件系统）
    dirty = false;
    updateTabTitle();
    status.setMessage("查询已保存");
  }

  private void onBeautifySql()
  {
    String sql = editorPanel.getSqlText();
    if(sql == null || sql.isBlank())
    {
      return;
    }
    editorPanel.setSql(beautify(sql));
    status.setMessage("SQL 已格式化");
  }

  /**
   * 简单 SQL 美化：关键字大写 + 换行对齐
   */
  private static String beautify(String sql)
  {
    // 将主要关键词前加换行
    String[] keywords =
        { "SELECT", "FROM", "WHERE", "JOIN", "LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "OUTER JOIN", "CROSS JOIN", "ON", "GROUP BY",
            "ORDER BY", "HAVING", "LIMIT", "OFFSET", "UNION", "UNION ALL", "INSERT INTO", "VALUES", "UPDATE", "SET", "DELETE FROM",
            "CREATE TABLE", "ALTER TABLE", "DROP TABLE", "AND", "OR" };
    String result = sql.trim();
    // 大写关键词并换行
    for(String kw : keywords)
    {
      result = result.replaceAll("(?i)\\b" + kw.replace(" ", "\\\\s+") + "\\b", "\n" + kw);
    }
    // 整理多余空行
    result = result.replaceAll("\\n{2,}", "\n").trim();
    // 缩进非第一行
    StringBuilder sb = new StringBuilder();
    String[] lines = result.split("\n");
    for(int i = 0; i < lines.length; i++)
    {
      String line = lines[i].trim();
      if(line.isEmpty())
      {
        continue;
      }
      if(i == 0)
      {
        sb.append(line);
      }
      else
      {
        sb.append("\n  ").append(line);
      }
    }
    return sb.toString();
  }

  private void markDirty()
  {
    if(!dirty)
    {
      dirty = true;
      updateTabTitle();
    }
  }

  private void updateTabTitle()
  {
    Container parent = getParent();
    if(!(parent instanceof JTabbedPane tabs))
    {
      return;
    }
    int idx = tabs.indexOfComponent(this);
    if(idx >= 0)
    {
      tabs.setTitleAt(idx, baseTitle + (dirty ? " *" : ""));
    }
  }

  @Override
  public boolean isDirty()
  {
    return dirty;
  }

  private void runSql()
  {
    runCurrentSql();
  }

  private void runExplain()
  {
    if(currentWorker != null && !currentWorker.isDone())
    {
      status.setMessage("已有 SQL 正在执行中");
      return;
    }
    String sql = editorPanel.getSelectedSqlOrAll().trim();
    sql = normalizeSql(sql);
    if(sql.isEmpty())
    {
      status.setMessage("SQL 为空");
      return;
    }
    if(!sql.toUpperCase(Locale.ROOT).startsWith("EXPLAIN "))
    {
      sql = "EXPLAIN " + sql;
    }
    currentPage = 0;
    lastPagedSql = sql;
    executeSqlPage(sql, false);
  }

  private void stopSql()
  {
    cancelCurrentSql();
  }

  private void loadPrevPage()
  {
    if(lastPagedSql == null || currentPage <= 0)
    {
      return;
    }
    currentPage--;
    executeSqlPage(lastPagedSql, false);
  }

  private void loadNextPage()
  {
    if(lastPagedSql == null)
    {
      return;
    }
    currentPage++;
    executeSqlPage(lastPagedSql, false);
  }

  private void addHistory(String sql)
  {
    sqlHistory.remove(sql);
    sqlHistory.addFirst(sql);
    while(sqlHistory.size() > HISTORY_LIMIT)
    {
      sqlHistory.removeLast();
    }
  }

  private String normalizeSql(String sql)
  {
    String normalized = sql.strip();
    while(normalized.endsWith(";"))
    {
      normalized = normalized.substring(0, normalized.length() - 1).strip();
    }
    return normalized;
  }

  private boolean isDangerousSql(String sql)
  {
    String lower = sql.toLowerCase(Locale.ROOT);
    return lower.startsWith("truncate ") || lower.startsWith("drop ");
  }
}