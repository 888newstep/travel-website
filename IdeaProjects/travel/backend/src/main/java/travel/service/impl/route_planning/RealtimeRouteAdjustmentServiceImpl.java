package travel.service.impl.route_planning;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.route_planning.Route;
import travel.entity.route_planning.RouteAttraction;
import travel.entity.travel_recommendation.Attraction;
import travel.service.route_planning.RealtimeRouteAdjustmentService;
import travel.service.route_planning.RouteAttractionService;
import travel.service.route_planning.RouteService;
import travel.service.travel_recommendation.AttractionService;
import travel.service.user_community.NotificationService;
import travel.utils.CacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeRouteAdjustmentServiceImpl implements RealtimeRouteAdjustmentService {

    @Autowired
    private RouteService routeService;

    @Autowired
    private RouteAttractionService routeAttractionService;

    @Autowired
    private AttractionService attractionService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CacheUtil cacheUtil;

    private static final String ROUTE_STATUS_PREFIX = "route:status:";
    private static final String CROWD_STATUS_PREFIX = "crowd:status:";
    private static final String WEATHER_IMPACT_PREFIX = "weather:impact:";

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> monitorRouteStatus(Integer routeId) {
        String cacheKey = ROUTE_STATUS_PREFIX + routeId;

        Map<String, Object> cachedStatus = cacheUtil.get(cacheKey, Map.class);
        if (cachedStatus != null) {
            return cachedStatus;
        }

        Route route = routeService.getById(routeId);
        if (route == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> status = new HashMap<>();
        status.put("routeId", routeId);
        status.put("routeName", route.getTitle());
        status.put("overallStatus", getOverallRouteStatus(routeId));
        status.put("attractionStatuses", getAttractionStatuses(routeId));
        status.put("trafficStatus", getTrafficStatus(routeId));
        status.put("weatherStatus", getWeatherStatus(routeId));
        status.put("lastUpdate", new Date());

        cacheUtil.set(cacheKey, status, 5, java.util.concurrent.TimeUnit.MINUTES);

        return status;
    }

    @Override
    public Map<String, Object> adjustRouteByWeather(Integer routeId, Map<String, Object> weatherCondition) {
        Map<String, Object> adjustment = new HashMap<>();

        String weatherType = (String) weatherCondition.get("type");
        Integer severity = (Integer) weatherCondition.getOrDefault("severity", 1);

        adjustment.put("routeId", routeId);
        adjustment.put("weatherType", weatherType);
        adjustment.put("severity", severity);
        adjustment.put("adjustments", generateWeatherAdjustments(routeId, weatherType, severity));
        adjustment.put("recommendations", generateWeatherRecommendations(weatherType, severity));
        adjustment.put("affectedAttractions", getWeatherAffectedAttractions(routeId, weatherType));

        return adjustment;
    }

    @Override
    public Map<String, Object> adjustRouteByCrowd(Integer routeId, Map<String, Object> crowdData) {
        Map<String, Object> adjustment = new HashMap<>();

        adjustment.put("routeId", routeId);
        adjustment.put("crowdLevel", crowdData.get("level"));
        adjustment.put("adjustments", generateCrowdAdjustments(routeId, crowdData));
        adjustment.put("alternativeTimeSlots", suggestAlternativeTimeSlots(crowdData));
        adjustment.put("estimatedWaitTime", crowdData.getOrDefault("waitTime", 0));

        return adjustment;
    }

    @Override
    public Map<String, Object> adjustRouteByTraffic(Integer routeId, Map<String, Object> trafficData) {
        Map<String, Object> adjustment = new HashMap<>();

        String trafficStatus = (String) trafficData.get("status");
        Integer delayMinutes = (Integer) trafficData.getOrDefault("delay", 0);

        adjustment.put("routeId", routeId);
        adjustment.put("trafficStatus", trafficStatus);
        adjustment.put("delayMinutes", delayMinutes);
        adjustment.put("alternativeRoutes", generateTrafficAlternativeRoutes(routeId, trafficData));
        adjustment.put("departureTimeAdjustment", calculateDepartureAdjustment(delayMinutes));

        return adjustment;
    }

    @Override
    public Map<String, Object> intelligentReroute(Integer routeId, Map<String, Object> realTimeFactors) {
        Map<String, Object> reroute = new HashMap<>();

        Route route = routeService.getById(routeId);
        if (route == null) {
            return Collections.emptyMap();
        }

        reroute.put("originalRouteId", routeId);
        reroute.put("reason", realTimeFactors.get("reason"));
        reroute.put("newRoute", generateNewRoute(route, realTimeFactors));
        reroute.put("changes", analyzeRouteChanges(routeId, reroute));
        reroute.put("estimatedImprovement", calculateImprovement(route, reroute));

        return reroute;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAttractionCrowdStatus(Integer attractionId) {
        String cacheKey = CROWD_STATUS_PREFIX + attractionId;

        Map<String, Object> cachedStatus = cacheUtil.get(cacheKey, Map.class);
        if (cachedStatus != null) {
            return cachedStatus;
        }

        Attraction attraction = attractionService.getById(attractionId);
        if (attraction == null) {
            return Collections.emptyMap();
        }

        // 模拟人流数据
        Map<String, Object> status = new HashMap<>();
        status.put("attractionId", attractionId);
        status.put("attractionName", attraction.getName());
        status.put("crowdLevel", new Random().nextInt(5) + 1); // 1-5级
        status.put("currentVisitors", 100 + new Random().nextInt(900));
        status.put("capacity", 1000);
        status.put("waitTime", new Random().nextInt(60)); // 分钟
        status.put("bestVisitTime", suggestBestVisitTime());
        status.put("lastUpdate", new Date());

        cacheUtil.set(cacheKey, status, 10, java.util.concurrent.TimeUnit.MINUTES);

        return status;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> assessWeatherImpact(Integer attractionId, String weatherType) {
        String cacheKey = WEATHER_IMPACT_PREFIX + attractionId + ":" + weatherType;

        Map<String, Object> cachedImpact = cacheUtil.get(cacheKey, Map.class);
        if (cachedImpact != null) {
            return cachedImpact;
        }

        Attraction attraction = attractionService.getById(attractionId);
        if (attraction == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> impact = new HashMap<>();
        impact.put("attractionId", attractionId);
        impact.put("weatherType", weatherType);
        impact.put("impactLevel", calculateWeatherImpactLevel(attraction, weatherType));
        impact.put("recommendation", generateWeatherRecommendation(attraction, weatherType));
        impact.put("alternativeAttractions", suggestWeatherAlternatives(attractionId, weatherType));

        cacheUtil.set(cacheKey, impact, 30, java.util.concurrent.TimeUnit.MINUTES);

        return impact;
    }

    @Override
    public List<Map<String, Object>> generateAlternativeRoutes(Integer routeId, String reason) {
        List<Map<String, Object>> alternatives = new ArrayList<>();

        // 生成3个备选方案
        alternatives.add(createAlternativeRoute(routeId, "time", "时间优化方案", "缩短总体游览时间"));
        alternatives.add(createAlternativeRoute(routeId, "crowd", "人流避开方案", "避开人流高峰景点"));
        alternatives.add(createAlternativeRoute(routeId, "weather", "天气适应方案", "根据天气调整室内/室外景点"));

        return alternatives;
    }

    @Override
    public boolean pushRealtimeAlert(Integer routeId, String alertType, String message) {
        try {
            Route route = routeService.getById(routeId);
            if (route == null || route.getUser() == null) {
                return false;
            }

            // 发送通知给路线创建者
            notificationService.sendSystemNotification(
                    route.getUser().getId(),
                    "ROUTE_ALERT",
                    "路线实时预警",
                    message,
                    "/route/" + routeId
            );

            log.info("推送实时预警: routeId={}, type={}, message={}", routeId, alertType, message);
            return true;
        } catch (Exception e) {
            log.error("推送实时预警失败: routeId={}, error={}", routeId, e.getMessage());
            return false;
        }
    }

    @Override
    public Map<String, Object> getRealtimeRoadCondition(Integer routeId) {
        Map<String, Object> condition = new HashMap<>();

        List<RouteAttraction> attractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId);

        List<Map<String, Object>> segmentConditions = new ArrayList<>();
        for (int i = 0; i < attractions.size() - 1; i++) {
            RouteAttraction from = attractions.get(i);
            RouteAttraction to = attractions.get(i + 1);

            Map<String, Object> segment = new HashMap<>();
            segment.put("fromAttractionId", from.getAttractionId());
            segment.put("toAttractionId", to.getAttractionId());
            segment.put("status", getRandomRoadStatus());
            segment.put("congestionLevel", new Random().nextInt(100));
            segment.put("estimatedTime", 15 + new Random().nextInt(45));

            segmentConditions.add(segment);
        }

        condition.put("routeId", routeId);
        condition.put("overallStatus", calculateOverallRoadStatus(segmentConditions));
        condition.put("segmentConditions", segmentConditions);

        return condition;
    }

    @Override
    public Integer adjustVisitDuration(Integer routeId, Integer attractionId, Integer crowdLevel) {
        // 基础游览时间
        int baseDuration = 120; // 2小时

        // 根据人流等级调整
        int adjustment = 0;
        switch (crowdLevel) {
            case 1: // 很少
                adjustment = -30;
                break;
            case 2: // 较少
                adjustment = -15;
                break;
            case 3: // 适中
                adjustment = 0;
                break;
            case 4: // 较多
                adjustment = 30;
                break;
            case 5: // 拥挤
                adjustment = 60;
                break;
        }

        return Math.max(30, baseDuration + adjustment); // 最少30分钟
    }

    // 辅助方法
    private String getOverallRouteStatus(Integer routeId) {
        int random = new Random().nextInt(100);
        if (random < 60) return "normal";
        if (random < 85) return "warning";
        return "critical";
    }

    private List<Map<String, Object>> getAttractionStatuses(Integer routeId) {
        List<Map<String, Object>> statuses = new ArrayList<>();
        List<RouteAttraction> attractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId);

        for (RouteAttraction ra : attractions) {
            Attraction attraction = attractionService.getById(ra.getAttractionId());
            if (attraction != null) {
                Map<String, Object> status = new HashMap<>();
                status.put("attractionId", ra.getAttractionId());
                status.put("attractionName", attraction.getName());
                status.put("crowdLevel", new Random().nextInt(5) + 1);
                status.put("isOpen", true);
                status.put("waitTime", new Random().nextInt(60));
                statuses.add(status);
            }
        }

        return statuses;
    }

    private Map<String, Object> getTrafficStatus(Integer routeId) {
        Map<String, Object> status = new HashMap<>();
        status.put("overall", getRandomRoadStatus());
        status.put("congestionIndex", new Random().nextInt(100));
        status.put("averageSpeed", 20 + new Random().nextInt(40));
        return status;
    }

    private Map<String, Object> getWeatherStatus(Integer routeId) {
        Map<String, Object> status = new HashMap<>();
        String[] weathers = {"sunny", "cloudy", "rainy", "snowy"};
        status.put("type", weathers[new Random().nextInt(weathers.length)]);
        status.put("temperature", 15 + new Random().nextInt(20));
        status.put("impact", new Random().nextInt(3)); // 0-2
        return status;
    }

    private List<Map<String, Object>> generateWeatherAdjustments(Integer routeId, String weatherType, Integer severity) {
        List<Map<String, Object>> adjustments = new ArrayList<>();

        if ("rainy".equals(weatherType) || "snowy".equals(weatherType)) {
            Map<String, Object> adjustment = new HashMap<>();
            adjustment.put("type", "indoor_priority");
            adjustment.put("description", "优先安排室内景点");
            adjustment.put("affectedAttractions", getOutdoorAttractions(routeId));
            adjustments.add(adjustment);
        }

        if (severity >= 3) {
            Map<String, Object> adjustment = new HashMap<>();
            adjustment.put("type", "time_extension");
            adjustment.put("description", "延长游览时间，减少景点数量");
            adjustment.put("extraTime", 30); // 分钟
            adjustments.add(adjustment);
        }

        return adjustments;
    }

    private List<String> generateWeatherRecommendations(String weatherType, Integer severity) {
        List<String> recommendations = new ArrayList<>();

        switch (weatherType) {
            case "rainy":
                recommendations.add("携带雨具，穿着防滑鞋");
                recommendations.add("优先参观室内景点");
                recommendations.add("预留更多交通时间");
                break;
            case "sunny":
                recommendations.add("注意防晒，携带遮阳帽");
                recommendations.add("多准备饮用水");
                break;
            case "snowy":
                recommendations.add("注意保暖，穿防滑鞋");
                recommendations.add("部分景点可能关闭，请提前确认");
                break;
            default:
                recommendations.add("天气良好，适合出游");
        }

        if (severity >= 3) {
            recommendations.add("天气条件较差，建议调整行程");
        }

        return recommendations;
    }

    private List<Integer> getWeatherAffectedAttractions(Integer routeId, String weatherType) {
        // 返回受天气影响的景点ID列表
        return new ArrayList<>();
    }

    private List<Integer> getOutdoorAttractions(Integer routeId) {
        List<Integer> outdoorAttractions = new ArrayList<>();
        List<RouteAttraction> attractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId);

        for (RouteAttraction ra : attractions) {
            Attraction attraction = attractionService.getById(ra.getAttractionId());
            if (attraction != null && attraction.getDescription() != null) {
                String desc = attraction.getDescription();
                if (desc.contains("公园") || desc.contains("山") || desc.contains("户外")) {
                    outdoorAttractions.add(ra.getAttractionId());
                }
            }
        }

        return outdoorAttractions;
    }

    private List<Map<String, Object>> generateCrowdAdjustments(Integer routeId, Map<String, Object> crowdData) {
        List<Map<String, Object>> adjustments = new ArrayList<>();
        Integer crowdLevel = (Integer) crowdData.get("level");

        if (crowdLevel >= 4) {
            Map<String, Object> adjustment = new HashMap<>();
            adjustment.put("type", "reorder");
            adjustment.put("description", "调整游览顺序，避开高峰");
            adjustments.add(adjustment);
        }

        return adjustments;
    }

    private List<Map<String, Object>> suggestAlternativeTimeSlots(Map<String, Object> crowdData) {
        List<Map<String, Object>> slots = new ArrayList<>();

        slots.add(createTimeSlot("08:00-10:00", "早上", "人流较少"));
        slots.add(createTimeSlot("14:00-16:00", "下午", "人流适中"));
        slots.add(createTimeSlot("17:00-19:00", "傍晚", "人流较少"));

        return slots;
    }

    private Map<String, Object> createTimeSlot(String time, String period, String crowdStatus) {
        Map<String, Object> slot = new HashMap<>();
        slot.put("time", time);
        slot.put("period", period);
        slot.put("crowdStatus", crowdStatus);
        return slot;
    }

    private List<Map<String, Object>> generateTrafficAlternativeRoutes(Integer routeId, Map<String, Object> trafficData) {
        List<Map<String, Object>> alternatives = new ArrayList<>();

        Map<String, Object> alt1 = new HashMap<>();
        alt1.put("type", "public_transport");
        alt1.put("description", "改乘地铁/公交，避开拥堵");
        alt1.put("timeSaving", 15);
        alternatives.add(alt1);

        Map<String, Object> alt2 = new HashMap<>();
        alt2.put("type", "reroute");
        alt2.put("description", "更换行驶路线");
        alt2.put("timeSaving", 10);
        alternatives.add(alt2);

        return alternatives;
    }

    private String calculateDepartureAdjustment(Integer delayMinutes) {
        if (delayMinutes <= 15) {
            return "建议按时出发，预留充足时间";
        } else if (delayMinutes <= 30) {
            return "建议提前" + (delayMinutes / 10 * 10) + "分钟出发";
        } else {
            return "建议提前" + ((delayMinutes / 15 + 1) * 15) + "分钟出发，或调整行程";
        }
    }

    private Map<String, Object> generateNewRoute(Route originalRoute, Map<String, Object> realTimeFactors) {
        Map<String, Object> newRoute = new HashMap<>();
        newRoute.put("title", originalRoute.getTitle() + " (优化版)");
        newRoute.put("cityId", originalRoute.getCity() != null ? originalRoute.getCity().getId() : null);
        newRoute.put("durationDays", originalRoute.getDurationDays());
        newRoute.put("reason", realTimeFactors.get("reason"));
        return newRoute;
    }

    private List<Map<String, Object>> analyzeRouteChanges(Integer originalRouteId, Map<String, Object> newRoute) {
        List<Map<String, Object>> changes = new ArrayList<>();
        // 分析路线变化
        return changes;
    }

    private String calculateImprovement(Route originalRoute, Map<String, Object> newRoute) {
        return "预计节省30分钟，避开3个拥挤景点";
    }

    private String suggestBestVisitTime() {
        String[] times = {"08:00-10:00", "14:00-16:00", "17:00-19:00"};
        return times[new Random().nextInt(times.length)];
    }

    private Integer calculateWeatherImpactLevel(Attraction attraction, String weatherType) {
        // 根据景点类型和天气计算影响等级
        String desc = attraction.getDescription();
        if (desc == null) return 1;

        if (("rainy".equals(weatherType) || "snowy".equals(weatherType)) &&
            (desc.contains("户外") || desc.contains("山") || desc.contains("公园"))) {
            return 3; // 高影响
        }

        return 1; // 低影响
    }

    private String generateWeatherRecommendation(Attraction attraction, String weatherType) {
        int impact = calculateWeatherImpactLevel(attraction, weatherType);

        if (impact >= 3) {
            return "天气条件较差，建议更换景点或调整时间";
        } else if (impact >= 2) {
            return "天气一般，可以参观但需做好准备";
        }
        return "天气适宜，适合参观";
    }

    private List<Integer> suggestWeatherAlternatives(Integer attractionId, String weatherType) {
        // 返回替代景点建议
        return new ArrayList<>();
    }

    private Map<String, Object> createAlternativeRoute(Integer routeId, String type, String name, String description) {
        Map<String, Object> alternative = new HashMap<>();
        alternative.put("type", type);
        alternative.put("name", name);
        alternative.put("description", description);
        alternative.put("estimatedTime", 120 + new Random().nextInt(60));
        alternative.put("estimatedCost", 200 + new Random().nextInt(300));
        return alternative;
    }

    private String getRandomRoadStatus() {
        String[] statuses = {"smooth", "moderate", "congested", "blocked"};
        int[] weights = {40, 35, 20, 5};

        int total = 0;
        for (int weight : weights) {
            total += weight;
        }

        int random = new Random().nextInt(total);
        int current = 0;
        for (int i = 0; i < statuses.length; i++) {
            current += weights[i];
            if (random < current) {
                return statuses[i];
            }
        }

        return statuses[0];
    }

    private String calculateOverallRoadStatus(List<Map<String, Object>> segmentConditions) {
        long congestedCount = segmentConditions.stream()
                .filter(s -> "congested".equals(s.get("status")) || "blocked".equals(s.get("status")))
                .count();

        if (congestedCount == 0) return "smooth";
        if (congestedCount <= segmentConditions.size() / 3) return "moderate";
        return "congested";
    }
}
