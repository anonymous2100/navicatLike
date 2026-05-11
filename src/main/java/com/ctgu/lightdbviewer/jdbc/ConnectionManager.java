package com.ctgu.lightdbviewer.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ConnectionManager - 连接管理器（支持连接池）
 * <p>
 * 改进：
 * - 使用 HikariCP 连接池提高性能
 * - 添加连接健康检查
 * - 添加连接超时控制
 * - 支持多连接管理
 * - 添加连接泄漏检测
 * - PostgreSQL 切换 database 时自动新建轻量连接池（无需重连）
 */
/**
 * @author lihuahui
 * @version 1.0
 * @description:
 */
public final class ConnectionManager
{
  private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

  private static final Map<String, HikariDataSource> dataSourceMap = new ConcurrentHashMap<>();
  private static String currentConnectionKey;
  private static String currentDatabaseName;
  private static boolean databaseSelected = false;

  /**
   * 连接凭据缓存：key = connectionKey，value = 创建该连接池所用的原始凭据
   * 用于 PostgreSQL 切换 database 时按需构造新 JDBC URL
   */
  private static final Map<String, ConnectionCredentials> credentialsMap = new ConcurrentHashMap<>();

  /**
   * 派生池追踪：记录每个"按需切库"连接池对应的"根连接 key"
   * 用于 close() 时一并回收同根的所有派生池
   */
  private static final Map<String, String> derivedPoolRoots = new ConcurrentHashMap<>();

  /**
   * 连接池配置
   */
  private static final int MAX_POOL_SIZE = 10;
  private static final long CONNECTION_TIMEOUT = 30000; // 30秒
  private static final long IDLE_TIMEOUT = 600000; // 10分钟
  private static final long MAX_LIFETIME = 1800000; // 30分钟
  private static final long LEAK_DETECTION_THRESHOLD = 60000; // 1分钟

