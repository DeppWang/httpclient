package wang.depp.httpclient;

import org.apache.http.conn.socket.ConnectionSocketFactory;
import wang.depp.httpclient.exception.ConnectTimeoutException;
import wang.depp.httpclient.exception.ConnectionRequestTimeoutException;
import wang.depp.httpclient.exception.HttpStatusException;
import wang.depp.httpclient.exception.ReadTimeoutException;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpClientHelper {
    private static final Logger log = LoggerFactory.getLogger(HttpClientHelper.class);
    private static final ConcurrentMap<String, KeyStoreEntry> keyStoreMap = new ConcurrentHashMap();
    private static HttpClient httpClient = null;
    private static final HttpClientHelperConfig config = new HttpClientHelperConfig();
    private static HttpClientHelper.IdleConnMonitor connMonitor;
    private static volatile boolean shutdown;

    public HttpClientHelper() {
    }

    private static synchronized void destroy() {
        if (config.getShutdownDelay() > 0) {
            try {
                Thread.sleep((long) config.getShutdownDelay());
            } catch (InterruptedException var1) {
            }
        }

        shutdown = true;
        if (httpClient != null && httpClient.getConnectionManager() != null) {
            httpClient.getConnectionManager().shutdown();
            log.info("[HttpClientHelper] httpClient.connectionManager shutdown");
        }

        log.info("[HttpClientHelper] destroyed");
    }

    public static synchronized void addKeyStore(String alias, KeyStore keyStore, char[] keyPassword) throws Exception {
        HttpClientHelper.KeyStoreEntry old = (HttpClientHelper.KeyStoreEntry) keyStoreMap.putIfAbsent(alias, new HttpClientHelper.KeyStoreEntry(keyStore, keyPassword));
        if (old == null) {
            recreateHttpClient();
        }

    }

    private static void recreateHttpClient() throws Exception {
        createHttpClient();
    }

    private static void createHttpClient() throws Exception {
        long start = System.currentTimeMillis();
        HttpClientHelper.IdleConnMonitor oldConnMonitor = connMonitor;
        HttpClientConnectionManager connMgr = getConnectionManager();
        HttpClientHelper.IdleConnMonitor monitor = new HttpClientHelper.IdleConnMonitor(connMgr);
        monitor.start();
        CloseableHttpClient client = HttpClientBuilder.create().setConnectionManager(connMgr).setRetryHandler(config.isEnableAutoRetry() ? getRetryHandler() : null).setKeepAliveStrategy(getKeepAliveStrategy()).build();
        httpClient = client;
        connMonitor = monitor;
        if (oldConnMonitor != null) {
            oldConnMonitor.shutdown();
            final HttpClientConnectionManager f = oldConnMonitor.getConnectionManager();
            (new Timer(true)).schedule(new TimerTask() {
                public void run() {
                    f.shutdown();
                }
            }, (long) config.getIdleConnMonitorShutdownDelay());
        }

        log.info("[perf] HttpClientHelper.createHttpClient {}", System.currentTimeMillis() - start);
    }

    private static HttpClientConnectionManager getConnectionManager() throws Exception {
        long start = System.currentTimeMillis();
        SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
        sslContextBuilder.loadTrustMaterial(null, (chain, authType) -> {
            return true;
        });
        Iterator var3 = keyStoreMap.values().iterator();

        while (var3.hasNext()) {
            HttpClientHelper.KeyStoreEntry keyStoreEntry = (HttpClientHelper.KeyStoreEntry) var3.next();
            sslContextBuilder.loadKeyMaterial(keyStoreEntry.keyStore, keyStoreEntry.keyPassword);
        }

        SSLContext sslContext = sslContextBuilder.build();
        Registry socketFactoryRegistry = RegistryBuilder.create().register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE)).build();
        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connMgr.setMaxTotal(config.getPoolMaxConn());
        connMgr.setDefaultMaxPerRoute(config.getPoolDefaultMaxConnPerRouter());
        log.info("[perf] HttpClientHelper.getConnectionManager {}", System.currentTimeMillis() - start);
        return connMgr;
    }

    private static HttpRequestRetryHandler getRetryHandler() {
        return new DefaultHttpRequestRetryHandler(config.getAutoRetryTimes(), true);
    }

    private static ConnectionKeepAliveStrategy getKeepAliveStrategy() {
        long start = System.currentTimeMillis();
        ConnectionKeepAliveStrategy connectionKeepAliveStrategy = new ConnectionKeepAliveStrategy() {
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                BasicHeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator("Keep-Alive"));

                while (true) {
                    String param;
                    String value;
                    do {
                        do {
                            if (!it.hasNext()) {
                                HttpClientHelper.log.info("not found available keep-alive-timeout in response, so use default {} ms", HttpClientHelper.config.getMaxKeepAliveTime());
                                return (long) HttpClientHelper.config.getMaxKeepAliveTime();
                            }

                            HeaderElement he = it.nextElement();
                            param = he.getName();
                            value = he.getValue();
                        } while (value == null);
                    } while (!param.equalsIgnoreCase("timeout"));

                    try {
                        return Long.parseLong(value) * 1000L;
                    } catch (NumberFormatException var8) {
                        HttpClientHelper.log.warn("getKeepAliveStrategy parse timeout error", var8);
                    }
                }
            }
        };
        log.info("[perf] HttpClientHelper.getKeepAliveStrategy {}", System.currentTimeMillis() - start);
        return connectionKeepAliveStrategy;
    }

    public static String get(String url, Map<String, String> params, HttpClientRequestConfig httpClientRequestConfig) throws ConnectTimeoutException, ReadTimeoutException, ConnectionRequestTimeoutException, HttpStatusException {
        HttpGet httpGet;
        if (params != null && !params.isEmpty()) {
            try {
                URIBuilder uriBuilder = new URIBuilder(url);
                params.forEach((name, value) -> {
                    uriBuilder.addParameter(name, value);
                });
                httpGet = new HttpGet(uriBuilder.build());
            } catch (URISyntaxException var5) {
                throw new RuntimeException(var5);
            }
        } else {
            httpGet = new HttpGet(url);
        }

        httpGet.setConfig(httpClientRequestConfig.toRequestConfig());
        return launchRequest(httpGet, httpClientRequestConfig);
    }

    public static String get(String url, MultiValuedMap<String, String> params, HttpClientRequestConfig httpClientRequestConfig) throws ConnectTimeoutException, ReadTimeoutException, ConnectionRequestTimeoutException, HttpStatusException {
        HttpGet httpGet;
        if (params != null && !params.isEmpty()) {
            try {
                URIBuilder uriBuilder = new URIBuilder(url);
                MapIterator iter = params.mapIterator();

                while (iter.hasNext()) {
                    iter.next();
                    uriBuilder.addParameter((String) iter.getKey(), (String) iter.getValue());
                }

                httpGet = new HttpGet(uriBuilder.build());
            } catch (URISyntaxException var6) {
                throw new RuntimeException(var6);
            }
        } else {
            httpGet = new HttpGet(url);
        }

        httpGet.setConfig(httpClientRequestConfig.toRequestConfig());
        return launchRequest(httpGet, httpClientRequestConfig);
    }

    public static String get(String url, HttpClientRequestConfig httpClientRequestConfig) throws ConnectTimeoutException, ReadTimeoutException, ConnectionRequestTimeoutException, HttpStatusException {
        return get(url, (Map) null, httpClientRequestConfig);
    }

    public static String get(String url) throws ConnectTimeoutException, ReadTimeoutException, ConnectionRequestTimeoutException, HttpStatusException {
        return get(url, HttpClientRequestConfig.newInstance());
    }

    public static String post(String url, HttpClientRequestConfig httpClientRequestConfig) throws ConnectTimeoutException, ReadTimeoutException, ConnectionRequestTimeoutException, HttpStatusException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(httpClientRequestConfig.toRequestConfig());
        return launchRequest(httpPost, httpClientRequestConfig);
    }

    public static String postForm(String url, Map<String, ?> form, HttpClientRequestConfig httpClientRequestConfig) throws ConnectTimeoutException, ReadTimeoutException, ConnectionRequestTimeoutException, HttpStatusException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(httpClientRequestConfig.toRequestConfig());
        List<NameValuePair> nameValuePairList = new ArrayList<>();
        if (form != null && !form.isEmpty()) {
            form.forEach((name, value) -> {
                if (value != null) {
                    nameValuePairList.add(new BasicNameValuePair(name, value.toString()));
                }

            });
        }

        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairList, httpClientRequestConfig.charset()));
        return launchRequest(httpPost, httpClientRequestConfig);
    }

    public static String postForm(String url, Map<String, ?> form) throws ConnectTimeoutException, ReadTimeoutException, ConnectionRequestTimeoutException, HttpStatusException {
        return postForm(url, form, HttpClientRequestConfig.newInstance());
    }

    public static String postJSON(String url, String json, HttpClientRequestConfig httpClientRequestConfig) throws ConnectTimeoutException, ReadTimeoutException, ConnectionRequestTimeoutException, HttpStatusException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(httpClientRequestConfig.toRequestConfig());
        StringEntity stringEntity = new StringEntity(json, ContentType.create("application/json", httpClientRequestConfig.charset()));
        httpPost.setEntity(stringEntity);
        return launchRequest(httpPost, httpClientRequestConfig);
    }

    public static String postJSON(String url, String json) throws ConnectTimeoutException, ReadTimeoutException, ConnectionRequestTimeoutException, HttpStatusException {
        return postJSON(url, json, HttpClientRequestConfig.newInstance());
    }

    private static String launchRequest(HttpUriRequest httpRequest, HttpClientRequestConfig httpClientRequestConfig) throws ConnectTimeoutException, ReadTimeoutException, ConnectionRequestTimeoutException, HttpStatusException {
        long start = System.currentTimeMillis();
        log.info("using HttpClientRequestConfig {}", httpClientRequestConfig);
        if (httpClientRequestConfig != null) {
            Map<String, String> headers = httpClientRequestConfig.headers();
            if (!headers.isEmpty()) {
                headers.forEach((name, value) -> {
                    httpRequest.addHeader(name, value);
                });
            }
        }

        HttpResponse response = null;

        String var11;
        try {
            HttpClient client = httpClient;
            log.info("[perf] HttpClientHelper.launchRequest prepare {}", System.currentTimeMillis() - start);
            response = client.execute(httpRequest, HttpClientContext.create());
            long innerStart = System.currentTimeMillis();
            int httpStatus = response.getStatusLine().getStatusCode();
            if (httpClientRequestConfig.expectedStatus().isEmpty()) {
                if (httpStatus != 200) {
                    throw new HttpStatusException(httpStatus, response.getStatusLine().getReasonPhrase());
                }
            } else if (!httpClientRequestConfig.expectedStatus().contains(httpStatus)) {
                throw new HttpStatusException(httpStatus, response.getStatusLine().getReasonPhrase());
            }

            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity, httpClientRequestConfig.charset());
            log.info("[perf] HttpClientHelper.launchRequest after {}", System.currentTimeMillis() - innerStart);
            log.info("[perf] HttpClientHelper.launchRequest elapsed {}", System.currentTimeMillis() - start);
            var11 = responseBody;
        } catch (ConnectionPoolTimeoutException var23) {
            throw new ConnectionRequestTimeoutException(var23.getMessage());
        } catch (org.apache.http.conn.ConnectTimeoutException var24) {
            throw new ConnectTimeoutException(var24.getMessage());
        } catch (SocketTimeoutException var25) {
            throw new ReadTimeoutException(var25.getMessage());
        } catch (IOException var26) {
            throw new RuntimeException(var26);
        } finally {
            try {
                if (response != null) {
                    EntityUtils.consume(response.getEntity());
                }
            } catch (IOException var22) {
                log.error("EntityUtils.consume error", var22);
            }

        }

        return var11;
    }

    static {
        log.info("using HttpClientHelperConfig {}", config);

        try {
            createHttpClient();
        } catch (Exception var1) {
            throw new RuntimeException(var1);
        }

        log.info("[HttpClientHelper] preloaded");
    }

    private static class KeyStoreEntry {
        private KeyStore keyStore;
        private char[] keyPassword;

        KeyStoreEntry(KeyStore keyStore, char[] keyPassword) {
            this.keyStore = keyStore;
            this.keyPassword = keyPassword;
        }
    }

    private static class IdleConnMonitor extends Thread {
        private static final AtomicInteger counter = new AtomicInteger();
        private final HttpClientConnectionManager cm;
        private volatile boolean running;

        public IdleConnMonitor(HttpClientConnectionManager connMgr) {
            super("IdleConnMonitorThread-" + counter.getAndIncrement());
            this.setDaemon(true);
            this.cm = connMgr;
            this.running = true;
        }

        public void run() {
            while (this.running && !HttpClientHelper.shutdown) {
                try {
                    synchronized (this) {
                        this.wait((long) HttpClientHelper.config.getIdleConnCheckInterval());
                        this.cm.closeExpiredConnections();
                        this.cm.closeIdleConnections((long) HttpClientHelper.config.getMaxKeepAliveTime(), TimeUnit.MILLISECONDS);
                    }
                } catch (InterruptedException var4) {
                    HttpClientHelper.log.info("IdleConnMonitor interrupted");
                } catch (Throwable var5) {
                    HttpClientHelper.log.error("IdleConnMonitor error", var5);
                }
            }

        }

        public void shutdown() {
            this.running = false;
            this.interrupt();
        }

        public HttpClientConnectionManager getConnectionManager() {
            return this.cm;
        }
    }
}
