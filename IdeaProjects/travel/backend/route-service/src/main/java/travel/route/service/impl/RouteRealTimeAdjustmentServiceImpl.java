package travel.route.service.impl;

import travel.common.entity.route_planning.Route;
import travel.common.vo.AdjustmentSuggestionVO;
import travel.common.vo.RouteAdjustmentVO;
import travel.common.mapper.route_planning_mapper.RouteMapper;
import travel.route.service.RouteRealTimeAdjustmentService;
import travel.common.utils.ThirdApiUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 路线实时调整服务实现
 */
@Service
public class RouteRealTimeAdjustmentServiceImpl implements RouteRealTimeAdjustmentService {

    private static final Logger log = LoggerFactory.getLogger(RouteRealTimeAdjustmentServiceImpl.class);

    private final RouteMapper routeMapper;
    private final ThirdApiUtil thirdApiUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    public RouteRealTimeAdjustmentServiceImpl(RouteMapper routeMapper, ThirdApiUtil thirdApiUtil,
                                              RedisTemplate<String, Object> redisTemplate) {
        this.routeMapper = routeMapper;
        this.thirdApiUtil = thirdApiUtil;
        this.redisTemplate = redisTemplate;
    }

    // 缓存键前缀
    private static final String ADJUSTMENT_SUGGESTION_PREFIX = "route:adjustment:suggestion:";
    private static final String REAL_TIME_TRAFFIC_PREFIX = "route:traffic:";
    private static final String REAL_TIME_ATTTRACTION_STATUS_PREFIX = "attraction:status:";
    private static final String ROUTE_CONGESTION_PREDICTION_PREFIX = "route:congestion:prediction:";
    private static final String ALTERNATIVE_ROUTES_PREFIX = "route:alternative:";
    private static final String ADJUSTMENT_HISTORY_PREFIX = "route:adjustment:history:";

    // 缓存过期时间（分钟）
    private static final long CACHE_EXPIRE_MINUTES = 5;

