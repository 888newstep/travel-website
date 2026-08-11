package travel.route.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import travel.common.entity.route_planning.Route;
import travel.common.entity.route_planning.RouteAttraction;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.utils.CacheUtil;
import travel.common.utils.ExceptionUtil;
import travel.route.dto.route.SmartRouteItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IntelligentRouteRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(IntelligentRouteRecommendationService.class);

    private static final String POPULAR_ROUTES_PREFIX = "route:popular:";
    private static final String SIMILAR_ROUTES_PREFIX = "route:similar:";
    private static final String SEASONAL_ROUTES_PREFIX = "route:seasonal:";
    private static final String THEME_ROUTES_PREFIX = "route:theme:";
    private static final long CACHE_EXPIRE_HOURS = 2;

    private final RouteService routeService;
    private final AttractionService attractionService;
    private final RouteAttractionService routeAttractionService;
    private final RouteOptimizationService routeOptimizationService;
    private final CacheUtil cacheUtil;

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

    @SuppressWarnings("unchecked")
    public List<SmartRouteItem> getPopularRoutes(Integer cityId, int days, int limit) {
        try {
            String cacheKey = POPULAR_ROUTES_PREFIX + cityId + ":" + days + ":" + limit;
            List<SmartRouteItem> cachedRoutes = cacheUtil.get(cacheKey, List.class);
            if (cachedRoutes != null) {
                log.info("Get popular routes from cache: count={}", cachedRoutes.size());
                return cachedRoutes;
            }

            List<Route> routes = routeService.list().stream()
                    .filter(route -> Objects.equals(resolveRouteCityId(route), cityId)
                            && route.getDurationDays() == days
                            && Boolean.TRUE.equals(route.getIsPublic()))
                    .sorted((r1, r2) -> Integer.compare(routePopularityScore(r2), routePopularityScore(r1)))
                    .limit(limit)
                    .collect(Collectors.toList());

            List<SmartRouteItem> popularRoutes = routes.stream()
                    .map(this::buildPopularRouteItem)
                    .collect(Collectors.toList());

            cacheUtil.set(cacheKey, popularRoutes, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.info("Get popular routes success: count={}", popularRoutes.size());
            return popularRoutes;
        } catch (Exception e) {
            log.error("Get popular routes failed", e);
            throw new RuntimeException("Get popular routes failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<SmartRouteItem> getSimilarRoutes(Integer routeId, int limit) {
        try {
            String cacheKey = SIMILAR_ROUTES_PREFIX + routeId + ":" + limit;
            List<SmartRouteItem> cachedRoutes = cacheUtil.get(cacheKey, List.class);
            if (cachedRoutes != null) {
                log.info("Get similar routes from cache: count={}", cachedRoutes.size());
                return cachedRoutes;
            }

            Route targetRoute = routeService.getById(routeId);
            ExceptionUtil.checkNotNull(targetRoute, "路线不存在");

            List<RouteAttraction> targetRouteAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId.longValue());
            Set<Integer> targetAttractionIds = targetRouteAttractions.stream()
                    .map(RouteAttraction::getAttractionId)
                    .collect(Collectors.toSet());

            List<Route> candidateRoutes = routeService.list().stream()
                    .filter(route -> Objects.equals(resolveRouteCityId(route), resolveRouteCityId(targetRoute))
                            && !Objects.equals(route.getId(), routeId)
                            && Boolean.TRUE.equals(route.getIsPublic())
                            && Math.abs(route.getDurationDays() - targetRoute.getDurationDays()) <= 1)
                    .collect(Collectors.toList());

            List<SmartRouteItem> similarRoutes = candidateRoutes.stream()
                    .map(route -> buildSimilarRouteItem(route, targetAttractionIds))
                    .sorted((r1, r2) -> Double.compare(valueOrZero(r2.getSimilarity()), valueOrZero(r1.getSimilarity())))
                    .limit(limit)
                    .collect(Collectors.toList());

            cacheUtil.set(cacheKey, similarRoutes, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.info("Get similar routes success: count={}", similarRoutes.size());
            return similarRoutes;
        } catch (Exception e) {
            log.error("Get similar routes failed: routeId={}", routeId, e);
            throw new RuntimeException("Get similar routes failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<SmartRouteItem> getSeasonalRoutes(Integer cityId, String season, int days) {
        try {
            String cacheKey = SEASONAL_ROUTES_PREFIX + cityId + ":" + season + ":" + days;
            List<SmartRouteItem> cachedRoutes = cacheUtil.get(cacheKey, List.class);
            if (cachedRoutes != null) {
                log.info("Get seasonal routes from cache: count={}", cachedRoutes.size());
                return cachedRoutes;
            }

            String normalizedSeason = season == null ? "" : season.toLowerCase();
            List<String> seasonKeywords = seasonAttractionMap.getOrDefault(normalizedSeason, Collections.emptyList());
            List<Attraction> attractions = attractionService.getByCityId(cityId);
            List<Attraction> seasonalAttractions = filterAttractionsByKeywords(attractions, seasonKeywords);
            if (seasonalAttractions.isEmpty()) {
                seasonalAttractions = attractions;
            }

            List<Integer> attractionIds = seasonalAttractions.stream().map(Attraction::getId).collect(Collectors.toList());
            List<SmartRouteItem> seasonalRoutes = buildOptimizedRoutes(attractionIds, days, item -> item.season(season));

            cacheUtil.set(cacheKey, seasonalRoutes, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.info("Get seasonal routes success: count={}", seasonalRoutes.size());
            return seasonalRoutes;
        } catch (Exception e) {
            log.error("Get seasonal routes failed", e);
            throw new RuntimeException("Get seasonal routes failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<SmartRouteItem> getThemeRoutes(String theme, Integer cityId, int days) {
        try {
            String cacheKey = THEME_ROUTES_PREFIX + theme + ":" + cityId + ":" + days;
            List<SmartRouteItem> cachedRoutes = cacheUtil.get(cacheKey, List.class);
            if (cachedRoutes != null) {
                log.info("Get theme routes from cache: count={}", cachedRoutes.size());
                return cachedRoutes;
            }

            List<String> themeKeywords = themeAttractionMap.getOrDefault(theme, Collections.emptyList());
            List<Attraction> attractions = attractionService.getByCityId(cityId);
            List<Attraction> themeAttractions = filterAttractionsByKeywords(attractions, themeKeywords);
            if (themeAttractions.isEmpty()) {
                themeAttractions = attractions;
            }

            List<Integer> attractionIds = themeAttractions.stream().map(Attraction::getId).collect(Collectors.toList());
            List<SmartRouteItem> themeRoutes = buildOptimizedRoutes(attractionIds, days, item -> item.theme(theme));

            cacheUtil.set(cacheKey, themeRoutes, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.info("Get theme routes success: count={}", themeRoutes.size());
            return themeRoutes;
        } catch (Exception e) {
            log.error("Get theme routes failed", e);
            throw new RuntimeException("Get theme routes failed: " + e.getMessage(), e);
        }
    }

    public List<SmartRouteItem> recommendRoutesByUserHistory(Integer userId, Integer cityId, int days, int limit) {
        try {
            log.info("Recommend routes by user history: userId={}, cityId={}, days={}, limit={}", userId, cityId, days, limit);
            return getPopularRoutes(cityId, days, limit);
        } catch (Exception e) {
            log.error("Recommend routes by user history failed", e);
            throw new RuntimeException("Recommend routes failed: " + e.getMessage(), e);
        }
    }

    public List<SmartRouteItem> recommendRoutesByPopularAttractions(Integer cityId, int days, int limit) {
        try {
            log.info("Recommend routes by popular attractions: cityId={}, days={}, limit={}", cityId, days, limit);
            return getPopularRoutes(cityId, days, limit);
        } catch (Exception e) {
            log.error("Recommend routes by popular attractions failed", e);
            throw new RuntimeException("Recommend routes failed: " + e.getMessage(), e);
        }
    }

    public List<SmartRouteItem> recommendRoutesBySeasonAndWeather(Integer cityId, int days, String season, String weather, int limit) {
        try {
            log.info("Recommend routes by season and weather: cityId={}, days={}, season={}, weather={}, limit={}", cityId, days, season, weather, limit);
            return getSeasonalRoutes(cityId, season, days);
        } catch (Exception e) {
            log.error("Recommend routes by season and weather failed", e);
            throw new RuntimeException("Recommend routes failed: " + e.getMessage(), e);
        }
    }

    public List<SmartRouteItem> recommendRoutesBySocialNetwork(Integer userId, Integer cityId, int days, int limit) {
        try {
            log.info("Recommend routes by social network: userId={}, cityId={}, days={}, limit={}", userId, cityId, days, limit);
            return getPopularRoutes(cityId, days, limit);
        } catch (Exception e) {
            log.error("Recommend routes by social network failed", e);
            throw new RuntimeException("Recommend routes failed: " + e.getMessage(), e);
        }
    }

    public List<SmartRouteItem> recommendRoutesByRating(Integer cityId, int days, int limit) {
        try {
            log.info("Recommend routes by rating: cityId={}, days={}, limit={}", cityId, days, limit);
            return getPopularRoutes(cityId, days, limit);
        } catch (Exception e) {
            log.error("Recommend routes by rating failed", e);
            throw new RuntimeException("Recommend routes failed: " + e.getMessage(), e);
        }
    }

    public List<SmartRouteItem> recommendRoutesBySimilarity(Integer routeId, int limit) {
        try {
            log.info("Recommend routes by similarity: routeId={}, limit={}", routeId, limit);
            return getSimilarRoutes(routeId, limit);
        } catch (Exception e) {
            log.error("Recommend routes by similarity failed", e);
            throw new RuntimeException("Recommend routes failed: " + e.getMessage(), e);
        }
    }

    private SmartRouteItem buildPopularRouteItem(Route route) {
        List<RouteAttraction> routeAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(route.getId().longValue());
        List<Attraction> attractions = routeAttractions.stream()
                .map(ra -> attractionService.getById(ra.getAttractionId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return SmartRouteItem.builder()
                .routeId(route.getId())
                .title(route.getTitle())
                .description(route.getDescription())
                .durationDays(route.getDurationDays())
                .difficulty(route.getDifficulty())
                .coverImage(route.getCoverImage())
                .viewCount(route.getViewCount())
                .likeCount(route.getLikeCount())
                .attractionCount(attractions.size())
                .build();
    }

    private SmartRouteItem buildSimilarRouteItem(Route route, Set<Integer> targetAttractionIds) {
        List<RouteAttraction> routeAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(route.getId().longValue());
        Set<Integer> attractionIds = routeAttractions.stream()
                .map(RouteAttraction::getAttractionId)
                .collect(Collectors.toSet());

        Set<Integer> intersection = new HashSet<>(targetAttractionIds);
        intersection.retainAll(attractionIds);
        Set<Integer> union = new HashSet<>(targetAttractionIds);
        union.addAll(attractionIds);
        double similarity = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();

        return SmartRouteItem.builder()
                .routeId(route.getId())
                .title(route.getTitle())
                .description(route.getDescription())
                .durationDays(route.getDurationDays())
                .similarity(similarity)
                .viewCount(route.getViewCount())
                .likeCount(route.getLikeCount())
                .build();
    }

    private List<SmartRouteItem> buildOptimizedRoutes(List<Integer> attractionIds, int days, java.util.function.Consumer<SmartRouteItem.Builder> customizer) {
        List<SmartRouteItem> routes = new ArrayList<>();
        for (String preference : Arrays.asList("balanced", "lowCost", "fast")) {
            try {
                Object optimalRoute = routeOptimizationService.planOptimalRoute(attractionIds, days, BigDecimal.valueOf(1000), preference);
                SmartRouteItem.Builder builder = SmartRouteItem.builder()
                        .preference(preference)
                        .route(optimalRoute instanceof travel.route.algorithm.RoutePlanAlgorithm.OptimalRoute
                                ? (travel.route.algorithm.RoutePlanAlgorithm.OptimalRoute) optimalRoute : null)
                        .attractionCount(attractionIds.size());
                customizer.accept(builder);
                routes.add(builder.build());
            } catch (Exception e) {
                log.warn("Build optimized recommendation route failed: preference={}, error={}", preference, e.getMessage());
            }
        }
        return routes;
    }

    private int routePopularityScore(Route route) {
        return route.getViewCount() * 2 + route.getLikeCount() * 5;
    }

    private double valueOrZero(Double value) {
        return value != null ? value : 0.0;
    }

    private List<Attraction> filterAttractionsByKeywords(List<Attraction> attractions, List<String> keywords) {
        if (attractions == null || attractions.isEmpty()) {
            return List.of();
        }
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

    private Integer resolveRouteCityId(Route route) {
        if (route == null) {
            return null;
        }
        if (route.getCity() != null && route.getCity().getId() != null) {
            return route.getCity().getId();
        }
        return route.getCityId();
    }
}