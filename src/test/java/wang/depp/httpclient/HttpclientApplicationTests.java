package wang.depp.httpclient;

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
    public void testPost() throws Exception {
        Map<String, Object> form = new HashMap<>();
        form.put("userId", USER_ID);
        form.put("address", "3/151 Hartley Road, Smeaton Grange, New South Wales, Australia");
        form.put("unitNumber", "3");
        form.put("streetNumber", "151");
        form.put("streetName", "Hartley");
        form.put("streetType", "Rd");
        form.put("suburb", "Smeaton Grange");
        form.put("state", "NSW");
        form.put("postcode", "2567");

        String url = URL + "/profile/v1/address/update";
        String result = HttpClientHelper.postForm(url, form, addHeader());
        logger.info("[result: {}]", result);
    }

    public HttpClientRequestConfig addHeader() {
        HttpClientRequestConfig config = HttpClientRequestConfig.newInstance();
        config.addHeader("forward-user", USER_ID);
        return config;
    }
}
