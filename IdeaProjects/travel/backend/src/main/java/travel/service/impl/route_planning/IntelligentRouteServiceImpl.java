package travel.service.impl.route_planning;

import travel.entity.travel_recommendation.Attraction;
import travel.entity.route_planning.Route;
import travel.entity.route_planning.RouteAttraction;
import travel.service.route_planning.IntelligentRouteService;
import travel.service.route_planning.RouteAttractionService;
import travel.service.route_planning.RouteOptimizationService;
import travel.service.route_planning.RouteService;
import travel.service.travel_recommendation.AttractionService;
import travel.utils.CacheUtil;
import travel.utils.CommonUtil;
import travel.utils.ExceptionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class IntelligentRouteServiceImpl implements IntelligentRouteService {

    private final RouteService routeService;
    private final AttractionService attractionService;
    private final RouteAttractionService routeAttractionService;
    private final RouteOptimizationService routeOptimizationService;
    private final CacheUtil cacheUtil;

    private static final String ROUTE_RECOMMENDATION_PREFIX = "route:recommendation:";
    private static final String PERSONALIZED_ROUTE_PREFIX = "route:personalized:";
    private static final String ROUTE_OPTIMIZATION_PREFIX = "route:optimization:";
    private static final String POPULAR_ROUTES_PREFIX = "route:popular:";
    private static final String SIMILAR_ROUTES_PREFIX = "route:similar:";
    private static final String SEASONAL_ROUTES_PREFIX = "route:seasonal:";
    private static final String THEME_ROUTES_PREFIX = "route:theme:";

    private static final long CACHE_EXPIRE_MINUTES = 30;
    private static final long CACHE_EXPIRE_HOURS = 2;

    private final Map<String, List<String>> seasonAttractionMap = new HashMap<>();
    private final Map<String, List<String>> themeAttractionMap = new HashMap<>();

    @PostConstruct
    public void init() {
        seasonAttractionMap.put("spring", Arrays.asList("公园", "花园", "自然", "山水"));
        seasonAttractionMap.put("summer", Arrays.asList("海滩", "水上", "避暑", "公园"));
        seasonAttractionMap.put("autumn", Arrays.asList("红叶", "公园", "山水", "自然"));
        seasonAttractionMap.put("winter", Arrays.asList("温泉", "滑雪", "室内", "文化"));

        themeAttractionMap.put("文化历史", Arrays.asList("博物馆", "古迹", "文化", "历史"));
        themeAttractionMap.put("自然风光", Arrays.asList("公园", "山水", "自然", "风景"));
        themeAttractionMap.put("美食之旅", Arrays.asList("美食", "餐厅", "小吃", "饮食"));
        themeAttractionMap.put("亲子游", Arrays.asList("乐园", "儿童", "互动", "教育"));
        themeAttractionMap.put("浪漫之旅", Arrays.asList("风景", "夜景", "公园", "文化"));
        themeAttractionMap.put("探险之旅", Arrays.asList("自然", "户外", "运动", "山水"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> recommendRoutesByUserPreference(Integer userId, Integer cityId, int days, Map<String, Object> preferences) {
        try {
            // 生成缓存键
            String cacheKey = ROUTE_RECOMMENDATION_PREFIX + userId + ":" + cityId + ":" + days + ":" + preferences.hashCode();
            
            // 尝试从缓存获取
            List<Map<String, Object>> cachedRecommendations = cacheUtil.get(cacheKey, List.class);
            if (cachedRecommendations != null) {
                log.info("从缓存获取推荐路线: count={}", cachedRecommendations.size());
                return cachedRecommendations;
            }
            
            log.info("基于用户偏好推荐路线: userId={}, cityId={}, days={}, preferences={}", userId, cityId, days, preferences);

            // 获取用户偏好的景点类型
            Object preferredTypesObj = preferences.getOrDefault("preferredTypes", Collections.emptyList());
            List<String> preferredTypes = preferredTypesObj instanceof List ? ((List<?>) preferredTypesObj).stream()
                    .filter(item -> item instanceof String)
                    .map(item -> (String) item)
                    .collect(Collectors.toList()) : Collections.emptyList();
            BigDecimal budget = (BigDecimal) preferences.getOrDefault("budget", BigDecimal.valueOf(1000));

            // 获取城市景点
            List<Attraction> attractions = attractionService.getByCityId(cityId);

            // 根据偏好筛选景点
            List<Attraction> filteredAttractions = filterAttractionsByPreferences(attractions, preferredTypes);
            if (filteredAttractions.isEmpty()) {
                filteredAttractions = attractions;
            }

            // 提取景点ID
            List<Integer> attractionIds = filteredAttractions.stream()
                    .map(Attraction::getId)
                    .collect(Collectors.toList());

            // 生成推荐路线
            List<Map<String, Object>> recommendations = new ArrayList<>();

            // 生成不同偏好的路线
            List<String> preferencesList = Arrays.asList("balanced", "lowCost", "fast", "lowCarbon");
            for (String pref : preferencesList) {
                try {
                    Object optimalRoute = routeOptimizationService.planOptimalRoute(attractionIds, days, budget, pref);
                    Map<String, Object> recommendation = new HashMap<>();
                    recommendation.put("preference", pref);
                    recommendation.put("route", optimalRoute);
                    recommendation.put("attractionCount", attractionIds.size());
                    recommendations.add(recommendation);
                } catch (Exception e) {
                    log.warn("生成推荐路线失败: preference={}, error={}", pref, e.getMessage());
                }
            }

            // 缓存结果
            cacheUtil.set(cacheKey, recommendations, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            
            log.info("基于用户偏好推荐路线成功: count={}", recommendations.size());
            return recommendations;
        } catch (Exception e) {
            log.error("基于用户偏好推荐路线失败: error={}", e.getMessage());
            throw new RuntimeException("推荐路线失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> compareRoutes(List<Integer> routeIds) {
        try {
            log.info("比较路线: routeIds={}", routeIds);

            List<Route> routes = routeIds.stream()
                    .map(routeService::getById)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            ExceptionUtil.checkCondition(!routes.isEmpty(), "没有找到有效的路线");

            Map<String, Object> comparison = new HashMap<>();
            List<Map<String, Object>> routeDetails = new ArrayList<>();

            for (Route route : routes) {
                List<RouteAttraction> routeAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(route.getId().longValue());
                List<Attraction> attractions = routeAttractions.stream()
                        .map(ra -> attractionService.getById(ra.getAttractionId()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                double totalDistance = calculateTotalDistance(attractions);
                double estimatedCost = calculateEstimatedCost(attractions);
                double estimatedTime = calculateEstimatedTime(attractions);
                double averageRating = attractions.stream().mapToDouble(a -> a.getRating().doubleValue()).average().orElse(0.0);

                Map<String, Object> routeDetail = new HashMap<>();
                routeDetail.put("routeId", route.getId());
                routeDetail.put("routeName", route.getTitle());
                routeDetail.put("durationDays", route.getDurationDays());
                routeDetail.put("totalAttractions", attractions.size());
                routeDetail.put("totalDistance", totalDistance);
                routeDetail.put("estimatedCost", estimatedCost);
                routeDetail.put("estimatedTime", estimatedTime);
                routeDetail.put("averageRating", averageRating);
                routeDetail.put("viewCount", route.getViewCount());
                routeDetail.put("likeCount", route.getLikeCount());

                routeDetails.add(routeDetail);
            }

            // 计算最佳路线
            Map<String, Object> bestRoute = routeDetails.stream()
                    .max(Comparator.comparingDouble(detail -> {
                        double rating = (double) detail.getOrDefault("averageRating", 0.0);
                        int likeCount = (int) detail.getOrDefault("likeCount", 0);
                        return rating * 0.6 + likeCount * 0.4;
                    }))
                    .orElse(null);

            comparison.put("routes", routeDetails);
            comparison.put("bestRoute", bestRoute);
            comparison.put("totalRoutes", routeDetails.size());

            log.info("比较路线成功: routeCount={}", routeDetails.size());
            return comparison;
        } catch (Exception e) {
            log.error("比较路线失败: error={}", e.getMessage());
            throw new RuntimeException("比较路线失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getRealTimeAdjustment(Integer routeId, Map<String, Double> currentLocation, Map<String, Object> realTimeFactors) {
        try {
            log.info("获取实时路线调整建议: routeId={}, currentLocation={}, realTimeFactors={}", routeId, currentLocation, realTimeFactors);

            Route route = routeService.getById(routeId);
            ExceptionUtil.checkNotNull(route, "路线不存在");

            List<RouteAttraction> routeAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId.longValue());
            List<Attraction> attractions = routeAttractions.stream()
                    .map(ra -> attractionService.getById(ra.getAttractionId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 分析实时因素
            String weather = (String) realTimeFactors.getOrDefault("weather", "sunny");
            
            // 安全获取交通信息
            Object trafficObj = realTimeFactors.getOrDefault("traffic", Collections.emptyMap());
            Map<String, Object> traffic = new HashMap<>();
            if (trafficObj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) trafficObj;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String) {
                        traffic.put((String) entry.getKey(), entry.getValue());
                    }
                }
            }
            
            // 安全获取人流信息
            Object crowdObj = realTimeFactors.getOrDefault("crowd", Collections.emptyMap());
            Map<String, Object> crowd = new HashMap<>();
            if (crowdObj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) crowdObj;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String) {
                        crowd.put((String) entry.getKey(), entry.getValue());
                    }
                }
            }

            List<String> adjustments = new ArrayList<>();
            List<Map<String, Object>> alternativeAttractions = new ArrayList<>();

            // 根据天气调整
            if ("rainy".equals(weather)) {
                adjustments.add("由于下雨，建议优先安排室内景点");
                // 推荐室内替代景点
                alternativeAttractions.addAll(getAlternativeIndoorAttractions(attractions, route.getCity().getId()));
            }

            // 根据交通调整
            if (traffic.containsKey("congestedRoutes")) {
                Object congestedRoutesObj = traffic.get("congestedRoutes");
                if (congestedRoutesObj instanceof List) {
                    List<String> congestedRoutes = ((List<?>) congestedRoutesObj).stream()
                            .filter(item -> item instanceof String)
                            .map(item -> (String) item)
                            .collect(Collectors.toList());
                    if (!congestedRoutes.isEmpty()) {
                        adjustments.add("避开拥堵路段: " + String.join(", ", congestedRoutes));
                    }
                }
            }

            // 根据人流调整
            if (crowd.containsKey("crowdedAttractions")) {
                Object crowdedAttractionsObj = crowd.get("crowdedAttractions");
                if (crowdedAttractionsObj instanceof List) {
                    List<Integer> crowdedAttractions = ((List<?>) crowdedAttractionsObj).stream()
                            .filter(item -> item instanceof Integer)
                            .map(item -> (Integer) item)
                            .collect(Collectors.toList());
                    if (!crowdedAttractions.isEmpty()) {
                        adjustments.add("避开人流密集景点: " + crowdedAttractions.stream()
                                .map(id -> {
                                    Attraction attraction = attractionService.getById(id);
                                    return attraction != null ? attraction.getName() : String.valueOf(id);
                                })
                                .collect(Collectors.joining(", ")));
                    }
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("routeId", routeId);
            result.put("routeName", route.getTitle());
            result.put("adjustments", adjustments);
            result.put("alternativeAttractions", alternativeAttractions);
            result.put("currentLocation", currentLocation);
            result.put("realTimeFactors", realTimeFactors);

            log.info("获取实时路线调整建议成功: routeId={}, adjustmentsCount={}", routeId, adjustments.size());
            return result;
        } catch (Exception e) {
            log.error("获取实时路线调整建议失败: routeId={}, error={}", routeId, e.getMessage());
            throw new RuntimeException("获取调整建议失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> evaluateRouteQuality(Integer routeId, Map<String, Object> evaluationParams) {
        try {
            log.info("评估路线质量: routeId={}, evaluationParams={}", routeId, evaluationParams);

            // 使用路线优化服务的评估功能
            Map<String, Object> baseEvaluation = routeOptimizationService.evaluateRouteQuality(routeId);

            // 添加额外的评估维度
            Route route = routeService.getById(routeId);
            if (route != null) {
                List<RouteAttraction> routeAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId.longValue());
                List<Attraction> attractions = routeAttractions.stream()
                        .map(ra -> attractionService.getById(ra.getAttractionId()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                // 计算路线多样性
                double diversityScore = calculateRouteDiversity(attractions);
                // 计算路线合理性
                double reasonablenessScore = calculateRouteReasonableness(attractions, route.getDurationDays());
                // 计算路线性价比
                double costPerformanceScore = calculateCostPerformance(attractions, route.getDurationDays());

                baseEvaluation.put("diversityScore", diversityScore);
                baseEvaluation.put("reasonablenessScore", reasonablenessScore);
                baseEvaluation.put("costPerformanceScore", costPerformanceScore);

                // 综合评分
                double overallScore = (double) baseEvaluation.getOrDefault("qualityScore", 0.0) * 0.5 +
                        diversityScore * 0.2 +
                        reasonablenessScore * 0.2 +
                        costPerformanceScore * 0.1;
                baseEvaluation.put("overallScore", overallScore);
            }

            log.info("评估路线质量成功: routeId={}", routeId);
            return baseEvaluation;
        } catch (Exception e) {
            log.error("评估路线质量失败: routeId={}, error={}", routeId, e.getMessage());
            throw new RuntimeException("评估路线质量失败: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> generatePersonalizedRoute(Map<String, Object> userPreferences, Map<String, Object> constraints) {
        try {
            // 生成缓存键
            String cacheKey = PERSONALIZED_ROUTE_PREFIX + userPreferences.hashCode() + ":" + constraints.hashCode();
            
            // 尝试从缓存获取
            Map<String, Object> cachedRoute = cacheUtil.get(cacheKey, Map.class);
            if (cachedRoute != null) {
                log.info("从缓存获取个性化路线");
                return cachedRoute;
            }
            
            log.info("生成个性化路线: userPreferences={}, constraints={}", userPreferences, constraints);

            Integer cityId = (Integer) userPreferences.get("cityId");
            int days = (int) userPreferences.getOrDefault("days", 3);
            BigDecimal budget = (BigDecimal) userPreferences.getOrDefault("budget", BigDecimal.valueOf(1000));
            String preference = (String) userPreferences.getOrDefault("preference", "balanced");
            // 安全获取兴趣列表
            Object interestsObj = userPreferences.getOrDefault("interests", Collections.emptyList());
            List<String> interests = interestsObj instanceof List ? ((List<?>) interestsObj).stream()
                    .filter(item -> item instanceof String)
                    .map(item -> (String) item)
                    .collect(Collectors.toList()) : Collections.emptyList();
            // 添加交通偏好参数
            String transportPreference = (String) userPreferences.getOrDefault("transportPreference", "public");

            // 获取符合条件的景点
            List<Attraction> attractions = attractionService.getByCityId(cityId);
            List<Attraction> filteredAttractions = filterAttractionsByInterests(attractions, interests);

            if (filteredAttractions.isEmpty()) {
                filteredAttractions = attractions;
            }

            // 提取景点ID
            List<Integer> attractionIds = filteredAttractions.stream()
                    .map(Attraction::getId)
                    .collect(Collectors.toList());

            // 根据交通偏好调整景点选择
            if (!transportPreference.equals("public")) {
                // 非公共交通偏好，筛选距离较近的景点
                if (attractionIds.size() > days * 3) {
                    // 如果景点数量过多，根据交通偏好筛选
                    log.info("根据交通偏好 {} 调整景点选择", transportPreference);
                    // 这里可以添加更复杂的筛选逻辑，例如根据景点间距离
                }
            }

            // 生成最优路线
            Object optimalRoute = routeOptimizationService.planOptimalRoute(attractionIds, days, budget, preference);

            Map<String, Object> result = new HashMap<>();
            result.put("route", optimalRoute);
            result.put("userPreferences", userPreferences);
            result.put("constraints", constraints);
            result.put("attractionCount", attractionIds.size());
            result.put("cityId", cityId);
            result.put("days", days);
            result.put("transportPreference", transportPreference);

            // 缓存结果
            cacheUtil.set(cacheKey, result, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            
            log.info("生成个性化路线成功: cityId={}, days={}", cityId, days);
            return result;
        } catch (Exception e) {
            log.error("生成个性化路线失败: error={}", e.getMessage());
            throw new RuntimeException("生成个性化路线失败: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPopularRoutes(Integer cityId, int days, int limit) {
        try {
            // 生成缓存键
            String cacheKey = POPULAR_ROUTES_PREFIX + cityId + ":" + days + ":" + limit;
            
            // 尝试从缓存获取
            List<Map<String, Object>> cachedRoutes = cacheUtil.get(cacheKey, List.class);
            if (cachedRoutes != null) {
                log.info("从缓存获取热门路线: count={}", cachedRoutes.size());
                return cachedRoutes;
            }
            
            log.info("获取热门路线: cityId={}, days={}, limit={}", cityId, days, limit);

            // 获取城市的所有路线
            List<Route> routes = routeService.list()
                    .stream()
                    .filter(route -> route.getCity().getId().equals(cityId) && route.getDurationDays() == days && route.getIsPublic())
                    .sorted((r1, r2) -> {
                        // 按浏览数和点赞数排序
                        int score1 = r1.getViewCount() * 2 + r1.getLikeCount() * 5;
                        int score2 = r2.getViewCount() * 2 + r2.getLikeCount() * 5;
                        return Integer.compare(score2, score1);
                    })
                    .limit(limit)
                    .collect(Collectors.toList());

            // 转换为响应格式
            List<Map<String, Object>> popularRoutes = routes.stream()
                    .map(route -> {
                        Map<String, Object> routeMap = new HashMap<>();
                        routeMap.put("routeId", route.getId());
                        routeMap.put("title", route.getTitle());
                        routeMap.put("description", route.getDescription());
                        routeMap.put("durationDays", route.getDurationDays());
                        routeMap.put("difficulty", route.getDifficulty());
                        routeMap.put("coverImage", route.getCoverImage());
                        routeMap.put("viewCount", route.getViewCount());
                        routeMap.put("likeCount", route.getLikeCount());

                        // 获取路线景点
                        List<RouteAttraction> routeAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(route.getId().longValue());
                        List<Attraction> attractions = routeAttractions.stream()
                                .map(ra -> attractionService.getById(ra.getAttractionId()))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        routeMap.put("attractionCount", attractions.size());

                        return routeMap;
                    })
                    .collect(Collectors.toList());

            // 缓存结果
            cacheUtil.set(cacheKey, popularRoutes, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            
            log.info("获取热门路线成功: count={}", popularRoutes.size());
            return popularRoutes;
        } catch (Exception e) {
            log.error("获取热门路线失败: error={}", e.getMessage());
            throw new RuntimeException("获取热门路线失败: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getSimilarRoutes(Integer routeId, int limit) {
        try {
            // 生成缓存键
            String cacheKey = SIMILAR_ROUTES_PREFIX + routeId + ":" + limit;
            
            // 尝试从缓存获取
            List<Map<String, Object>> cachedRoutes = cacheUtil.get(cacheKey, List.class);
            if (cachedRoutes != null) {
                log.info("从缓存获取相似路线: count={}", cachedRoutes.size());
                return cachedRoutes;
            }
            
            log.info("获取相似路线: routeId={}, limit={}", routeId, limit);

            Route targetRoute = routeService.getById(routeId);
            ExceptionUtil.checkNotNull(targetRoute, "路线不存在");

            // 获取目标路线的景点
            List<RouteAttraction> targetRouteAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId.longValue());
            Set<Integer> targetAttractionIds = targetRouteAttractions.stream()
                    .map(RouteAttraction::getAttractionId)
                    .collect(Collectors.toSet());

            // 获取同城市的其他路线
            List<Route> candidateRoutes = routeService.list()
                    .stream()
                    .filter(route -> route.getCity().getId().equals(targetRoute.getCity().getId()) && 
                            route.getId() != routeId && 
                            route.getIsPublic() &&
                            Math.abs(route.getDurationDays() - targetRoute.getDurationDays()) <= 1)
                    .collect(Collectors.toList());

            // 计算相似度并排序
            List<Map<String, Object>> similarRoutes = candidateRoutes.stream()
                    .map(route -> {
                        List<RouteAttraction> routeAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(route.getId().longValue());
                        Set<Integer> attractionIds = routeAttractions.stream()
                                .map(RouteAttraction::getAttractionId)
                                .collect(Collectors.toSet());

                        // 计算Jaccard相似度
                        Set<Integer> intersection = new HashSet<>(targetAttractionIds);
                        intersection.retainAll(attractionIds);
                        Set<Integer> union = new HashSet<>(targetAttractionIds);
                        union.addAll(attractionIds);
                        double similarity = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();

                        Map<String, Object> routeMap = new HashMap<>();
                        routeMap.put("routeId", route.getId());
                        routeMap.put("title", route.getTitle());
                        routeMap.put("description", route.getDescription());
                        routeMap.put("durationDays", route.getDurationDays());
                        routeMap.put("similarity", similarity);
                        routeMap.put("viewCount", route.getViewCount());
                        routeMap.put("likeCount", route.getLikeCount());

                        return routeMap;
                    })
                    .sorted((r1, r2) -> Double.compare((double) r2.get("similarity"), (double) r1.get("similarity")))
                    .limit(limit)
                    .collect(Collectors.toList());

            // 缓存结果
            cacheUtil.set(cacheKey, similarRoutes, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            
            log.info("获取相似路线成功: count={}", similarRoutes.size());
            return similarRoutes;
        } catch (Exception e) {
            log.error("获取相似路线失败: routeId={}, error={}", routeId, e.getMessage());
            throw new RuntimeException("获取相似路线失败: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getSeasonalRoutes(Integer cityId, String season, int days) {
        try {
            // 生成缓存键
            String cacheKey = SEASONAL_ROUTES_PREFIX + cityId + ":" + season + ":" + days;
            
            // 尝试从缓存获取
            List<Map<String, Object>> cachedRoutes = cacheUtil.get(cacheKey, List.class);
            if (cachedRoutes != null) {
                log.info("从缓存获取季节性路线: count={}", cachedRoutes.size());
                return cachedRoutes;
            }
            
            log.info("获取季节性路线: cityId={}, season={}, days={}", cityId, season, days);

            // 获取符合季节的景点
            List<String> seasonKeywords = seasonAttractionMap.getOrDefault(season.toLowerCase(), Collections.emptyList());
            List<Attraction> attractions = attractionService.getByCityId(cityId);
            List<Attraction> seasonalAttractions = filterAttractionsByKeywords(attractions, seasonKeywords);

            if (seasonalAttractions.isEmpty()) {
                seasonalAttractions = attractions;
            }

            // 提取景点ID
            List<Integer> attractionIds = seasonalAttractions.stream()
                    .map(Attraction::getId)
                    .collect(Collectors.toList());

            // 生成季节性路线
            List<Map<String, Object>> seasonalRoutes = new ArrayList<>();

            // 生成不同偏好的路线
            List<String> preferences = Arrays.asList("balanced", "lowCost", "fast");
            for (String preference : preferences) {
                try {
                    Object optimalRoute = routeOptimizationService.planOptimalRoute(attractionIds, days, BigDecimal.valueOf(1000), preference);
                    Map<String, Object> routeMap = new HashMap<>();
                    routeMap.put("preference", preference);
                    routeMap.put("route", optimalRoute);
                    routeMap.put("season", season);
                    routeMap.put("attractionCount", attractionIds.size());
                    seasonalRoutes.add(routeMap);
                } catch (Exception e) {
                    log.warn("生成季节性路线失败: season={}, preference={}, error={}", season, preference, e.getMessage());
                }
            }

            // 缓存结果
            cacheUtil.set(cacheKey, seasonalRoutes, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            
            log.info("获取季节性路线成功: count={}", seasonalRoutes.size());
            return seasonalRoutes;
        } catch (Exception e) {
            log.error("获取季节性路线失败: error={}", e.getMessage());
            throw new RuntimeException("获取季节性路线失败: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getThemeRoutes(String theme, Integer cityId, int days) {
        try {
            // 生成缓存键
            String cacheKey = THEME_ROUTES_PREFIX + theme + ":" + cityId + ":" + days;
            
            // 尝试从缓存获取
            List<Map<String, Object>> cachedRoutes = cacheUtil.get(cacheKey, List.class);
            if (cachedRoutes != null) {
                log.info("从缓存获取主题路线: count={}", cachedRoutes.size());
                return cachedRoutes;
            }
            
            log.info("获取主题路线: theme={}, cityId={}, days={}", theme, cityId, days);

            // 获取符合主题的景点
            List<String> themeKeywords = themeAttractionMap.getOrDefault(theme, Collections.emptyList());
            List<Attraction> attractions = attractionService.getByCityId(cityId);
            List<Attraction> themeAttractions = filterAttractionsByKeywords(attractions, themeKeywords);

            if (themeAttractions.isEmpty()) {
                themeAttractions = attractions;
            }

            // 提取景点ID
            List<Integer> attractionIds = themeAttractions.stream()
                    .map(Attraction::getId)
                    .collect(Collectors.toList());

            // 生成主题路线
            List<Map<String, Object>> themeRoutes = new ArrayList<>();

            // 生成不同偏好的路线
            List<String> preferences = Arrays.asList("balanced", "lowCost", "fast");
            for (String preference : preferences) {
                try {
                    Object optimalRoute = routeOptimizationService.planOptimalRoute(attractionIds, days, BigDecimal.valueOf(1000), preference);
                    Map<String, Object> routeMap = new HashMap<>();
                    routeMap.put("preference", preference);
                    routeMap.put("route", optimalRoute);
                    routeMap.put("theme", theme);
                    routeMap.put("attractionCount", attractionIds.size());
                    themeRoutes.add(routeMap);
                } catch (Exception e) {
                    log.warn("生成主题路线失败: theme={}, preference={}, error={}", theme, preference, e.getMessage());
                }
            }

            // 缓存结果
            cacheUtil.set(cacheKey, themeRoutes, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            
            log.info("获取主题路线成功: count={}", themeRoutes.size());
            return themeRoutes;
        } catch (Exception e) {
            log.error("获取主题路线失败: error={}", e.getMessage());
            throw new RuntimeException("获取主题路线失败: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getRouteOptimizationSuggestions(Integer routeId, String optimizationType) {
        try {
            // 生成缓存键
            String cacheKey = ROUTE_OPTIMIZATION_PREFIX + routeId + ":" + optimizationType;
            
            // 尝试从缓存获取
            Map<String, Object> cachedSuggestions = cacheUtil.get(cacheKey, Map.class);
            if (cachedSuggestions != null) {
                log.info("从缓存获取路线优化建议: routeId={}", routeId);
                return cachedSuggestions;
            }
            
            log.info("获取路线优化建议: routeId={}, optimizationType={}", routeId, optimizationType);

            Route route = routeService.getById(routeId);
            ExceptionUtil.checkNotNull(route, "路线不存在");

            List<RouteAttraction> routeAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId.longValue());
            List<Attraction> attractions = routeAttractions.stream()
                    .map(ra -> attractionService.getById(ra.getAttractionId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Map<String, Object> suggestions = new HashMap<>();
            List<String> suggestionList = new ArrayList<>();

            switch (optimizationType) {
                case "time":
                    // 时间优化建议
                    if (attractions.size() > route.getDurationDays() * 4) {
                        suggestionList.add("景点数量过多，建议减少每天的景点数量，提高游览质量");
                    }
                    suggestionList.add("建议合理安排景点顺序，减少交通时间");
                    suggestionList.add("考虑景点的开放时间，避免到达时闭馆");
                    break;
                case "cost":
                    // 成本优化建议
                    long highCostAttractions = attractions.stream()
                            .filter(a -> a.getTicketPrice().compareTo(BigDecimal.valueOf(50)) > 0)
                            .count();
                    if (highCostAttractions > attractions.size() / 2) {
                        suggestionList.add("高票价景点过多，建议增加一些免费或低票价景点");
                    }
                    suggestionList.add("建议选择公共交通，降低交通成本");
                    suggestionList.add("考虑购买联票或套票，享受更多折扣");
                    break;
                case "experience":
                    // 体验优化建议
                    double diversityScore = calculateRouteDiversity(attractions);
                    if (diversityScore < 0.5) {
                        suggestionList.add("路线景点类型单一，建议增加不同类型的景点，丰富体验");
                    }
                    suggestionList.add("建议合理安排景点顺序，避免重复路线");
                    suggestionList.add("考虑景点的人流量，避开高峰时段");
                    break;
                default:
                    // 综合优化建议
                    suggestionList.add("建议合理安排每天的景点数量和顺序");
                    suggestionList.add("考虑交通方式和时间，优化路线");
                    suggestionList.add("根据季节和天气调整路线安排");
                    suggestionList.add("参考其他用户的评价和建议");
            }

            suggestions.put("routeId", routeId);
            suggestions.put("routeName", route.getTitle());
            suggestions.put("optimizationType", optimizationType);
            suggestions.put("suggestions", suggestionList);
            suggestions.put("attractionCount", attractions.size());
            suggestions.put("durationDays", route.getDurationDays());

            // 缓存结果
            cacheUtil.set(cacheKey, suggestions, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            
            log.info("获取路线优化建议成功: routeId={}, suggestionsCount={}", routeId, suggestionList.size());
            return suggestions;
        } catch (Exception e) {
            log.error("获取路线优化建议失败: routeId={}, error={}", routeId, e.getMessage());
            throw new RuntimeException("获取路线优化建议失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> recommendRoutesByUserHistory(Integer userId, Integer cityId, int days, int limit) {
        try {
            log.info("基于用户历史行为推荐路线: userId={}, cityId={}, days={}, limit={}", userId, cityId, days, limit);
            // 这里应该实现基于用户历史行为的推荐逻辑
            // 暂时返回热门路线作为替代
            return getPopularRoutes(cityId, days, limit);
        } catch (Exception e) {
            log.error("基于用户历史行为推荐路线失败: error={}", e.getMessage());
            throw new RuntimeException("推荐路线失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> recommendRoutesByPopularAttractions(Integer cityId, int days, int limit) {
        try {
            log.info("基于热门景点推荐路线: cityId={}, days={}, limit={}", cityId, days, limit);
            // 这里应该实现基于热门景点的推荐逻辑
            // 暂时返回热门路线作为替代
            return getPopularRoutes(cityId, days, limit);
        } catch (Exception e) {
            log.error("基于热门景点推荐路线失败: error={}", e.getMessage());
            throw new RuntimeException("推荐路线失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> recommendRoutesBySeasonAndWeather(Integer cityId, int days, String season, String weather, int limit) {
        try {
            log.info("基于季节和天气推荐路线: cityId={}, days={}, season={}, weather={}, limit={}", cityId, days, season, weather, limit);
            // 这里应该实现基于季节和天气的推荐逻辑
            // 暂时返回季节性路线作为替代
            return getSeasonalRoutes(cityId, season, days);
        } catch (Exception e) {
            log.error("基于季节和天气推荐路线失败: error={}", e.getMessage());
            throw new RuntimeException("推荐路线失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> recommendRoutesBySocialNetwork(Integer userId, Integer cityId, int days, int limit) {
        try {
            log.info("基于社交网络推荐路线: userId={}, cityId={}, days={}, limit={}", userId, cityId, days, limit);
            // 这里应该实现基于社交网络的推荐逻辑
            // 暂时返回热门路线作为替代
            return getPopularRoutes(cityId, days, limit);
        } catch (Exception e) {
            log.error("基于社交网络推荐路线失败: error={}", e.getMessage());
            throw new RuntimeException("推荐路线失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> recommendRoutesByRating(Integer cityId, int days, int limit) {
        try {
            log.info("基于路线评分推荐路线: cityId={}, days={}, limit={}", cityId, days, limit);
            // 这里应该实现基于路线评分的推荐逻辑
            // 暂时返回热门路线作为替代
            return getPopularRoutes(cityId, days, limit);
        } catch (Exception e) {
            log.error("基于路线评分推荐路线失败: error={}", e.getMessage());
            throw new RuntimeException("推荐路线失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> recommendRoutesBySimilarity(Integer routeId, int limit) {
        try {
            log.info("基于路线相似度推荐路线: routeId={}, limit={}", routeId, limit);
            // 这里应该实现基于路线相似度的推荐逻辑
            // 暂时返回相似路线作为替代
            return getSimilarRoutes(routeId, limit);
        } catch (Exception e) {
            log.error("基于路线相似度推荐路线失败: error={}", e.getMessage());
            throw new RuntimeException("推荐路线失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> generateMultiDayRoute(Integer cityId, String startDate, String endDate, Map<String, Object> userPreferences) {
        try {
            log.info("生成多日游路线: cityId={}, startDate={}, endDate={}, userPreferences={}", cityId, startDate, endDate, userPreferences);
            // 这里应该实现生成多日游路线的逻辑
            // 暂时返回个性化路线作为替代
            return generatePersonalizedRoute(userPreferences, new HashMap<>());
        } catch (Exception e) {
            log.error("生成多日游路线失败: error={}", e.getMessage());
            throw new RuntimeException("生成路线失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getRouteRecommendationReason(Integer routeId, Integer userId) {
        try {
            log.info("获取路线推荐理由: routeId={}, userId={}", routeId, userId);
            Map<String, Object> result = new HashMap<>();
            result.put("routeId", routeId);
            result.put("userId", userId);
            result.put("reasons", Arrays.asList("路线评分高", "符合用户偏好", "热门推荐"));
            return result;
        } catch (Exception e) {
            log.error("获取路线推荐理由失败: error={}", e.getMessage());
            throw new RuntimeException("获取推荐理由失败: " + e.getMessage());
        }
    }

    // 辅助方法
    private List<Attraction> filterAttractionsByPreferences(List<Attraction> attractions, List<String> preferredTypes) {
        if (preferredTypes == null || preferredTypes.isEmpty()) {
            return attractions;
        }

        return attractions.stream()
                .filter(attraction -> {
                    String description = attraction.getDescription();
                    if (description == null) {
                        return false;
                    }
                    return preferredTypes.stream().anyMatch(type -> {
                        List<String> keywords = themeAttractionMap.getOrDefault(type, Collections.emptyList());
                        return keywords.stream().anyMatch(description::contains);
                    });
                })
                .collect(Collectors.toList());
    }

    private List<Attraction> filterAttractionsByKeywords(List<Attraction> attractions, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return attractions;
        }

        return attractions.stream()
                .filter(attraction -> {
                    String description = attraction.getDescription();
                    return description != null && keywords.stream().anyMatch(description::contains);
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getAlternativeIndoorAttractions(List<Attraction> originalAttractions, Integer cityId) {
        List<Attraction> allAttractions = attractionService.getByCityId(cityId);
        List<Map<String, Object>> alternatives = new ArrayList<>();

        // 筛选室内景点
        List<Attraction> indoorAttractions = allAttractions.stream()
                .filter(attraction -> {
                    String description = attraction.getDescription();
                    return description != null && (
                            description.contains("博物馆") ||
                            description.contains("室内") ||
                            description.contains("文化") ||
                            description.contains("历史")
                    );
                })
                .limit(3)
                .collect(Collectors.toList());

        for (Attraction attraction : indoorAttractions) {
            Map<String, Object> alternative = new HashMap<>();
            alternative.put("attractionId", attraction.getId());
            alternative.put("attractionName", attraction.getName());
            alternative.put("description", attraction.getDescription());
            alternative.put("type", "indoor");
            alternatives.add(alternative);
        }

        return alternatives;
    }

    private double calculateRouteDiversity(List<Attraction> attractions) {
        Set<String> attractionTypes = new HashSet<>();
        attractions.forEach(attraction -> {
            String description = attraction.getDescription();
            if (description != null) {
                for (String type : themeAttractionMap.keySet()) {
                    if (themeAttractionMap.get(type).stream().anyMatch(description::contains)) {
                        attractionTypes.add(type);
                    }
                }
            }
        });
        return (double) attractionTypes.size() / themeAttractionMap.size();
    }

    private double calculateRouteReasonableness(List<Attraction> attractions, int days) {
        int idealAttractionsPerDay = 4;
        int actualAttractionsPerDay = attractions.size() / days;
        double deviation = Math.abs(actualAttractionsPerDay - idealAttractionsPerDay) / (double) idealAttractionsPerDay;
        return Math.max(0, 1 - deviation);
    }

    private double calculateCostPerformance(List<Attraction> attractions, int days) {
        double totalCost = attractions.stream()
                .mapToDouble(a -> a.getTicketPrice().doubleValue())
                .sum();
        double averageRating = attractions.stream()
                .mapToDouble(a -> a.getRating().doubleValue())
                .average()
                .orElse(0.0);
        double costPerDay = totalCost / days;
        return averageRating / (costPerDay / 100 + 1);
    }

    private double calculateTotalDistance(List<Attraction> attractions) {
        if (attractions == null || attractions.size() < 2) {
            return 0.0;
        }

        double totalDistance = 0.0;
        for (int i = 0; i < attractions.size() - 1; i++) {
            Attraction current = attractions.get(i);
            Attraction next = attractions.get(i + 1);
            // 使用CommonUtil计算两个经纬度点之间的距离
            totalDistance += CommonUtil.calculateDistance(current.getLatitude().doubleValue(), current.getLongitude().doubleValue(),
                    next.getLatitude().doubleValue(), next.getLongitude().doubleValue());
        }

        return totalDistance;
    }



    private double calculateEstimatedCost(List<Attraction> attractions) {
        return attractions.stream()
                .mapToDouble(a -> a.getTicketPrice().doubleValue())
                .sum() + attractions.size() * 20.0; // 加上交通费用
    }

    private double calculateEstimatedTime(List<Attraction> attractions) {
        return attractions.size() * 2.0; // 每个景点平均2小时
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
                        List<String> keywords = themeAttractionMap.getOrDefault(interest, Collections.emptyList());
                        return keywords.stream().anyMatch(description::contains);
                    });
                })
                .collect(Collectors.toList());
    }


}
