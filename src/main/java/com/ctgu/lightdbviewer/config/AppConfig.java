package com.ctgu.lightdbviewer.config;

import com.ctgu.lightdbviewer.db.DbType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @author lihuahui
 * @version 1.0
 * @date 2026-04-23 18:11
 * @description: AppConfig — 全局配置单例
 * <p>
 * 持久化到程序运行目录下的 lightdbviewer.properties（UTF-8编码）
 * <p>
 * 管理：
 * - 已保存的数据库连接（密码 AES 加密）
 * - UI 字体设置（字体族 + 字号）
 */
public final class AppConfig
{
  private static final String CONFIG_FILE = "lightdbviewer.properties";
  private static final String DEFAULT_FONT_FAMILY = "微软雅黑";
  private static final int DEFAULT_FONT_SIZE = 16;
  private static final String DEFAULT_EDITOR_FONT_FAMILY = "Consolas";
  private static final int DEFAULT_EDITOR_FONT_SIZE = 16;
  /**
   * 默认主题：GitHub Material
   */
  private static final String DEFAULT_THEME = "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTGitHubIJTheme";
  /**
   * 默认背景色：护眼绿-绿豆沙
   */
  private static final String DEFAULT_BG_COLOR = "#C7EDCC";

  private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
  private static final AppConfig INSTANCE = new AppConfig();

  private final Properties props = new Properties();
  private String fontFamily = DEFAULT_FONT_FAMILY;
  private int fontSize = DEFAULT_FONT_SIZE;
  private String editorFontFamily = DEFAULT_EDITOR_FONT_FAMILY;
  private int editorFontSize = DEFAULT_EDITOR_FONT_SIZE;
  private String themeName = DEFAULT_THEME;
  private String bgColorHex = DEFAULT_BG_COLOR;
  private final List<SavedConnection> connections = new ArrayList<>();

  private AppConfig()
  {
  }

  public static AppConfig getInstance()
  {
    return INSTANCE;
  }

