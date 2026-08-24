package travel.route.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import travel.route.dto.ai.AIAccommodationGuide;
import travel.route.dto.ai.AIBudgetBreakdown;
import travel.route.dto.ai.AIBudgetDetails;
import travel.route.dto.ai.AIDailyPlan;
import travel.route.dto.ai.AIDailyItinerary;
import travel.route.dto.ai.AIFoodRecommendation;
import travel.route.dto.ai.AIPersonalizedRecommendationItem;
import travel.route.dto.ai.AIPlanRouteConstraints;
import travel.route.dto.ai.AIPlanRoutePreferences;
import travel.route.dto.ai.AIPlanRouteResponse;
import travel.route.dto.ai.AISafetyAdviceResponse;
import travel.route.dto.ai.AITravelGuideContent;
import travel.route.dto.ai.AITravelGuideSection;
import travel.route.dto.ai.AITransportationGuide;
import travel.route.service.AIAdvancedService;
import travel.common.utils.CacheUtil;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 高级AI功能服务实现
 */
@Service
@RequiredArgsConstructor
public class AIAdvancedServiceImpl implements AIAdvancedService {

    private static final Logger log = LoggerFactory.getLogger(AIAdvancedServiceImpl.class);

    private final CacheUtil cacheUtil;

    private static final int MAX_RECOMMENDATION_LIMIT = 50;
    private static final int DEFAULT_PLAN_DAYS = 3;
    private static final int MIN_PLAN_DAYS = 1;
    private static final int MAX_PLAN_DAYS = 30;
    private static final AIPlanConstraintScheduler PLAN_CONSTRAINT_SCHEDULER =
            new AIPlanConstraintScheduler();

    private static final String AI_ADVANCED_PREFIX = "ai:advanced:";

    @Override
    public List<AIPersonalizedRecommendationItem> getPersonalizedRecommendations(Integer userId, String recommendationType, int limit) {
        int normalizedLimit = normalizeRecommendationLimit(limit);
        if (normalizedLimit == 0) {
            log.debug("推荐数量不合法，返回空结果: userId={}, limit={}", userId, limit);
            return List.of();
        }

        String normalizedType = normalizeRecommendationType(recommendationType);
        String normalizedUserId = userId == null ? "anonymous" : userId.toString();
        // limit 必须参与缓存键，避免小 limit 的结果污染大 limit 请求。
        String cacheKey = CacheUtil.generateKey(
                AI_ADVANCED_PREFIX + "recommendation", normalizedUserId, normalizedType, normalizedLimit);
        Object cachedObj = cacheUtil.get(cacheKey, Object.class);
        if (cachedObj instanceof List<?> cachedList) {
            List<AIPersonalizedRecommendationItem> cachedRecommendations = new ArrayList<>();
            for (Object item : cachedList) {
                if (item instanceof AIPersonalizedRecommendationItem recommendation) {
                    cachedRecommendations.add(recommendation);
                }
            }
            if (cachedRecommendations.size() >= normalizedLimit) {
                return List.copyOf(cachedRecommendations.subList(0, normalizedLimit));
            }
        }

        log.warn("个性化推荐无可用数据源，返回空列表: userId={}, type={}, limit={}",
                userId, normalizedType, normalizedLimit);
        return List.of();
    }

    @Override
    public AIPlanRouteResponse planRoute(AIPlanRoutePreferences preferences, AIPlanRouteConstraints constraints) {
        String destination = stringPreference(
                preferences == null ? null : preferences.getDestination(), null);
        if (destination == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_MISSING);
        }
        if (constraints == null || constraints.getMustVisitAttractions() == null
                || constraints.getMustVisitAttractions().isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_PLANNING_INSUFFICIENT_ATTRACTIONS);
        }
        int days = normalizeDays(
                preferences == null ? null : preferences.getDays(), DEFAULT_PLAN_DAYS);
        String travelStyle = stringPreference(
                preferences == null ? null : preferences.getTravelStyle(), "balanced");

        List<AIDailyPlan> dailyPlans = PLAN_CONSTRAINT_SCHEDULER.buildDailyPlans(days, constraints);

        return AIPlanRouteResponse.builder()
                .success(true)
                .planType("constraint-scheduler")
                .timestamp(LocalDateTime.now())
                .destination(destination)
                .days(days)
                .travelStyle(travelStyle)
                .dailyPlans(dailyPlans)
                .estimatedCost(null)
                .optimizationScore(null)
                .build();
    }


    @Override
    public AITravelGuideContent generateTravelGuide(Integer cityId, int days, Map<String, JsonNode> preferences) {
        throw new BusinessException(ErrorCodeEnum.INTELLIGENT_RECOMMENDATION_NO_DATA);
    }


    @Override
    public AIBudgetDetails estimateBudget(Integer cityId, int days, Map<String, JsonNode> preferences) {
        throw new BusinessException(ErrorCodeEnum.INTELLIGENT_RECOMMENDATION_NO_DATA);
    }


    @Override
    public AISafetyAdviceResponse getSafetyAdvice(Integer cityId) {
        throw new BusinessException(ErrorCodeEnum.INTELLIGENT_RECOMMENDATION_NO_DATA);
    }


    private int normalizeRecommendationLimit(int limit) {
        if (limit <= 0) {
            return 0;
        }
        return Math.min(limit, MAX_RECOMMENDATION_LIMIT);
    }

    private String normalizeRecommendationType(String recommendationType) {
        if (recommendationType == null || recommendationType.isBlank()) {
            return "general";
        }
        return recommendationType.trim().toLowerCase(Locale.ROOT);
    }

    private int normalizeDays(Object value, int defaultDays) {
        if (value == null) {
            return defaultDays;
        }
        if (value instanceof Number number) {
            return normalizeDays(number.intValue());
        }
        if (value instanceof String text) {
            try {
                return normalizeDays(Integer.parseInt(text.trim()));
            } catch (NumberFormatException e) {
                log.debug("无法解析行程天数，使用默认值: value={}", text);
                return defaultDays;
            }
        }
        return defaultDays;
    }

    private int normalizeDays(int days) {
        if (days <= 0) {
            return MIN_PLAN_DAYS;
        }
        return Math.min(days, MAX_PLAN_DAYS);
    }

    private String stringPreference(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? defaultValue : text;
    }

}
