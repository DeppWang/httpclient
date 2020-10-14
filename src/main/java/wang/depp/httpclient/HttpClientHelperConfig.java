package wang.depp.httpclient;

import org.apache.commons.lang3.StringUtils;

class HttpClientHelperConfig implements HttpClientHelperConfigMXBean {
    private static final int DEFAULT_RETRY_TIMES = 3;
    private static final int DEFAULT_POOL_MAX_CONN = 500;
    private static final int DEFAULT_POOL_MAX_CONN_PER_ROUTER = 100;
    private static final int DEFAULT_MAX_KEEP_ALIVE_TIME = 30000;
    private static final int DEFAULT_IDLE_CONN_CHECK_INTERVAL = 5000;
    private static final int DEFAULT_IDLE_CONN_MONITOR_SHUTDOWN_DELAY = 120000;
    private int poolMaxConn;
    private int poolDefaultMaxConnPerRouter;
    private int maxKeepAliveTime;
    private int idleConnCheckInterval;
    private int shutdownDelay;
    private boolean enableAutoRetry;
    private int autoRetryTimes;
    private int idleConnMonitorShutdownDelay;

    HttpClientHelperConfig() {
        String property = System.getProperty("HttpClientHelperConfig.pool-max-conn");
        this.poolMaxConn = StringUtils.isBlank(property) ? 500 : Integer.parseInt(property);
        property = System.getProperty("HttpClientHelperConfig.pool-default-max-conn-per-router");
        this.poolDefaultMaxConnPerRouter = StringUtils.isBlank(property) ? 100 : Integer.parseInt(property);
        property = System.getProperty("HttpClientHelperConfig.max-keep-alive-time");
        this.maxKeepAliveTime = StringUtils.isBlank(property) ? 30000 : Integer.parseInt(property);
        property = System.getProperty("HttpClientHelperConfig.idle-conn-check-interval");
        this.idleConnCheckInterval = StringUtils.isBlank(property) ? 5000 : Integer.parseInt(property);
        property = System.getProperty("HttpClientHelperConfig.shutdown-delay");
        this.shutdownDelay = StringUtils.isBlank(property) ? 0 : Integer.parseInt(property);
        property = System.getProperty("HttpClientHelperConfig.enable-auto-retry");
        this.enableAutoRetry = StringUtils.isBlank(property) ? false : Boolean.parseBoolean(property);
        property = System.getProperty("HttpClientHelperConfig.auto-retry-times");
        this.autoRetryTimes = StringUtils.isBlank(property) ? 3 : Integer.parseInt(property);
        property = System.getProperty("HttpClientHelperConfig.idle-conn-monitor-shutdown-delay");
        this.idleConnMonitorShutdownDelay = StringUtils.isBlank(property) ? 120000 : Integer.parseInt(property);
    }

    public String toString() {
        return "HttpClientHelperConfig{poolMaxConn=" + this.poolMaxConn + ", poolDefaultMaxConnPerRouter=" + this.poolDefaultMaxConnPerRouter + ", maxKeepAliveTime=" + this.maxKeepAliveTime + ", idleConnCheckInterval=" + this.idleConnCheckInterval + ", shutdownDelay=" + this.shutdownDelay + ", enableAutoRetry=" + this.enableAutoRetry + ", autoRetryTimes=" + this.autoRetryTimes + ", idleConnMonitorShutdownDelay=" + this.idleConnMonitorShutdownDelay + '}';
    }

    public int getPoolMaxConn() {
        return this.poolMaxConn;
    }

    public int getPoolDefaultMaxConnPerRouter() {
        return this.poolDefaultMaxConnPerRouter;
    }

    public int getMaxKeepAliveTime() {
        return this.maxKeepAliveTime;
    }

    public int getIdleConnCheckInterval() {
        return this.idleConnCheckInterval;
    }

    public int getIdleConnMonitorShutdownDelay() {
        return this.idleConnMonitorShutdownDelay;
    }

    public int getShutdownDelay() {
        return this.shutdownDelay;
    }

    public boolean isEnableAutoRetry() {
        return this.enableAutoRetry;
    }

    public int getAutoRetryTimes() {
        return this.autoRetryTimes;
    }
}

