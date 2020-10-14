package wang.depp.httpclient;

public interface HttpClientHelperConfigMXBean {
    int getPoolMaxConn();

    int getPoolDefaultMaxConnPerRouter();

    int getMaxKeepAliveTime();

    int getIdleConnCheckInterval();

    int getIdleConnMonitorShutdownDelay();

    int getShutdownDelay();

    boolean isEnableAutoRetry();

    int getAutoRetryTimes();
}
