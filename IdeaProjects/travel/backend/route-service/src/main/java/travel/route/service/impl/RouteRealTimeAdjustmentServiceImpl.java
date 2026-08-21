package travel.route.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import travel.common.entity.route_planning.Route;
import travel.common.entity.route_planning.RouteAttraction;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.entity.travel_realtime.AttractionRealtimeStatus;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.mapper.route_planning_mapper.RouteMapper;
import travel.common.mapper.route_planning_mapper.RouteAttractionMapper;
import travel.common.mapper.travel_recommendation_mapper.AttractionMapper;
import travel.common.mapper.travel_realtime_mapper.AttractionRealtimeStatusMapper;
import travel.common.utils.AMapService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import travel.route.service.RouteRealTimeAdjustmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 路线实时调整服务实现
 */
@Service
public class RouteRealTimeAdjustmentServiceImpl implements RouteRealTimeAdjustmentService {

    private static final Logger log = LoggerFactory.getLogger(RouteRealTimeAdjustmentServiceImpl.class);

    private final RouteMapper routeMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RouteAttractionMapper routeAttractionMapper;
    private final AttractionMapper attractionMapper;
    private final AttractionRealtimeStatusMapper realtimeStatusMapper;
    private final AMapService aMapService;

    public RouteRealTimeAdjustmentServiceImpl(RouteMapper routeMapper,
                                               RedisTemplate<String, Object> redisTemplate,
                                               RouteAttractionMapper routeAttractionMapper,
                                               AttractionMapper attractionMapper,
                                               AttractionRealtimeStatusMapper realtimeStatusMapper,
                                               AMapService aMapService) {
        this.routeMapper = routeMapper;
        this.redisTemplate = redisTemplate;
        this.routeAttractionMapper = routeAttractionMapper;
        this.attractionMapper = attractionMapper;
        this.realtimeStatusMapper = realtimeStatusMapper;
        this.aMapService = aMapService;
    }

    // 缓存键前缀
    private static final String REAL_TIME_TRAFFIC_PREFIX = "route:traffic:v2:";
    private static final String REAL_TIME_ATTTRACTION_STATUS_PREFIX = "attraction:status:";

    // 缓存过期时间（分钟）
    private static final long CACHE_EXPIRE_MINUTES = 5;

    @Override
    public Map<String, Object> getRealTimeTrafficInfo(Long routeId) {
        return getRealTimeTrafficInfo(routeId, null);
    }

