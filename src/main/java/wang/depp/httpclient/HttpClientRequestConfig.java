package wang.depp.httpclient;

import org.apache.http.client.config.RequestConfig;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class HttpClientRequestConfig {
    public static final Charset DEFAULT_CHARSET;
    public static final int DEFAULT_CONN_TIMEOUT = 60000;
    public static final int DEFAULT_READ_TIMEOUT = 60000;
    public static final int DEFAULT_CONN_REQUEST_TIMEOUT = 10000;
    private Charset charset;
    private int connTimeout = 60000;
    private int readTimeout = 60000;
    private int connRequestTimeout = 10000;
    private Map<String, String> headers;
    private Set<Integer> expectedStatus;

    public String toString() {
        return "HttpClientRequestConfig{charset=" + this.charset + ", connTimeout=" + this.connTimeout + ", readTimeout=" + this.readTimeout + ", connRequestTimeout=" + this.connRequestTimeout + ", headers=" + this.headers + ", expectedStatus=" + this.expectedStatus + '}';
    }

    private HttpClientRequestConfig() {
        this.charset = DEFAULT_CHARSET;
        this.headers = new LinkedHashMap();
        this.expectedStatus = new LinkedHashSet();
    }

    public static HttpClientRequestConfig newInstance() {
        return new HttpClientRequestConfig();
    }

    public HttpClientRequestConfig charset(Charset charset) {
        if (charset == null) {
            throw new IllegalArgumentException("arg cant be null");
        } else {
            this.charset = charset;
            return this;
        }
    }

    public HttpClientRequestConfig connTimeout(int connTimeout) {
        this.connTimeout = connTimeout;
        return this;
    }

    public HttpClientRequestConfig readTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public HttpClientRequestConfig connRequestTimeout(int connRequestTimeout) {
        this.connRequestTimeout = connRequestTimeout;
        return this;
    }

    public HttpClientRequestConfig addHeader(String header, String value) {
        this.headers.put(header, value);
        return this;
    }

    public HttpClientRequestConfig addExpectedStatus(int status) {
        this.expectedStatus.add(status);
        return this;
    }

    RequestConfig toRequestConfig() {
        RequestConfig.Builder builder = RequestConfig.custom().setConnectTimeout(this.connTimeout()).setSocketTimeout(this.readTimeout()).setConnectionRequestTimeout(this.connRequestTimeout());
        return builder.build();
    }

    Charset charset() {
        return this.charset;
    }

    int connTimeout() {
        return this.connTimeout;
    }

    int readTimeout() {
        return this.readTimeout;
    }

    int connRequestTimeout() {
        return this.connRequestTimeout;
    }

    Map<String, String> headers() {
        return this.headers;
    }

    Set<Integer> expectedStatus() {
        return this.expectedStatus;
    }

    static {
        DEFAULT_CHARSET = StandardCharsets.UTF_8;
    }
}
