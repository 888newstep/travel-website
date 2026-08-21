package travel.route.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.entity.route_planning.Route;
import travel.common.entity.route_planning.RouteAttraction;

import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.service.DistributedLockService;
import travel.route.algorithm.GeneticAlgorithmTSP;
import travel.route.algorithm.RoutePlanAlgorithm;
import travel.route.dto.optimization.ApplyOptimizationRequest;
import travel.route.dto.optimization.OptimizationHistoryItem;
import travel.route.dto.optimization.*;
import travel.route.service.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import travel.common.utils.AMapRouteService;
import travel.common.utils.CommonUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteOptimizationServiceImpl implements RouteOptimizationService {

    private static final Logger log = LoggerFactory.getLogger(RouteOptimizationServiceImpl.class);
    private static final String OPTIMIZATION_HISTORY_PREFIX = "route:optimization:history:v2:";
    private static final int MAX_OPTIMIZATION_HISTORY = 20;
    private static final int MAX_OPTIMIZABLE_ATTRACTIONS = 100;

    private final RouteService routeService;
    private final AttractionService attractionService;
    private final RouteAttractionService routeAttractionService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AMapRouteService aMapRouteService;
    private final GeneticAlgorithmTSP geneticAlgorithmTSP;
    private final DistributedLockService distributedLockService;
    private final TransactionTemplate transactionTemplate;

    // 兴趣标签与景点类型映射
    private final Map<String, List<String>> interestAttractionMap = createInterestMap();

    private static Map<String, List<String>> createInterestMap() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("历史文化", Arrays.asList("博物馆", "古迹", "文化", "历史"));
        map.put("自然风光", Arrays.asList("公园", "山水", "自然", "风景"));
        map.put("美食", Arrays.asList("美食", "餐厅", "小吃", "饮食"));
        map.put("购物", Arrays.asList("商场", "购物", "步行街", "商圈"));
        map.put("娱乐", Arrays.asList("乐园", "娱乐", "休闲", "活动"));
        return map;
    }

    @Override
    public RoutePlanAlgorithm.OptimalRoute planOptimalRoute(List<Integer> attractionIds, int maxDays, BigDecimal budget, String preference) {
        if (attractionIds == null || attractionIds.isEmpty() || maxDays <= 0
                || budget == null || budget.signum() <= 0 || preference == null || preference.isBlank()) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_OPTIMIZATION_PARAM_ERROR);
        }
        log.info("开始智能规划路线: 景点数={}, 天数={}, 预算={}, 偏好={}",
                attractionIds.size(), maxDays, budget, preference);

        RoutePlanAlgorithm.OptimalRoute optimalRoute = new RoutePlanAlgorithm.OptimalRoute();

        try {
            List<Attraction> attractions = attractionIds.stream()
                    .map(attractionService::getById)
                    .filter(Objects::nonNull)
                    .toList();

            if (attractions.isEmpty()) {
                throw new BusinessException(ErrorCodeEnum.ROUTE_OPTIMIZATION_NO_DATA);
            }

            Map<String, Double> weights = calculateWeights(preference);
            Map<Integer, List<Attraction>> dailyAttractions = distributeAttractionsToDays(attractions, maxDays);

            double totalDistance = 0.0;
            double totalCost = 0.0;
            double totalTime = 0.0;

            for (Map.Entry<Integer, List<Attraction>> entry : dailyAttractions.entrySet()) {
                int dayNumber = entry.getKey();
                List<Attraction> dayAttractionList = entry.getValue();

                List<Attraction> optimizedOrder = geneticAlgorithmTSP.optimizeRoute(dayAttractionList, preference);
                RoutePlanAlgorithm.RouteDayPlan dayPlan = createDayPlanWithRealData(dayNumber, optimizedOrder);
                optimalRoute.getDayPlans().add(dayPlan);

                totalDistance += dayPlan.getDistance();
                totalCost += dayPlan.getCost();
                totalTime += dayPlan.getTime();
            }

            double fitness = calculateFitness(totalDistance, totalCost, totalTime, budget.doubleValue(), weights);
            optimalRoute.setTotalDistance(totalDistance);
            optimalRoute.setTotalCost(totalCost);
            optimalRoute.setTotalTime(totalTime);
            optimalRoute.setTotalFitness(fitness);

            log.info("智能路线规划完成: days={}, 总距离={}km, 总成本={}元, 总时间={}分钟, 适应度={}",
                    maxDays, totalDistance, totalCost, totalTime, fitness);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("智能路线规划失败: {}", e.getMessage(), e);
            throw new RuntimeException("路线规划失败", e);
        }

        return optimalRoute;
    }
    private RoutePlanAlgorithm.RouteDayPlan createDayPlanWithRealData(int dayNumber, List<Attraction> attractions) {
        RoutePlanAlgorithm.RouteDayPlan dayPlan = new RoutePlanAlgorithm.RouteDayPlan();
        dayPlan.setDayNumber(dayNumber);

        List<Integer> attractionIds = new ArrayList<>();
        List<RoutePlanAlgorithm.RoutePoint> points = new ArrayList<>();
        double totalCost = 0.0;

        boolean coordinatesComplete = attractions.stream()
                .allMatch(attraction -> attraction.getLatitude() != null && attraction.getLongitude() != null);
        if (!coordinatesComplete) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_OPTIMIZATION_NO_DATA);
        }
        List<double[]> coordinates = attractions.stream()
                .map(a -> new double[]{a.getLongitude().doubleValue(), a.getLatitude().doubleValue()})
                .toList();

        AMapRouteService.RouteInfo routeInfo = coordinates.size() >= 2
                ? aMapRouteService.calculateMultiPointRoute(coordinates)
                : null;
        if (coordinates.size() >= 2 && routeInfo == null) {
            throw new BusinessException(ErrorCodeEnum.REALTIME_DATA_FETCH_FAILED);
        }

        for (Attraction attraction : attractions) {
            attractionIds.add(attraction.getId());

            RoutePlanAlgorithm.RoutePoint point = new RoutePlanAlgorithm.RoutePoint();
            point.setAttractionId(attraction.getId());
            totalCost += attraction.getTicketPrice() == null ? 0.0 : attraction.getTicketPrice().doubleValue();
            points.add(point);
        }

        if (routeInfo != null) {
            totalCost += routeInfo.getCost();
        }

        dayPlan.setAttractionIds(attractionIds);
        dayPlan.setPoints(points);
        dayPlan.setDistance(routeInfo == null ? 0.0 : routeInfo.getDistance());
        dayPlan.setTime(routeInfo == null ? 0.0 : routeInfo.getDuration());
        dayPlan.setCost(totalCost);

        return dayPlan;
    }

    private Map<String, Double> calculateWeights(String preference) {
        return switch (preference.toLowerCase()) {
            case "lowcost" -> Map.of("distance", 0.2, "cost", 0.6, "time", 0.2);
            case "fast" -> Map.of("distance", 0.3, "cost", 0.2, "time", 0.5);
            case "lowcarbon" -> Map.of("distance", 0.5, "cost", 0.3, "time", 0.2);
            default -> Map.of("distance", 0.33, "cost", 0.33, "time", 0.34);
        };
    }

    private Map<Integer, List<Attraction>> distributeAttractionsToDays(List<Attraction> attractions, int maxDays) {
        Map<Integer, List<Attraction>> dailyAttractions = new HashMap<>();
        int attractionsPerDay = (int) Math.ceil((double) attractions.size() / maxDays);

        for (int day = 1; day <= maxDays; day++) {
            int startIndex = (day - 1) * attractionsPerDay;
            int endIndex = Math.min(startIndex + attractionsPerDay, attractions.size());

            if (startIndex < attractions.size()) {
                dailyAttractions.put(day, attractions.subList(startIndex, endIndex));
            }
        }

        return dailyAttractions;
    }

    private double calculateFitness(double distance, double cost, double time, double budget, Map<String, Double> weights) {
        double distanceScore = 1.0 / (1.0 + distance);
        double costScore = 1.0 / (1.0 + cost / budget);
        double timeScore = 1.0 / (1.0 + time);

        return distanceScore * weights.getOrDefault("distance", 0.33) +
                costScore * weights.getOrDefault("cost", 0.33) +
                timeScore * weights.getOrDefault("time", 0.34);
    }

    @Override
    public RouteQualityEvaluationResult evaluateRouteQuality(Integer routeId) {
        try {
            Route route = routeService.getById(routeId);
            if (route == null) {
                throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
            }
            Integer durationDays = route.getDurationDays();
            if (durationDays == null || durationDays <= 0) {
                throw new BusinessException(ErrorCodeEnum.ROUTE_DURATION_ERROR);
            }

            List<RouteAttraction> routeAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId.longValue());
            List<Attraction> attractions = routeAttractions.stream()
                    .map(ra -> attractionService.getById(ra.getAttractionId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 计算各项指标
            double averageRating = attractions.stream()
                    .map(Attraction::getRating)
                    .filter(Objects::nonNull)
                    .mapToDouble(BigDecimal::doubleValue)
                    .average()
                    .orElse(0.0);
            int totalAttractions = attractions.size();
            double attractionsPerDay = (double) totalAttractions / durationDays;

            // 计算路线多样性
            Set<String> attractionTypes = new HashSet<>();
            attractions.forEach(attraction -> {
                String description = attraction.getDescription();
                if (description != null) {
                    for (String type : interestAttractionMap.keySet()) {
                        if (interestAttractionMap.get(type).stream().anyMatch(description::contains)) {
                            attractionTypes.add(type);
                        }
                    }
                }
            });
            double diversityScore = (double) attractionTypes.size() / interestAttractionMap.size();

            double ratingScore = Math.max(0.0, Math.min(1.0, averageRating / 5.0));
            double scheduleScore = Math.max(0.0, 1.0 - Math.abs(attractionsPerDay - 4.0) / 4.0);
            double qualityScore = ratingScore * 0.4 + scheduleScore * 0.3 + diversityScore * 0.3;

            log.info("评估路线质量成功: routeId={}, qualityScore={}", routeId, qualityScore);
            return RouteQualityEvaluationResult.builder()
                    .routeId(routeId)
                    .routeName(route.getTitle())
                    .averageRating(averageRating)
                    .totalAttractions(totalAttractions)
                    .attractionsPerDay(attractionsPerDay)
                    .diversityScore(diversityScore)
                    .qualityScore(qualityScore)
                    .recommendationLevel(getRecommendationLevel(qualityScore))
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("评估路线质量失败: routeId={}, error={}", routeId, e.getMessage());
            throw new RuntimeException("评估路线质量失败", e);
        }
    }

    private String getRecommendationLevel(double qualityScore) {
        if (qualityScore >= 0.8) {
            return "强烈推荐";
        } else if (qualityScore >= 0.6) {
            return "推荐";
        } else if (qualityScore >= 0.4) {
            return "一般";
        } else {
            return "不推荐";
        }
    }

    

    @Override
    public List<OptimizationSuggestion> getOptimizationSuggestions(Integer routeId) {
        if (routeId == null || routeId <= 0 || routeService.getById(routeId) == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }
        log.info("获取优化建议: routeId={}", routeId);
        return List.of(OptimizationSuggestion.builder()
                .id(1)
                .title("游览顺序优化")
                .description("保留每日景点安排，按地理距离优化当天游览顺序")
                .type("distance")
                .message("按景点经纬度执行最近邻顺序调整")
                .build());
    }

    @Override
    public boolean applyOptimization(ApplyOptimizationRequest request) {
        if (request == null || request.getRouteId() == null || request.getRouteId() <= 0) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_OPTIMIZATION_PARAM_ERROR);
        }
        Integer routeId = request.getRouteId();
        String optimizationType = resolveOptimizationType(request);
        return distributedLockService.executeWithLock("route-optimization:" + routeId,
                () -> {
                    Boolean applied = transactionTemplate.execute(
                            status -> applyOptimizationLocked(routeId, optimizationType, request));
                    if (!Boolean.TRUE.equals(applied)) {
                        throw new BusinessException(ErrorCodeEnum.ROUTE_OPTIMIZATION_FAILED);
                    }
                    return true;
                });
    }

    @Override
    public List<OptimizationHistoryItem> getOptimizationHistory(Integer routeId) {
        if (routeId == null || routeId <= 0) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_OPTIMIZATION_PARAM_ERROR);
        }
        log.info("获取优化历史: routeId={}", routeId);
        List<Object> records = redisTemplate.opsForList().range(
                OPTIMIZATION_HISTORY_PREFIX + routeId, 0, MAX_OPTIMIZATION_HISTORY - 1);
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream()
                .map(this::toOptimizationHistoryItem)
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean applyOptimizationLocked(
            Integer routeId, String optimizationType, ApplyOptimizationRequest request) {
        if (routeService.getById(routeId) == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }
        List<RouteAttraction> relations =
                routeAttractionService.getByRouteIdOrderByDayAndVisitForUpdate(routeId.longValue());
        if (relations.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_OPTIMIZATION_NO_DATA);
        }
        if (relations.size() > MAX_OPTIMIZABLE_ATTRACTIONS) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_OPTIMIZATION_PARAM_ERROR);
        }

        List<Integer> explicitOrder = readExplicitOrder(request);
        Map<Integer, Integer> explicitRanks = validateAndBuildRanks(explicitOrder, relations);
        Map<Integer, List<RouteAttraction>> relationsByDay = relations.stream()
                .collect(Collectors.groupingBy(
                        relation -> Optional.ofNullable(relation.getDayNumber()).orElse(1),
                        TreeMap::new,
                        Collectors.toList()));

        boolean changed = false;
        for (List<RouteAttraction> dailyRelations : relationsByDay.values()) {
            List<RouteAttraction> optimized = explicitRanks == null
                    ? optimizeDailyRelations(dailyRelations)
                    : dailyRelations.stream()
                            .sorted(Comparator.comparingInt(relation -> explicitRanks.get(relation.getAttractionId())))
                            .toList();
            for (int index = 0; index < optimized.size(); index++) {
                RouteAttraction relation = optimized.get(index);
                int expectedOrder = index + 1;
                if (!Objects.equals(relation.getVisitOrder(), expectedOrder)) {
                    relation.setVisitOrder(expectedOrder);
                    changed = true;
                }
            }
        }

        if (changed) {
            routeAttractionService.replaceRouteSchedule(routeId, relations);
            saveOptimizationHistoryAfterCommit(routeId, optimizationType);
        }
        log.info("应用路线优化完成: routeId={}, type={}, changed={}",
                routeId, optimizationType, changed);
        return true;
    }

    private List<RouteAttraction> optimizeDailyRelations(List<RouteAttraction> dailyRelations) {
        if (dailyRelations.size() <= 2) {
            return dailyRelations;
        }
        List<RouteAttraction> optimized = new ArrayList<>();
        List<RouteAttraction> remaining = new ArrayList<>(dailyRelations);
        Map<Integer, Attraction> attractionsById = new HashMap<>();
        for (RouteAttraction relation : dailyRelations) {
            Attraction attraction = attractionService.getById(relation.getAttractionId());
            if (attraction == null || attraction.getLatitude() == null || attraction.getLongitude() == null) {
                throw new BusinessException(ErrorCodeEnum.ROUTE_OPTIMIZATION_NO_DATA);
            }
            attractionsById.put(relation.getAttractionId(), attraction);
        }
        RouteAttraction current = remaining.remove(0);
        optimized.add(current);
        while (!remaining.isEmpty()) {
            Attraction currentAttraction = attractionsById.get(current.getAttractionId());
            RouteAttraction nearest = remaining.stream()
                    .min(Comparator.<RouteAttraction>comparingDouble(candidate -> distance(
                                    currentAttraction, attractionsById.get(candidate.getAttractionId())))
                            .thenComparing(RouteAttraction::getId))
                    .orElseThrow(() -> new BusinessException(ErrorCodeEnum.ROUTE_OPTIMIZATION_NO_DATA));
            optimized.add(nearest);
            remaining.remove(nearest);
            current = nearest;
        }
        return optimized;
    }

    private double distance(Attraction from, Attraction to) {
        double latitudeDifference = Math.toRadians(
                to.getLatitude().doubleValue() - from.getLatitude().doubleValue());
        double longitudeDifference = Math.toRadians(
                to.getLongitude().doubleValue() - from.getLongitude().doubleValue());
        double value = Math.sin(latitudeDifference / 2) * Math.sin(latitudeDifference / 2)
                + Math.cos(Math.toRadians(from.getLatitude().doubleValue()))
                * Math.cos(Math.toRadians(to.getLatitude().doubleValue()))
                * Math.sin(longitudeDifference / 2) * Math.sin(longitudeDifference / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
    }

    private String resolveOptimizationType(ApplyOptimizationRequest request) {
        String type = request.getOptimizationType();
        if ((type == null || type.isBlank()) && request.getSuggestion() != null) {
            type = getText(request.getSuggestion().get("optimizationType"));
            if (type == null) {
                type = getText(request.getSuggestion().get("type"));
            }
        }
        if (type == null || type.isBlank()) {
            type = "distance";
        }
        return switch (type.trim().toLowerCase(Locale.ROOT)) {
            case "distance", "shortest" -> "distance";
            default -> throw new BusinessException(ErrorCodeEnum.ROUTE_OPTIMIZATION_PARAM_ERROR);
        };
    }

    private String getText(com.fasterxml.jackson.databind.JsonNode node) {
        return node != null && node.isTextual() ? node.textValue() : null;
    }

    private List<Integer> readExplicitOrder(ApplyOptimizationRequest request) {
        com.fasterxml.jackson.databind.JsonNode orderNode = null;
        if (request.getSuggestion() != null) {
            orderNode = request.getSuggestion().get("attractionOrder");
        }
        if (orderNode == null && request.getParameters() != null) {
            orderNode = request.getParameters().get("attractionOrder");
        }
        if (orderNode == null) {
            return null;
        }
        if (!orderNode.isArray() || orderNode.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_OPTIMIZATION_PARAM_ERROR);
        }
        List<Integer> order = new ArrayList<>();
        for (com.fasterxml.jackson.databind.JsonNode item : orderNode) {
            if (!item.canConvertToInt() || item.intValue() <= 0) {
                throw new BusinessException(ErrorCodeEnum.ROUTE_OPTIMIZATION_PARAM_ERROR);
            }
            order.add(item.intValue());
        }
        return order;
    }

    private Map<Integer, Integer> validateAndBuildRanks(
            List<Integer> explicitOrder, List<RouteAttraction> relations) {
        if (explicitOrder == null) {
            return null;
        }
        Set<Integer> routeAttractionIds = relations.stream()
                .map(RouteAttraction::getAttractionId)
                .collect(Collectors.toSet());
        if (explicitOrder.size() != routeAttractionIds.size()
                || new HashSet<>(explicitOrder).size() != explicitOrder.size()
                || !routeAttractionIds.equals(new HashSet<>(explicitOrder))) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_OPTIMIZATION_PARAM_ERROR);
        }
        Map<Integer, Integer> ranks = new HashMap<>();
        for (int index = 0; index < explicitOrder.size(); index++) {
            ranks.put(explicitOrder.get(index), index);
        }
        return ranks;
    }

    private void saveOptimizationHistoryAfterCommit(Integer routeId, String optimizationType) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            saveOptimizationHistorySafely(routeId, optimizationType);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                saveOptimizationHistorySafely(routeId, optimizationType);
            }
        });
    }

    private void saveOptimizationHistorySafely(Integer routeId, String optimizationType) {
        try {
            saveOptimizationHistory(routeId, optimizationType);
        } catch (RuntimeException exception) {
            log.warn("路线优化已提交，但历史缓存写入失败: routeId={}, type={}, errorType={}",
                    routeId, optimizationType, exception.getClass().getSimpleName());
        }
    }

    private void saveOptimizationHistory(Integer routeId, String optimizationType) {
        String historyKey = OPTIMIZATION_HISTORY_PREFIX + routeId;
        OptimizationHistoryItem item = OptimizationHistoryItem.builder()
                .routeId(routeId)
                .optimizationType(optimizationType)
                .description("已优化每日景点游览顺序")
                .appliedAt(LocalDateTime.now())
                .build();
        redisTemplate.opsForList().leftPush(historyKey, item);
        redisTemplate.opsForList().trim(historyKey, 0, MAX_OPTIMIZATION_HISTORY - 1);
        redisTemplate.expire(historyKey, 30, TimeUnit.DAYS);
    }

    private OptimizationHistoryItem toOptimizationHistoryItem(Object value) {
        if (value instanceof OptimizationHistoryItem item) {
            return item;
        }
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }
        Object routeId = map.get("routeId");
        Object appliedAt = map.get("appliedAt");
        if (!(routeId instanceof Number number)) {
            return null;
        }
        LocalDateTime timestamp;
        try {
            timestamp = appliedAt instanceof LocalDateTime localDateTime
                    ? localDateTime
                    : LocalDateTime.parse(String.valueOf(appliedAt));
        } catch (Exception ignored) {
            return null;
        }
        return new OptimizationHistoryItem(
                number.intValue(),
                Objects.toString(map.get("optimizationType"), "distance"),
                Objects.toString(map.get("description"), ""),
                timestamp);
    }
    
}
