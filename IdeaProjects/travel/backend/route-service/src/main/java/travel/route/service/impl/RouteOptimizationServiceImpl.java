package travel.route.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.entity.route_planning.Route;
import travel.common.entity.route_planning.RouteAttraction;

import travel.common.repository.RoutePlanRepository;
import travel.route.algorithm.GeneticAlgorithmTSP;
import travel.route.algorithm.RoutePlanAlgorithm;
import travel.route.service.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import travel.common.utils.AMapRouteService;
import travel.common.utils.CommonUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteOptimizationServiceImpl implements RouteOptimizationService {

    private static final Logger log = LoggerFactory.getLogger(RouteOptimizationServiceImpl.class);

    private final RoutePlanAlgorithm routePlanAlgorithm;
    private final RouteService routeService;
    private final AttractionService attractionService;
    private final RouteAttractionService routeAttractionService;
    private final RoutePlanRepository routePlanRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AMapRouteService aMapRouteService;
    private final GeneticAlgorithmTSP geneticAlgorithmTSP;

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
        log.info("开始智能规划路线: 景点数={}, 天数={}, 预算={}, 偏好={}",
                attractionIds.size(), maxDays, budget, preference);

        RoutePlanAlgorithm.OptimalRoute optimalRoute = new RoutePlanAlgorithm.OptimalRoute();

        try {
            List<Attraction> attractions = attractionIds.stream()
                    .map(attractionService::getById)
                    .filter(Objects::nonNull)
                    .toList();

            if (attractions.isEmpty()) {
                throw new RuntimeException("没有有效的景点数据");
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

        } catch (Exception e) {
            log.error("智能路线规划失败: {}", e.getMessage(), e);
            throw new RuntimeException("路线规划失败: " + e.getMessage());
        }

        return optimalRoute;
    }
    private RoutePlanAlgorithm.RouteDayPlan createDayPlanWithRealData(int dayNumber, List<Attraction> attractions) {
        RoutePlanAlgorithm.RouteDayPlan dayPlan = new RoutePlanAlgorithm.RouteDayPlan();
        dayPlan.setDayNumber(dayNumber);

        List<Integer> attractionIds = new ArrayList<>();
        List<RoutePlanAlgorithm.RoutePoint> points = new ArrayList<>();
        double totalDistance = 0.0;
        double totalTime = 0.0;
        double totalCost = 0.0;

        List<double[]> coordinates = attractions.stream()
                .filter(a -> a.getLatitude() != null && a.getLongitude() != null)
                .map(a -> new double[]{a.getLongitude().doubleValue(), a.getLatitude().doubleValue()})
                .toList();

        AMapRouteService.RouteInfo routeInfo = coordinates.size() >= 2
                ? aMapRouteService.calculateMultiPointRoute(coordinates)
                : null;

        for (int i = 0; i < attractions.size(); i++) {
            Attraction attraction = attractions.get(i);
            attractionIds.add(attraction.getId());

            RoutePlanAlgorithm.RoutePoint point = new RoutePlanAlgorithm.RoutePoint();
            point.setAttractionId(attraction.getId());

            if (i > 0 && routeInfo != null && !routeInfo.getSteps().isEmpty()) {
                AMapRouteService.RouteStep step = routeInfo.getSteps().get(Math.min(i - 1, routeInfo.getSteps().size() - 1));
                point.setDistance(step.getDistance());
                point.setTime(step.getDuration());
                totalDistance += step.getDistance();
                totalTime += step.getDuration();
            } else if (i > 0) {
                Attraction prevAttraction = attractions.get(i - 1);
                double distance = CommonUtil.calculateDistance(
                        prevAttraction.getLatitude().doubleValue(), prevAttraction.getLongitude().doubleValue(),
                        attraction.getLatitude().doubleValue(), attraction.getLongitude().doubleValue());
                double time = estimateTravelTime(distance);
                point.setDistance(distance);
                point.setTime(time);
                totalDistance += distance;
                totalTime += time;
            }

            totalCost += attraction.getTicketPrice().doubleValue();
            points.add(point);
        }

        if (routeInfo != null) {
            totalCost += routeInfo.getCost();
        }

        dayPlan.setAttractionIds(attractionIds);
        dayPlan.setPoints(points);
        dayPlan.setDistance(totalDistance);
        dayPlan.setTime(totalTime);
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

    private double estimateTravelTime(double distanceKm) {
        return distanceKm / 30.0 * 60;
    }
    
    @Override
    public Map<String, Object> adjustRoute(Integer routeId, String adjustmentType, Map<String, Object> adjustmentParams) {
        try {
            Route route = routeService.getById(routeId);
            if (route == null) {
                throw new RuntimeException("路线不存在");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("originalRoute", route);

            switch (adjustmentType) {
                case "addAttraction":
                    // 添加景点
                    Integer attractionId = (Integer) adjustmentParams.get("attractionId");
                    Integer dayNumber = (Integer) adjustmentParams.get("dayNumber");
                    if (attractionId != null && dayNumber != null) {
                        // 实现添加景点逻辑
                        log.info("添加景点: attractionId={}, dayNumber={}", attractionId, dayNumber);
                        // 1. 验证景点是否存在
                        Attraction attraction = attractionService.getById(attractionId);
                        if (attraction != null) {
                            // 2. 创建新的RouteAttraction记录
                            RouteAttraction routeAttraction = new RouteAttraction();
                            // 设置关联对象
                            Route newRoute = new Route();
                            newRoute.setId(routeId);
                            routeAttraction.setRoute(newRoute);
                            routeAttraction.setAttraction(attraction);
                            routeAttraction.setDayNumber(dayNumber);
                            routeAttraction.setVisitOrder(1); // 默认顺序
                            // 3. 保存到数据库
                            routeAttractionService.save(routeAttraction);
                            result.put("addedAttraction", attraction);
                        }
                    }
                    break;
                case "removeAttraction":
                    // 移除景点
                    Integer removeAttractionId = (Integer) adjustmentParams.get("attractionId");
                    if (removeAttractionId != null) {
                        // 实现移除景点逻辑
                        log.info("移除景点: attractionId={}", removeAttractionId);
                        // 1. 查找并删除RouteAttraction记录
                        List<RouteAttraction> routeAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId.longValue());
                        for (RouteAttraction ra : routeAttractions) {
                            if (ra.getAttractionId().equals(removeAttractionId)) {
                                routeAttractionService.removeById(ra.getId());
                                result.put("removedAttractionId", removeAttractionId);
                                break;
                            }
                        }
                    }
                    break;
                case "reorderAttractions":
                    // 重新排序景点
                    Object attractionOrderObj = adjustmentParams.get("attractionOrder");
                    if (attractionOrderObj instanceof List) {
                        List<Integer> attractionOrder = ((List<?>) attractionOrderObj).stream()
                                .filter(item -> item instanceof Integer)
                                .map(item -> (Integer) item)
                                .collect(Collectors.toList());
                        if (!attractionOrder.isEmpty()) {
                            // 实现重新排序逻辑
                            log.info("重新排序景点: attractionOrder={}", attractionOrder);
                            // 1. 获取当前路线的景点
                            List<RouteAttraction> routeAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId.longValue());
                            // 2. 更新顺序
                            int order = 1;
                            for (Integer attractionIdToReorder : attractionOrder) {
                                for (RouteAttraction ra : routeAttractions) {
                                    if (ra.getAttractionId().equals(attractionIdToReorder)) {
                                        ra.setVisitOrder(order++);
                                        routeAttractionService.updateById(ra);
                                        break;
                                    }
                                }
                            }
                            result.put("newOrder", attractionOrder);
                        }
                    }
                    break;
                case "adjustDayDistribution":
                    // 调整天数分配
                    Integer newDays = (Integer) adjustmentParams.get("newDays");
                    if (newDays != null && newDays > 0) {
                        // 实现调整天数逻辑
                        log.info("调整天数分配: newDays={}", newDays);
                        // 1. 更新路线的天数
                        route.setDurationDays(newDays);
                        routeService.updateById(route);
                        // 2. 重新分配景点到每天
                        List<RouteAttraction> routeAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId.longValue());
                        int totalAttractions = routeAttractions.size();
                        int attractionsPerDay = totalAttractions / newDays;
                        int remainder = totalAttractions % newDays;
                        
                        int currentDay = 1;
                        int count = 0;
                        for (RouteAttraction ra : routeAttractions) {
                            ra.setDayNumber(currentDay);
                            routeAttractionService.updateById(ra);
                            count++;
                            
                            if (count >= attractionsPerDay + (currentDay <= remainder ? 1 : 0)) {
                                currentDay++;
                                count = 0;
                            }
                        }
                        result.put("newDays", newDays);
                    }
                    break;
                default:
                    throw new RuntimeException("不支持的调整类型");
            }

            log.info("调整路线成功: routeId={}, adjustmentType={}", routeId, adjustmentType);
            result.put("success", true);
            return result;
        } catch (Exception e) {
            log.error("调整路线失败: routeId={}, error={}", routeId, e.getMessage());
            throw new RuntimeException("调整路线失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getRouteRecommendations(Integer cityId, int days, List<String> interests, BigDecimal budget) {
        try {
            List<Attraction> attractions = attractionService.getByCityId(cityId);
            List<Attraction> filteredAttractions = filterAttractionsByInterests(attractions, interests);

            if (filteredAttractions.isEmpty()) {
                filteredAttractions = attractions;
            }

            List<Integer> attractionIds = filteredAttractions.stream()
                    .map(Attraction::getId)
                    .toList();

            List<Map<String, Object>> recommendations = new ArrayList<>();

            List<String> preferences = Arrays.asList("balanced", "lowCost", "fast", "lowCarbon");
            for (String preference : preferences) {
                try {
                    RoutePlanAlgorithm.OptimalRoute optimalRoute = routePlanAlgorithm.planOptimalRoute(attractionIds, days, budget, preference);
                    Map<String, Object> recommendation = convertToRecommendation(optimalRoute, filteredAttractions, preference);
                    recommendations.add(recommendation);
                } catch (Exception e) {
                    log.warn("生成推荐路线失败: preference={}, error={}", preference, e.getMessage());
                }
            }

            recommendations.sort((a, b) -> Double.compare(
                    (double) b.getOrDefault("fitness", 0.0),
                    (double) a.getOrDefault("fitness", 0.0)));

            log.info("获取路线推荐成功: cityId={}, days={}, interests={}, count={}", cityId, days, interests, recommendations.size());
            return recommendations;
        } catch (Exception e) {
            log.error("获取路线推荐失败: cityId={}, error={}", cityId, e.getMessage());
            throw new RuntimeException("获取路线推荐失败: " + e.getMessage());
        }
    }
    @Override
    public double calculateRouteSimilarity(Integer routeId1, Integer routeId2) {
        try {
            // 获取两条路线的景点
            List<RouteAttraction> attractions1 = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId1.longValue());
            List<RouteAttraction> attractions2 = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId2.longValue());

            Set<Integer> attractionSet1 = attractions1.stream().map(RouteAttraction::getAttractionId).collect(Collectors.toSet());
            Set<Integer> attractionSet2 = attractions2.stream().map(RouteAttraction::getAttractionId).collect(Collectors.toSet());

            // 计算Jaccard相似度
            Set<Integer> intersection = new HashSet<>(attractionSet1);
            intersection.retainAll(attractionSet2);

            Set<Integer> union = new HashSet<>(attractionSet1);
            union.addAll(attractionSet2);

            double similarity = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();

            log.info("计算路线相似度成功: routeId1={}, routeId2={}, similarity={}", routeId1, routeId2, similarity);
            return similarity;
        } catch (Exception e) {
            log.error("计算路线相似度失败: routeId1={}, routeId2={}, error={}", routeId1, routeId2, e.getMessage());
            throw new RuntimeException("计算路线相似度失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> evaluateRouteQuality(Integer routeId) {
        try {
            Route route = routeService.getById(routeId);
            if (route == null) {
                throw new RuntimeException("路线不存在");
            }

            List<RouteAttraction> routeAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId.longValue());
            List<Attraction> attractions = routeAttractions.stream()
                    .map(ra -> attractionService.getById(ra.getAttractionId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 计算各项指标
            double averageRating = attractions.stream().mapToDouble(a -> a.getRating().doubleValue()).average().orElse(0.0);
            int totalAttractions = attractions.size();
            int days = route.getDurationDays();
            double attractionsPerDay = (double) totalAttractions / days;

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

            // 综合评分
            double qualityScore = (averageRating * 0.4) + (attractionsPerDay * 0.3) + (diversityScore * 0.3);

            Map<String, Object> evaluation = new HashMap<>();
            evaluation.put("routeId", routeId);
            evaluation.put("routeName", route.getTitle());
            evaluation.put("averageRating", averageRating);
            evaluation.put("totalAttractions", totalAttractions);
            evaluation.put("attractionsPerDay", attractionsPerDay);
            evaluation.put("diversityScore", diversityScore);
            evaluation.put("qualityScore", qualityScore);
            evaluation.put("recommendationLevel", getRecommendationLevel(qualityScore));

            log.info("评估路线质量成功: routeId={}, qualityScore={}", routeId, qualityScore);
            return evaluation;
        } catch (Exception e) {
            log.error("评估路线质量失败: routeId={}, error={}", routeId, e.getMessage());
            throw new RuntimeException("评估路线质量失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> generateRouteAlternatives(Integer routeId, int alternativeCount) {
        try {
            Route route = routeService.getById(routeId);
            if (route == null) {
                throw new RuntimeException("路线不存在");
            }

            List<RouteAttraction> routeAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId.longValue());
            List<Integer> attractionIds = routeAttractions.stream()
                    .map(RouteAttraction::getAttractionId)
                    .collect(Collectors.toList());

            List<Map<String, Object>> alternatives = new ArrayList<>();

            // 生成不同偏好的备选方案
            List<String> preferences = Arrays.asList("balanced", "lowCost", "fast", "lowCarbon");
            for (int i = 0; i < alternativeCount && i < preferences.size(); i++) {
                try {
                    RoutePlanAlgorithm.OptimalRoute optimalRoute = routePlanAlgorithm.planOptimalRoute(attractionIds, route.getDurationDays(), new BigDecimal(1000), preferences.get(i));
                    Map<String, Object> alternative = convertToRecommendation(optimalRoute, new ArrayList<>(), preferences.get(i));
                    alternative.put("originalRouteId", routeId);
                    alternatives.add(alternative);
                } catch (Exception e) {
                    log.warn("生成备选路线失败: index={}, error={}", i, e.getMessage());
                }
            }

            log.info("生成路线备选方案成功: routeId={}, count={}", routeId, alternatives.size());
            return alternatives;
        } catch (Exception e) {
            log.error("生成路线备选方案失败: routeId={}, error={}", routeId, e.getMessage());
            throw new RuntimeException("生成路线备选方案失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getRouteAnalysis(Integer routeId) {
        try {
            Route route = routeService.getById(routeId);
            if (route == null) {
                throw new RuntimeException("路线不存在");
            }

            List<RouteAttraction> routeAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId.longValue());
            List<Attraction> attractions = routeAttractions.stream()
                    .map(ra -> attractionService.getById(ra.getAttractionId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 计算各项分析指标
            double totalDistance = calculateTotalDistance(attractions);
            double estimatedCost = calculateEstimatedCost(attractions);
            double estimatedTime = calculateEstimatedTime(attractions);
            double averageRating = attractions.stream().mapToDouble(a -> a.getRating().doubleValue()).average().orElse(0.0);

            // 分析景点类型分布
            Map<String, Integer> attractionTypeDistribution = analyzeAttractionTypes(attractions);

            Map<String, Object> analysis = new HashMap<>();
            analysis.put("routeId", routeId);
            analysis.put("routeName", route.getTitle());
            analysis.put("totalAttractions", attractions.size());
            analysis.put("totalDistance", totalDistance);
            analysis.put("estimatedCost", estimatedCost);
            analysis.put("estimatedTime", estimatedTime);
            analysis.put("averageRating", averageRating);
            analysis.put("attractionTypeDistribution", attractionTypeDistribution);
            analysis.put("recommendations", generateRouteRecommendations(attractions));

            log.info("获取路线详细分析成功: routeId={}", routeId);
            return analysis;
        } catch (Exception e) {
            log.error("获取路线详细分析失败: routeId={}, error={}", routeId, e.getMessage());
            throw new RuntimeException("获取路线详细分析失败: " + e.getMessage());
        }
    }
    
    private Map<String, Object> convertToRecommendation(RoutePlanAlgorithm.OptimalRoute optimalRoute, List<Attraction> attractions, String preference) {
        Map<String, Object> recommendation = new HashMap<>();
        recommendation.put("preference", preference);
        recommendation.put("fitness", optimalRoute.getTotalFitness());
        recommendation.put("totalDistance", optimalRoute.getTotalDistance());
        recommendation.put("totalCost", optimalRoute.getTotalCost());
        recommendation.put("totalTime", optimalRoute.getTotalTime());

        List<Map<String, Object>> dayPlans = new ArrayList<>();
        if (optimalRoute.getDayPlans() != null) {
            for (RoutePlanAlgorithm.RouteDayPlan dayPlan : optimalRoute.getDayPlans()) {
                Map<String, Object> dayPlanMap = new HashMap<>();
                dayPlanMap.put("dayNumber", dayPlan.getDayNumber());
                dayPlanMap.put("attractionIds", dayPlan.getAttractionIds());
                dayPlanMap.put("distance", dayPlan.getDistance());
                dayPlanMap.put("cost", dayPlan.getCost());
                dayPlanMap.put("time", dayPlan.getTime());
                dayPlans.add(dayPlanMap);
            }
        }
        recommendation.put("dayPlans", dayPlans);

        return recommendation;
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

    

    private boolean isHoliday(LocalDate date) {
        // 简单实现节假日判断逻辑
        // 这里可以根据实际的节假日列表进行判断
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        
        // 元旦
        if (month == 1 && day == 1) {
            return true;
        }
        // 春节（这里简单假设为正月初一到初七）
        // 实际项目中应该根据农历计算
        if (month == 2 && day <= 7) {
            return true;
        }
        // 清明节
        if (month == 4 && day >= 4 && day <= 6) {
            return true;
        }
        // 劳动节
        if (month == 5 && day <= 3) {
            return true;
        }
        // 端午节
        if (month == 6 && day >= 12 && day <= 14) {
            return true;
        }
        // 中秋节
        if (month == 9 && day >= 19 && day <= 21) {
            return true;
        }
        // 国庆节
        if (month == 10 && day <= 7) {
            return true;
        }
        
        return false;
    }

    private String getCrowdLevel(int crowd) {
        if (crowd > 1000) {
            return "拥挤";
        } else if (crowd > 500) {
            return "较多";
        } else if (crowd > 200) {
            return "适中";
        } else {
            return "较少";
        }
    }

    private String getSuggestedTime(int crowd, boolean isWeekend) {
        if (crowd > 1000) {
            return isWeekend ? "9:00前或16:00后" : "10:00前或15:00后";
        } else {
            return "正常时间";
        }
    }

    private double calculateTotalDistance(List<Attraction> attractions) {
        if (attractions == null || attractions.size() < 2) {
            return 0.0;
        }

        double totalDistance = 0.0;
        for (int i = 0; i < attractions.size() - 1; i++) {
            Attraction current = attractions.get(i);
            Attraction next = attractions.get(i + 1);
            // 使用Haversine公式计算两个经纬度点之间的距离
            totalDistance += calculateDistance(current.getLatitude().doubleValue(), current.getLongitude().doubleValue(),
                    next.getLatitude().doubleValue(), next.getLongitude().doubleValue());
        }

        return totalDistance;
    }

    /**
     * 使用Haversine公式计算两个经纬度点之间的距离（公里）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // 地球半径（公里）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    

    @Override
    public Map<String, Object> predictRouteCrowd(Integer routeId, String date) {
        try {
            Route route = routeService.getById(routeId);
            if (route == null) {
                throw new RuntimeException("路线不存在");
            }

            LocalDate predictDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            boolean isWeekend = predictDate.getDayOfWeek().getValue() >= 6;
            boolean isHoliday = isHoliday(predictDate);

            List<RouteAttraction> routeAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId.longValue());
            List<Attraction> attractions = routeAttractions.stream()
                    .map(ra -> attractionService.getById(ra.getAttractionId()))
                    .filter(Objects::nonNull)
                    .toList();

            List<Map<String, Object>> crowdPredictions = attractions.stream()
                    .map(attraction -> {
                        int baseCrowd = attraction.getViewCount() / 1000;
                        int multiplier = isHoliday ? 3 : (isWeekend ? 2 : 1);
                        int predictedCrowd = baseCrowd * multiplier;

                        Map<String, Object> prediction = new HashMap<>();
                        prediction.put("attractionId", attraction.getId());
                        prediction.put("attractionName", attraction.getName());
                        prediction.put("predictedCrowd", predictedCrowd);
                        prediction.put("crowdLevel", getCrowdLevel(predictedCrowd));
                        prediction.put("suggestedTime", getSuggestedTime(predictedCrowd, isWeekend));
                        return prediction;
                    })
                    .toList();

            Map<String, Object> result = new HashMap<>();
            result.put("routeId", routeId);
            result.put("routeName", route.getTitle());
            result.put("predictDate", date);
            result.put("isWeekend", isWeekend);
            result.put("isHoliday", isHoliday);
            result.put("crowdPredictions", crowdPredictions);

            log.info("预测路线人流量成功: routeId={}, date={}", routeId, date);
            return result;
        } catch (Exception e) {
            log.error("预测路线人流量失败: routeId={}, error={}", routeId, e.getMessage());
            throw new RuntimeException("预测路线人流量失败: " + e.getMessage());
        }
    }
    
    private double calculateEstimatedCost(List<Attraction> attractions) {
        if (attractions == null || attractions.isEmpty()) {
            return 0.0;
        }

        // 计算门票总成本
        double ticketCost = attractions.stream()
                .mapToDouble(a -> a.getTicketPrice().doubleValue())
                .sum();

        // 计算交通成本（假设每天50元）
        double transportCost = 50.0;

        // 计算餐饮成本（假设每天100元）
        double foodCost = 100.0;

        // 计算总成本
        double totalCost = ticketCost + transportCost + foodCost;

        return totalCost;
    }

    private double calculateEstimatedTime(List<Attraction> attractions) {
        if (attractions == null || attractions.isEmpty()) {
            return 0.0;
        }

        // 每个景点的平均游览时间（小时）
        double avgVisitTimePerAttraction = 2.0;
        // 景点间的平均交通时间（小时）
        double avgTransportTimeBetweenAttractions = 0.5;

        // 计算游览时间
        double visitTime = attractions.size() * avgVisitTimePerAttraction;
        // 计算交通时间
        double transportTime = (attractions.size() - 1) * avgTransportTimeBetweenAttractions;

        // 计算总时间
        double totalTime = visitTime + transportTime;

        return totalTime;
    }

    private Map<String, Integer> analyzeAttractionTypes(List<Attraction> attractions) {
        Map<String, Integer> typeDistribution = new HashMap<>();
        for (Attraction attraction : attractions) {
            String description = attraction.getDescription();
            if (description != null) {
                for (String type : interestAttractionMap.keySet()) {
                    if (interestAttractionMap.get(type).stream().anyMatch(description::contains)) {
                        typeDistribution.put(type, typeDistribution.getOrDefault(type, 0) + 1);
                    }
                }
            }
        }
        return typeDistribution;
    }

    private List<String> generateRouteRecommendations(List<Attraction> attractions) {
        List<String> recommendations = new ArrayList<>();
        if (attractions.size() > 10) {
            recommendations.add("建议适当减少景点数量，提高游览体验");
        }
        if (attractions.stream().filter(a -> a.getTicketPrice().compareTo(BigDecimal.valueOf(50)) > 0).count() > attractions.size() / 2) {
            recommendations.add("建议增加一些免费景点，降低游览成本");
        }
        return recommendations;
    }

    @Override
    public boolean saveUserRoutePreferences(Integer userId, Map<String, Object> preferences) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId无效");
        }
        if (preferences == null) {
            throw new IllegalArgumentException("preferences为null");
        }

        try {
            String redisKey = "user:route:preferences:" + userId;
            redisTemplate.opsForValue().set(redisKey, preferences);
            redisTemplate.expire(redisKey, 30, TimeUnit.DAYS);

            log.info("保存用户路线偏好成功: userId={}", userId);
            return true;
        } catch (Exception e) {
            log.error("保存用户路线偏好失败: userId={}, error={}", userId, e.getMessage());
            throw new RuntimeException("保存用户路线偏好失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getOptimizationSuggestions(Integer routeId) {
        log.info("获取优化建议: routeId={}", routeId);
        return List.of(Map.of("type", "general", "message", "建议合理安排景点游览顺序"));
    }

    @Override
    public boolean applyOptimization(Map<String, Object> optimizationData) {
        log.info("应用优化: optimizationData={}", optimizationData);
        return true;
    }

    @Override
    public List<Map<String, Object>> getOptimizationHistory(Integer routeId) {
        log.info("获取优化历史: routeId={}", routeId);
        return Collections.emptyList();
    }
    
    @Override
    public List<Map<String, Object>> getPersonalizedRouteRecommendations(Integer userId, Integer cityId, int days) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId无效");
        }
        if (cityId == null || cityId <= 0) {
            throw new IllegalArgumentException("cityId无效");
        }
        if (days <= 0 || days > 30) {
            throw new IllegalArgumentException("days无效，应为1-30天");
        }

        try {
            String redisKey = "user:route:preferences:" + userId;
            Map<String, Object> preferences = null;

            Object preferencesObj = redisTemplate.opsForValue().get(redisKey);
            if (preferencesObj instanceof Map<?, ?> map) {
                preferences = new HashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String key) {
                        preferences.put(key, entry.getValue());
                    }
                }
            }

            List<Attraction> attractions = attractionService.getByCityId(cityId);

            if (attractions == null || attractions.isEmpty()) {
                log.warn("获取用户个性化路线推荐: 城市无景点数据: cityId={}", cityId);
                return Collections.emptyList();
            }

            List<String> interests = Optional.ofNullable(preferences)
                    .map(p -> p.get("interests"))
                    .filter(obj -> obj instanceof List<?>)
                    .map(obj -> ((List<?>) obj).stream()
                            .filter(item -> item instanceof String)
                            .map(item -> (String) item)
                            .toList())
                    .orElse(Collections.emptyList());

            List<Attraction> filteredAttractions = interests.isEmpty()
                    ? attractions
                    : filterAttractionsByInterests(attractions, interests);

            if (filteredAttractions.isEmpty()) {
                filteredAttractions = attractions;
            }

            List<Route> recommendedRoutes = routePlanRepository.recommendRoutesByPreferences(cityId.longValue(), interests);

            List<Integer> attractionIds = filteredAttractions.stream()
                    .map(Attraction::getId)
                    .toList();

            List<Map<String, Object>> recommendations = recommendedRoutes.stream()
                    .map(route -> {
                        Map<String, Object> rec = new HashMap<>();
                        rec.put("routeId", route.getId());
                        rec.put("routeName", route.getTitle());
                        rec.put("preference", "user_preferred");
                        rec.put("days", route.getDurationDays());
                        rec.put("userId", userId);
                        return rec;
                    })
                    .collect(Collectors.toCollection(ArrayList::new));

            List<String> preferenceList = Arrays.asList("balanced", "lowCost", "fast", "lowCarbon");
            for (String pref : preferenceList) {
                try {
                    RoutePlanAlgorithm.OptimalRoute optimalRoute = routePlanAlgorithm.planOptimalRoute(
                            attractionIds, days, new BigDecimal(1000), pref);
                    Map<String, Object> recommendation = convertToRecommendation(optimalRoute, filteredAttractions, pref);
                    recommendation.put("userId", userId);
                    recommendations.add(recommendation);
                } catch (Exception e) {
                    log.warn("生成个性化路线推荐失败: userId={}, preference={}, error={}", userId, pref, e.getMessage());
                }
            }

            recommendations.sort((a, b) -> Double.compare(
                    (double) b.getOrDefault("fitness", 0.0),
                    (double) a.getOrDefault("fitness", 0.0)));

            log.info("获取用户个性化路线推荐成功: userId={}, cityId={}, days={}, count={}", userId, cityId, days, recommendations.size());
            return recommendations;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取用户个性化路线推荐失败: userId={}, error={}", userId, e.getMessage());
            throw new RuntimeException("获取用户个性化路线推荐失败: " + e.getMessage());
        }
    }

    

    private List<Attraction> filterAttractionsByInterests(List<Attraction> attractions, List<String> interests) {
        if (interests == null || interests.isEmpty()) {
            return attractions;
        }

        return attractions.stream()
                .filter(attraction -> {
                    String description = attraction.getDescription();
                    if (description == null) {
                        return false;
                    }
                    return interests.stream().anyMatch(interest -> {
                        List<String> keywords = interestAttractionMap.getOrDefault(interest, Collections.emptyList());
                        return keywords.stream().anyMatch(description::contains);
                    });
                })
                .toList();
    }
}