    @Override
    public AdjustmentSuggestionVO getAdjustmentSuggestion(Long routeId, String currentPoint, Long userId) {
        // 尝试从缓存获取
        String cacheKey = ADJUSTMENT_SUGGESTION_PREFIX + routeId + ":" + currentPoint + ":" + userId;
        AdjustmentSuggestionVO suggestionVO = null;
        if (redisTemplate != null) {
            suggestionVO = (AdjustmentSuggestionVO) redisTemplate.opsForValue().get(cacheKey);
        }
        if (suggestionVO != null) {
            return suggestionVO;
        }

        // 从数据库查询路线信息
        Route route = routeMapper.selectById(routeId);
        if (route == null) {
            return null;
        }

        // 获取实时路况信息
        Map<String, Object> trafficInfo = getRealTimeTrafficInfo(routeId);

        // 获取景点实时状态
        // 这里需要从路线中提取景点ID列表
        List<Long> attractionIds = extractAttractionIdsFromRoute(route);
        Map<Long, Map<String, Object>> attractionStatus = getRealTimeAttractionStatus(attractionIds);

        // 构建调整建议
        suggestionVO = new AdjustmentSuggestionVO();
        suggestionVO.setNeedAdjustment(false);
        List<Integer> suggestionTypes = new ArrayList<>();

        // 分析路况，判断是否需要调整
        boolean hasCongestion = trafficInfo.values().stream()
                .anyMatch(status -> {
                    if (status instanceof String) {
                        String statusStr = (String) status;
                        return "heavy".equals(statusStr) || "severe".equals(statusStr);
                    }
                    return false;
                });
        if (hasCongestion) {
            suggestionVO.setNeedAdjustment(true);
            suggestionTypes.add(1); // 避开拥堵
        }

        // 分析景点状态，判断是否需要调整
        boolean hasCrowdedAttraction = attractionStatus.values().stream()
                .anyMatch(status -> {
                    Integer crowdLevel = (Integer) status.get("crowdLevel");
                    return crowdLevel != null && crowdLevel >= 4;
                });
        if (hasCrowdedAttraction) {
            suggestionVO.setNeedAdjustment(true);
            suggestionTypes.add(4); // 避开景点
        }

        suggestionVO.setSuggestionTypes(suggestionTypes);

        // 构建详细建议
        if (suggestionVO.getNeedAdjustment()) {
            StringBuilder detailedSuggestion = new StringBuilder();
            if (hasCongestion) {
                detailedSuggestion.append("当前路线存在拥堵路段，建议调整路线以避开拥堵。");
            }
            if (hasCrowdedAttraction) {
                if (detailedSuggestion.length() > 0) {
                    detailedSuggestion.append(" ");
                }
                detailedSuggestion.append("部分景点人流量较大，建议调整游览顺序或避开高峰期。");
            }
            suggestionVO.setDetailedSuggestion(detailedSuggestion.toString());
        }

        // 构建拥堵路段信息
        List<Map<String, Object>> congestionSegments = new ArrayList<>();
        trafficInfo.forEach((segmentId, status) -> {
            if ("heavy".equals(status) || "severe".equals(status)) {
                Map<String, Object> segmentInfo = new HashMap<>();
                segmentInfo.put("segmentId", segmentId);
                segmentInfo.put("status", status);
                segmentInfo.put("suggestion", "建议避开此路段");
                congestionSegments.add(segmentInfo);
            }
        });
        suggestionVO.setCongestionSegments(congestionSegments);

        // 构建景点排队情况
        Map<Long, Map<String, Object>> attractionQueueInfo = new HashMap<>();
        attractionStatus.forEach((attractionId, status) -> {
            Map<String, Object> queueInfo = new HashMap<>();
            queueInfo.put("crowdLevel", status.get("crowdLevel"));
            queueInfo.put("waitTime", status.get("waitTime"));
            queueInfo.put("suggestion", status.get("suggestion"));
            attractionQueueInfo.put(attractionId, queueInfo);
        });
        suggestionVO.setAttractionQueueInfo(attractionQueueInfo);

        // 获取备选路线数量
        List<RouteAdjustmentVO> alternativeRoutes = getAlternativeRoutes(routeId, currentPoint, userId);
        suggestionVO.setAlternativeRouteCount(alternativeRoutes.size());

        // 构建预计调整效果
        Map<String, Object> estimatedEffect = new HashMap<>();
        if (hasCongestion) {
            estimatedEffect.put("timeSaving", 15.0); // 预计节省15分钟
            estimatedEffect.put("distanceIncrease", 2.0); // 预计增加2公里
        }
        suggestionVO.setEstimatedEffect(estimatedEffect);

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, suggestionVO, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        return suggestionVO;
    }

    @Override
    public RouteAdjustmentVO adjustRoute(Long routeId, RouteAdjustmentVO adjustmentVO, Long userId) {
        // 从数据库查询路线信息
        Route route = routeMapper.selectById(routeId);
        if (route == null) {
            return null;
        }

        // 执行路线调整逻辑
        // 这里需要根据调整类型执行不同的调整逻辑
        switch (adjustmentVO.getAdjustmentType()) {
            case 1: // 避开拥堵
                adjustForCongestion(route, adjustmentVO);
                break;
            case 2: // 缩短距离
                adjustForDistance(route, adjustmentVO);
                break;
            case 3: // 减少时间
                adjustForTime(route, adjustmentVO);
                break;
            case 4: // 避开景点
                adjustForAttractions(route, adjustmentVO);
                break;
            case 5: // 添加景点
                adjustForAddAttractions(route, adjustmentVO);
                break;
            default:
                break;
        }

        // 计算调整后的路线信息
        calculateAdjustedRouteInfo(adjustmentVO);

        // 保存调整历史
        saveAdjustmentHistory(routeId, adjustmentVO, userId);

        return adjustmentVO;
    }

    @Override
    public Map<String, Object> getRealTimeTrafficInfo(Long routeId) {
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

        // 从第三方API获取实时路况信息
        try {
            trafficInfo = thirdApiUtil.getRealTimeTrafficInfo(routeId.toString());
        } catch (Exception e) {
            log.error("获取实时路况信息失败: {}", e.getMessage());
            // 模拟路况信息
            trafficInfo = new HashMap<>();
            trafficInfo.put("segment1", "light");
            trafficInfo.put("segment2", "heavy");
            trafficInfo.put("segment3", "moderate");
        }

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, trafficInfo, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        return trafficInfo;
    }

