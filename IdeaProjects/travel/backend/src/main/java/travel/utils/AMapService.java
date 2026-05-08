package travel.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class AMapService {

    private static final Logger log = LoggerFactory.getLogger(AMapService.class);

    @Value("${amap.api-key}")
    private String apiKey;

    @Value("${amap.api-url:https://restapi.amap.com/v3}")
    private String apiUrl;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 地理编码：地址转经纬度
     */
    public Map<String, Object> geocode(String address) {
        try {
            String url = String.format("%s/geocode/geo?address=%s&key=%s",
                    apiUrl, address, apiKey);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);

                    if ("1".equals(jsonNode.get("status").asText())) {
                        JsonNode geocodes = jsonNode.get("geocodes");
                        if (geocodes.isArray() && geocodes.size() > 0) {
                            JsonNode location = geocodes.get(0).get("location");
                            String[] coords = location.asText().split(",");

                            Map<String, Object> result = new HashMap<>();
                            result.put("longitude", Double.parseDouble(coords[0]));
                            result.put("latitude", Double.parseDouble(coords[1]));
                            result.put("formattedAddress", geocodes.get(0).get("formatted_address").asText());
                            return result;
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("地理编码失败: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 逆地理编码：经纬度转地址
     */
    public Map<String, Object> reverseGeocode(double longitude, double latitude) {
        try {
            String url = String.format("%s/geocode/regeo?location=%f,%f&key=%s",
                    apiUrl, longitude, latitude, apiKey);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);

                    if ("1".equals(jsonNode.get("status").asText())) {
                        JsonNode regeocode = jsonNode.get("regeocode");
                        Map<String, Object> result = new HashMap<>();
                        result.put("formattedAddress", regeocode.get("formatted_address").asText());
                        result.put("addressComponent", regeocode.get("addressComponent"));
                        return result;
                    }
                }
            }
        } catch (IOException e) {
            log.error("逆地理编码失败: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 路径规划：驾车路线
     */
    public Map<String, Object> drivingRoute(double originLng, double originLat,
                                            double destLng, double destLat) {
        try {
            String url = String.format("%s/direction/driving?origin=%f,%f&destination=%f,%f&key=%s",
                    apiUrl, originLng, originLat, destLng, destLat, apiKey);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);

                    if ("1".equals(jsonNode.get("status").asText())) {
                        JsonNode route = jsonNode.get("route").get("paths").get(0);

                        Map<String, Object> result = new HashMap<>();
                        result.put("distance", route.get("distance").asDouble());
                        result.put("duration", route.get("duration").asInt());
                        result.put("steps", route.get("steps"));
                        return result;
                    }
                }
            }
        } catch (IOException e) {
            log.error("路径规划失败: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 步行路径规划
     */
    public Map<String, Object> walkingRoute(double originLng, double originLat,
                                            double destLng, double destLat) {
        try {
            String url = String.format("%s/direction/walking?origin=%f,%f&destination=%f,%f&key=%s",
                    apiUrl, originLng, originLat, destLng, destLat, apiKey);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);

                    if ("1".equals(jsonNode.get("status").asText())) {
                        JsonNode route = jsonNode.get("route").get("paths").get(0);

                        Map<String, Object> result = new HashMap<>();
                        result.put("distance", route.get("distance").asDouble());
                        result.put("duration", route.get("duration").asInt());
                        result.put("steps", route.get("steps"));
                        return result;
                    }
                }
            }
        } catch (IOException e) {
            log.error("步行路径规划失败: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 公交路径规划
     */
    public Map<String, Object> transitRoute(double originLng, double originLat,
                                            double destLng, double destLat, String city) {
        try {
            String url = String.format("%s/direction/transit/integrated?origin=%f,%f&destination=%f,%f&city=%s&key=%s",
                    apiUrl, originLng, originLat, destLng, destLat, city, apiKey);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);

                    if ("1".equals(jsonNode.get("status").asText())) {
                        JsonNode route = jsonNode.get("route").get("transits").get(0);

                        Map<String, Object> result = new HashMap<>();
                        result.put("distance", route.get("distance").asDouble());
                        result.put("duration", route.get("duration").asInt());
                        result.put("cost", route.get("cost").asText());
                        result.put("segments", route.get("segments"));
                        return result;
                    }
                }
            }
        } catch (IOException e) {
            log.error("公交路径规划失败: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 搜索周边POI
     */
    public List<Map<String, Object>> searchNearby(String keyword, double longitude,
                                                  double latitude, int radius) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            String url = String.format("%s/place/around?keywords=%s&location=%f,%f&radius=%d&key=%s",
                    apiUrl, keyword, longitude, latitude, radius, apiKey);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);

                    if ("1".equals(jsonNode.get("status").asText())) {
                        JsonNode pois = jsonNode.get("pois");
                        if (pois.isArray()) {
                            for (JsonNode poi : pois) {
                                Map<String, Object> item = new HashMap<>();
                                item.put("id", poi.get("id").asText());
                                item.put("name", poi.get("name").asText());
                                item.put("type", poi.get("type").asText());
                                item.put("address", poi.get("address").asText());

                                String location = poi.get("location").asText();
                                String[] coords = location.split(",");
                                item.put("longitude", Double.parseDouble(coords[0]));
                                item.put("latitude", Double.parseDouble(coords[1]));

                                item.put("distance", poi.has("distance") ? poi.get("distance").asInt() : 0);

                                results.add(item);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("周边搜索失败: {}", e.getMessage(), e);
        }
        return results;
    }

    /**
     * 计算两点间距离
     */
    public double calculateDistance(double lng1, double lat1, double lng2, double lat2) {
        try {
            String url = String.format("%s/distance?origins=%f,%f&destination=%f,%f&type=1&key=%s",
                    apiUrl, lng1, lat1, lng2, lat2, apiKey);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);

                    if ("1".equals(jsonNode.get("status").asText())) {
                        JsonNode result = jsonNode.get("results").get(0);
                        return result.get("distance").asDouble();
                    }
                }
            }
        } catch (IOException e) {
            log.error("距离计算失败: {}", e.getMessage(), e);
        }
        return 0;
    }

    /**
     * 获取实时天气
     */
    public Map<String, Object> getWeather(String cityCode) {
        try {
            String url = String.format("%s/weather/weatherInfo?city=%s&key=%s&extensions=all",
                    apiUrl, cityCode, apiKey);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);

                    if ("1".equals(jsonNode.get("status").asText())) {
                        JsonNode forecast = jsonNode.get("forecasts").get(0).get("casts").get(0);

                        Map<String, Object> result = new HashMap<>();
                        result.put("date", forecast.get("date").asText());
                        result.put("week", forecast.get("week").asText());
                        result.put("dayweather", forecast.get("dayweather").asText());
                        result.put("nightweather", forecast.get("nightweather").asText());
                        result.put("daytemp", forecast.get("daytemp").asInt());
                        result.put("nighttemp", forecast.get("nighttemp").asInt());
                        result.put("daywind", forecast.get("daywind").asText());
                        result.put("nightwind", forecast.get("nightwind").asText());
                        return result;
                    }
                }
            }
        } catch (IOException e) {
            log.error("天气查询失败: {}", e.getMessage(), e);
        }
        return null;
    }
}
