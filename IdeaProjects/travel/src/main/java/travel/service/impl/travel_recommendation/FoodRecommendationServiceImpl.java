package travel.service.impl.travel_recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.route_planning.Route;
import travel.entity.route_planning.RouteAttraction;
import travel.entity.travel_recommendation.Attraction;
import travel.service.route_planning.RouteAttractionService;
import travel.service.route_planning.RouteService;
import travel.service.travel_recommendation.AttractionService;
import travel.service.travel_recommendation.FoodRecommendationService;
import travel.utils.CacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodRecommendationServiceImpl implements FoodRecommendationService {

    @Autowired
    private AttractionService attractionService;

    @Autowired
    private RouteService routeService;

    @Autowired
    private RouteAttractionService routeAttractionService;

    @Autowired
    private CacheUtil cacheUtil;

    private static final String FOOD_PREFIX = "food:";

    @Override
    public List<Map<String, Object>> getNearbyFood(Integer attractionId, String foodType, BigDecimal budget) {
        Attraction attraction = attractionService.getById(attractionId);
        if (attraction == null) {
            return Collections.emptyList();
        }

        String cacheKey = FOOD_PREFIX + "nearby:" + attractionId + ":" + foodType;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cachedFoods = cacheUtil.get(cacheKey, List.class);
        if (cachedFoods != null) {
            return cachedFoods;
        }

        // 模拟美食数据
        List<Map<String, Object>> foods = generateMockFoodData(foodType, budget);

        cacheUtil.set(cacheKey, foods, 30, java.util.concurrent.TimeUnit.MINUTES);

        return foods;
    }

    @Override
    public List<Map<String, Object>> getCitySpecialties(Integer cityId) {
        String cacheKey = FOOD_PREFIX + "specialties:" + cityId;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cachedSpecialties = cacheUtil.get(cacheKey, List.class);
        if (cachedSpecialties != null) {
            return cachedSpecialties;
        }

        // 模拟城市特色美食
        List<Map<String, Object>> specialties = new ArrayList<>();

        Map<String, Object> food1 = new HashMap<>();
        food1.put("name", "当地特色小吃");
        food1.put("type", "snack");
        food1.put("price", new BigDecimal("15"));
        food1.put("rating", 4.5);
        food1.put("description", "当地最具代表性的传统小吃");
        specialties.add(food1);

        Map<String, Object> food2 = new HashMap<>();
        food2.put("name", "老字号餐厅");
        food2.put("type", "restaurant");
        food2.put("price", new BigDecimal("80"));
        food2.put("rating", 4.7);
        food2.put("description", "百年老店，正宗本地菜");
        specialties.add(food2);

        cacheUtil.set(cacheKey, specialties, 60, java.util.concurrent.TimeUnit.MINUTES);

        return specialties;
    }

    @Override
    public List<Map<String, Object>> recommendRestaurantsByRoute(Integer routeId, String mealTime) {
        Route route = routeService.getById(routeId);
        if (route == null) {
            return Collections.emptyList();
        }

        List<RouteAttraction> attractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId);
        List<Map<String, Object>> recommendations = new ArrayList<>();

        // 为每个景点推荐附近餐厅
        for (RouteAttraction ra : attractions) {
            Attraction attraction = attractionService.getById(ra.getAttractionId());
            if (attraction != null) {
                Map<String, Object> recommendation = new HashMap<>();
                recommendation.put("attractionId", ra.getAttractionId());
                recommendation.put("attractionName", attraction.getName());
                recommendation.put("dayNumber", ra.getDayNumber());
                recommendation.put("mealTime", mealTime);
                recommendation.put("restaurants", getNearbyFood(ra.getAttractionId(), "restaurant", new BigDecimal("100")));
                recommendations.add(recommendation);
            }
        }

        return recommendations;
    }

    @Override
    public Map<String, Object> planFoodRoute(Integer cityId, Integer days, List<String> foodPreference) {
        Map<String, Object> foodRoute = new HashMap<>();

        foodRoute.put("cityId", cityId);
        foodRoute.put("days", days);
        foodRoute.put("foodPreference", foodPreference);

        // 每日美食安排
        List<Map<String, Object>> dailyFoodPlan = new ArrayList<>();
        for (int day = 1; day <= days; day++) {
            Map<String, Object> dayPlan = new HashMap<>();
            dayPlan.put("day", day);
            dayPlan.put("breakfast", getRandomFoodRecommendation("breakfast", foodPreference));
            dayPlan.put("lunch", getRandomFoodRecommendation("lunch", foodPreference));
            dayPlan.put("dinner", getRandomFoodRecommendation("dinner", foodPreference));
            dayPlan.put("snacks", getRandomFoodRecommendation("snack", foodPreference));
            dailyFoodPlan.add(dayPlan);
        }

        foodRoute.put("dailyPlan", dailyFoodPlan);
        foodRoute.put("estimatedCost", calculateFoodCost(days));
        foodRoute.put("mustTryFoods", getCitySpecialties(cityId));

        return foodRoute;
    }

    @Override
    public Map<String, Object> getRestaurantDetail(Integer restaurantId) {
        // 模拟餐厅详情
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", restaurantId);
        detail.put("name", "示例餐厅");
        detail.put("type", "本地菜");
        detail.put("rating", 4.5);
        detail.put("priceRange", "人均¥50-100");
        detail.put("address", "示例地址");
        detail.put("phone", "010-12345678");
        detail.put("openingHours", "10:00-22:00");
        detail.put("signatureDishes", Arrays.asList("招牌菜1", "招牌菜2", "招牌菜3"));
        detail.put("features", Arrays.asList("老字号", "本地特色", "环境优雅"));

        return detail;
    }

    @Override
    public Map<String, Object> getFoodRating(Integer restaurantId) {
        Map<String, Object> rating = new HashMap<>();
        rating.put("restaurantId", restaurantId);
        rating.put("overallRating", 4.5);
        rating.put("tasteRating", 4.6);
        rating.put("serviceRating", 4.3);
        rating.put("environmentRating", 4.4);
        rating.put("valueRating", 4.5);
        rating.put("reviewCount", 1234);
        rating.put("recommendationRate", "92%");

        return rating;
    }

    @Override
    public List<Map<String, Object>> filterRestaurantsByPrice(Integer cityId, BigDecimal minPrice, BigDecimal maxPrice) {
        List<Map<String, Object>> allRestaurants = getCitySpecialties(cityId);
        List<Map<String, Object>> filtered = new ArrayList<>();

        for (Map<String, Object> restaurant : allRestaurants) {
            BigDecimal price = (BigDecimal) restaurant.get("price");
            if (price != null && price.compareTo(minPrice) >= 0 && price.compareTo(maxPrice) <= 0) {
                filtered.add(restaurant);
            }
        }

        return filtered;
    }

    @Override
    public List<Map<String, Object>> getMustTryFoods(Integer cityId, Integer limit) {
        List<Map<String, Object>> specialties = getCitySpecialties(cityId);

        // 按评分排序
        specialties.sort((a, b) -> {
            Double ratingA = (Double) a.get("rating");
            Double ratingB = (Double) b.get("rating");
            return ratingB.compareTo(ratingA);
        });

        return specialties.stream().limit(limit).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getFoodMapData(Integer cityId) {
        List<Map<String, Object>> mapData = new ArrayList<>();

        // 获取城市特色美食的地图坐标
        List<Map<String, Object>> foods = getCitySpecialties(cityId);
        for (int i = 0; i < foods.size(); i++) {
            Map<String, Object> food = foods.get(i);
            Map<String, Object> marker = new HashMap<>();
            marker.put("id", i + 1);
            marker.put("name", food.get("name"));
            marker.put("type", food.get("type"));
            marker.put("rating", food.get("rating"));
            // 模拟坐标
            marker.put("latitude", 30.0 + Math.random() * 0.1);
            marker.put("longitude", 120.0 + Math.random() * 0.1);
            mapData.add(marker);
        }

        return mapData;
    }

    @Override
    public List<Map<String, Object>> recommendFoodPairing(Integer attractionId) {
        List<Map<String, Object>> pairings = new ArrayList<>();

        Attraction attraction = attractionService.getById(attractionId);
        if (attraction == null) {
            return pairings;
        }

        // 根据景点类型推荐搭配美食
        String description = attraction.getDescription();
        if (description != null) {
            if (description.contains("公园") || description.contains("湖")) {
                Map<String, Object> pairing = new HashMap<>();
                pairing.put("type", "休闲小吃");
                pairing.put("recommendation", "建议携带轻便零食，如当地特色糕点");
                pairing.put("suitableFor", "公园漫步");
                pairings.add(pairing);
            } else if (description.contains("博物馆") || description.contains("古迹")) {
                Map<String, Object> pairing = new HashMap<>();
                pairing.put("type", "文化体验");
                pairing.put("recommendation", "参观后品尝传统茶馆，体验当地文化");
                pairing.put("suitableFor", "文化游览");
                pairings.add(pairing);
            }
        }

        // 默认推荐
        if (pairings.isEmpty()) {
            Map<String, Object> pairing = new HashMap<>();
            pairing.put("type", "通用推荐");
            pairing.put("recommendation", "附近有多家高评分餐厅可供选择");
            pairing.put("suitableFor", "一般游览");
            pairings.add(pairing);
        }

        return pairings;
    }

    @Override
    public Map<String, Object> analyzeFoodPreference(Integer userId) {
        Map<String, Object> analysis = new HashMap<>();

        // 模拟用户美食偏好分析
        analysis.put("userId", userId);
        analysis.put("preferredType", "local");
        analysis.put("spicyLevel", "medium");
        analysis.put("pricePreference", "mid-range");
        analysis.put("diningStyle", "casual");
        analysis.put("favoriteCuisines", Arrays.asList("本地菜", "小吃", "火锅"));
        analysis.put("allergies", Collections.emptyList());
        analysis.put("dietaryRestrictions", Collections.emptyList());

        return analysis;
    }

    // 辅助方法
    private List<Map<String, Object>> generateMockFoodData(String foodType, BigDecimal budget) {
        List<Map<String, Object>> foods = new ArrayList<>();

        if ("snack".equals(foodType)) {
            Map<String, Object> food1 = new HashMap<>();
            food1.put("name", "特色煎饼");
            food1.put("price", new BigDecimal("8"));
            food1.put("rating", 4.3);
            foods.add(food1);

            Map<String, Object> food2 = new HashMap<>();
            food2.put("name", "传统豆花");
            food2.put("price", new BigDecimal("6"));
            food2.put("rating", 4.5);
            foods.add(food2);
        } else {
            Map<String, Object> food1 = new HashMap<>();
            food1.put("name", "家常菜馆");
            food1.put("price", new BigDecimal("60"));
            food1.put("rating", 4.4);
            foods.add(food1);

            Map<String, Object> food2 = new HashMap<>();
            food2.put("name", "精品餐厅");
            food2.put("price", new BigDecimal("120"));
            food2.put("rating", 4.7);
            foods.add(food2);
        }

        // 根据预算筛选
        if (budget != null) {
            foods.removeIf(food -> ((BigDecimal) food.get("price")).compareTo(budget) > 0);
        }

        return foods;
    }

    private Map<String, Object> getRandomFoodRecommendation(String mealTime, List<String> preferences) {
        Map<String, Object> recommendation = new HashMap<>();

        switch (mealTime) {
            case "breakfast":
                recommendation.put("type", "早餐");
                recommendation.put("suggestion", "当地特色早点");
                recommendation.put("estimatedPrice", new BigDecimal("15"));
                break;
            case "lunch":
                recommendation.put("type", "午餐");
                recommendation.put("suggestion", "老字号餐厅");
                recommendation.put("estimatedPrice", new BigDecimal("60"));
                break;
            case "dinner":
                recommendation.put("type", "晚餐");
                recommendation.put("suggestion", "特色餐厅");
                recommendation.put("estimatedPrice", new BigDecimal("80"));
                break;
            case "snack":
                recommendation.put("type", "小吃");
                recommendation.put("suggestion", "街边特色小吃");
                recommendation.put("estimatedPrice", new BigDecimal("20"));
                break;
        }

        return recommendation;
    }

    private BigDecimal calculateFoodCost(Integer days) {
        // 每天三餐加小吃
        BigDecimal dailyCost = new BigDecimal("150"); // 早餐15 + 午餐60 + 晚餐80 + 小吃20
        return dailyCost.multiply(new BigDecimal(days));
    }
}
