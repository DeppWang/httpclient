用于像 Postman 一样测试接口，无需像传统测试时，启动 Spirng 来调用 Service

## 使用 

### Get

```Java
	private final String URL = "https://depp.wang";

	@Test
    void testGet() throws Exception {
        String result = HttpClientHelper.get(URL);
        logger.info("[result: {}]", result);
    }
```

### Post

- application/x-www-form-urlencoded

```Java
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
```

- application/json

```Java
    @Test
    @DisplayName("post application/json")
    public void testPostJson() throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.set("pid", "31502");

        String url = URL + "/profile/v1/address/update";
        String result = HttpClientHelper.postJSON(url, jsonObject.toString());
        logger.info("[result: {}]", result);
    }
```

## 原理

本质上还是使用 [HttpClient](https://hc.apache.org/httpcomponents-client-4.5.x/httpclient/apidocs/org/apache/http/client/HttpClient.html) 来实现的

## 打 jar 包

```
mvn clean package
```

