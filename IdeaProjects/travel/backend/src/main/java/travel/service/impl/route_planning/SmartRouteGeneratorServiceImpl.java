package travel.service.impl.route_planning;

import lombok.RequiredArgsConstructor;
import travel.entity.route_planning.Route;
import travel.entity.route_planning.RouteAttraction;
import travel.entity.travel_recommendation.Attraction;
import travel.service.route_planning.RouteAttractionService;
import travel.service.route_planning.RouteService;
import travel.service.route_planning.SmartRouteGeneratorService;
import travel.service.travel_recommendation.AttractionService;
import travel.utils.CacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SmartRouteGeneratorServiceImpl implements SmartRouteGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(SmartRouteGeneratorServiceImpl.class);

    @Autowired
    private RouteService routeService;

    @Autowired
    private RouteAttractionService routeAttractionService;

    @Autowired
    private AttractionService attractionService;

    @Autowired
    private CacheUtil cacheUtil;

    private static final String SMART_ROUTE_PREFIX = "smart:route:";
    private static final String BUDGET_PLAN_PREFIX = "budget:plan:";

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> generateSmartRoute(Integer cityId, Integer travelerCount, 
                                                  Integer days, BigDecimal budget, 
                                                  Map<String, Object> preferences) {
        String cacheKey = SMART_ROUTE_PREFIX + cityId + ":" + travelerCount + ":" + days + ":" + budget;
        
        Map<String, Object> cachedRoute = cacheUtil.get(cacheKey, Map.class);
        if (cachedRoute != null) {
            log.info("从缓存获取智能路线: cityId={}, days={}", cityId, days);
            return cachedRoute;
        }

        log.info("生成智能路线: cityId={}, travelerCount={}, days={}, budget={}", 
                cityId, travelerCount, days, budget);

        // 获取城市所有景点
        List<Attraction> allAttractions = attractionService.getByCityId(cityId);
        
        // 根据预算筛选景点
        List<Attraction> filteredAttractions = filterAttractionsByBudget(allAttractions, budget, days);
        
        // 根据偏好排序
        String preference = (String) preferences.getOrDefault("preference", "balanced");
        filteredAttractions = sortAttractionsByPreference(filteredAttractions, preference);
        
        // 智能分配每日行程
        List<List<Attraction>> dailySchedule = distributeDailySchedule(filteredAttractions, days, 8.0);
        
        // 计算费用明细
        Map<String, BigDecimal> costBreakdown = calculateCostBreakdown(dailySchedule, travelerCount);
        
        // 生成路线建议
        Map<String, Object> result = new HashMap<>();
        result.put("cityId", cityId);
        result.put("travelerCount", travelerCount);
        result.put("days", days);
        result.put("budget", budget);
        result.put("dailySchedule", dailySchedule);
        result.put("costBreakdown", costBreakdown);
        result.put("totalCost", costBreakdown.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        result.put("recommendations", generateRecommendations(dailySchedule, preferences));
        
        // 缓存结果
        cacheUtil.set(cacheKey, result, 30, java.util.concurrent.TimeUnit.MINUTES);
        
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> recommendBudgetPlans(Integer cityId, Integer days, 
                                                          BigDecimal budget, Integer travelerCount) {
        String cacheKey = BUDGET_PLAN_PREFIX + cityId + ":" + days + ":" + budget;
        
        List<Map<String, Object>> cachedPlans = cacheUtil.get(cacheKey, List.class);
        if (cachedPlans != null) {
            return cachedPlans;
        }

        List<Map<String, Object>> plans = new ArrayList<>();
        
        // 经济型方案 (60%预算)
        BigDecimal economyBudget = budget.multiply(new BigDecimal("0.6"));
        plans.add(createPlanOption("economy", "经济型", economyBudget, cityId, days, travelerCount));
        
        // 舒适型方案 (100%预算)
        plans.add(createPlanOption("standard", "舒适型", budget, cityId, days, travelerCount));
        
        // 豪华型方案 (150%预算)
        BigDecimal luxuryBudget = budget.multiply(new BigDecimal("1.5"));
        plans.add(createPlanOption("luxury", "豪华型", luxuryBudget, cityId, days, travelerCount));
        
        cacheUtil.set(cacheKey, plans, 30, java.util.concurrent.TimeUnit.MINUTES);
        
        return plans;
    }

    @Override
    public List<List<Attraction>> distributeDailySchedule(List<Attraction> attractions, 
                                                          Integer days, Double dailyTimeLimit) {
        List<List<Attraction>> schedule = new ArrayList<>();
        
        // 按地理位置聚类
        List<List<Attraction>> clusters = clusterAttractionsByLocation(attractions, days);
        
        // 为每天分配景点
        for (int i = 0; i < days; i++) {
            List<Attraction> dailyAttractions = new ArrayList<>();
            double dailyTime = 0.0;
            
            if (i < clusters.size()) {
                for (Attraction attraction : clusters.get(i)) {
                    double visitTime = estimateVisitTime(attraction);
                    if (dailyTime + visitTime <= dailyTimeLimit) {
                        dailyAttractions.add(attraction);
                        dailyTime += visitTime;
                    }
                }
            }
            
            schedule.add(dailyAttractions);
        }
        
        return schedule;
    }

    @Override
    public Map<String, BigDecimal> calculateRouteCost(Integer routeId, Integer travelerCount) {
        Route route = routeService.getById(routeId);
        if (route == null) {
            return Collections.emptyMap();
        }

        Map<String, BigDecimal> costBreakdown = new HashMap<>();
        
        // 获取路线的景点
        List<RouteAttraction> routeAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId);

        BigDecimal ticketCost = BigDecimal.ZERO;
        for (RouteAttraction ra : routeAttractions) {
            Attraction attraction = attractionService.getById(ra.getAttractionId());
            if (attraction != null && attraction.getTicketPrice() != null) {
                ticketCost = ticketCost.add(attraction.getTicketPrice());
            }
        }
        
        costBreakdown.put("tickets", ticketCost.multiply(new BigDecimal(travelerCount)));
        costBreakdown.put("transport", estimateTransportCost(routeAttractions.size(), travelerCount));
        costBreakdown.put("food", new BigDecimal("100").multiply(new BigDecimal(travelerCount * route.getDurationDays())));
        costBreakdown.put("accommodation", new BigDecimal("300").multiply(new BigDecimal(travelerCount * (route.getDurationDays() - 1))));
        
        return costBreakdown;
    }

    @Override
    public Route optimizeTimeAllocation(Integer routeId) {
        Route route = routeService.getById(routeId);
        if (route == null) {
            return null;
        }

        List<RouteAttraction> attractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId);

        // 按游览顺序重新排序
        attractions.sort(Comparator.comparing(RouteAttraction::getVisitOrder));
        
        // 优化每天的景点分配
        int attractionsPerDay = (int) Math.ceil((double) attractions.size() / route.getDurationDays());
        
        for (int i = 0; i < attractions.size(); i++) {
            RouteAttraction ra = attractions.get(i);
            ra.setDayNumber(i / attractionsPerDay + 1);
            ra.setVisitOrder(i % attractionsPerDay + 1);
            routeAttractionService.updateById(ra);
        }
        
        return route;
    }

    @Override
    public Map<String, Object> getSmartRecommendationParams(Integer cityId) {
        Map<String, Object> params = new HashMap<>();
        
        // 获取城市景点统计
        List<Attraction> attractions = attractionService.getByCityId(cityId);
        
        params.put("totalAttractions", attractions.size());
        params.put("avgTicketPrice", attractions.stream()
                .map(Attraction::getTicketPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(Math.max(1, attractions.size())), 2, RoundingMode.HALF_UP));
        params.put("recommendedDays", Math.min(7, Math.max(1, attractions.size() / 3)));
        params.put("difficultyLevels", Arrays.asList("简单", "中等", "困难"));
        params.put("travelStyles", Arrays.asList("休闲", "紧凑", "深度"));
        
        return params;
    }

    // 辅助方法
    private List<Attraction> filterAttractionsByBudget(List<Attraction> attractions, BigDecimal budget, Integer days) {
        BigDecimal dailyBudget = budget.divide(new BigDecimal(days), 2, RoundingMode.HALF_UP);
        BigDecimal maxTicketPrice = dailyBudget.multiply(new BigDecimal("0.3")); // 30%预算用于门票
        
        return attractions.stream()
                .filter(a -> a.getTicketPrice() == null || a.getTicketPrice().compareTo(maxTicketPrice) <= 0)
                .collect(Collectors.toList());
    }

    private List<Attraction> sortAttractionsByPreference(List<Attraction> attractions, String preference) {
        switch (preference) {
            case "rating":
                return attractions.stream()
                        .sorted(Comparator.comparing(Attraction::getRating, Comparator.nullsLast(Comparator.reverseOrder())))
                        .collect(Collectors.toList());
            case "popularity":
                return attractions.stream()
                        .sorted(Comparator.comparing(Attraction::getViewCount, Comparator.nullsLast(Comparator.reverseOrder())))
                        .collect(Collectors.toList());
            case "price":
                return attractions.stream()
                        .sorted(Comparator.comparing(Attraction::getTicketPrice, Comparator.nullsFirst(Comparator.naturalOrder())))
                        .collect(Collectors.toList());
            default:
                return attractions;
        }
    }

    private List<List<Attraction>> clusterAttractionsByLocation(List<Attraction> attractions, int clusterCount) {
        // 简化的地理位置聚类
        List<List<Attraction>> clusters = new ArrayList<>();
        int clusterSize = (int) Math.ceil((double) attractions.size() / clusterCount);
        
        for (int i = 0; i < clusterCount; i++) {
            int start = i * clusterSize;
            int end = Math.min(start + clusterSize, attractions.size());
            if (start < attractions.size()) {
                clusters.add(new ArrayList<>(attractions.subList(start, end)));
            }
        }
        
        return clusters;
    }

    private double estimateVisitTime(Attraction attraction) {
        // 根据景点类型估算游览时间
        String description = attraction.getDescription();
        if (description != null) {
            if (description.contains("博物馆") || description.contains("故宫")) {
                return 3.0;
            } else if (description.contains("公园") || description.contains("山")) {
                return 4.0;
            } else if (description.contains("塔") || description.contains("建筑")) {
                return 1.5;
            }
        }
        return 2.0; // 默认2小时
    }

    private Map<String, BigDecimal> calculateCostBreakdown(List<List<Attraction>> dailySchedule, Integer travelerCount) {
        Map<String, BigDecimal> costs = new HashMap<>();
        
        BigDecimal ticketTotal = BigDecimal.ZERO;
        int totalAttractions = 0;
        
        for (List<Attraction> daily : dailySchedule) {
            for (Attraction attraction : daily) {
                if (attraction.getTicketPrice() != null) {
                    ticketTotal = ticketTotal.add(attraction.getTicketPrice());
                }
                totalAttractions++;
            }
        }
        
        costs.put("tickets", ticketTotal.multiply(new BigDecimal(travelerCount)));
        costs.put("transport", estimateTransportCost(totalAttractions, travelerCount));
        costs.put("food", new BigDecimal("100").multiply(new BigDecimal(travelerCount * dailySchedule.size())));
        costs.put("accommodation", new BigDecimal("300").multiply(new BigDecimal(travelerCount * (dailySchedule.size() - 1))));
        
        return costs;
    }

    private BigDecimal estimateTransportCost(int attractionCount, Integer travelerCount) {
        // 估算交通费用
        BigDecimal baseCost = new BigDecimal("50"); // 基础交通费
        BigDecimal perAttractionCost = new BigDecimal("20"); // 每个景点交通费
        return baseCost.add(perAttractionCost.multiply(new BigDecimal(attractionCount)))
                .multiply(new BigDecimal(travelerCount));
    }

    private Map<String, Object> createPlanOption(String type, String name, BigDecimal budget, 
                                                 Integer cityId, Integer days, Integer travelerCount) {
        Map<String, Object> plan = new HashMap<>();
        plan.put("type", type);
        plan.put("name", name);
        plan.put("budget", budget);
        plan.put("description", generatePlanDescription(type, days, travelerCount));
        plan.put("included", getPlanIncludedItems(type));
        plan.put("accommodationLevel", getAccommodationLevel(type));
        plan.put("transportLevel", getTransportLevel(type));
        return plan;
    }

    private String generatePlanDescription(String type, Integer days, Integer travelerCount) {
        switch (type) {
            case "economy":
                return String.format("%d天经济型行程，适合%d人出行，精选免费及低价景点", days, travelerCount);
            case "luxury":
                return String.format("%d天豪华型行程，适合%d人出行，尊享VIP体验", days, travelerCount);
            default:
                return String.format("%d天舒适型行程，适合%d人出行，平衡体验与性价比", days, travelerCount);
        }
    }

    private List<String> getPlanIncludedItems(String type) {
        List<String> items = new ArrayList<>();
        items.add("景点门票");
        items.add("行程规划");
        
        switch (type) {
            case "economy":
                items.add("公共交通指南");
                items.add("经济型住宿推荐");
                break;
            case "luxury":
                items.add("专车接送");
                items.add("五星级酒店");
                items.add("私人导游");
                items.add("特色餐饮");
                break;
            default:
                items.add("交通方案");
                items.add("舒适型住宿推荐");
                items.add("餐饮建议");
        }
        
        return items;
    }

    private String getAccommodationLevel(String type) {
        switch (type) {
            case "economy":
                return "经济型酒店/青旅";
            case "luxury":
                return "五星级/豪华酒店";
            default:
                return "舒适型酒店";
        }
    }

    private String getTransportLevel(String type) {
        switch (type) {
            case "economy":
                return "公共交通为主";
            case "luxury":
                return "专车/包车服务";
            default:
                return "混合交通方案";
        }
    }

    private List<String> generateRecommendations(List<List<Attraction>> dailySchedule, Map<String, Object> preferences) {
        List<String> recommendations = new ArrayList<>();
        
        recommendations.add("建议早上8:00出发，避开人流高峰");
        recommendations.add("每个景点预留充足的游览时间");
        
        if (dailySchedule.size() > 3) {
            recommendations.add("行程较长，建议安排适当的休息时间");
        }
        
        String travelStyle = (String) preferences.getOrDefault("travelStyle", "balanced");
        if ("leisure".equals(travelStyle)) {
            recommendations.add("休闲模式：每天安排1-2个主要景点即可");
        } else if ("intensive".equals(travelStyle)) {
            recommendations.add("紧凑模式：充分利用时间，体验更多景点");
        }
        
        return recommendations;
    }
}
