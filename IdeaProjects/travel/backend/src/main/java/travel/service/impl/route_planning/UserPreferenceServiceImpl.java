package travel.service.impl.route_planning;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.route_planning.Route;
import travel.entity.route_planning.RouteRating;
import travel.service.route_planning.RouteRatingService;
import travel.service.route_planning.RouteService;
import travel.service.route_planning.UserPreferenceService;
import travel.utils.CacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements UserPreferenceService {

    @Autowired
    private RouteService routeService;

    @Autowired
    private RouteRatingService routeRatingService;

    @Autowired
    private CacheUtil cacheUtil;

    private static final String PREFERENCE_PREFIX = "user:preference:";
    private static final String PROFILE_PREFIX = "user:profile:";

    // 关键词库用于NLP分析 - 使用ConcurrentHashMap保证线程安全
    private static final Map<String, List<String>> PREFERENCE_KEYWORDS = new ConcurrentHashMap<>();

    static {
        PREFERENCE_KEYWORDS.put("transport_cost", Arrays.asList("便宜", "省钱", "划算", "经济", "票价", "费用"));
        PREFERENCE_KEYWORDS.put("transport_time", Arrays.asList("快", "省时", "迅速", "高效", "直达"));
        PREFERENCE_KEYWORDS.put("comfort", Arrays.asList("舒适", "宽敞", "安静", "服务好", "体验好"));
        PREFERENCE_KEYWORDS.put("budget", Arrays.asList("预算", "省钱", "划算", "性价比", "便宜"));
        PREFERENCE_KEYWORDS.put("culture", Arrays.asList("文化", "历史", "古迹", "博物馆", "传统"));
        PREFERENCE_KEYWORDS.put("food", Arrays.asList("美食", "好吃", "餐厅", "特色菜", "小吃"));
        PREFERENCE_KEYWORDS.put("scenery", Arrays.asList("风景", "美景", "拍照", "打卡", "漂亮"));
        PREFERENCE_KEYWORDS.put("convenience", Arrays.asList("方便", "便利", "交通好", "位置好"));
    }

    @Override
    public void recordPreference(Integer userId, String preferenceType, String preferenceValue) {
        String cacheKey = PREFERENCE_PREFIX + userId + ":" + preferenceType;

        Map<String, Object> preference = new HashMap<>();
        preference.put("userId", userId);
        preference.put("type", preferenceType);
        preference.put("value", preferenceValue);
        preference.put("timestamp", new Date());

        cacheUtil.set(cacheKey, preference, 30, java.util.concurrent.TimeUnit.DAYS);
        log.info("记录用户偏好: userId={}, type={}, value={}", userId, preferenceType, preferenceValue);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeUserProfile(Integer userId) {
        String cacheKey = PROFILE_PREFIX + userId;

        Map<String, Object> cachedProfile = cacheUtil.get(cacheKey, Map.class);
        if (cachedProfile != null) {
            return cachedProfile;
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", userId);
        profile.put("transportPreference", getTransportPreference(userId));
        profile.put("budgetSensitivity", getBudgetSensitivity(userId));
        profile.put("comfortLevel", getComfortLevel(userId));
        profile.put("culturePreferences", getCulturePreferences(userId));
        profile.put("foodPreferences", getFoodPreferences(userId));
        profile.put("tripType", getTripTypePreference(userId));
        profile.put("analyzedAt", new Date());

        cacheUtil.set(cacheKey, profile, 7, java.util.concurrent.TimeUnit.DAYS);

        return profile;
    }

    @Override
    public List<String> extractPreferencesFromReview(String reviewContent) {
        List<String> extractedPreferences = new ArrayList<>();

        if (reviewContent == null || reviewContent.isEmpty()) {
            return extractedPreferences;
        }

        for (Map.Entry<String, List<String>> entry : PREFERENCE_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (reviewContent.contains(keyword)) {
                    extractedPreferences.add(entry.getKey());
                    break;
                }
            }
        }

        return extractedPreferences.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public Map<String, Double> getTransportPreference(Integer userId) {
        Map<String, Double> preferences = new HashMap<>();

        // 获取用户历史评分数据
        List<RouteRating> ratings = routeRatingService.getByUserId(userId);

        // 统计不同交通方式相关路线的评分
        Map<String, List<Integer>> transportRatings = new HashMap<>();
        transportRatings.put("bus", new ArrayList<>());
        transportRatings.put("subway", new ArrayList<>());
        transportRatings.put("taxi", new ArrayList<>());
        transportRatings.put("car", new ArrayList<>());
        transportRatings.put("walking", new ArrayList<>());

        for (RouteRating rating : ratings) {
            Route route = routeService.getById(rating.getRouteId());
            if (route != null && route.getDescription() != null) {
                String desc = route.getDescription();
                if (desc.contains("公交") || desc.contains("地铁")) {
                    transportRatings.get("bus").add(rating.getRating());
                }
                if (desc.contains("打车") || desc.contains("出租车")) {
                    transportRatings.get("taxi").add(rating.getRating());
                }
                if (desc.contains("自驾") || desc.contains("开车")) {
                    transportRatings.get("car").add(rating.getRating());
                }
                if (desc.contains("步行") || desc.contains("走路")) {
                    transportRatings.get("walking").add(rating.getRating());
                }
            }
        }

        // 计算权重
        for (Map.Entry<String, List<Integer>> entry : transportRatings.entrySet()) {
            List<Integer> scores = entry.getValue();
            if (!scores.isEmpty()) {
                double avgScore = scores.stream().mapToInt(Integer::intValue).average().orElse(3.0);
                preferences.put(entry.getKey(), avgScore / 5.0); // 归一化到0-1
            } else {
                preferences.put(entry.getKey(), 0.5); // 默认中等偏好
            }
        }

        return preferences;
    }

    @Override
    public Double getBudgetSensitivity(Integer userId) {
        List<RouteRating> ratings = routeRatingService.getByUserId(userId);

        if (ratings.isEmpty()) {
            return 0.5; // 默认中等敏感度
        }

        // 分析评价内容中的预算相关关键词
        int budgetMentionCount = 0;
        int totalReviews = 0;

        for (RouteRating rating : ratings) {
            if (rating.getReview() != null && !rating.getReview().isEmpty()) {
                totalReviews++;
                List<String> preferences = extractPreferencesFromReview(rating.getReview());
                if (preferences.contains("budget")) {
                    budgetMentionCount++;
                }
            }
        }

        if (totalReviews == 0) {
            return 0.5;
        }

        return (double) budgetMentionCount / totalReviews;
    }

    @Override
    public String getComfortLevel(Integer userId) {
        List<RouteRating> ratings = routeRatingService.getByUserId(userId);

        if (ratings.isEmpty()) {
            return "standard"; // 默认标准
        }

        double avgRating = ratings.stream().mapToInt(RouteRating::getRating).average().orElse(3.0);

        if (avgRating >= 4.5) {
            return "luxury";
        } else if (avgRating >= 3.5) {
            return "standard";
        } else {
            return "economy";
        }
    }

    @Override
    public List<String> getCulturePreferences(Integer userId) {
        List<String> preferences = new ArrayList<>();
        List<RouteRating> ratings = routeRatingService.getByUserId(userId);

        for (RouteRating rating : ratings) {
            if (rating.getReview() != null) {
                List<String> extracted = extractPreferencesFromReview(rating.getReview());
                if (extracted.contains("culture")) {
                    preferences.add("history");
                    preferences.add("museum");
                    preferences.add("heritage");
                }
            }
        }

        return preferences.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public List<String> getFoodPreferences(Integer userId) {
        List<String> preferences = new ArrayList<>();
        List<RouteRating> ratings = routeRatingService.getByUserId(userId);

        for (RouteRating rating : ratings) {
            if (rating.getReview() != null) {
                List<String> extracted = extractPreferencesFromReview(rating.getReview());
                if (extracted.contains("food")) {
                    preferences.add("local_cuisine");
                    preferences.add("restaurant");
                    preferences.add("snack");
                }
            }
        }

        return preferences.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> recommendRoutesByProfile(Integer userId, Integer cityId, Integer days) {
        Map<String, Object> profile = analyzeUserProfile(userId);

        // 获取城市所有路线
        List<Route> allRoutes = routeService.getByCityId(cityId);

        List<Map<String, Object>> recommendations = new ArrayList<>();

        for (Route route : allRoutes) {
            if (route.getDurationDays().equals(days)) {
                Integer matchScore = calculateMatchScore(route.getId(), userId);

                Map<String, Object> recommendation = new HashMap<>();
                recommendation.put("route", route);
                recommendation.put("matchScore", matchScore);
                recommendation.put("reason", generateRecommendationReason(profile, route));

                recommendations.add(recommendation);
            }
        }

        // 按匹配度排序
        recommendations.sort((a, b) ->
                ((Integer) b.get("matchScore")).compareTo((Integer) a.get("matchScore")));

        return recommendations.stream().limit(10).collect(Collectors.toList());
    }

    @Override
    public void updatePreferenceModel(Integer userId, Integer routeId, Integer rating, String review) {
        // 提取评价中的偏好
        List<String> preferences = extractPreferencesFromReview(review);

        // 记录偏好
        for (String preference : preferences) {
            recordPreference(userId, preference, String.valueOf(rating));
        }

        // 清除缓存，下次重新分析
        String cacheKey = PROFILE_PREFIX + userId;
        cacheUtil.delete(cacheKey);

        log.info("更新用户偏好模型: userId={}, routeId={}, rating={}", userId, routeId, rating);
    }

    @Override
    public List<Integer> getSimilarUsers(Integer userId) {
        // 简化的相似度计算 - 实际应该使用更复杂的算法
        List<Integer> similarUsers = new ArrayList<>();

        // 这里应该查询数据库找出偏好相似的用户
        // 暂时返回空列表，实际实现需要更复杂的逻辑

        return similarUsers;
    }

    @Override
    public List<Map<String, Object>> collaborativeFilteringRecommend(Integer userId) {
        List<Integer> similarUsers = getSimilarUsers(userId);

        if (similarUsers.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取相似用户喜欢的路线
        Set<Integer> recommendedRouteIds = new HashSet<>();
        for (Integer similarUserId : similarUsers) {
            List<RouteRating> ratings = routeRatingService.getByUserId(similarUserId);
            for (RouteRating rating : ratings) {
                if (rating.getRating() >= 4) {
                    recommendedRouteIds.add(rating.getRouteId());
                }
            }
        }

        // 构建推荐列表
        List<Map<String, Object>> recommendations = new ArrayList<>();
        for (Integer routeId : recommendedRouteIds) {
            Route route = routeService.getById(routeId);
            if (route != null) {
                Map<String, Object> recommendation = new HashMap<>();
                recommendation.put("route", route);
                recommendation.put("reason", "相似用户推荐");
                recommendations.add(recommendation);
            }
        }

        return recommendations;
    }

    @Override
    public String getTripTypePreference(Integer userId) {
        List<RouteRating> ratings = routeRatingService.getByUserId(userId);

        if (ratings.isEmpty()) {
            return "leisure"; // 默认休闲
        }

        // 根据评分分布判断旅程类型
        double avgRating = ratings.stream().mapToInt(RouteRating::getRating).average().orElse(3.0);
        int routeCount = ratings.size();

        if (routeCount >= 5 && avgRating >= 4.0) {
            return "adventure"; // 经常出行且评分高 = 探险型
        } else if (routeCount <= 2) {
            return "business"; // 出行少 = 商务型
        } else if (avgRating >= 4.5) {
            return "couple"; // 高评分 = 情侣/品质型
        } else {
            return "family"; // 其他 = 家庭型
        }
    }

    @Override
    public Integer calculateMatchScore(Integer routeId, Integer userId) {
        Map<String, Object> profile = analyzeUserProfile(userId);
        Route route = routeService.getById(routeId);

        if (route == null) {
            return 0;
        }

        int score = 50; // 基础分

        // 交通偏好匹配
        @SuppressWarnings("unchecked")
        Map<String, Double> transportPref = (Map<String, Double>) profile.get("transportPreference");
        if (transportPref != null) {
            score += transportPref.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 20;
        }

        // 舒适度匹配
        String comfortLevel = (String) profile.get("comfortLevel");
        if (comfortLevel != null && route.getDifficulty() != null) {
            if ("luxury".equals(comfortLevel) && "简单".equals(route.getDifficulty())) {
                score += 15;
            } else if ("economy".equals(comfortLevel) && "困难".equals(route.getDifficulty())) {
                score += 10;
            }
        }

        // 预算敏感度匹配
        Double budgetSensitivity = (Double) profile.get("budgetSensitivity");
        if (budgetSensitivity != null && budgetSensitivity > 0.5) {
            // 预算敏感用户，检查路线性价比
            score += 10;
        }

        return Math.min(100, score);
    }

    // 辅助方法
    private String generateRecommendationReason(Map<String, Object> profile, Route route) {
        StringBuilder reason = new StringBuilder();

        String tripType = (String) profile.get("tripType");
        if ("adventure".equals(tripType)) {
            reason.append("适合探险爱好者");
        } else if ("family".equals(tripType)) {
            reason.append("适合家庭出游");
        } else if ("couple".equals(tripType)) {
            reason.append("适合情侣出行");
        } else {
            reason.append("符合您的出行偏好");
        }

        return reason.toString();
    }
}
