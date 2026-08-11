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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

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
    private static final long RECOMMENDATION_CACHE_EXPIRE_HOURS = 12;

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

        List<AIPersonalizedRecommendationItem> recommendations = new ArrayList<>(normalizedLimit);
        SplittableRandom random = new SplittableRandom(
                31L * Objects.hashCode(userId) + Objects.hashCode(normalizedType));
        LocalDateTime recommendedAt = LocalDateTime.now();

        // 当前数据源为模拟数据，但使用稳定种子保证缓存失效后结果可复现，便于测试和问题排查。
        for (int i = 1; i <= normalizedLimit; i++) {
            AIPersonalizedRecommendationItem recommendation;
            if ("attractions".equals(normalizedType)) {
                recommendation = AIPersonalizedRecommendationItem.builder()
                        .id(i)
                        .type("attraction")
                        .name("热门景点" + i)
                        .description("这是一个值得参观的热门景点")
                        .rating(4.5 + random.nextDouble(0.0, 0.5))
                        .distance(10 + random.nextDouble(0.0, 20.0))
                        .score(0.8 + random.nextDouble(0.0, 0.2))
                        .recommendedAt(recommendedAt)
                        .build();
            } else if ("restaurants".equals(normalizedType)) {
                recommendation = AIPersonalizedRecommendationItem.builder()
                        .id(i)
                        .type("restaurant")
                        .name("特色餐厅" + i)
                        .description("这是一家提供当地特色美食的餐厅")
                        .rating(4.0 + random.nextDouble(0.0, 1.0))
                        .priceLevel("中等")
                        .score(0.8 + random.nextDouble(0.0, 0.2))
                        .recommendedAt(recommendedAt)
                        .build();
            } else if ("routes".equals(normalizedType)) {
                recommendation = AIPersonalizedRecommendationItem.builder()
                        .id(i)
                        .type("route")
                        .name("精选路线" + i)
                        .description("这是一条精心设计的旅游路线")
                        .days(2 + (i % 3))
                        .difficulty("中等")
                        .score(0.8 + random.nextDouble(0.0, 0.2))
                        .recommendedAt(recommendedAt)
                        .build();
            } else {
                recommendation = AIPersonalizedRecommendationItem.builder()
                        .id(i)
                        .type("general")
                        .name("推荐项目" + i)
                        .description("这是一个个性化推荐项目")
                        .score(0.8 + random.nextDouble(0.0, 0.2))
                        .recommendedAt(recommendedAt)
                        .build();
            }
            recommendations.add(recommendation);
        }

        cacheUtil.set(cacheKey, List.copyOf(recommendations), RECOMMENDATION_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        return List.copyOf(recommendations);
    }

    @Override
    public AIPlanRouteResponse planRoute(AIPlanRoutePreferences preferences, AIPlanRouteConstraints constraints) {
        String destination = stringPreference(
                preferences == null ? null : preferences.getDestination(), "北京");
        int days = normalizeDays(
                preferences == null ? null : preferences.getDays(), DEFAULT_PLAN_DAYS);
        String travelStyle = stringPreference(
                preferences == null ? null : preferences.getTravelStyle(), "balanced");

        List<AIDailyPlan> dailyPlans = PLAN_CONSTRAINT_SCHEDULER.buildDailyPlans(days, constraints);

        return AIPlanRouteResponse.builder()
                .success(true)
                .planType("intelligent")
                .timestamp(LocalDateTime.now())
                .destination(destination)
                .days(days)
                .travelStyle(travelStyle)
                .dailyPlans(dailyPlans)
                .estimatedCost(1500 * days)
                .optimizationScore(85)
                .build();
    }


    @Override
    public AITravelGuideContent generateTravelGuide(Integer cityId, int days, Map<String, JsonNode> preferences) {
        int normalizedDays = normalizeDays(days);

        List<String> preparationTips = List.of(
                "准备舒适的鞋子，北京景点之间距离较远",
                "随身携带身份证，很多景点需要实名制购票",
                "根据季节准备合适的衣物",
                "下载当地交通APP，方便出行");

        AITransportationGuide transportation = AITransportationGuide.builder()
                .airport("首都国际机场、大兴国际机场")
                .train("北京南站、北京站、北京西站")
                .localTransport("地铁、公交、出租车")
                .tips("推荐购买北京市政交通一卡通，乘坐公共交通更方便")
                .build();

        List<AIDailyItinerary> dailyItineraries = new ArrayList<>(normalizedDays);
        for (int day = 1; day <= normalizedDays; day++) {
            dailyItineraries.add(AIDailyItinerary.builder()
                    .day(day)
                    .title("第" + day + "天行程")
                    .description("详细的每日行程安排")
                    .build());
        }

        List<AIFoodRecommendation> foodRecommendations = List.of(
                AIFoodRecommendation.builder()
                        .name("北京烤鸭")
                        .description("北京特色美食，皮脆肉嫩")
                        .recommendedRestaurants(List.of("全聚德", "大董烤鸭"))
                        .build(),
                AIFoodRecommendation.builder()
                        .name("炸酱面")
                        .description("老北京传统面食")
                        .recommendedRestaurants(List.of("方砖厂69号炸酱面", "老北京炸酱面大王"))
                        .build());

        AIAccommodationGuide accommodation = AIAccommodationGuide.builder()
                .budget("200-500元/晚")
                .recommendedAreas(List.of("王府井", "西单", "国贸"))
                .tips("建议提前预订住宿，尤其是旅游旺季")
                .build();

        AITravelGuideSection guideSection = AITravelGuideSection.builder()
                .preparationTips(preparationTips)
                .transportation(transportation)
                .dailyItineraries(dailyItineraries)
                .foodRecommendations(foodRecommendations)
                .accommodation(accommodation)
                .shoppingTips(List.of("王府井步行街：大型商场和特色商店", "西单：年轻时尚的购物区", "南锣鼓巷：特色小店和纪念品"))
                .notes(List.of("尊重当地风俗习惯", "注意保管好个人财物", "遵守景区规定，文明游览", "关注天气变化，做好相应准备"))
                .build();

        return AITravelGuideContent.builder()
                .success(true)
                .cityId(cityId)
                .cityName("北京")
                .days(normalizedDays)
                .generatedAt(LocalDateTime.now())
                .guideContent(guideSection)
                .guideQualityScore(92)
                .build();
    }

    @Override
    public AIBudgetDetails estimateBudget(Integer cityId, int days, Map<String, JsonNode> preferences) {
        int normalizedDays = normalizeDays(days);

        double accommodation = 300.0 * normalizedDays;
        double transportation = 100.0 * normalizedDays;
        double food = 150.0 * normalizedDays;
        double attractions = 200.0 * normalizedDays;
        double shopping = 200.0 * normalizedDays;
        double miscellaneous = 100.0 * normalizedDays;
        double totalBudget = accommodation + transportation + food + attractions + shopping + miscellaneous;

        AIBudgetBreakdown budgetBreakdown = AIBudgetBreakdown.builder()
                .accommodation(accommodation)
                .transportation(transportation)
                .food(food)
                .attractions(attractions)
                .shopping(shopping)
                .miscellaneous(miscellaneous)
                .total(totalBudget)
                .build();

        return AIBudgetDetails.builder()
                .success(true)
                .cityId(cityId)
                .cityName("北京")
                .days(normalizedDays)
                .estimatedAt(LocalDateTime.now())
                .budgetDetails(budgetBreakdown)
                .savingTips(List.of(
                        "选择性价比高的住宿，如快捷酒店或民宿",
                        "使用公共交通，购买交通卡",
                        "尝试当地小吃，比高档餐厅更实惠",
                        "购买景点联票，比单独购票更便宜",
                        "避开旅游旺季，价格会更实惠"))
                .currency("CNY")
                .build();
    }

    @Override
    public AISafetyAdviceResponse getSafetyAdvice(Integer cityId) {
        String cityName = "北京";

        List<String> generalAdvice = new ArrayList<>();
        generalAdvice.add("保管好个人财物，尤其是在人多的地方");
        generalAdvice.add("随身携带身份证件，很多地方需要实名制");
        generalAdvice.add("注意交通安全，遵守交通规则");
        generalAdvice.add("关注天气变化，做好相应准备");
        generalAdvice.add("紧急情况可拨打110报警");

        List<String> travelAdvice = new ArrayList<>();
        travelAdvice.add("选择正规的旅行社和导游");
        travelAdvice.add("不要接受陌生人的搭讪和推销");
        travelAdvice.add("在景区内跟随指示牌，不要进入未开放区域");
        travelAdvice.add("注意饮食卫生，选择正规餐厅");
        travelAdvice.add("购买旅游保险，保障自身安全");

        Map<String, List<String>> areaAdvice = new HashMap<>();
        areaAdvice.put("景区", List.of("注意保管好门票和个人物品", "遵守景区规定，文明游览", "注意台阶和斜坡，防止摔倒"));
        areaAdvice.put("地铁", List.of("排队上下车，不要拥挤", "保管好随身物品", "注意站台间隙"));
        areaAdvice.put("商业区", List.of("注意扒手", "比较价格，避免被骗", "保管好购物凭证"));

        return AISafetyAdviceResponse.builder()
                .success(true)
                .cityId(cityId)
                .cityName(cityName)
                .advisedAt(LocalDateTime.now())
                .safetyLevel("high")
                .safetyScore(90)
                .generalAdvice(generalAdvice)
                .travelAdvice(travelAdvice)
                .areaAdvice(areaAdvice)
                .build();
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
