package com.ctgu.lightdbviewer.ai;

public class AiModelItem
{
  private String name = "";
  private String provider = "";
  private String host = "";
  private String endpoint = "";
  private String apiKey = "";
  private String model = "";
  private double temperature = 0.7;
  private String description = "";

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getProvider() { return provider; }
  public void setProvider(String provider) { this.provider = provider; }
  public String getHost() { return host; }
  public void setHost(String host) { this.host = host; }
  public String getEndpoint() { return endpoint; }
  public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
  public String getApiKey() { return apiKey; }
  public void setApiKey(String apiKey) { this.apiKey = apiKey; }
  public String getModel() { return model; }
  public void setModel(String model) { this.model = model; }
  public double getTemperature() { return temperature; }
  public void setTemperature(double temperature) { this.temperature = temperature; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  @Override
  public String toString() { return name.isEmpty() ? model : name; }
}
