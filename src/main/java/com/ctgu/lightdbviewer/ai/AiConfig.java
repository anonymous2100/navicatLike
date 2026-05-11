package com.ctgu.lightdbviewer.ai;

/**
 * AI 配置模型
 */
public class AiConfig
{
  private boolean enabled = true;
  private String apiKey = "";
  private String model = "deepseek-chat";
  private String endpoint = "https://api.deepseek.com/v1";
  private int maxTokens = 2048;
  private int timeoutSeconds = 30;

  public boolean isEnabled()
  {
    return enabled;
  }

  public void setEnabled(boolean enabled)
  {
    this.enabled = enabled;
  }

  public String getApiKey()
  {
    return apiKey;
  }

  public void setApiKey(String apiKey)
  {
    this.apiKey = apiKey;
  }

  public String getModel()
  {
    return model;
  }

  public void setModel(String model)
  {
    this.model = model;
  }

  public String getEndpoint()
  {
    return endpoint;
  }

  public void setEndpoint(String endpoint)
  {
    this.endpoint = endpoint;
  }

  public int getMaxTokens()
  {
    return maxTokens;
  }

  public void setMaxTokens(int maxTokens)
  {
    this.maxTokens = maxTokens;
  }

  public int getTimeoutSeconds()
  {
    return timeoutSeconds;
  }

  public void setTimeoutSeconds(int timeoutSeconds)
  {
    this.timeoutSeconds = timeoutSeconds;
  }
}