  /**
   * 健康检查调度器
   */
  private static final ScheduledExecutorService healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "connection-health-check");
    t.setDaemon(true);
    return t;
  });

  static
  {
    // 显式加载 JDBC 驱动 — 确保 Launch4j 打包后（非标准 classloader）也能找到驱动
    try
    {
      Class.forName("com.mysql.cj.jdbc.Driver");
    }
    catch(ClassNotFoundException ignored)
    {
      // MySQL driver not on classpath
    }
    try
    {
      Class.forName("org.postgresql.Driver");
    }
    catch(ClassNotFoundException ignored)
    {
      // PostgreSQL driver not on classpath
    }
    // 启动健康检查
    healthCheckExecutor.scheduleAtFixedRate(ConnectionManager::performHealthCheck, 1, 1, TimeUnit.MINUTES);
  }

  private ConnectionManager()
  {
    // utility class
  }

  /**
   * 连接凭据：originalJdbcUrl 保存建立初始连接时的 URL
   * 用于后续 PostgreSQL 切库时重建 JDBC URL
   */
  private record ConnectionCredentials(String jdbcUrl, String user, String password)
  {
  }

  /**
   * 建立数据库连接
   *
   * @param jdbcUrl  JDBC URL
   * @param user     用户名
   * @param password 密码
   * @throws SQLException 连接失败
   */
  public static synchronized void connect(String jdbcUrl, String user, String password) throws SQLException
  {
    String connectionKey = buildConnectionKey(jdbcUrl, user);

    // 如果已存在相同连接，则切换到该连接
    if(dataSourceMap.containsKey(connectionKey))
    {
      currentConnectionKey = connectionKey;
      currentDatabaseName = extractDatabaseName(jdbcUrl);
      logger.info("切换到已有连接 {}", connectionKey);
      return;
    }
    try
    {
      // 创建新的数据源
      HikariDataSource dataSource = createDataSource(jdbcUrl, user, password);
      dataSourceMap.put(connectionKey, dataSource);
      credentialsMap.put(connectionKey, new ConnectionCredentials(jdbcUrl, user, password));
      currentConnectionKey = connectionKey;
      currentDatabaseName = extractDatabaseName(jdbcUrl);
      databaseSelected = true;
      logger.info("创建新连接 {}", connectionKey);
    }
    catch(Exception e)
    {
      logger.error("连接数据库失败 {}", jdbcUrl, e);
      throw new SQLException("连接数据库失败 " + e.getMessage(), e);
    }
  }

  /**
   * 切换到指定数据库
   * <ul>
   *   <li><b>MySQL</b>：调用 {@code setCatalog}，在同一连接池内切换（原地切库）</li>
   *   <li><b>PostgreSQL</b>：PostgreSQL 不支持在已有连接上切换 database，
   *       此方法会自动为目标 database 构建新 JDBC URL 并创建/复用一个轻量连接池，
   *       这是 Navicat 等工具对 PostgreSQL 多库浏览的标准做法。</li>
   * </ul>
   *
   * @param dbName 目标数据库名
   * @throws SQLException 切换失败
   */
  public static synchronized void switchDatabase(String dbName) throws SQLException
  {
    if(currentConnectionKey == null)
    {
      throw new IllegalStateException("No active connection. Call connect() first.");
    }
    // 如果目标数据库和当前数据库相同，直接返回
    if(dbName != null && dbName.equals(currentDatabaseName))
    {
      return;
    }
    HikariDataSource ds = dataSourceMap.get(currentConnectionKey);
    if(ds == null || ds.isClosed())
    {
      throw new SQLException("Connection pool is closed");
    }
    String oldKey = currentConnectionKey;
    String oldDbName = currentDatabaseName;
    try (Connection conn = ds.getConnection())
    {
      String product = conn.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
      if(product.contains("mysql"))
      {
        // ── MySQL：setCatalog 原地切换 ──────────────────────────
        conn.setCatalog(dbName);
        currentDatabaseName = dbName;
        databaseSelected = true;
        logger.info("MySQL 切换到数据库: {}", dbName);
      }
      else
      {
        // ── PostgreSQL / 其他：按需建立新连接池 ──────────────────
        // PostgreSQL 的不同 database 之间完全隔离，必须使用包含目标
        // database 的新 JDBC URL 建立独立连接，这是 PostgreSQL 的协议规定。
        ConnectionCredentials creds = credentialsMap.get(currentConnectionKey);
        if(creds == null)
        {
          // 当前连接是派生池时，向上查找根连接的凭据
          String rootKey = derivedPoolRoots.get(currentConnectionKey);
          if(rootKey != null)
          {
            creds = credentialsMap.get(rootKey);
          }
        }
        if(creds == null)
        {
          throw new SQLException("无法获取连接凭据以切换 PostgreSQL 数据库：" + dbName);
        }
        String newUrl = buildUrlWithDatabase(creds.jdbcUrl(), dbName);
        String newKey = buildConnectionKey(newUrl, creds.user());
        // 追踪根连接 key（用于 close() 时整组回收）
        String rootKey = derivedPoolRoots.getOrDefault(currentConnectionKey, currentConnectionKey);
        if(!dataSourceMap.containsKey(newKey))
        {
          // 创建轻量连接池：initializationFailTimeout=-1 表示懒初始化（不阻塞），
          // minimumIdle=0 表示无流量时不保留空闲连接
          HikariDataSource newDs = createDerivedDataSource(newUrl, creds.user(), creds.password());
          dataSourceMap.put(newKey, newDs);
          credentialsMap.put(newKey, new ConnectionCredentials(newUrl, creds.user(), creds.password()));
          derivedPoolRoots.put(newKey, rootKey);
          logger.info("PostgreSQL 为数据库 '{}' 创建新连接池", dbName);
        }
        currentConnectionKey = newKey;
        currentDatabaseName = dbName;
        databaseSelected = true;
        logger.info("PostgreSQL 切换到数据库: {}", dbName);
      }
    }
    catch(SQLException e)
    {
      // 切换失败，恢复原来的连接状态
      currentConnectionKey = oldKey;
      currentDatabaseName = oldDbName;
      logger.error("切换数据库失败 {}", dbName, e);
      throw e;
    }
  }

  /**
   * 获取当前连接（从连接池）
   *
   * @return 数据库连接
   * @throws SQLException 获取连接失败
   */
  public static Connection get() throws SQLException
  {
    // 仅在读取共享状态时持有锁，HikariCP.getConnection() 与 USE 执行在锁外
    HikariDataSource dataSource;
    String dbName;
    String connKey;
    boolean useDb;
    synchronized(ConnectionManager.class)
    {
      if(currentConnectionKey == null)
      {
        throw new IllegalStateException("No active connection. Call connect() first.");
      }
      connKey = currentConnectionKey;
      dataSource = dataSourceMap.get(connKey);
      if(dataSource == null || dataSource.isClosed())
      {
        throw new SQLException("Connection pool is closed");
      }
      dbName = currentDatabaseName;
      useDb = databaseSelected && dbName != null && !dbName.isEmpty();
    }
    try
    {
      Connection conn = dataSource.getConnection();
      // 每次获取连接时确保数据库上下文正确（连接池可能返回未设置 catalog 的连接）
      if(useDb)
      {
        try
        {
          String product = conn.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
          // 对 MySQL 使用 setCatalog，避免拼接执行 USE 带来的转义注入与上下文竞争问题
          if(product.contains("mysql"))
          {
            conn.setCatalog(dbName);
          }
          // PostgreSQL: JDBC URL 已包含 database，无需额外设置
        }
        catch(SQLException ex)
        {
          logger.warn("设置 catalog/schema 失败", ex);
        }
      }
      return conn;
    }
    catch(SQLException e)
    {
      logger.error("从连接池获取连接失败: {}", connKey, e);
      throw e;
    }
  }

  /**
   * 是否已连接
   */
  public static synchronized boolean isConnected()
  {
    if(currentConnectionKey == null)
    {
      return false;
    }

    HikariDataSource dataSource = dataSourceMap.get(currentConnectionKey);
    return dataSource != null && !dataSource.isClosed();
  }

  /**
   * 关闭当前连接及其所有派生连接池（PostgreSQL 多 database 池）
   */
  public static synchronized void close()
  {
    if(currentConnectionKey == null)
    {
      return;
    }
    // 找到根 key（如果当前 key 本身是派生的）
    String rootKey = derivedPoolRoots.getOrDefault(currentConnectionKey, currentConnectionKey);
    // 收集所有需要关闭的 key：根 key + 所有以该根 key 为父的派生 key
    List<String> toClose = new ArrayList<>();
    toClose.add(rootKey);
    for(Map.Entry<String, String> e : derivedPoolRoots.entrySet())
    {
      if(rootKey.equals(e.getValue()))
      {
        toClose.add(e.getKey());
      }
    }
    for(String key : toClose)
    {
      HikariDataSource ds = dataSourceMap.remove(key);
      if(ds != null && !ds.isClosed())
      {
        ds.close();
        logger.info("关闭连接 {}", key);
      }
      credentialsMap.remove(key);
      derivedPoolRoots.remove(key);
    }
    currentConnectionKey = null;
    currentDatabaseName = null;
    databaseSelected = false;
  }

  /**
   * 关闭所有连接
   */
  public static synchronized void closeAll()
  {
    for(Map.Entry<String, HikariDataSource> entry : dataSourceMap.entrySet())
    {
      HikariDataSource dataSource = entry.getValue();
      if(dataSource != null && !dataSource.isClosed())
      {
        dataSource.close();
        logger.info("关闭连接: {}", entry.getKey());
      }
    }
    dataSourceMap.clear();
    credentialsMap.clear();
    derivedPoolRoots.clear();
    currentConnectionKey = null;
    currentDatabaseName = null;
    databaseSelected = false;
  }

  /**
   * 标记是否已显式选择了数据库
   */
  public static synchronized void setDatabaseSelected(boolean selected)
  {
    databaseSelected = selected;
  }

  /**
   * 是否已显式选择了数据库
   */
  public static synchronized boolean isDatabaseSelected()
  {
    return databaseSelected;
  }

  /**
   * 提交事务
   */
  public static void commit() throws SQLException
  {
    try (Connection conn = get())
    {
      conn.commit();
      logger.debug("事务已提交");
    }
  }

  /**
   * 回滚事务
   */
  public static void rollback() throws SQLException
  {
    try (Connection conn = get())
    {
      conn.rollback();
      logger.debug("事务已回滚");
    }
  }

  /**
   * 设置自动提交模式
   */
  public static void setAutoCommit(boolean autoCommit) throws SQLException
  {
    try (Connection conn = get())
    {
      conn.setAutoCommit(autoCommit);
      logger.debug("设置自动提交: {}", autoCommit);
    }
  }

  /**
   * 当前是否为自动提交
   */
  public static boolean isAutoCommit() throws SQLException
  {
    try (Connection conn = get())
    {
      return conn.getAutoCommit();
    }
  }

  /**
   * 当前数据源 URL
   */
  public static String getUrl()
  {
    try (Connection conn = get())
    {
      return conn.getMetaData().getURL();
    }
    catch(Exception e)
    {
      logger.error("获取数据库URL失败", e);
      return "";
    }
  }

  /**
   * 当前数据库产品名（MySQL / PostgreSQL）
   */
  public static String getDatabaseProduct()
  {
    try (Connection conn = get())
    {
      return conn.getMetaData().getDatabaseProductName();
    }
    catch(Exception e)
    {
      logger.error("获取数据库产品名失败", e);
      return "";
    }
  }

  /**
   * 当前连接的用户名
   */
  public static String getUser()
  {
    try (Connection conn = get())
    {
      return conn.getMetaData().getUserName();
    }
    catch(Exception e)
    {
      logger.error("获取用户名失败", e);
      return "";
    }
  }

  /**
   * 当前连接的数据库
   */
  public static synchronized String getCurrentDatabase()
  {
    return currentDatabaseName;
  }

  /**
   * 执行健康检查
   */
  private static void performHealthCheck()
  {
    for(Map.Entry<String, HikariDataSource> entry : dataSourceMap.entrySet())
    {
      HikariDataSource dataSource = entry.getValue();
      if(dataSource != null && !dataSource.isClosed())
      {
        try (Connection conn = dataSource.getConnection())
        {
          if(conn.isValid(5)) // 5秒超时
          {
            logger.debug("连接健康检查通过: {}", entry.getKey());
          }
          else
          {
            logger.warn("连接健康检查失败 {}", entry.getKey());
          }
        }
        catch(SQLException e)
        {
          logger.error("连接健康检查异常 {}", entry.getKey(), e);
        }
      }
    }
  }

  /**
   * 创建 HikariCP 数据源（主连接池，预热若干连接）
   */
  private static HikariDataSource createDataSource(String jdbcUrl, String user, String password)
  {
    HikariConfig config = buildBaseConfig(jdbcUrl, user, password);
    config.setMaximumPoolSize(MAX_POOL_SIZE);
    config.setConnectionTimeout(CONNECTION_TIMEOUT);
    config.setIdleTimeout(IDLE_TIMEOUT);
    config.setMaxLifetime(MAX_LIFETIME);
    config.setLeakDetectionThreshold(LEAK_DETECTION_THRESHOLD);
    return new HikariDataSource(config);
  }

  /**
   * 创建轻量级派生数据源（PostgreSQL 切库按需创建）
   * <ul>
   *   <li>{@code initializationFailTimeout = -1}：完全懒初始化，构造函数立即返回，不阻塞 UI</li>
   *   <li>{@code minimumIdle = 0}：无流量时不保留空闲连接，节省连接资源</li>
   * </ul>
   */
  private static HikariDataSource createDerivedDataSource(String jdbcUrl, String user, String password)
  {
    HikariConfig config = buildBaseConfig(jdbcUrl, user, password);
    config.setMaximumPoolSize(5);
    config.setMinimumIdle(0);
    config.setConnectionTimeout(CONNECTION_TIMEOUT);
    config.setIdleTimeout(IDLE_TIMEOUT);
    config.setMaxLifetime(MAX_LIFETIME);
    // 懒初始化：不在构造时建立连接（避免在 synchronized 块内长时间阻塞）
    config.setInitializationFailTimeout(-1);
    return new HikariDataSource(config);
  }

  /**
   * 构建通用 HikariConfig（URL、用户名、密码、数据库专用属性、autoCommit）
   */
  private static HikariConfig buildBaseConfig(String jdbcUrl, String user, String password)
  {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(jdbcUrl);
    config.setUsername(user);
    config.setPassword(password);
    // 显式指定驱动类名 — 避免 Launch4j 非标准 classloader 下 ServiceLoader 失效
    if(jdbcUrl.contains("mysql"))
    {
      config.setDriverClassName("com.mysql.cj.jdbc.Driver");
      config.addDataSourceProperty("cachePrepStmts", "true");
      config.addDataSourceProperty("prepStmtCacheSize", "250");
      config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
      config.addDataSourceProperty("useServerPrepStmts", "true");
      config.addDataSourceProperty("useLocalSessionState", "true");
      config.addDataSourceProperty("rewriteBatchedStatements", "true");
      config.addDataSourceProperty("cacheResultSetMetadata", "true");
      config.addDataSourceProperty("cacheServerConfiguration", "true");
      config.addDataSourceProperty("elideSetAutoCommits", "true");
      config.addDataSourceProperty("maintainTimeStats", "false");
    }
    // PostgreSQL 特定配置
    if(jdbcUrl.contains("postgresql"))
    {
      config.setDriverClassName("org.postgresql.Driver");
      config.addDataSourceProperty("preparedStatementCacheQueries", "256");
      config.addDataSourceProperty("preparedStatementCacheSizeMiB", "5");
      config.addDataSourceProperty("cachePrepStmts", "true");
      config.addDataSourceProperty("prepStmtCacheSize", "256");
      config.addDataSourceProperty("useServerPrepStmts", "true");
    }
    // 禁用自动提交，支持事务
    config.setAutoCommit(false);
    return config;
  }

  /**
   * 构建连接 key
   */
  private static String buildConnectionKey(String jdbcUrl, String user)
  {
    return user + "@" + jdbcUrl;
  }

  /**
   * 将 JDBC URL 中的 database 部分替换为 {@code dbName}
   * <p>
   * 示例：
   * <pre>
   *   jdbc:postgresql://localhost:5432/          + "chs"  -> jdbc:postgresql://localhost:5432/chs
   *   jdbc:postgresql://localhost:5432/olddb     + "chs"  -> jdbc:postgresql://localhost:5432/chs
   *   jdbc:postgresql://localhost:5432/old?ssl=t + "chs"  -> jdbc:postgresql://localhost:5432/chs?ssl=t
   * </pre>
   */
  static String buildUrlWithDatabase(String jdbcUrl, String dbName)
  {
    int slashSlash = jdbcUrl.indexOf("//");
    if(slashSlash < 0)
    {
      return jdbcUrl + "/" + dbName;
    }
    String afterAuth = jdbcUrl.substring(slashSlash + 2); // "host:port/db?..." or "host:port"
    int firstSlash = afterAuth.indexOf('/');
    if(firstSlash < 0)
    {
      // URL 中没有 db 路径段：jdbc:postgresql://host:port
      return jdbcUrl + "/" + dbName;
    }
    // "jdbc:postgresql://host:port"
    String base = jdbcUrl.substring(0, slashSlash + 2) + afterAuth.substring(0, firstSlash);
    // "olddb?params" or "" or "?params"
    String remainder = afterAuth.substring(firstSlash + 1);
    // 保留查询参数（ssl=true 等）
    int q = remainder.indexOf('?');
    String params = q >= 0 ? remainder.substring(q) : "";
    return base + "/" + dbName + params;
  }

  /**
   * 从 JDBC URL 中提取数据库名
   * <p>
   * 如 {@code jdbc:mysql://localhost:3306/mydb} -> {@code mydb}
   */
  private static String extractDatabaseName(String jdbcUrl)
  {
    int slashSlash = jdbcUrl.indexOf("//");
    if(slashSlash < 0)
      return null;
    String afterHost = jdbcUrl.substring(slashSlash + 2); // "host:port/db"
    int slash = afterHost.indexOf('/');
    if(slash < 0)
      return null;
    String db = afterHost.substring(slash + 1);
    int question = db.indexOf('?');
    if(question >= 0)
      db = db.substring(0, question);
    return db.isEmpty() ? null : db;
  }

  /**
   * 根据数据库元数据获取正确的标识符引用符并引用标识符
   */
  public static String quoteIdentifier(Connection conn, String name)
  {
    if(name == null || name.isEmpty())
      return name;
    if(name.contains("."))
    {
      String[] parts = name.split("\\.", 2);
      return quoteIdentifier(conn, parts[0]) + "." + quoteIdentifier(conn, parts[1]);
    }
    try
    {
      String q = conn.getMetaData().getIdentifierQuoteString();
      if(q == null || q.isBlank() || " ".equals(q))
        return name;
      return q + name.replace(q, q + q) + q;
    }
    catch(SQLException e)
    {
      return "\"" + name.replace("\"", "\"\"") + "\"";
    }
  }

  /**
   * 关闭健康检查调度器（应用退出时调用）
   */
  public static void shutdown()
  {
    logger.info("关闭连接管理");
    closeAll();
    healthCheckExecutor.shutdown();
    try
    {
      if(!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS))
      {
        healthCheckExecutor.shutdownNow();
      }
    }
    catch(InterruptedException e)
    {
      healthCheckExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}


