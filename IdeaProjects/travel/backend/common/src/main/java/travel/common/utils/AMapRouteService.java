package travel.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * 高德地图路径规划服务
 * 提供真实的距离、时间、成本计算
 */
@Slf4j
@Component
public class AMapRouteService {

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

        try {
            // 构建路径规划请求
            StringBuilder origin = new StringBuilder();
            StringBuilder destination = new StringBuilder();
            StringBuilder viaPoints = new StringBuilder();

            origin.append(waypoints.get(0)[0]).append(",").append(waypoints.get(0)[1]);
            destination.append(waypoints.get(waypoints.size() - 1)[0]).append(",")
                    .append(waypoints.get(waypoints.size() - 1)[1]);

            for (int i = 1; i < waypoints.size() - 1; i++) {
                if (viaPoints.length() > 0) viaPoints.append("|");
                viaPoints.append(waypoints.get(i)[0]).append(",").append(waypoints.get(i)[1]);
            }

            // 调用高德地图驾车路径规划API
            String url = String.format(
                    "%s/direction/driving?origin=%s&destination=%s&waypoints=%s&key=%s&strategy=0",
                    apiUrl, origin, destination, viaPoints, apiKey
            );

            String responseBody;
            try (ExternalCallBulkhead.Permit ignored = bulkheadRegistry
                    .get(ExternalCallBulkheadRegistry.AMAP).acquire()) {
                responseBody = restTemplate.execute(
                        url,
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
            log.error("高德地图路径规划失败: {}", e.getMessage());
        }

        // 失败时返回估算值
        return estimateRouteInfo(waypoints);
    }

    /**
     * 解析高德地图响应
     */
    private RouteInfo parseRouteResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode route = root.path("route").path("paths").get(0);

            if (route == null) {
                return null;
            }

            RouteInfo info = new RouteInfo();
            info.setDistance(route.path("distance").asDouble() / 1000.0); // 转换为公里
            info.setDuration(route.path("duration").asDouble() / 60.0);   // 转换为分钟

            // 估算成本（考虑过路费和油费）
            double tolls = route.path("tolls").asDouble();
            double fuelCost = info.getDistance() * 0.8; // 每公里0.8元油费
            info.setCost(tolls + fuelCost);

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
     * 估算路径信息（当API调用失败时使用）
     */
    private RouteInfo estimateRouteInfo(List<double[]> waypoints) {
        RouteInfo info = new RouteInfo();
        double totalDistance = 0.0;

        for (int i = 0; i < waypoints.size() - 1; i++) {
            totalDistance += haversineDistance(
                    waypoints.get(i)[0], waypoints.get(i)[1],
                    waypoints.get(i + 1)[0], waypoints.get(i + 1)[1]
            );
        }

        info.setDistance(totalDistance);
        info.setDuration(totalDistance / 30.0 * 60); // 假设平均速度30km/h
        info.setCost(totalDistance * 1.5); // 估算成本

        return info;
    }

    /**
     * Haversine公式计算两点间距离（公里）
     */
    private double haversineDistance(double lon1, double lat1, double lon2, double lat2) {
        final double R = 6371.0; // 地球半径（公里）

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
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
