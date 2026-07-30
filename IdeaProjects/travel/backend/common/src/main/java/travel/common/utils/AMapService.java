package travel.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 高德地图服务类
 */
@Slf4j
@Service
public class AMapService {

    @Value("${amap.api-key}")
    private String apiKey;

    @Value("${amap.api-url:https://restapi.amap.com/v3}")
    private String apiUrl;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AICacheManager cacheManager;

    public AMapService(AICacheManager cacheManager) {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
        this.cacheManager = cacheManager;
    }

    /**
     * 获取天气信息（通过城市编码）- 带缓存
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getWeather(String cityCode) {
        return (Map<String, Object>) cacheManager.getOrSetWeatherCache(cityCode, () -> {
            try {
                String url = String.format("%s/weather/weatherInfo?city=%s&key=%s&extensions=base",
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
                            JsonNode lives = jsonNode.get("lives").get(0);
                            Map<String, Object> weather = new HashMap<>();
                            weather.put("city", lives.get("city").asText());
                            weather.put("weather", lives.get("weather").asText());
                            weather.put("temperature", lives.get("temperature").asInt());
                            weather.put("winddirection", lives.get("winddirection").asText());
                            weather.put("windpower", lives.get("windpower").asText());
                            weather.put("humidity", lives.get("humidity").asText());
                            log.info("从高德API获取天气成功: cityCode={}", cityCode);
                            return weather;
                        }
                    }
                }
            } catch (IOException e) {
                log.error("获取天气信息失败: {}", e.getMessage(), e);
            }
            return null;
        });
    }

    /**
     * 根据经纬度获取天气信息 - 带缓存
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getWeatherByLocation(String location) {
        try {
            // 先通过逆地理编码获取城市编码（带缓存）
            String[] coords = location.split(",");
            double lng = Double.parseDouble(coords[0]);
            double lat = Double.parseDouble(coords[1]);

            Map<String, Object> geoResult = reverseGeocode(lng, lat);
            if (geoResult != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> addressComponent = (Map<String, Object>) geoResult.get("addressComponent");
                if (addressComponent != null && addressComponent.get("adcode") != null) {
                    String adcode = addressComponent.get("adcode").toString();
                    return getWeather(adcode);
                }
            }
        } catch (Exception e) {
            log.error("根据位置获取天气失败: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 驾车路径规划 - 带缓存
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> drivingRoute(double originLng, double originLat,
                                            double destLng, double destLat) {
        return (Map<String, Object>) cacheManager.getOrSetRouteCache(
                originLng, originLat, destLng, destLat, "driving", () -> {
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
                                    result.put("distance", route.get("distance").asInt());
                                    result.put("duration", route.get("duration").asInt());
                                    result.put("steps", route.get("steps"));
                                    log.info("从高德API获取驾车路线成功");
                                    return result;
                                }
                            }
                        }
                    } catch (IOException e) {
                        log.error("驾车路径规划失败: {}", e.getMessage(), e);
                    }
                    return null;
                });
    }

    /**
     * 步行路径规划 - 带缓存
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> walkingRoute(double originLng, double originLat,
                                            double destLng, double destLat) {
        return (Map<String, Object>) cacheManager.getOrSetRouteCache(
                originLng, originLat, destLng, destLat, "walking", () -> {
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
                                    result.put("distance", route.get("distance").asInt());
                                    result.put("duration", route.get("duration").asInt());
                                    result.put("steps", route.get("steps"));
                                    log.info("从高德API获取步行路线成功");
                                    return result;
                                }
                            }
                        }
                    } catch (IOException e) {
                        log.error("步行路径规划失败: {}", e.getMessage(), e);
                    }
                    return null;
                });
    }

    /**
     * 公共交通路径规划 - 带缓存
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> transitRoute(double originLng, double originLat,
                                            double destLng, double destLat, String city) {
        return (Map<String, Object>) cacheManager.getOrSetRouteCache(
                originLng, originLat, destLng, destLat, "transit_" + city, () -> {
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
                                    result.put("distance", route.get("distance").asInt());
                                    result.put("duration", route.get("duration").asInt());
                                    result.put("cost", route.get("cost").asText());
                                    log.info("从高德API获取公交路线成功");
                                    return result;
                                }
                            }
                        }
                    } catch (IOException e) {
                        log.error("公共交通路径规划失败: {}", e.getMessage(), e);
                    }
                    return null;
                });
    }

    /**
     * 地点搜索 - 带缓存
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> searchPlaces(String keywords, String city, int page) {
        return (Map<String, Object>) cacheManager.getOrSetPlaceCache(keywords, city, page, () -> {
            try {
                String url = String.format("%s/place/text?keywords=%s&city=%s&page=%d&key=%s",
                        apiUrl, keywords, city, page, apiKey);

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JsonNode jsonNode = objectMapper.readTree(responseBody);

                        if ("1".equals(jsonNode.get("status").asText())) {
                            Map<String, Object> result = new HashMap<>();
                            result.put("count", jsonNode.get("count").asInt());
                            result.put("pois", jsonNode.get("pois"));
                            log.info("从高德API搜索地点成功: keywords={}, city={}", keywords, city);
                            return result;
                        }
                    }
                }
            } catch (IOException e) {
                log.error("地点搜索失败: {}", e.getMessage(), e);
            }
            return null;
        });
    }

    /**
     * 获取实时交通状况 - 带缓存
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getTrafficStatus(String cityCode) {
        return (Map<String, Object>) cacheManager.getOrSetTrafficCache(cityCode, () -> {
            try {
                String url = String.format("%s/traffic/status/road?city=%s&key=%s",
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
                            Map<String, Object> result = new HashMap<>();
                            result.put("trafficinfo", jsonNode.get("trafficinfo"));
                            log.info("从高德API获取交通状况成功: cityCode={}", cityCode);
                            return result;
                        }
                    }
                }
            } catch (IOException e) {
                log.error("获取交通状况失败: {}", e.getMessage(), e);
            }
            return null;
        });
    }

    /**
     * 逆地理编码（经纬度转地址）- 带缓存
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> reverseGeocode(double longitude, double latitude) {
        String locationKey = String.format("%.6f,%.6f", longitude, latitude);
        return (Map<String, Object>) cacheManager.getOrSetPlaceCache(locationKey, "geo", 1, () -> {
            try {
                String url = String.format("%s/geocode/regeo?location=%f,%f&key=%s&extensions=base",
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
                            result.put("formattedAddress", regeocode.get("formattedAddress").asText());
                            result.put("addressComponent", regeocode.get("addressComponent"));
                            log.info("从高德API逆地理编码成功");
                            return result;
                        }
                    }
                }
            } catch (IOException e) {
                log.error("逆地理编码失败: {}", e.getMessage(), e);
            }
            return null;
        });
    }

    /**
     * 获取景点周边设施 - 带缓存
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getNearbyPlaces(double longitude, double latitude,
                                               String types, int radius) {
        String locationKey = String.format("%.6f,%.6f_%s_%d", longitude, latitude, types, radius);
        return (Map<String, Object>) cacheManager.getOrSetPlaceCache(locationKey, "nearby", 1, () -> {
            try {
                String url = String.format("%s/place/around?location=%f,%f&types=%s&radius=%d&key=%s",
                        apiUrl, longitude, latitude, types, radius, apiKey);

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JsonNode jsonNode = objectMapper.readTree(responseBody);

                        if ("1".equals(jsonNode.get("status").asText())) {
                            Map<String, Object> result = new HashMap<>();
                            result.put("count", jsonNode.get("count").asInt());
                            result.put("pois", jsonNode.get("pois"));
                            log.info("从高德API获取周边设施成功");
                            return result;
                        }
                    }
                }
            } catch (IOException e) {
                log.error("获取周边设施失败: {}", e.getMessage(), e);
            }
            return null;
        });
    }

}