    @Override
    public Map<Long, Map<String, Object>> getRealTimeAttractionStatus(List<Long> attractionIds) {
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

            // 从第三方API获取景点实时状态
            try {
                status = thirdApiUtil.getRealTimeAttractionStatus(attractionId.toString());
            } catch (Exception e) {
                log.error("获取景点实时状态失败: {}", e.getMessage());
                // 模拟景点状态
                status = new HashMap<>();
                status.put("crowdLevel", 3); // 1-5，5最拥挤
                status.put("waitTime", 30); // 分钟
                status.put("suggestion", "建议错峰游览");
            }

            // 缓存结果
            redisTemplate.opsForValue().set(cacheKey, status, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            statusMap.put(attractionId, status);
        }

        return statusMap;
    }

    @Override
    public Map<String, Object> predictRouteCongestion(Long routeId, String departureTime) {
        // 尝试从缓存获取
        String cacheKey = ROUTE_CONGESTION_PREDICTION_PREFIX + routeId + ":" + departureTime;
        Object predictionObj = redisTemplate.opsForValue().get(cacheKey);
        Map<String, Object> prediction = null;
        if (predictionObj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) predictionObj;
            prediction = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String) {
                    prediction.put((String) entry.getKey(), entry.getValue());
                }
            }
        }
        if (prediction != null && !prediction.isEmpty()) {
            return prediction;
        }

        // 从数据库查询路线信息
        Route route = routeMapper.selectById(routeId);
        if (route == null) {
            return null;
        }

        // 预测拥堵情况
        prediction = new HashMap<>();

        // 解析出发时间
        LocalDateTime departure = LocalDateTime.parse(departureTime, DateTimeFormatter.ISO_DATE_TIME);
        int hour = departure.getHour();

        // 基于时间预测拥堵情况
        if (hour >= 7 && hour <= 9) {
            prediction.put("congestionLevel", "high");
            prediction.put("estimatedDelay", 20); // 预计延迟20分钟
            prediction.put("suggestion", "建议提前出发或选择其他路线");
        } else if (hour >= 17 && hour <= 19) {
            prediction.put("congestionLevel", "high");
            prediction.put("estimatedDelay", 15); // 预计延迟15分钟
            prediction.put("suggestion", "建议错峰出行");
        } else {
            prediction.put("congestionLevel", "low");
            prediction.put("estimatedDelay", 5); // 预计延迟5分钟
            prediction.put("suggestion", "路况良好，可正常出行");
        }

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, prediction, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        return prediction;
    }

    @Override
    public List<RouteAdjustmentVO> getAlternativeRoutes(Long routeId, String currentPoint, Long userId) {
        // 尝试从缓存获取
        String cacheKey = ALTERNATIVE_ROUTES_PREFIX + routeId + ":" + currentPoint + ":" + userId;
        Object alternativeRoutesObj = redisTemplate.opsForValue().get(cacheKey);
        List<RouteAdjustmentVO> alternativeRoutes = new ArrayList<>();
        if (alternativeRoutesObj instanceof List) {
            List<?> list = (List<?>) alternativeRoutesObj;
            for (Object item : list) {
                if (item instanceof RouteAdjustmentVO) {
                    alternativeRoutes.add((RouteAdjustmentVO) item);
                }
            }
        }
        if (!alternativeRoutes.isEmpty()) {
            return alternativeRoutes;
        }

        // 从数据库查询路线信息
        Route route = routeMapper.selectById(routeId);
        if (route == null) {
            return Collections.emptyList();
        }

        // 生成备选路线
        alternativeRoutes = new ArrayList<>();

        // 备选路线1：最短距离
        RouteAdjustmentVO route1 = new RouteAdjustmentVO();
        route1.setAdjustmentType(2);
        route1.setAdjustmentReason("最短距离路线");
        route1.setAdjustedTotalDistance(10.0 * 0.9); // 使用默认值
        route1.setAdjustedEstimatedTime(60.0 * 1.05); // 使用默认值
        alternativeRoutes.add(route1);

        // 备选路线2：最短时间
        RouteAdjustmentVO route2 = new RouteAdjustmentVO();
        route2.setAdjustmentType(3);
        route2.setAdjustmentReason("最短时间路线");
        route2.setAdjustedTotalDistance(10.0 * 1.1); // 使用默认值
        route2.setAdjustedEstimatedTime(60.0 * 0.9); // 使用默认值
        alternativeRoutes.add(route2);

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, alternativeRoutes, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        return alternativeRoutes;
    }

    @Override
    public boolean saveAdjustmentHistory(Long routeId, RouteAdjustmentVO adjustmentVO, Long userId) {
        try {
            // 构建调整历史记录
            Map<String, Object> historyRecord = new HashMap<>();
            historyRecord.put("routeId", routeId);
            historyRecord.put("userId", userId);
            historyRecord.put("adjustmentType", adjustmentVO.getAdjustmentType());
            historyRecord.put("adjustmentReason", adjustmentVO.getAdjustmentReason());
            historyRecord.put("adjustedTotalDistance", adjustmentVO.getAdjustedTotalDistance());
            historyRecord.put("adjustedEstimatedTime", adjustmentVO.getAdjustedEstimatedTime());
            historyRecord.put("adjustedEstimatedCost", adjustmentVO.getAdjustedEstimatedCost());
            historyRecord.put("estimatedTimeSaving", adjustmentVO.getEstimatedTimeSaving());
            historyRecord.put("estimatedDistanceSaving", adjustmentVO.getEstimatedDistanceSaving());
            historyRecord.put("adjustmentTime", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

            // 保存到Redis（实际项目中应该保存到数据库）
            String cacheKey = ADJUSTMENT_HISTORY_PREFIX + routeId + ":" + userId;
            Object historyListObj = redisTemplate.opsForValue().get(cacheKey);
            List<Map<String, Object>> historyList = null;
            if (historyListObj instanceof List) {
                List<?> list = (List<?>) historyListObj;
                if (!list.isEmpty() && list.get(0) instanceof Map) {
                    historyList = new ArrayList<>();
                    for (Object item : list) {
                        if (item instanceof Map) {
                            Map<?, ?> map = (Map<?, ?>) item;
                            Map<String, Object> historyItem = new HashMap<>();
                            for (Map.Entry<?, ?> entry : map.entrySet()) {
                                if (entry.getKey() instanceof String) {
                                    historyItem.put((String) entry.getKey(), entry.getValue());
                                }
                            }
                            historyList.add(historyItem);
                        }
                    }
                }
            }
            if (historyList == null) {
                historyList = new ArrayList<>();
            }
            historyList.add(historyRecord);
            redisTemplate.opsForValue().set(cacheKey, historyList, 7, TimeUnit.DAYS); // 保存7天

            return true;
        } catch (Exception e) {
            log.error("保存路线调整历史失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getAdjustmentHistory(Long routeId, Long userId) {
        // 从Redis获取调整历史（实际项目中应该从数据库查询）
        String cacheKey = ADJUSTMENT_HISTORY_PREFIX + routeId + ":" + userId;
        Object historyListObj = redisTemplate.opsForValue().get(cacheKey);
        List<Map<String, Object>> historyList = new ArrayList<>();
        if (historyListObj instanceof List) {
            List<?> list = (List<?>) historyListObj;
            for (Object item : list) {
                if (item instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) item;
                    Map<String, Object> historyItem = new HashMap<>();
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        if (entry.getKey() instanceof String) {
                            historyItem.put((String) entry.getKey(), entry.getValue());
                        }
                    }
                    historyList.add(historyItem);
                }
            }
        }
        return historyList;
    }

    /**
     * 从路线中提取景点ID列表
     *
     * @param route 路线
     * @return 景点ID列表
     */
    private List<Long> extractAttractionIdsFromRoute(Route route) {
        // 这里需要根据实际的路线数据结构实现
        // 模拟返回一些景点ID
        return Arrays.asList(1L, 2L, 3L, 4L, 5L);
    }

    /**
     * 针对拥堵情况调整路线
     *
     * @param route 原路线
     * @param adjustmentVO 调整参数
     */
    private void adjustForCongestion(Route route, RouteAdjustmentVO adjustmentVO) {
        // 这里需要实现具体的调整逻辑
        // 模拟调整
        adjustmentVO.setAdjustmentReason("避开拥堵路段");
        adjustmentVO.setEstimatedTimeSaving(15.0);
        adjustmentVO.setEstimatedDistanceSaving(0.0);
    }

    /**
     * 针对距离调整路线
     *
     * @param route 原路线
     * @param adjustmentVO 调整参数
     */
    private void adjustForDistance(Route route, RouteAdjustmentVO adjustmentVO) {
        // 这里需要实现具体的调整逻辑
        // 模拟调整
        adjustmentVO.setAdjustmentReason("缩短距离");
        adjustmentVO.setEstimatedTimeSaving(0.0);
        adjustmentVO.setEstimatedDistanceSaving(2.0);
    }

    /**
     * 针对时间调整路线
     *
     * @param route 原路线
     * @param adjustmentVO 调整参数
     */
    private void adjustForTime(Route route, RouteAdjustmentVO adjustmentVO) {
        // 这里需要实现具体的调整逻辑
        // 模拟调整
        adjustmentVO.setAdjustmentReason("减少时间");
        adjustmentVO.setEstimatedTimeSaving(20.0);
        adjustmentVO.setEstimatedDistanceSaving(0.0);
    }

    /**
     * 针对景点调整路线
     *
     * @param route 原路线
     * @param adjustmentVO 调整参数
     */
    private void adjustForAttractions(Route route, RouteAdjustmentVO adjustmentVO) {
        // 这里需要实现具体的调整逻辑
        // 模拟调整
        adjustmentVO.setAdjustmentReason("避开人流量大的景点");
        adjustmentVO.setEstimatedTimeSaving(10.0);
        adjustmentVO.setEstimatedDistanceSaving(0.0);
    }

    /**
     * 针对添加景点调整路线
     *
     * @param route 原路线
     * @param adjustmentVO 调整参数
     */
    private void adjustForAddAttractions(Route route, RouteAdjustmentVO adjustmentVO) {
        // 这里需要实现具体的调整逻辑
        // 模拟调整
        adjustmentVO.setAdjustmentReason("添加用户感兴趣的景点");
        adjustmentVO.setEstimatedTimeSaving(-15.0); // 时间增加
        adjustmentVO.setEstimatedDistanceSaving(-1.0); // 距离增加
    }

    /**
     * 计算调整后的路线信息
     *
     * @param adjustmentVO 调整参数
     */
    private void calculateAdjustedRouteInfo(RouteAdjustmentVO adjustmentVO) {
        // 这里需要根据实际的调整逻辑计算
        // 模拟计算
        adjustmentVO.setAdjustedTotalDistance(10.0);
        adjustmentVO.setAdjustedEstimatedTime(2.5);
        adjustmentVO.setAdjustedEstimatedCost(50.0);
    }

    @Override
    public Map<String, Object> getRealTimeAdjustment(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("suggestions", List.of("避开拥堵路段", "调整游览顺序"));
        result.put("estimatedTimeSaving", 15);
        return result;
    }

    @Override
    public boolean applyRouteAdjustment(Map<String, Object> adjustmentData) {
        return true;
    }

    @Override
    public Map<String, Object> getTrafficConditions(String location, String route) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "normal");
        result.put("congestionLevel", "light");
        result.put("estimatedDelay", 5);
        return result;
    }

    @Override
    public Map<String, Object> getWeatherImpact(String location, String route) {
        Map<String, Object> result = new HashMap<>();
        result.put("impact", "low");
        result.put("suggestion", "天气良好，适合出行");
        return result;
    }

    @Override
    public List<Map<String, Object>> getCongestionAlerts(String route) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        Map<String, Object> alert = new HashMap<>();
        alert.put("segment", "segment1");
        alert.put("level", "heavy");
        alert.put("suggestion", "建议避开此路段");
        alerts.add(alert);
        return alerts;
    }

    @Override
    public List<Map<String, Object>> getAlternativeRoutes(Map<String, Object> request) {
        List<Map<String, Object>> alternatives = new ArrayList<>();
        Map<String, Object> route1 = new HashMap<>();
        route1.put("id", 1);
        route1.put("name", "备选路线1");
        route1.put("distance", 12.5);
        route1.put("time", 45);
        alternatives.add(route1);
        return alternatives;
    }

    @Override
    public boolean updateRealTimeLocation(Map<String, Object> locationData) {
        return true;
    }

    @Override
    public Map<String, Object> getEstimatedArrivalTime(Integer routeId, Double currentDistance) {
        Map<String, Object> result = new HashMap<>();
        result.put("eta", 30);
        result.put("distanceRemaining", 5.5);
        return result;
    }

    @Override
    public List<Map<String, Object>> getAdjustmentHistory(Integer routeId) {
        return new ArrayList<>();
    }
}
