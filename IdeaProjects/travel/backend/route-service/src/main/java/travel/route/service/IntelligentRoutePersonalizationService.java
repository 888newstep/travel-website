package travel.route.service;

import jakarta.annotation.PostConstruct;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.utils.CacheUtil;
import travel.route.dto.route.MultiDayRouteResult;
import travel.route.dto.route.PersonalizedRouteConstraints;
import travel.route.dto.route.PersonalizedRoutePreferences;
import travel.route.dto.route.PersonalizedRouteResult;
import travel.route.dto.route.RouteRecommendationReason;
import travel.route.dto.route.UserPreferenceRecommendation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IntelligentRoutePersonalizationService {

    private static final Logger log = LoggerFactory.getLogger(IntelligentRoutePersonalizationService.class);

    private static final String ROUTE_RECOMMENDATION_PREFIX = "route:recommendation:";
    private static final String PERSONALIZED_ROUTE_PREFIX = "route:personalized:v2:";
    private static final long CACHE_EXPIRE_MINUTES = 30;
    private static final BigDecimal DEFAULT_BUDGET = BigDecimal.valueOf(1000);
    private static final int DEFAULT_DAYS = 3;

    private final AttractionService attractionService;
    private final RouteOptimizationService routeOptimizationService;
    private final CacheUtil cacheUtil;

    private final Map<String, List<String>> themeAttractionMap = new HashMap<>();

    @PostConstruct
    public void init() {
        themeAttractionMap.put("\u6587\u5316\u5386\u53f2", Arrays.asList("\u535a\u7269\u9986", "\u53e4\u8ff9", "\u6587\u5316", "\u5386\u53f2"));
        themeAttractionMap.put("\u81ea\u7136\u98ce\u5149", Arrays.asList("\u516c\u56ed", "\u5c71\u6c34", "\u81ea\u7136", "\u98ce\u666f"));
        themeAttractionMap.put("\u7f8e\u98df\u4e4b\u65c5", Arrays.asList("\u7f8e\u98df", "\u9910\u5385", "\u5c0f\u5403", "\u996e\u98df"));
        themeAttractionMap.put("\u4eb2\u5b50\u6e38", Arrays.asList("\u4e50\u56ed", "\u513f\u7ae5", "\u4e92\u52a8", "\u6559\u80b2"));
        themeAttractionMap.put("\u6d6a\u6f2b\u4e4b\u65c5", Arrays.asList("\u98ce\u666f", "\u591c\u666f", "\u516c\u56ed", "\u6587\u5316"));
        themeAttractionMap.put("\u63a2\u9669\u4e4b\u65c5", Arrays.asList("\u81ea\u7136", "\u6237\u5916", "\u8fd0\u52a8", "\u5c71\u6c34"));
    }

    @SuppressWarnings("unchecked")
    public List<UserPreferenceRecommendation> recommendRoutesByUserPreference(Integer userId, Integer cityId, int days, Map<String, Object> preferences) {
        try {
            Map<String, Object> safePreferences = preferences == null ? Collections.emptyMap() : preferences;
            String cacheKey = ROUTE_RECOMMENDATION_PREFIX + userId + ":" + cityId + ":" + days + ":" + safePreferences.hashCode();
            List<UserPreferenceRecommendation> cachedRecommendations = cacheUtil.get(cacheKey, List.class);
            if (cachedRecommendations != null) {
                log.info("Get personalized recommendations from cache: count={}", cachedRecommendations.size());
                return cachedRecommendations;
            }

            List<String> preferredTypes = extractStringList(safePreferences, "preferredTypes");
            BigDecimal budget = resolveBudget(safePreferences.get("budget"));

            List<Attraction> attractions = attractionService.getByCityId(cityId);
            List<Attraction> filteredAttractions = filterAttractionsByPreferences(attractions, preferredTypes);
            if (filteredAttractions.isEmpty()) {
                filteredAttractions = attractions;
            }

            List<Integer> attractionIds = filteredAttractions.stream()
                    .map(Attraction::getId)
                    .collect(Collectors.toList());

            List<UserPreferenceRecommendation> recommendations = new ArrayList<>();
            for (String preference : List.of("balanced", "lowCost", "fast", "lowCarbon")) {
                try {
                    travel.route.algorithm.RoutePlanAlgorithm.OptimalRoute optimalRoute =
                            routeOptimizationService.planOptimalRoute(attractionIds, days, budget, preference);
                    recommendations.add(UserPreferenceRecommendation.builder()
                            .preference(preference)
                            .route(optimalRoute)
                            .attractionCount(attractionIds.size())
                            .build());
                } catch (Exception e) {
                    log.warn("Generate preference recommendation failed: preference={}, error={}", preference, e.getMessage());
                }
            }

            cacheUtil.set(cacheKey, recommendations, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            log.info("Generate preference recommendations success: count={}", recommendations.size());
            return recommendations;
        } catch (Exception e) {
            log.error("Generate preference recommendations failed", e);
            throw new RuntimeException("Generate preference recommendations failed: " + e.getMessage(), e);
        }
    }

    public PersonalizedRouteResult generatePersonalizedRoute(PersonalizedRoutePreferences userPreferences,
                                                             PersonalizedRouteConstraints constraints) {
        try {
            PersonalizedRoutePreferences safePreferences = userPreferences == null
                    ? new PersonalizedRoutePreferences()
                    : userPreferences.copy();
            PersonalizedRouteConstraints safeConstraints = constraints == null
                    ? new PersonalizedRouteConstraints()
                    : constraints.copy();
            String cacheKey = PERSONALIZED_ROUTE_PREFIX
                    + preferenceCacheKey(safePreferences) + ":" + constraintCacheKey(safeConstraints);

            PersonalizedRouteResult cachedRoute = cacheUtil.get(cacheKey, PersonalizedRouteResult.class);
            if (cachedRoute != null) {
                log.info("Get personalized route from cache");
                return cachedRoute;
            }

            Integer cityId = safePreferences.getCityId();
            if (cityId == null) {
                throw new IllegalArgumentException("cityId is required for personalized route generation");
            }
            int days = resolveDays(safePreferences.getDays());
            BigDecimal budget = safePreferences.getBudget() == null
                    ? DEFAULT_BUDGET : safePreferences.getBudget();
            String preference = safePreferences.getPreference() == null
                    ? "balanced" : safePreferences.getPreference();
            List<String> interests = safePreferences.getInterests() == null
                    ? Collections.emptyList() : safePreferences.getInterests();
            String transportPreference = safePreferences.getTransportPreference() == null
                    ? "public" : safePreferences.getTransportPreference();

            List<Attraction> attractions = attractionService.getByCityId(cityId);
            List<Attraction> filteredAttractions = filterAttractionsByInterests(attractions, interests);
            if (filteredAttractions.isEmpty()) {
                filteredAttractions = attractions;
            }

            List<Integer> attractionIds = filteredAttractions.stream()
                    .map(Attraction::getId)
                    .collect(Collectors.toList());

            if (!"public".equals(transportPreference) && attractionIds.size() > days * 3) {
                log.info("Adjust attraction selection by transport preference: {}", transportPreference);
            }

            travel.route.algorithm.RoutePlanAlgorithm.OptimalRoute optimalRoute =
                    routeOptimizationService.planOptimalRoute(attractionIds, days, budget, preference);

            PersonalizedRouteResult result = PersonalizedRouteResult.builder()
                    .route(optimalRoute)
                    .userPreferences(safePreferences)
                    .constraints(safeConstraints)
                    .attractionCount(attractionIds.size())
                    .cityId(cityId)
                    .days(days)
                    .transportPreference(transportPreference)
                    .build();

            cacheUtil.set(cacheKey, result, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            log.info("Generate personalized route success: cityId={}, days={}", cityId, days);
            return result;
        } catch (Exception e) {
            log.error("Generate personalized route failed", e);
            throw new RuntimeException("Generate personalized route failed: " + e.getMessage(), e);
        }
    }

    public MultiDayRouteResult generateMultiDayRoute(Integer cityId, String startDate, String endDate, Map<String, Object> userPreferences) {
        try {
            Map<String, Object> effectivePreferences = buildMultiDayPreferences(cityId, startDate, endDate, userPreferences);
            PersonalizedRouteResult personalized = generatePersonalizedRoute(
                    toPersonalizedRoutePreferences(effectivePreferences),
                    new PersonalizedRouteConstraints());

            return MultiDayRouteResult.builder()
                    .route(personalized.getRoute())
                    .cityId(cityId)
                    .startDate(startDate)
                    .endDate(endDate)
                    .userPreferences(effectivePreferences)
                    .attractionCount(personalized.getAttractionCount())
                    .build();
        } catch (Exception e) {
            log.error("Generate multi-day route failed", e);
            throw new RuntimeException("Generate route failed: " + e.getMessage(), e);
        }
    }

    public RouteRecommendationReason getRouteRecommendationReason(Integer routeId, Integer userId) {
        try {
            log.info("Build route recommendation reason: routeId={}, userId={}", routeId, userId);
            return RouteRecommendationReason.builder()
                    .routeId(routeId)
                    .userId(userId)
                    .reasons(Arrays.asList("\u8def\u7ebf\u8bc4\u5206\u9ad8", "\u7b26\u5408\u7528\u6237\u504f\u597d", "\u70ed\u95e8\u63a8\u8350"))
                    .build();
        } catch (Exception e) {
            log.error("Build route recommendation reason failed", e);
            throw new RuntimeException("Build route recommendation reason failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildMultiDayPreferences(Integer cityId, String startDate, String endDate, Map<String, Object> userPreferences) {
        Map<String, Object> effectivePreferences = userPreferences == null ? new HashMap<>() : new HashMap<>(userPreferences);
        if (cityId != null) {
            effectivePreferences.put("cityId", cityId);
        }
        if (!effectivePreferences.containsKey("days")) {
            effectivePreferences.put("days", resolveTripDays(startDate, endDate));
        }
        return effectivePreferences;
    }

    private int resolveTripDays(String startDate, String endDate) {
        try {
            if (startDate == null || endDate == null) {
                return DEFAULT_DAYS;
            }
            long days = ChronoUnit.DAYS.between(LocalDate.parse(startDate), LocalDate.parse(endDate)) + 1;
            return days > 0 ? (int) days : DEFAULT_DAYS;
        } catch (DateTimeParseException e) {
            log.warn("Parse trip days failed: startDate={}, endDate={}", startDate, endDate);
            return DEFAULT_DAYS;
        }
    }

    private int extractDays(Object daysObj) {
        if (daysObj instanceof Number number) {
            return number.intValue();
        }
        if (daysObj instanceof String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return DEFAULT_DAYS;
            }
        }
        return DEFAULT_DAYS;
    }

    private int resolveDays(Integer days) {
        return days == null || days < 1 || days > 30 ? DEFAULT_DAYS : days;
    }

    private String preferenceCacheKey(PersonalizedRoutePreferences preferences) {
        return Integer.toHexString(Objects.hash(
                preferences.getCityId(),
                preferences.getDays(),
                preferences.getBudget(),
                preferences.getPreference(),
                preferences.getInterests(),
                preferences.getTransportPreference(),
                preferences.getExtensions()));
    }

    private String constraintCacheKey(PersonalizedRouteConstraints constraints) {
        return Integer.toHexString(Objects.hash(constraints.getExtensions()));
    }

    private PersonalizedRoutePreferences toPersonalizedRoutePreferences(Map<String, Object> source) {
        PersonalizedRoutePreferences result = new PersonalizedRoutePreferences();
        if (source == null || source.isEmpty()) {
            return result;
        }
        Map<String, JsonNode> extensions = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            switch (entry.getKey()) {
                case "cityId" -> result.setCityId(toInteger(entry.getValue()));
                case "days" -> result.setDays(toInteger(entry.getValue()));
                case "budget" -> result.setBudget(toBigDecimal(entry.getValue()));
                case "preference" -> result.setPreference(toStringValue(entry.getValue()));
                case "interests" -> result.setInterests(toStringList(entry.getValue()));
                case "transportPreference" -> result.setTransportPreference(toStringValue(entry.getValue()));
                default -> extensions.put(entry.getKey(), toJsonNode(entry.getValue()));
            }
        }
        result.setExtensions(extensions);
        return result;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string) {
            try {
                return Integer.valueOf(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (value instanceof String string) {
            try {
                return new BigDecimal(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private List<String> toStringList(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return Collections.emptyList();
        }
        return collection.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toList());
    }

    private JsonNode toJsonNode(Object value) {
        if (value == null) {
            return JsonNodeFactory.instance.nullNode();
        }
        if (value instanceof JsonNode jsonNode) {
            return jsonNode;
        }
        if (value instanceof String string) {
            return JsonNodeFactory.instance.textNode(string);
        }
        if (value instanceof Boolean bool) {
            return JsonNodeFactory.instance.booleanNode(bool);
        }
        if (value instanceof Integer integer) {
            return JsonNodeFactory.instance.numberNode(integer);
        }
        if (value instanceof Long longValue) {
            return JsonNodeFactory.instance.numberNode(longValue);
        }
        if (value instanceof Number number) {
            return JsonNodeFactory.instance.numberNode(number.doubleValue());
        }
        if (value instanceof Map<?, ?> map) {
            ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
            map.forEach((key, nestedValue) -> objectNode.set(String.valueOf(key), toJsonNode(nestedValue)));
            return objectNode;
        }
        if (value instanceof Collection<?> collection) {
            ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
            collection.forEach(item -> arrayNode.add(toJsonNode(item)));
            return arrayNode;
        }
        return JsonNodeFactory.instance.textNode(String.valueOf(value));
    }

    private BigDecimal resolveBudget(Object budgetObj) {
        if (budgetObj instanceof BigDecimal budget) {
            return budget;
        }
        if (budgetObj instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (budgetObj instanceof String value) {
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException ignored) {
                return DEFAULT_BUDGET;
            }
        }
        return DEFAULT_BUDGET;
    }

    private List<String> extractStringList(Map<String, Object> source, String key) {
        Object listObj = source.getOrDefault(key, Collections.emptyList());
        if (!(listObj instanceof List<?> rawList)) {
            return Collections.emptyList();
        }
        return rawList.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toList());
    }

    private List<Attraction> filterAttractionsByPreferences(List<Attraction> attractions, List<String> preferredTypes) {
        if (attractions == null || attractions.isEmpty() || preferredTypes == null || preferredTypes.isEmpty()) {
            return attractions == null ? Collections.emptyList() : attractions;
        }

        return attractions.stream()
                .filter(attraction -> containsAnyThemeKeyword(attraction, preferredTypes))
                .collect(Collectors.toList());
    }

    private List<Attraction> filterAttractionsByInterests(List<Attraction> attractions, List<String> interests) {
        if (attractions == null || attractions.isEmpty() || interests == null || interests.isEmpty()) {
            return attractions == null ? Collections.emptyList() : attractions;
        }

        return attractions.stream()
                .filter(attraction -> containsAnyThemeKeyword(attraction, interests))
                .collect(Collectors.toList());
    }

    private boolean containsAnyThemeKeyword(Attraction attraction, List<String> themeKeys) {
        String description = attraction.getDescription();
        if (description == null) {
            return false;
        }
        return themeKeys.stream()
                .map(theme -> themeAttractionMap.getOrDefault(theme, Collections.emptyList()))
                .flatMap(List::stream)
                .anyMatch(description::contains);
    }
}
