package travel.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 高德地图路径规划服务
 * 提供真实的距离、时间、成本计算
 */
@Slf4j
@Component
public class AMapRouteService {

    private static final Pattern API_KEY_QUERY_PATTERN =
            Pattern.compile("(?i)([?&]key=)[^&\\s]+");

    @Value("${amap.api-key}")
    private String apiKey;

    @Value("${amap.api-url:https://restapi.amap.com/v3}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final long maxResponseBytes;
    private final ExternalCallBulkheadRegistry bulkheadRegistry;

    public AMapRouteService(
            @Value("${amap.route.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${amap.route.read-timeout-ms:5000}") int readTimeoutMs,
            @Value("${travel.external.max-response-bytes:1048576}") long maxResponseBytes,
            ExternalCallBulkheadRegistry bulkheadRegistry) {
        if (connectTimeoutMs <= 0 || readTimeoutMs <= 0) {
            throw new IllegalArgumentException("高德地图 HTTP 超时必须为正数");
        }
        if (maxResponseBytes <= 0) {
            throw new IllegalArgumentException("外部 HTTP 响应体上限必须为正数");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        this.restTemplate = new RestTemplate(requestFactory);
        this.maxResponseBytes = maxResponseBytes;
        this.bulkheadRegistry = bulkheadRegistry;
    }

    /**
     * 批量获取多个点之间的路径信息
     * @param waypoints 途经点列表 [经度,纬度]
     * @return 路径详情
     */
    public RouteInfo calculateMultiPointRoute(List<double[]> waypoints) {
        if (waypoints == null || waypoints.size() < 2) {
            throw new IllegalArgumentException("至少需要2个坐标点");
        }
        validateWaypoints(waypoints);
        if (apiKey == null || apiKey.isBlank()) {
            log.error("高德地图路径规划失败: AMAP_API_KEY 未配置");
            return null;
        }

        try {
            URI requestUri = buildRouteUri(waypoints);

            String responseBody;
            try (ExternalCallBulkhead.Permit ignored = bulkheadRegistry
                    .get(ExternalCallBulkheadRegistry.AMAP).acquire()) {
                responseBody = restTemplate.execute(
                        requestUri,
                        HttpMethod.GET,
                        null,
                        clientHttpResponse -> {
                            if (clientHttpResponse.getBody() == null) {
                                throw new IOException("高德地图响应体为空");
                            }
                            return BoundedHttpBodyReader.readUtf8(
                                    clientHttpResponse.getBody(), maxResponseBytes);
                        });
            }

            if (responseBody != null) {
                return parseRouteResponse(responseBody);
            }

        } catch (Exception e) {
            log.error("高德地图路径规划失败: type={}, message={}",
                    e.getClass().getSimpleName(), sanitizeExceptionMessage(e));
        }

        return null;
    }

    private URI buildRouteUri(List<double[]> waypoints) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(apiUrl)
                .pathSegment("direction", "driving")
                .queryParam("origin", coordinate(waypoints.get(0)))
                .queryParam("destination", coordinate(waypoints.get(waypoints.size() - 1)))
                .queryParam("extensions", "all")
                .queryParam("strategy", "0")
                .queryParam("key", apiKey);

        if (waypoints.size() > 2) {
            StringJoiner viaPoints = new StringJoiner("|");
            for (int index = 1; index < waypoints.size() - 1; index++) {
                viaPoints.add(coordinate(waypoints.get(index)));
            }
            builder.queryParam("waypoints", viaPoints.toString());
        }
        return builder.build().encode().toUri();
    }

    private String coordinate(double[] point) {
        return Double.toString(point[0]) + "," + Double.toString(point[1]);
    }

    private void validateWaypoints(List<double[]> waypoints) {
        for (double[] point : waypoints) {
            if (point == null || point.length < 2
                    || !Double.isFinite(point[0]) || !Double.isFinite(point[1])
                    || point[0] < -180 || point[0] > 180
                    || point[1] < -90 || point[1] > 90) {
                throw new IllegalArgumentException("路线包含无效经纬度");
            }
        }
    }

    private String sanitizeExceptionMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        if (apiKey != null && !apiKey.isBlank()) {
            message = message.replace(apiKey, "***");
        }
        return API_KEY_QUERY_PATTERN.matcher(message).replaceAll("$1***");
    }

    /**
     * 解析高德地图响应
     */
    private RouteInfo parseRouteResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (!"1".equals(root.path("status").asText())) {
                log.error("高德地图路径规划返回失败: info={}, infocode={}",
                        root.path("info").asText(), root.path("infocode").asText());
                return null;
            }
            JsonNode paths = root.path("route").path("paths");
            if (!paths.isArray() || paths.isEmpty()) {
                log.warn("高德地图路径规划未返回可用路径");
                return null;
            }
            JsonNode route = paths.get(0);

            double distanceMeters = route.path("distance").asDouble(-1);
            double durationSeconds = route.path("duration").asDouble(-1);
            if (distanceMeters <= 0 || durationSeconds <= 0) {
                log.warn("高德地图路径规划返回无效距离或时长");
                return null;
            }

            RouteInfo info = new RouteInfo();
            info.setDistance(distanceMeters / 1000.0); // 转换为公里
            info.setDuration(durationSeconds / 60.0);   // 转换为分钟

            // 仅使用高德明确返回的过路费，不虚构车辆油耗成本。
            info.setCost(route.path("tolls").asDouble());

            // 提取路径步骤
            JsonNode steps = route.path("steps");
            List<RouteStep> routeSteps = new ArrayList<>();
            for (JsonNode step : steps) {
                RouteStep routeStep = new RouteStep();
                routeStep.setInstruction(step.path("instruction").asText());
                routeStep.setDistance(step.path("distance").asDouble() / 1000.0);
                routeStep.setDuration(step.path("duration").asDouble() / 60.0);
                routeSteps.add(routeStep);
            }
            info.setSteps(routeSteps);

            return info;

        } catch (Exception e) {
            log.error("解析高德地图响应失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 路径信息
     */
    public static class RouteInfo {
        private double distance;      // 距离（公里）
        private double duration;      // 时长（分钟）
        private double cost;          // 成本（元）
        private List<RouteStep> steps; // 路径步骤

        public double getDistance() { return distance; }
        public void setDistance(double distance) { this.distance = distance; }
        public double getDuration() { return duration; }
        public void setDuration(double duration) { this.duration = duration; }
        public double getCost() { return cost; }
        public void setCost(double cost) { this.cost = cost; }
        public List<RouteStep> getSteps() { return steps; }
        public void setSteps(List<RouteStep> steps) { this.steps = steps; }
    }

    /**
     * 路径步骤
     */
    public static class RouteStep {
        private String instruction;  // 导航指令
        private double distance;     // 该段距离
        private double duration;     // 该段时长

        public String getInstruction() { return instruction; }
        public void setInstruction(String instruction) { this.instruction = instruction; }
        public double getDistance() { return distance; }
        public void setDistance(double distance) { this.distance = distance; }
        public double getDuration() { return duration; }
        public void setDuration(double duration) { this.duration = duration; }
    }
}
