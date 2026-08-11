package travel.route.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 百度AI服务类
 */
@Service
public class BaiduAIService {

    private static final Logger log = LoggerFactory.getLogger(BaiduAIService.class);

    @Value("${ai.baidu.app-id}")
    private String appId;

    @Value("${ai.baidu.api-key}")
    private String apiKey;

    @Value("${ai.baidu.secret-key}")
    private String secretKey;

    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String SCENE_RECOGNIZE_URL = "https://aip.baidubce.com/rest/2.0/image-classify/v2/advanced_general";
    private static final String DISH_RECOGNIZE_URL = "https://aip.baidubce.com/rest/2.0/image-classify/v2/dish";
    private static final String OCR_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String accessToken;
    private long tokenExpireTime;

    public BaiduAIService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取访问令牌
     */
    private String getAccessToken() throws IOException {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return accessToken;
        }

        String url = String.format("%s?grant_type=client_credentials&client_id=%s&client_secret=%s",
                TOKEN_URL, apiKey, secretKey);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create("", MediaType.get("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                accessToken = jsonNode.get("access_token").asText();
                int expiresIn = jsonNode.get("expires_in").asInt();
                tokenExpireTime = System.currentTimeMillis() + (expiresIn - 60) * 1000L;

                log.info("百度AI AccessToken获取成功，有效期: {}秒", expiresIn);
                return accessToken;
            }
        }

        throw new IOException("获取百度AI AccessToken失败");
    }

    /**
     * 场景识别
     */
    public Map<String, Object> recognizeScene(byte[] imageData) {
        try {
            String accessToken = getAccessToken();
            String base64Image = java.util.Base64.getEncoder().encodeToString(imageData);

            RequestBody body = RequestBody.create(
                    "image=" + java.net.URLEncoder.encode(base64Image, "UTF-8"),
                    MediaType.get("application/x-www-form-urlencoded")
            );

            Request request = new Request.Builder()
                    .url(SCENE_RECOGNIZE_URL + "?access_token=" + accessToken)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);

                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("result", jsonNode.get("result"));
                    return result;
                }
            }
        } catch (IOException e) {
            log.error("场景识别失败: {}", e.getMessage(), e);
        }

        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("success", false);
        errorResult.put("error", "场景识别失败");
        return errorResult;
    }

    /**
     * 菜品识别
     */
    public Map<String, Object> recognizeDish(byte[] imageData) {
        try {
            String accessToken = getAccessToken();
            String base64Image = java.util.Base64.getEncoder().encodeToString(imageData);

            RequestBody body = RequestBody.create(
                    "image=" + java.net.URLEncoder.encode(base64Image, "UTF-8") +
                            "&top_num=5",
                    MediaType.get("application/x-www-form-urlencoded")
            );

            Request request = new Request.Builder()
                    .url(DISH_RECOGNIZE_URL + "?access_token=" + accessToken)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);

                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("result", jsonNode.get("result"));
                    return result;
                }
            }
        } catch (IOException e) {
            log.error("菜品识别失败: {}", e.getMessage(), e);
        }

        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("success", false);
        errorResult.put("error", "菜品识别失败");
        return errorResult;
    }

    /**
     * 文字识别（OCR）
     */
    public Map<String, Object> recognizeText(byte[] imageData) {
        try {
            String accessToken = getAccessToken();
            String base64Image = java.util.Base64.getEncoder().encodeToString(imageData);

            RequestBody body = RequestBody.create(
                    "image=" + java.net.URLEncoder.encode(base64Image, "UTF-8"),
                    MediaType.get("application/x-www-form-urlencoded")
            );

            Request request = new Request.Builder()
                    .url(OCR_URL + "?access_token=" + accessToken)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);

                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("words_result", jsonNode.get("words_result"));
                    result.put("words_result_num", jsonNode.get("words_result_num").asInt());
                    return result;
                }
            }
        } catch (IOException e) {
            log.error("文字识别失败: {}", e.getMessage(), e);
        }

        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("success", false);
        errorResult.put("error", "文字识别失败");
        return errorResult;
    }

    /**
     * 植物识别
     */
    public Map<String, Object> recognizePlant(byte[] imageData) {
        try {
            String accessToken = getAccessToken();
            String base64Image = java.util.Base64.getEncoder().encodeToString(imageData);

            String plantUrl = "https://aip.baidubce.com/rest/2.0/image-classify/v1/plant";

            RequestBody body = RequestBody.create(
                    "image=" + java.net.URLEncoder.encode(base64Image, "UTF-8") +
                            "&baike_num=1",
                    MediaType.get("application/x-www-form-urlencoded")
            );

            Request request = new Request.Builder()
                    .url(plantUrl + "?access_token=" + accessToken)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);

                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("result", jsonNode.get("result"));
                    return result;
                }
            }
        } catch (IOException e) {
            log.error("植物识别失败: {}", e.getMessage(), e);
        }

        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("success", false);
        errorResult.put("error", "植物识别失败");
        return errorResult;
    }

    /**
     * 动物识别
     */
    public Map<String, Object> recognizeAnimal(byte[] imageData) {
        try {
            String accessToken = getAccessToken();
            String base64Image = java.util.Base64.getEncoder().encodeToString(imageData);

            String animalUrl = "https://aip.baidubce.com/rest/2.0/image-classify/v1/animal";

            RequestBody body = RequestBody.create(
                    "image=" + java.net.URLEncoder.encode(base64Image, "UTF-8") +
                            "&top_num=1&baike_num=1",
                    MediaType.get("application/x-www-form-urlencoded")
            );

            Request request = new Request.Builder()
                    .url(animalUrl + "?access_token=" + accessToken)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);

                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("result", jsonNode.get("result"));
                    return result;
                }
            }
        } catch (IOException e) {
            log.error("动物识别失败: {}", e.getMessage(), e);
        }

        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("success", false);
        errorResult.put("error", "动物识别失败");
        return errorResult;
    }
}
