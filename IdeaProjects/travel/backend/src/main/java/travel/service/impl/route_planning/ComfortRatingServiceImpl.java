package travel.service.impl.route_planning;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.route_planning.Route;
import travel.entity.route_planning.RouteAttraction;
import travel.entity.travel_recommendation.Attraction;
import travel.service.route_planning.ComfortRatingService;
import travel.service.route_planning.RouteAttractionService;
import travel.service.route_planning.RouteService;
import travel.service.travel_recommendation.AttractionService;
import travel.utils.CacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComfortRatingServiceImpl implements ComfortRatingService {

    @Autowired
    private RouteService routeService;

    @Autowired
    private RouteAttractionService routeAttractionService;

    @Autowired
    private AttractionService attractionService;

    @Autowired
    private CacheUtil cacheUtil;

    private static final String COMFORT_PREFIX = "comfort:";

    // 舒适度维度权重 - 使用ConcurrentHashMap保证线程安全
    private static final Map<String, Double> DEFAULT_WEIGHTS = new ConcurrentHashMap<>();

    static {
        DEFAULT_WEIGHTS.put("transport", 0.25);
        DEFAULT_WEIGHTS.put("crowd", 0.20);
        DEFAULT_WEIGHTS.put("pace", 0.20);
        DEFAULT_WEIGHTS.put("accommodation", 0.15);
        DEFAULT_WEIGHTS.put("dining", 0.10);
        DEFAULT_WEIGHTS.put("luggage", 0.05);
        DEFAULT_WEIGHTS.put("accessibility", 0.05);
    }

    @Override
    public Integer calculateComfortScore(Integer routeId) {
        String cacheKey = COMFORT_PREFIX + "score:" + routeId;

        Integer cachedScore = cacheUtil.get(cacheKey, Integer.class);
        if (cachedScore != null) {
            return cachedScore;
        }

        // 计算各维度评分
        int transportScore = evaluateTransportComfort(routeId);
        int crowdScore = evaluateCrowdLevel(routeId);
        int paceScore = evaluatePaceComfort(routeId);
        int accommodationScore = evaluateAccommodationComfort(routeId);
        int diningScore = evaluateDiningQuality(routeId);
        int luggageScore = getLuggageFriendliness(routeId);
        int accessibilityScore = getAccessibilityScore(routeId);

        // 加权计算总分
        double totalScore = transportScore * DEFAULT_WEIGHTS.get("transport")
                + crowdScore * DEFAULT_WEIGHTS.get("crowd")
                + paceScore * DEFAULT_WEIGHTS.get("pace")
                + accommodationScore * DEFAULT_WEIGHTS.get("accommodation")
                + diningScore * DEFAULT_WEIGHTS.get("dining")
                + luggageScore * DEFAULT_WEIGHTS.get("luggage")
                + accessibilityScore * DEFAULT_WEIGHTS.get("accessibility");

        int finalScore = (int) Math.round(totalScore);

        cacheUtil.set(cacheKey, finalScore, 60, java.util.concurrent.TimeUnit.MINUTES);

        return finalScore;
    }

    @Override
    public Map<String, Object> getComfortDetails(Integer routeId) {
        Map<String, Object> details = new HashMap<>();

        details.put("routeId", routeId);
        details.put("totalScore", calculateComfortScore(routeId));
        details.put("transport", evaluateTransportComfort(routeId));
        details.put("crowd", evaluateCrowdLevel(routeId));
        details.put("pace", evaluatePaceComfort(routeId));
        details.put("accommodation", evaluateAccommodationComfort(routeId));
        details.put("dining", evaluateDiningQuality(routeId));
        details.put("luggage", getLuggageFriendliness(routeId));
        details.put("accessibility", getAccessibilityScore(routeId));
        details.put("weights", DEFAULT_WEIGHTS);

        return details;
    }

    @Override
    public Integer evaluateTransportComfort(Integer routeId) {
        Route route = routeService.getById(routeId);
        if (route == null) {
            return 50;
        }

        int score = 70; // 基础分

        // 根据路线难度调整
        String difficulty = route.getDifficulty();
        if ("简单".equals(difficulty)) {
            score += 20;
        } else if ("中等".equals(difficulty)) {
            score += 10;
        } else {
            score -= 10;
        }

        // 根据天数调整（天数越多，交通安排越复杂）
        Integer days = route.getDurationDays();
        if (days <= 2) {
            score += 10;
        } else if (days >= 5) {
            score -= 5;
        }

        return Math.max(0, Math.min(100, score));
    }

    @Override
    public Integer evaluateCrowdLevel(Integer routeId) {
        List<RouteAttraction> attractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId);

        if (attractions.isEmpty()) {
            return 50;
        }

        int totalScore = 0;
        for (RouteAttraction ra : attractions) {
            Attraction attraction = attractionService.getById(ra.getAttractionId());
            if (attraction != null) {
                // 根据景点评分和浏览量估算拥挤度
                Integer viewCount = attraction.getViewCount() != null ? attraction.getViewCount() : 0;

                if (viewCount > 100000) {
                    totalScore += 30; // 非常拥挤
                } else if (viewCount > 50000) {
                    totalScore += 50; // 较拥挤
                } else if (viewCount > 10000) {
                    totalScore += 70; // 适中
                } else {
                    totalScore += 90; // 不拥挤
                }
            }
        }

        return totalScore / attractions.size();
    }

    @Override
    public Integer evaluatePaceComfort(Integer routeId) {
        Route route = routeService.getById(routeId);
        if (route == null) {
            return 50;
        }

        List<RouteAttraction> attractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId);

        if (attractions.isEmpty()) {
            return 50;
        }

        int days = route.getDurationDays();
        int avgAttractionsPerDay = attractions.size() / days;

        // 每天2-4个景点最舒适
        if (avgAttractionsPerDay >= 2 && avgAttractionsPerDay <= 4) {
            return 85;
        } else if (avgAttractionsPerDay == 1 || avgAttractionsPerDay == 5) {
            return 70;
        } else if (avgAttractionsPerDay > 5) {
            return 50; // 太紧凑
        } else {
            return 60; // 太松散
        }
    }

    @Override
    public Integer evaluateAccommodationComfort(Integer routeId) {
        Route route = routeService.getById(routeId);
        if (route == null) {
            return 50;
        }

        // 根据路线难度和天数评估住宿安排
        String difficulty = route.getDifficulty();
        Integer days = route.getDurationDays();

        int score = 70;

        if ("简单".equals(difficulty)) {
            score += 15;
        }

        if (days <= 3) {
            score += 10; // 短途旅行住宿安排更简单
        }

        return Math.min(100, score);
    }

    @Override
    public Integer evaluateDiningQuality(Integer routeId) {
        List<RouteAttraction> attractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId);

        int score = 70; // 基础分

        // 检查是否有美食相关景点
        for (RouteAttraction ra : attractions) {
            Attraction attraction = attractionService.getById(ra.getAttractionId());
            if (attraction != null && attraction.getDescription() != null) {
                String desc = attraction.getDescription();
                if (desc.contains("美食") || desc.contains("餐厅") || desc.contains("小吃")) {
                    score += 10;
                    break;
                }
            }
        }

        return Math.min(100, score);
    }

    @Override
    public Integer getLuggageFriendliness(Integer routeId) {
        Route route = routeService.getById(routeId);
        if (route == null) {
            return 50;
        }

        int score = 70;

        // 根据交通方式和难度评估
        String difficulty = route.getDifficulty();
        if ("简单".equals(difficulty)) {
            score += 20; // 简单路线通常交通更便利，适合携带行李
        } else if ("困难".equals(difficulty)) {
            score -= 20; // 困难路线可能涉及徒步等，不适合行李
        }

        // 天数越多，行李友好度越低
        Integer days = route.getDurationDays();
        if (days > 5) {
            score -= 10;
        }

        return Math.max(0, Math.min(100, score));
    }

    @Override
    public Integer getAccessibilityScore(Integer routeId) {
        List<RouteAttraction> attractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId);

        if (attractions.isEmpty()) {
            return 50;
        }

        int totalScore = 0;
        int count = 0;

        for (RouteAttraction ra : attractions) {
            Attraction attraction = attractionService.getById(ra.getAttractionId());
            if (attraction != null) {
                int score = 60; // 基础分

                // 根据景点描述判断无障碍设施
                String desc = attraction.getDescription();
                if (desc != null) {
                    if (desc.contains("博物馆") || desc.contains("公园")) {
                        score += 20; // 通常有无障碍设施
                    }
                    if (desc.contains("山") || desc.contains("登山")) {
                        score -= 20; // 山地通常无障碍较差
                    }
                }

                totalScore += score;
                count++;
            }
        }

        return count > 0 ? totalScore / count : 50;
    }

    @Override
    public List<Map<String, Object>> compareComfort(List<Integer> routeIds) {
        List<Map<String, Object>> comparisons = new ArrayList<>();

        for (Integer routeId : routeIds) {
            Route route = routeService.getById(routeId);
            if (route != null) {
                Map<String, Object> comparison = new HashMap<>();
                comparison.put("routeId", routeId);
                comparison.put("routeName", route.getTitle());
                comparison.put("comfortScore", calculateComfortScore(routeId));
                comparison.put("details", getComfortDetails(routeId));
                comparisons.add(comparison);
            }
        }

        // 按舒适度排序
        comparisons.sort((a, b) ->
                ((Integer) b.get("comfortScore")).compareTo((Integer) a.get("comfortScore")));

        return comparisons;
    }

    @Override
    public List<Map<String, Object>> getComfortOptimizationSuggestions(Integer routeId) {
        List<Map<String, Object>> suggestions = new ArrayList<>();

        Map<String, Object> details = getComfortDetails(routeId);

        // 交通舒适度优化
        Integer transportScore = (Integer) details.get("transport");
        if (transportScore < 70) {
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("dimension", "交通");
            suggestion.put("currentScore", transportScore);
            suggestion.put("suggestion", "建议增加地铁/公交等便利交通方式，减少换乘次数");
            suggestion.put("priority", "high");
            suggestions.add(suggestion);
        }

        // 拥挤度优化
        Integer crowdScore = (Integer) details.get("crowd");
        if (crowdScore < 60) {
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("dimension", "拥挤度");
            suggestion.put("currentScore", crowdScore);
            suggestion.put("suggestion", "建议调整游览时间避开高峰，或替换为相似但人流较少的景点");
            suggestion.put("priority", "medium");
            suggestions.add(suggestion);
        }

        // 行程节奏优化
        Integer paceScore = (Integer) details.get("pace");
        if (paceScore < 70) {
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("dimension", "行程节奏");
            suggestion.put("currentScore", paceScore);
            suggestion.put("suggestion", paceScore < 60 ? "每天景点过多，建议减少或延长行程天数" : "景点安排较松散，可以适当增加体验项目");
            suggestion.put("priority", "high");
            suggestions.add(suggestion);
        }

        // 行李友好度优化
        Integer luggageScore = (Integer) details.get("luggage");
        if (luggageScore < 60) {
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("dimension", "行李携带");
            suggestion.put("currentScore", luggageScore);
            suggestion.put("suggestion", "路线涉及较多步行或换乘，建议精简行李或选择行李寄存服务");
            suggestion.put("priority", "low");
            suggestions.add(suggestion);
        }

        return suggestions;
    }

    @Override
    public List<Map<String, Object>> filterByComfort(Integer cityId, Integer minScore) {
        List<Route> routes = routeService.getByCityId(cityId);
        List<Map<String, Object>> filteredRoutes = new ArrayList<>();

        for (Route route : routes) {
            Integer comfortScore = calculateComfortScore(route.getId());
            if (comfortScore >= minScore) {
                Map<String, Object> routeInfo = new HashMap<>();
                routeInfo.put("route", route);
                routeInfo.put("comfortScore", comfortScore);
                filteredRoutes.add(routeInfo);
            }
        }

        // 按舒适度排序
        filteredRoutes.sort((a, b) ->
                ((Integer) b.get("comfortScore")).compareTo((Integer) a.get("comfortScore")));

        return filteredRoutes;
    }

    @Override
    public Map<String, Double> getComfortDimensionWeights() {
        return new HashMap<>(DEFAULT_WEIGHTS);
    }

    @Override
    public void updateComfortDimensionWeights(Map<String, Double> weights) {
        // 实际实现中应该保存到数据库或配置中心
        // 这里仅作演示
        log.info("更新舒适度维度权重: {}", weights);
    }
}
