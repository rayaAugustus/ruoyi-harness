package com.ruoyi.harness.adapter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="harness")
public class HarnessProperties {
    private boolean enabled=true; private long menuParentId=2100L; private final Runtime runtime=new Runtime(); private final Cache cache=new Cache(); private final Audit audit=new Audit();
    public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;} public long getMenuParentId(){return menuParentId;} public void setMenuParentId(long v){menuParentId=v;} public Runtime getRuntime(){return runtime;} public Cache getCache(){return cache;} public Audit getAudit(){return audit;}
    public static class Runtime { private String engine="graaljs"; private long maxExecutionMillis=3000; private int maxCapabilityCalls=50,maxSourceBytes=524288,maxInputBytes=262144,maxOutputBytes=1048576,maxPageNodes=2000,maxLogEvents=200,maxTableRowsInDefinition=1000,maxJsonDepth=64;
        public String getEngine(){return engine;} public void setEngine(String v){engine=v;} public long getMaxExecutionMillis(){return maxExecutionMillis;} public void setMaxExecutionMillis(long v){maxExecutionMillis=v;}
        public int getMaxCapabilityCalls(){return maxCapabilityCalls;} public void setMaxCapabilityCalls(int v){maxCapabilityCalls=v;} public int getMaxSourceBytes(){return maxSourceBytes;} public void setMaxSourceBytes(int v){maxSourceBytes=v;}
        public int getMaxInputBytes(){return maxInputBytes;} public void setMaxInputBytes(int v){maxInputBytes=v;} public int getMaxOutputBytes(){return maxOutputBytes;} public void setMaxOutputBytes(int v){maxOutputBytes=v;}
        public int getMaxPageNodes(){return maxPageNodes;} public void setMaxPageNodes(int v){maxPageNodes=v;} public int getMaxLogEvents(){return maxLogEvents;} public void setMaxLogEvents(int v){maxLogEvents=v;}
        public int getMaxTableRowsInDefinition(){return maxTableRowsInDefinition;} public void setMaxTableRowsInDefinition(int v){maxTableRowsInDefinition=v;} public int getMaxJsonDepth(){return maxJsonDepth;} public void setMaxJsonDepth(int v){maxJsonDepth=v;}}
    public static class Cache { private boolean enabled=true; public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;} }
    public static class Audit { private boolean enabled=true; public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;} }
}