  /**
   * 从程序目录加载配置（启动时调用一次）
   */
  public void load()
  {
    Path path = configFilePath();
    if(!Files.exists(path))
      return;
    try (Reader r = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8))
    {
      props.load(r);
      fontFamily = props.getProperty("ui.font.family", DEFAULT_FONT_FAMILY);
      fontSize = parseIntSafe(props.getProperty("ui.font.size"), DEFAULT_FONT_SIZE);
      editorFontFamily = props.getProperty("editor.font.family", DEFAULT_EDITOR_FONT_FAMILY);
      editorFontSize = parseIntSafe(props.getProperty("editor.font.size"), DEFAULT_EDITOR_FONT_SIZE);
      themeName = props.getProperty("ui.theme", DEFAULT_THEME);
      bgColorHex = props.getProperty("ui.bg.color", DEFAULT_BG_COLOR);
      loadConnections();
    }
    catch(IOException e)
    {
      logger.warn("加载配置失败", e);
    }
  }

  /**
   * 保存配置到程序目录
   */
  public void save()
  {
    props.setProperty("ui.font.family", fontFamily);
    props.setProperty("ui.font.size", String.valueOf(fontSize));
    props.setProperty("editor.font.family", editorFontFamily);
    props.setProperty("editor.font.size", String.valueOf(editorFontSize));
    props.setProperty("ui.theme", themeName != null ? themeName : DEFAULT_THEME);
    props.setProperty("ui.bg.color", bgColorHex != null ? bgColorHex : DEFAULT_BG_COLOR);
    saveConnections();

    Path path = configFilePath();
    try (Writer w = new OutputStreamWriter(Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING),
        StandardCharsets.UTF_8))
    {
      props.store(w, "LightDB Viewer Configuration - Do not edit manually");
    }
    catch(IOException e)
    {
      logger.warn("保存配置失败", e);
    }
  }

  public String getFontFamily()
  {
    return fontFamily;
  }

  public void setFontFamily(String family)
  {
    fontFamily = family;
  }

  public int getFontSize()
  {
    return fontSize;
  }

  public void setFontSize(int size)
  {
    fontSize = size;
  }

  public String getEditorFontFamily()
  {
    return editorFontFamily;
  }

  public void setEditorFontFamily(String family)
  {
    editorFontFamily = family;
  }

  public int getEditorFontSize()
  {
    return editorFontSize;
  }

  public void setEditorFontSize(int size)
  {
    editorFontSize = size;
  }

  public String getThemeName()
  {
    return themeName;
  }

  public void setThemeName(String name)
  {
    themeName = name;
  }

  public String getBgColorHex()
  {
    return bgColorHex;
  }

  public void setBgColorHex(String hex)
  {
    bgColorHex = hex;
  }

  public List<SavedConnection> getConnections()
  {
    return Collections.unmodifiableList(connections);
  }

  /**
   * 按名称更新（存在同名则替换，否则追加）
   */
  public void addOrUpdateConnection(SavedConnection sc)
  {
    for(int i = 0; i < connections.size(); i++)
    {
      if(connections.get(i).name.equals(sc.name))
      {
        connections.set(i, sc);
        return;
      }
    }
    connections.add(sc);
  }

  public void removeConnection(SavedConnection sc)
  {
    connections.remove(sc);
  }

  private void loadConnections()
  {
    connections.clear();
    int count = parseIntSafe(props.getProperty("connection.count"), 0);
    for(int i = 0; i < count; i++)
    {
      String prefix = "connection." + i + ".";
      SavedConnection sc = new SavedConnection();
      sc.name = props.getProperty(prefix + "name", "Connection " + (i + 1));
      String typeStr = props.getProperty(prefix + "type", "POSTGRESQL");
      try
      {
        sc.type = DbType.valueOf(typeStr);
      }
      catch(Exception e)
      {
        sc.type = DbType.POSTGRESQL;
      }
      sc.host = props.getProperty(prefix + "host", "localhost");
      sc.port = parseIntSafe(props.getProperty(prefix + "port"), 5432);
      sc.database = props.getProperty(prefix + "database", "");
      sc.username = props.getProperty(prefix + "username", "");
      String encPwd = props.getProperty(prefix + "password_enc", "");
      try
      {
        sc.password = PasswordCipher.decrypt(encPwd);
      }
      catch(PasswordCipher.PasswordCipherException e)
      {
        sc.password = "";
      }
      connections.add(sc);
    }
  }

  private void saveConnections()
  {
    // 先清除旧连接 key
    props.keySet().removeIf(k -> k.toString().startsWith("connection."));

    props.setProperty("connection.count", String.valueOf(connections.size()));
    for(int i = 0; i < connections.size(); i++)
    {
      SavedConnection sc = connections.get(i);
      String prefix = "connection." + i + ".";
      props.setProperty(prefix + "name", nvl(sc.name));
      props.setProperty(prefix + "type", sc.type != null ? sc.type.name() : "POSTGRESQL");
      props.setProperty(prefix + "host", nvl(sc.host, "localhost"));
      props.setProperty(prefix + "port", String.valueOf(sc.port));
      props.setProperty(prefix + "database", nvl(sc.database));
      props.setProperty(prefix + "username", nvl(sc.username));
      try
      {
        props.setProperty(prefix + "password_enc", PasswordCipher.encrypt(nvl(sc.password)));
      }
      catch(PasswordCipher.PasswordCipherException e)
      {
        props.setProperty(prefix + "password_enc", "");
      }
    }
  }

  private Path configFilePath()
  {
    return Paths.get(System.getProperty("user.dir"), CONFIG_FILE);
  }

  private static int parseIntSafe(String s, int def)
  {
    try
    {
      return Integer.parseInt(s);
    }
    catch(Exception e)
    {
      return def;
    }
  }

  private static String nvl(String s)
  {
    return s != null ? s : "";
  }

  private static String nvl(String s, String def)
  {
    return (s != null && !s.isEmpty()) ? s : def;
  }
}

