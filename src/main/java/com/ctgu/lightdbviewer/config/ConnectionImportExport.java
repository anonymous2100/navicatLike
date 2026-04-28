package com.ctgu.lightdbviewer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lihuahui
 * @version 1.0
 * @date 2026-04-23 18:11
 * @description: 连接配置导入导出工具
 * <p>
 * 支持将已保存的数据库连接配置导出为XML文件，或从XML文件导入
 */
@XmlRootElement(name = "connections")
public class ConnectionImportExport
{
  private static final Logger logger = LoggerFactory.getLogger(ConnectionImportExport.class);

  @XmlElementWrapper(name = "savedConnections")
  @XmlElement(name = "connection")
  private List<SavedConnection> connections = new ArrayList<>();

  public ConnectionImportExport()
  {
  }

  public List<SavedConnection> getConnections()
  {
    return connections;
  }

  public void setConnections(List<SavedConnection> connections)
  {
    this.connections = connections;
  }

  /**
   * 导出连接配置到文件
   *
   * @param filePath 目标文件路径
   * @throws IOException 导出失败
   */
  public void exportToFile(String filePath) throws IOException
  {
    try
    {
      // 从AppConfig获取当前连接
      this.connections = new ArrayList<>(AppConfig.getInstance().getConnections());

      JAXBContext context = JAXBContext.newInstance(ConnectionImportExport.class);
      Marshaller marshaller = context.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

      try (Writer writer = new OutputStreamWriter(Files.newOutputStream(Path.of(filePath)), StandardCharsets.UTF_8))
      {
        marshaller.marshal(this, writer);
      }

      logger.info("成功导出{}个连接配置到: {}", connections.size(), filePath);
    }
    catch(Exception e)
    {
      logger.error("导出连接配置失败: {}", filePath, e);
      throw new IOException("导出连接配置失败: " + e.getMessage(), e);
    }
  }

  /**
   * 从文件导入连接配置
   *
   * @param filePath 源文件路径
   * @return 导入的连接配置
   * @throws IOException 导入失败
   */
  public static ConnectionImportExport importFromFile(String filePath) throws IOException
  {
    try
    {
      JAXBContext context = JAXBContext.newInstance(ConnectionImportExport.class);
      Unmarshaller unmarshaller = context.createUnmarshaller();

      try (Reader reader = new InputStreamReader(Files.newInputStream(Path.of(filePath)), StandardCharsets.UTF_8))
      {
        ConnectionImportExport importData = (ConnectionImportExport)unmarshaller.unmarshal(reader);
        logger.info("成功从{}导入{}个连接配置", filePath, importData.getConnections().size());
        return importData;
      }
    }
    catch(Exception e)
    {
      logger.error("导入连接配置失败: {}", filePath, e);
      throw new IOException("导入连接配置失败: " + e.getMessage(), e);
    }
  }

  /**
   * 将导入的连接合并到AppConfig中
   *
   * @param overwrite 是否覆盖同名连接
   */
  public void mergeToAppConfig(boolean overwrite)
  {
    AppConfig config = AppConfig.getInstance();
    int added = 0;
    int updated = 0;
    int skipped = 0;

    for(SavedConnection importedConn : this.connections)
    {
      boolean exists = config.getConnections().stream().anyMatch(c -> c.name.equals(importedConn.name));

      if(exists)
      {
        if(overwrite)
        {
          config.addOrUpdateConnection(importedConn);
          updated++;
        }
        else
        {
          skipped++;
        }
      }
      else
      {
        config.addOrUpdateConnection(importedConn);
        added++;
      }
    }

    if(added > 0 || updated > 0)
    {
      config.save();
      logger.info("连接配置合并完成: 新增{}, 更新{}, 跳过{}", added, updated, skipped);
    }
  }

  /**
   * 导出为JSON格式
   *
   * @param filePath 目标文件路径
   * @throws IOException 导出失败
   */
  public void exportToJson(String filePath) throws IOException
  {
    try
    {
      this.connections = new ArrayList<>(AppConfig.getInstance().getConnections());

      StringBuilder json = new StringBuilder();
      json.append("{\n");
      json.append("  \"connections\": [\n");

      for(int i = 0; i < connections.size(); i++)
      {
        SavedConnection conn = connections.get(i);
        json.append("    {\n");
        json.append("      \"name\": \"").append(escapeJson(conn.name)).append("\",\n");
        json.append("      \"type\": \"").append(conn.type).append("\",\n");
        json.append("      \"host\": \"").append(escapeJson(conn.host)).append("\",\n");
        json.append("      \"port\": ").append(conn.port).append(",\n");
        json.append("      \"database\": \"").append(escapeJson(conn.database)).append("\",\n");
        json.append("      \"username\": \"").append(escapeJson(conn.username)).append("\"\n");
        json.append("    }").append(i < connections.size() - 1 ? "," : "").append("\n");
      }

      json.append("  ]\n");
      json.append("}");

      Files.writeString(Path.of(filePath), json.toString(), StandardCharsets.UTF_8);
      logger.info("成功导出{}个连接配置到JSON: {}", connections.size(), filePath);
    }
    catch(Exception e)
    {
      logger.error("导出JSON连接配置失败: {}", filePath, e);
      throw new IOException("导出JSON连接配置失败: " + e.getMessage(), e);
    }
  }

  /**
   * JSON字符串转义
   */
  private String escapeJson(String text)
  {
    if(text == null)
    {
      return "";
    }
    return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
  }
}
