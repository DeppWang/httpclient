package wang.depp.httpclient;

import cn.hutool.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class HttpclientApplicationTests {
    final Logger logger = LoggerFactory.getLogger(getClass());
    private final String URL = "https://depp.wang";
    private final String USER_ID = "123456";

    @Test
    void testGet() throws Exception {
        String result = HttpClientHelper.get(URL);
        logger.info("[result: {}]", result);
    }

    @Test
    void testGetAddHeader() throws Exception {
        String result = HttpClientHelper.get(URL, addHeader());
        logger.info("[result: {}]", result);
    }

    @Test
    @DisplayName("post application/x-www-form-urlencoded")
    public void testPostForm() throws Exception {
        Map<String, Object> form = new HashMap<>();
        form.put("userId", USER_ID);
        form.put("address", "3/151 Hartley Road, Smeaton Grange, New South Wales, Australia");

        String url = URL + "/profile/v1/address/update";
        String result = HttpClientHelper.postForm(url, form);
        logger.info("[result: {}]", result);
    }

    @Test
    @DisplayName("post application/json")
    public void testPostJson() throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.set("pid", "31502");

        String url = URL + "/profile/v1/address/update";
        String result = HttpClientHelper.postJSON(url, jsonObject.toString());
        logger.info("[result: {}]", result);
    }

    public HttpClientRequestConfig addHeader() {
        HttpClientRequestConfig config = HttpClientRequestConfig.newInstance();
        config.addHeader("forward-user", USER_ID);
        return config;
    }
}
