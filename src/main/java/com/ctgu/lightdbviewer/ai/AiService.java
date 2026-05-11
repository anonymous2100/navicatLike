package com.ctgu.lightdbviewer.ai;

import com.ctgu.lightdbviewer.config.AppConfig;
import com.ctgu.lightdbviewer.config.PasswordCipher;
import com.ctgu.lightdbviewer.jdbc.ConnectionManager;
import com.ctgu.lightdbviewer.metadata.MetadataService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * AI 服务单例 — LangChain4j 封装
 */
public final class AiService
{
  private static final Logger logger = LoggerFactory.getLogger(AiService.class);
  private static final AiService INSTANCE = new AiService();

  private ChatLanguageModel chatModel;
  private OpenAiStreamingChatModel streamingModel;
  private AiConfig config;

  private AiService()
  {
  }

  public static AiService getInstance()
  {
    return INSTANCE;
  }

  /**
   * 根据 AppConfig 中的 AI 配置（重新）初始化模型
   */
  public void initFromAppConfig()
  {
    java.util.Properties props = getAppConfigProps();
    config = new AiConfig();
    config.setEnabled("true".equals(props.getProperty("ai.enabled", "true")));
    String apiKeyEnc = props.getProperty("ai.api.key.enc", "");
    if(!apiKeyEnc.isBlank())
    {
      try
      {
        config.setApiKey(PasswordCipher.decrypt(apiKeyEnc));
      }
      catch(Exception e)
      {
        config.setApiKey(props.getProperty("ai.api.key", ""));
      }
    }
    else
    {
      config.setApiKey(props.getProperty("ai.api.key", ""));
    }
    config.setModel(props.getProperty("ai.model", "deepseek-chat"));
    config.setEndpoint(props.getProperty("ai.endpoint", "https://api.deepseek.com/v1"));
    config.setMaxTokens(parseIntSafe(props.getProperty("ai.max.tokens"), 2048));
    config.setTimeoutSeconds(parseIntSafe(props.getProperty("ai.timeout"), 30));

    if(!config.isEnabled() || config.getApiKey().isBlank())
    {
      chatModel = null;
      streamingModel = null;
      return;
    }
    try
    {
      chatModel = OpenAiChatModel.builder()
          .baseUrl(config.getEndpoint())
          .apiKey(config.getApiKey())
          .modelName(config.getModel())
          .maxTokens(config.getMaxTokens())
          .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
          .logRequests(false)
          .logResponses(false)
          .build();
      streamingModel = OpenAiStreamingChatModel.builder()
          .baseUrl(config.getEndpoint())
          .apiKey(config.getApiKey())
          .modelName(config.getModel())
          .maxTokens(config.getMaxTokens())
          .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
          .build();
      logger.info("AI 模型初始化成功: {}", config.getModel());
    }
    catch(Exception e)
    {
      logger.error("AI 模型初始化失败", e);
      chatModel = null;
      streamingModel = null;
    }
  }

  public boolean isAvailable()
  {
    return config != null && config.isEnabled() && chatModel != null;
  }

  public AiConfig getConfig()
  {
    return config;
  }

  /**
   * 普通调用（错误诊断 / SQL 解释）
   */
  public String chat(String systemPrompt, String userMessage)
  {
    if(!isAvailable())
    {
      return "AI 服务未启用或未配置 API Key。请在 工具 → 选项 → AI 中配置。";
    }
    try
    {
      List<ChatMessage> messages = List.of(SystemMessage.from(systemPrompt), UserMessage.from(userMessage));
      dev.langchain4j.model.output.Response<AiMessage> response = chatModel.generate(messages);
      return response.content().text();
    }
    catch(Exception e)
    {
      logger.error("AI 调用失败", e);
      return "AI 请求失败: " + e.getMessage();
    }
  }

  /**
   * 流式调用（NL2SQL）
   */
  public void chatStreaming(String systemPrompt, String userMessage,
      StreamingResponseHandler<AiMessage> handler)
  {
    if(!isAvailable())
    {
      handler.onError(new IllegalStateException("AI 服务未启用或未配置 API Key。"));
      return;
    }
    try
    {
      List<ChatMessage> messages = List.of(SystemMessage.from(systemPrompt), UserMessage.from(userMessage));
      streamingModel.generate(messages, handler);
    }
    catch(Exception e)
    {
      logger.error("AI 流式调用失败", e);
      handler.onError(e);
    }
  }

  /**
   * 带对话记忆的普通调用
   */
  public String chatWithMemory(ChatMemory memory)
  {
    if(!isAvailable())
    {
      return "AI 服务未启用或未配置 API Key。";
    }
    try
    {
      dev.langchain4j.model.output.Response<AiMessage> response = chatModel.generate(memory.messages());
      return response.content().text();
    }
    catch(Exception e)
    {
      logger.error("AI 调用失败", e);
      return "AI 请求失败: " + e.getMessage();
    }
  }

  /**
   * 创建对话记忆（最多保存 10 轮）
   */
  public ChatMemory createMemory()
  {
    return MessageWindowChatMemory.withMaxMessages(20);
  }

  /**
   * 收集当前数据库 Schema 摘要
   */
  public String collectSchemaSummary()
  {
    if(!ConnectionManager.isConnected())
    {
      return "（未连接数据库）";
    }
    try
    {
      StringBuilder sb = new StringBuilder();
      List<String> tables = MetadataService.listTables(null);
      int limit = Math.min(tables.size(), 50);
      for(int i = 0; i < limit; i++)
      {
        String table = tables.get(i);
        sb.append("表 ").append(table).append(": ");
        try
        {
          List<com.ctgu.lightdbviewer.metadata.ColumnInfo> cols = MetadataService.columns(table);
          for(int j = 0; j < cols.size(); j++)
          {
            if(j > 0)
            {
              sb.append(", ");
            }
            sb.append(cols.get(j).name).append(" ").append(cols.get(j).typeName);
          }
        }
        catch(Exception ignored)
        {
          sb.append("(无法获取列信息)");
        }
        sb.append("\n");
      }
      if(tables.size() > limit)
      {
        sb.append("... 共 ").append(tables.size()).append(" 张表，仅显示前 ").append(limit).append(" 张\n");
      }
      return sb.toString();
    }
    catch(Exception e)
    {
      return "（获取 Schema 失败: " + e.getMessage() + "）";
    }
  }

  public String getDbTypeVersion()
  {
    if(!ConnectionManager.isConnected())
    {
      return "未知";
    }
    try
    {
      try(java.sql.Connection conn = ConnectionManager.get())
      {
        return conn.getMetaData().getDatabaseProductName() + " " + conn.getMetaData().getDatabaseProductVersion();
      }
    }
    catch(Exception e)
    {
      return "未知";
    }
  }

  private static java.util.Properties getAppConfigProps()
  {
    try
    {
      var field = AppConfig.class.getDeclaredField("props");
      field.setAccessible(true);
      return (java.util.Properties)field.get(AppConfig.getInstance());
    }
    catch(Exception e)
    {
      return new java.util.Properties();
    }
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
}