    private Map<String, Object> getRealTimeTrafficInfo(Long routeId, List<RouteAttraction> loadedRelations) {
        // 尝试从缓存获取
        String cacheKey = REAL_TIME_TRAFFIC_PREFIX + routeId;
        Object trafficInfoObj = redisTemplate.opsForValue().get(cacheKey);
        Map<String, Object> trafficInfo = null;
        if (trafficInfoObj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) trafficInfoObj;
            trafficInfo = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String) {
                    trafficInfo.put((String) entry.getKey(), entry.getValue());
                }
            }
        }
        if (trafficInfo != null && !trafficInfo.isEmpty()) {
            return trafficInfo;
        }

        List<RouteAttraction> relations = loadedRelations == null
                ? loadOrderedRouteAttractions(routeId)
                : loadedRelations;
        List<Attraction> routeAttractions = relations.stream()
                .map(RouteAttraction::getAttractionId)
                .filter(Objects::nonNull)
                .map(attractionId -> attractionMapper.selectLatLngById(attractionId.longValue()))
                .filter(this::hasValidCoordinates)
                .toList();
        if (routeAttractions.size() < 2) {
            return unavailableTraffic(routeId, "路线至少需要两个带有效经纬度的景点");
        }

        trafficInfo = new LinkedHashMap<>();
        List<Map<String, Object>> routeDetails = new ArrayList<>();
        long totalDistance = 0;
        long totalDuration = 0;
        int availableSegments = 0;
        for (int index = 0; index < routeAttractions.size() - 1; index++) {
            Attraction origin = routeAttractions.get(index);
            Attraction destination = routeAttractions.get(index + 1);
            Map<String, Object> routeData = aMapService.drivingRoute(
                    origin.getLongitude().doubleValue(), origin.getLatitude().doubleValue(),
                    destination.getLongitude().doubleValue(), destination.getLatitude().doubleValue());
            if (routeData == null || routeData.isEmpty()) {
                continue;
            }

            String segmentKey = "segment:" + origin.getId() + "-" + destination.getId();
            String trafficLevel = resolveTrafficLevel(routeData.get("steps"));
            trafficInfo.put(segmentKey, trafficLevel);
            long distance = numberValue(routeData.get("distance"));
            long duration = numberValue(routeData.get("duration"));
            totalDistance += distance;
            totalDuration += duration;
            availableSegments++;

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("segmentId", segmentKey);
            detail.put("fromAttractionId", origin.getId());
            detail.put("toAttractionId", destination.getId());
            detail.put("status", trafficLevel);
            detail.put("distanceMeters", distance);
            detail.put("durationSeconds", duration);
            routeDetails.add(detail);
        }
        if (availableSegments == 0) {
            return unavailableTraffic(routeId, "高德 API 暂未返回有效路线数据");
        }

        trafficInfo.put("dataAvailable", true);
        trafficInfo.put("source", "amap");
        trafficInfo.put("totalDistanceMeters", totalDistance);
        trafficInfo.put("totalDurationSeconds", totalDuration);
        trafficInfo.put("routeDetails", routeDetails);
        redisTemplate.opsForValue().set(cacheKey, trafficInfo, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        return trafficInfo;
    }

    @Override
    public Map<Long, Map<String, Object>> getRealTimeAttractionStatus(List<Long> attractionIds) {
        if (attractionIds == null || attractionIds.size() > 100) {
            throw new IllegalArgumentException("attractionIds must contain at most 100 items");
        }
        Map<Long, Map<String, Object>> statusMap = new HashMap<>();

        for (Long attractionId : attractionIds) {
            // 尝试从缓存获取
            String cacheKey = REAL_TIME_ATTTRACTION_STATUS_PREFIX + attractionId;
            Object statusObj = redisTemplate.opsForValue().get(cacheKey);
            Map<String, Object> status = null;
            if (statusObj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) statusObj;
                status = new HashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String) {
                        status.put((String) entry.getKey(), entry.getValue());
                    }
                }
            }
            if (status != null && !status.isEmpty()) {
                statusMap.put(attractionId, status);
                continue;
            }

            AttractionRealtimeStatus realtimeStatus = realtimeStatusMapper.selectByAttractionId(attractionId);
            status = new HashMap<>();
            status.put("dataAvailable", realtimeStatus != null);
            status.put("source", realtimeStatus == null ? "unavailable" : "database_snapshot");
            if (realtimeStatus != null) {
                status.put("crowdLevel", realtimeStatus.getCrowdLevel());
                status.put("crowdCount", realtimeStatus.getCrowdCount());
                status.put("weather", realtimeStatus.getWeather());
                status.put("temperature", realtimeStatus.getTemperature());
                status.put("updateTime", realtimeStatus.getUpdateTime());
                redisTemplate.opsForValue().set(cacheKey, status, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            }
            statusMap.put(attractionId, status);
        }

        return statusMap;
    }

    private List<RouteAttraction> loadOrderedRouteAttractions(Long routeId) {
        return routeAttractionMapper.selectList(
                new LambdaQueryWrapper<RouteAttraction>()
                        .eq(RouteAttraction::getRouteId, routeId)
                        .orderByAsc(RouteAttraction::getDayNumber, RouteAttraction::getVisitOrder));
    }

    private boolean hasValidCoordinates(Attraction attraction) {
        if (attraction == null || attraction.getId() == null
                || attraction.getLongitude() == null || attraction.getLatitude() == null) {
            return false;
        }
        double longitude = attraction.getLongitude().doubleValue();
        double latitude = attraction.getLatitude().doubleValue();
        return longitude >= -180 && longitude <= 180 && latitude >= -90 && latitude <= 90;
    }

    private Map<String, Object> unavailableTraffic(Long routeId, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("routeId", routeId);
        result.put("dataAvailable", false);
        result.put("source", "amap");
        result.put("message", message);
        return result;
    }

    private String resolveTrafficLevel(Object stepsValue) {
        if (!(stepsValue instanceof JsonNode steps) || !steps.isArray()) {
            return "unknown";
        }
        int maxSeverity = 0;
        for (JsonNode step : steps) {
            JsonNode trafficSegments = step.path("tmcs");
            if (!trafficSegments.isArray()) {
                continue;
            }
            for (JsonNode trafficSegment : trafficSegments) {
                maxSeverity = Math.max(maxSeverity,
                        trafficSeverity(trafficSegment.path("status").asText()));
            }
        }
        return switch (maxSeverity) {
            case 4 -> "severe";
            case 3 -> "heavy";
            case 2 -> "moderate";
            case 1 -> "light";
            default -> "unknown";
        };
    }

    private int trafficSeverity(String status) {
        if (status == null) {
            return 0;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("严重拥堵") || normalized.contains("severe")) {
            return 4;
        }
        if (normalized.contains("拥堵") || normalized.contains("congested")
                || normalized.equals("heavy")) {
            return 3;
        }
        if (normalized.contains("缓行") || normalized.contains("slow")
                || normalized.equals("moderate")) {
            return 2;
        }
        if (normalized.contains("畅通") || normalized.contains("smooth")
                || normalized.equals("light")) {
            return 1;
        }
        return 0;
    }

    private long numberValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

}
