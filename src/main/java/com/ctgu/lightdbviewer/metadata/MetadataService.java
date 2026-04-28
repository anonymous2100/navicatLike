package com.ctgu.lightdbviewer.metadata;

import com.ctgu.lightdbviewer.jdbc.ConnectionManager;

import java.sql.*;
import java.util.*;

/**
 * @author lihuahui
 * @version 1.0
 * @description: MetadataService
 * <p>
 * 统一封装 DatabaseMetaData 的所有使用场景：
 * - 表列表
 * - 列信息
 * - 主键
 * - 外键
 * - ENUM
 * <p>
 * 设计目标：
 * MySQL / PostgreSQL 通用
 * UI 层 0 JDBC 依赖
 * 可长期扩展（Phase 3 / Phase 4）
 * @date 2026-04-23 18:32
 */
public class MetadataService
{
  /**
   * 列出当前数据库的所有用户 Schema（PostgreSQL 返回非系统 schema；MySQL 返回空列表）
   */
  public static List<String> listSchemas() throws SQLException
  {
    try (Connection conn = ConnectionManager.get())
    {
      String product = conn.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
      List<String> schemas = new ArrayList<>();
      if(product.contains("postgres"))
      {
        String sql = "SELECT schema_name FROM information_schema.schemata "
            + "WHERE schema_name NOT IN ('pg_catalog','information_schema','pg_toast') " + "AND schema_name NOT LIKE 'pg_%' "
            + "ORDER BY schema_name";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql))
        {
          while(rs.next())
            schemas.add(rs.getString(1));
        }
      }
      // MySQL 不需要 schema 层，返回空列表
      return schemas;
    }
  }

  /**
   * 列出服务器上的所有数据库
   */
  public static List<String> listDatabases() throws SQLException
  {
    try (Connection conn = ConnectionManager.get())
    {
      String dbProduct = conn.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
      List<String> databases = new ArrayList<>();
      if(dbProduct.contains("mysql"))
      {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SHOW DATABASES"))
        {
          while(rs.next())
          {
            databases.add(rs.getString(1));
          }
        }
      }
      else
      {
        // PostgreSQL
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(
            "SELECT datname FROM pg_database WHERE datistemplate = false ORDER BY datname"))
        {
          while(rs.next())
          {
            databases.add(rs.getString(1));
          }
        }
      }
      return databases;
    }
  }

  /**
   * 列出当前数据库中指定 schema 的所有用户表（schema=null 则不过滤）
   */
  public static List<String> listTables(String schema) throws SQLException
  {
    try (Connection conn = ConnectionManager.get())
    {
      DatabaseMetaData meta = conn.getMetaData();
      String catalog = resolveCatalog(conn);
      List<String> tables = new ArrayList<>();
      try (ResultSet rs = meta.getTables(catalog, schema, "%", new String[] { "TABLE" }))
      {
        while(rs.next())
          tables.add(rs.getString("TABLE_NAME"));
      }
      return tables;
    }
  }

  /**
   * 列出当前数据库中的所有用户表（使用默认 schema 过滤）
   */
  public static List<String> listTables() throws SQLException
  {
    try (Connection conn = ConnectionManager.get())
    {
      return listTables(resolveDefaultSchema(conn));
    }
  }

  /**
   * 列出当前数据库中指定 schema 的视图
   */
  public static List<String> listViews(String schema) throws SQLException
  {
    try (Connection conn = ConnectionManager.get())
    {
      DatabaseMetaData meta = conn.getMetaData();
      String catalog = resolveCatalog(conn);
      List<String> views = new ArrayList<>();
      try (ResultSet rs = meta.getTables(catalog, schema, "%", new String[] { "VIEW" }))
      {
        while(rs.next())
          views.add(rs.getString("TABLE_NAME"));
      }
      return views;
    }
  }

  /**
   * 列出当前数据库中的视图（使用默认 schema 过滤）
   */
  public static List<String> listViews() throws SQLException
  {
    try (Connection conn = ConnectionManager.get())
    {
      return listViews(resolveDefaultSchema(conn));
    }
  }

  /**
   * 根据数据库类型返回默认 schema：PostgreSQL → "public"，其他 → null（不过滤）
   */
  private static String resolveDefaultSchema(Connection conn) throws SQLException
  {
    String product = conn.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
    return product.contains("postgres") ? "public" : null;
  }

  /**
   * 根据数据库类型解析 catalog 参数：
   * MySQL → conn.getCatalog()（当前数据库名），确保 getTables/getColumns 等只返回当前数据库的对象；
   * PostgreSQL → null（PostgreSQL 的 metadata API 无需 catalog 过滤，schema 参数已足够）
   */
  private static String resolveCatalog(Connection conn) throws SQLException
  {
    String product = conn.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
    return product.contains("mysql") ? conn.getCatalog() : null;
  }

  public static List<String> listProcedures(String schema) throws SQLException
  {
    try (Connection conn = ConnectionManager.get())
    {
      DatabaseMetaData meta = conn.getMetaData();
      String catalog = resolveCatalog(conn);
      List<String> procedures = new ArrayList<>();
      try (ResultSet rs = meta.getProcedures(catalog, schema, "%"))
      {
        while(rs.next())
          procedures.add(rs.getString("PROCEDURE_NAME"));
      }
      return procedures;
    }
  }

  public static List<String> listProcedures() throws SQLException
  {
    try (Connection conn = ConnectionManager.get())
    {
      return listProcedures(resolveDefaultSchema(conn));
    }
  }

  public static List<String> listFunctions(String schema) throws SQLException
  {
    try (Connection conn = ConnectionManager.get())
    {
      DatabaseMetaData meta = conn.getMetaData();
      String catalog = resolveCatalog(conn);
      List<String> functions = new ArrayList<>();
      try (ResultSet rs = meta.getFunctions(catalog, schema, "%"))
      {
        while(rs.next())
          functions.add(rs.getString("FUNCTION_NAME"));
      }
      return functions;
    }
  }

  public static List<String> listFunctions() throws SQLException
  {
    try (Connection conn = ConnectionManager.get())
    {
      return listFunctions(resolveDefaultSchema(conn));
    }
  }

  public static List<String> listTriggers(String schema) throws SQLException
  {
    try (Connection conn = ConnectionManager.get())
    {
      String dbProduct = conn.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
      String sql;
      if(dbProduct.contains("postgres"))
      {
        sql = "SELECT DISTINCT trigger_name FROM information_schema.triggers " + "WHERE trigger_schema = ? ORDER BY trigger_name";
      }
      else
      {
        sql = "SELECT DISTINCT trigger_name FROM information_schema.triggers " + "WHERE trigger_schema = ? ORDER BY trigger_name";
      }
      LinkedHashSet<String> names = new LinkedHashSet<>();
      // schema 为 null 时回退到旧行为
      if(schema == null)
      {
        return listTriggers();
      }
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
        ps.setString(1, schema);
        try (ResultSet rs = ps.executeQuery())
        {
          while(rs.next())
            names.add(rs.getString(1));
        }
      }
      return new ArrayList<>(names);
    }
  }

  public static List<String> listTriggers() throws SQLException
  {
    try (Connection conn = ConnectionManager.get())
    {
      String dbProduct = conn.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
      String sql;
      if(dbProduct.contains("postgres"))
      {
        sql = "SELECT trigger_name FROM information_schema.triggers WHERE trigger_schema = current_schema() ORDER BY trigger_name";
      }
      else
      {
        sql = "SELECT trigger_name FROM information_schema.triggers WHERE trigger_schema = DATABASE() ORDER BY trigger_name";
      }
      LinkedHashSet<String> names = new LinkedHashSet<>();
      try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery())
      {
        while(rs.next())
        {
          names.add(rs.getString(1));
        }
      }
      return new ArrayList<>(names);
    }
  }

  /**
   * 列出数据库角色/用户
   */
  public static List<String[]> listRoles() throws SQLException
  {
    try (Connection conn = ConnectionManager.get())
    {
      String product = conn.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
      List<String[]> roles = new ArrayList<>();
      if(product.contains("postgres"))
      {
        String sql = "SELECT rolname, CASE WHEN rolsuper THEN 'Yes' ELSE 'No' END, " + "CASE WHEN rolcreatedb THEN 'Yes' ELSE 'No' END, "
            + "CASE WHEN rolcanlogin THEN 'Yes' ELSE 'No' END " + "FROM pg_roles ORDER BY rolname";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql))
        {
          while(rs.next())
            roles.add(new String[] { rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4) });
        }
      }
      else
      {
        // MySQL
        String sql = "SELECT user, host FROM mysql.user ORDER BY user";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql))
        {
          while(rs.next())
            roles.add(new String[] { rs.getString(1), rs.getString(2), "", "" });
        }
        catch(SQLException ignored)
        {
          // 权限不足时回退
          roles.add(new String[] { conn.getMetaData().getUserName(), "current", "", "" });
        }
      }
      return roles;
    }
  }

  /**
   * 获取表的所有列（完整信息）
   */
  public static List<ColumnInfo> columns(String tableName) throws SQLException
  {
    try (Connection conn = ConnectionManager.get())
    {
      DatabaseMetaData meta = conn.getMetaData();
      String catalog = resolveCatalog(conn);
      String schema = resolveDefaultSchema(conn);
      Map<String, ColumnInfo> cols = new LinkedHashMap<>();
      // ---------- 基本列信息 ----------
      try (ResultSet rs = meta.getColumns(catalog, schema, tableName, "%"))
      {
        while(rs.next())
        {
          ColumnInfo c = new ColumnInfo();
          c.name = rs.getString("COLUMN_NAME");
          c.jdbcType = rs.getInt("DATA_TYPE");
          c.typeName = rs.getString("TYPE_NAME");
          c.columnSize = rs.getInt("COLUMN_SIZE");
          c.decimalDigits = rs.getInt("DECIMAL_DIGITS");
          c.nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
          c.autoIncrement = "YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT"));
          c.defaultValue = rs.getString("COLUMN_DEF");
          c.remarks = rs.getString("REMARKS");
          cols.put(c.name, c);
        }
      }
      // ---------- 主键 ----------
      try (ResultSet rs = meta.getPrimaryKeys(catalog, schema, tableName))
      {
        while(rs.next())
        {
          String col = rs.getString("COLUMN_NAME");
          if(cols.containsKey(col))
          {
            cols.get(col).pk = true;
          }
        }
      }
      // ---------- 外键 ----------
      try (ResultSet rs = meta.getImportedKeys(catalog, schema, tableName))
      {
        while(rs.next())
        {
          String fkCol = rs.getString("FKCOLUMN_NAME");
          ColumnInfo c = cols.get(fkCol);
          if(c != null)
          {
            c.foreignKey = true;
            c.fkTable = rs.getString("PKTABLE_NAME");
            c.fkColumn = rs.getString("PKCOLUMN_NAME");
          }
        }
      }
      // ---------- ENUM（MySQL / Postgres） ----------
      for(ColumnInfo c : cols.values())
      {
        detectEnum(conn, c);
      }
      return new ArrayList<>(cols.values());
    }
  }

  /**
   * 仅获取主键字段名（用于 UPDATE / DELETE WHERE）
   */
  public static List<String> primaryKeys(String tableName) throws SQLException
  {
    try (Connection conn = ConnectionManager.get())
    {
      DatabaseMetaData meta = conn.getMetaData();
      String catalog = resolveCatalog(conn);
      String schema = resolveDefaultSchema(conn);
      List<String> pks = new ArrayList<>();
      try (ResultSet rs = meta.getPrimaryKeys(catalog, schema, tableName))
      {
        while(rs.next())
        {
          pks.add(rs.getString("COLUMN_NAME"));
        }
      }
      return pks;
    }
  }

  private static void detectEnum(Connection conn, ColumnInfo c) throws SQLException
  {
    if(c.typeName == null)
    {
      return;
    }
    String type = c.typeName.toLowerCase(Locale.ROOT);
    // ---------- MySQL ENUM ----------
    if(type.startsWith("enum"))
    {
      c.enumType = true;
      c.enumValues = parseMysqlEnum(type);
      return;
    }
    // ---------- PostgreSQL ENUM ----------
    if(isPostgresEnum(conn, c.typeName))
    {
      c.enumType = true;
      c.enumValues = loadPostgresEnumValues(conn, c.typeName);
    }
  }

  // MySQL enum('a','b','c')
  private static List<String> parseMysqlEnum(String typeName)
  {
    int start = typeName.indexOf('(');
    int end = typeName.lastIndexOf(')');
    if(start < 0 || end < 0)
    {
      return List.of();
    }
    String body = typeName.substring(start + 1, end);
    String[] parts = body.split(",");
    List<String> values = new ArrayList<>();
    for(String p : parts)
    {
      values.add(p.replace("'", "").trim());
    }
    return values;
  }

  // PostgreSQL ENUM detection
  private static boolean isPostgresEnum(Connection conn, String typeName) throws SQLException
  {
    String sql = """
        SELECT 1
        FROM pg_type
        WHERE typname = ?
          AND typtype = 'e'
        """;
    try (PreparedStatement ps = conn.prepareStatement(sql))
    {
      ps.setString(1, typeName);
      try (ResultSet rs = ps.executeQuery())
      {
        return rs.next();
      }
    }
  }

  private static List<String> loadPostgresEnumValues(Connection conn, String typeName) throws SQLException
  {
    String sql = """
        SELECT enumlabel
        FROM pg_enum
        JOIN pg_type t ON t.oid = pg_enum.enumtypid
        WHERE t.typname = ?
        ORDER BY enumsortorder
        """;
    List<String> values = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql))
    {
      ps.setString(1, typeName);
      try (ResultSet rs = ps.executeQuery())
      {
        while(rs.next())
        {
          values.add(rs.getString(1));
        }
      }
    }
    return values;
  }
}