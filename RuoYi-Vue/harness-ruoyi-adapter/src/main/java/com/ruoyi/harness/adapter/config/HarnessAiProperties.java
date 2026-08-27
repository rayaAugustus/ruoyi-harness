package com.ruoyi.harness.adapter.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="harness.ai")
public class HarnessAiProperties {
    private boolean enabled=false;private String provider="openai-compatible";private String baseUrl="";private String apiKey="";private String model="";
    private double temperature=0.2;private int maxOutputTokens=12000;private Duration connectTimeout=Duration.ofSeconds(10);private Duration readTimeout=Duration.ofSeconds(120);
    private int maxRepairAttempts=2;private int maxContextCapabilities=100;private int maxContextBytes=1500000;
    public boolean isEnabled(){return enabled;}public void setEnabled(boolean v){enabled=v;}public String getProvider(){return provider;}public void setProvider(String v){provider=v;}
    public String getBaseUrl(){return baseUrl;}public void setBaseUrl(String v){baseUrl=v;}public String getApiKey(){return apiKey;}public void setApiKey(String v){apiKey=v;}public String getModel(){return model;}public void setModel(String v){model=v;}
    public double getTemperature(){return temperature;}public void setTemperature(double v){temperature=v;}public int getMaxOutputTokens(){return maxOutputTokens;}public void setMaxOutputTokens(int v){maxOutputTokens=v;}
    public Duration getConnectTimeout(){return connectTimeout;}public void setConnectTimeout(Duration v){connectTimeout=v;}public Duration getReadTimeout(){return readTimeout;}public void setReadTimeout(Duration v){readTimeout=v;}
    public int getMaxRepairAttempts(){return maxRepairAttempts;}public void setMaxRepairAttempts(int v){maxRepairAttempts=v;}public int getMaxContextCapabilities(){return maxContextCapabilities;}public void setMaxContextCapabilities(int v){maxContextCapabilities=v;}
    public int getMaxContextBytes(){return maxContextBytes;}public void setMaxContextBytes(int v){maxContextBytes=v;}
}
