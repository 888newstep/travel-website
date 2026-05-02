package travel.service.impl.route_planning;

import lombok.RequiredArgsConstructor;
import travel.entity.route_planning.Route;
import travel.entity.route_planning.RouteAttraction;
import travel.entity.travel_recommendation.Attraction;
import travel.service.route_planning.EmergencyRouteService;
import travel.service.route_planning.RouteAttractionService;
import travel.service.route_planning.RouteService;
import travel.service.travel_recommendation.AttractionService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Service
@RequiredArgsConstructor
public class EmergencyRouteServiceImpl implements EmergencyRouteService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyRouteServiceImpl.class);

    private final RouteService routeService;
    private final RouteAttractionService routeAttractionService;
    private final AttractionService attractionService;



    @Override
    public Map<String, Object> handleAttractionClosure(Integer routeId, Integer closedAttractionId) {
        Map<String, Object> result = new HashMap<>();

        Route route = routeService.getById(routeId);
        if (route == null) {
            result.put("success", false);
            result.put("message", "路线不存在");
            return result;
        }

        // 获取闭园景点信息
        Attraction closedAttraction = attractionService.getById(closedAttractionId);
        String closedName = closedAttraction != null ? closedAttraction.getName() : "未知景点";

        // 查找相似替代景点
        List<Map<String, Object>> alternatives = findAlternativeAttractions(closedAttractionId, route.getCity().getId());

        result.put("success", true);
        result.put("routeId", routeId);
        result.put("closedAttractionId", closedAttractionId);
        result.put("closedAttractionName", closedName);
        result.put("alternatives", alternatives);
        result.put("message", closedName + " 已闭园，为您推荐以下替代景点");

        // 推送通知
        pushEmergencyAlert(routeId, "ATTRACTION_CLOSURE",
                closedName + " 临时闭园，请查看替代方案");

        return result;
    }

    @Override
    public Map<String, Object> handleTrafficCongestion(Integer routeId, String congestedSegment) {
        Map<String, Object> result = new HashMap<>();

        // 解析拥堵路段
        String[] segments = congestedSegment.split("-");
        if (segments.length != 2) {
            result.put("success", false);
            result.put("message", "拥堵路段格式错误");
            return result;
        }

        Integer fromId = Integer.parseInt(segments[0]);
        Integer toId = Integer.parseInt(segments[1]);

        // 生成替代路线
        List<Map<String, Object>> alternativeRoutes = generateAlternativeRoutes(routeId, fromId, toId);

        result.put("success", true);
        result.put("routeId", routeId);
        result.put("congestedSegment", congestedSegment);
        result.put("alternativeRoutes", alternativeRoutes);
        result.put("estimatedTimeSave", calculateTimeSave(alternativeRoutes));

        // 推送通知
        pushEmergencyAlert(routeId, "TRAFFIC_CONGESTION",
                "检测到路段拥堵，已为您规划替代路线");

        return result;
    }

    @Override
    public Map<String, Object> handleSevereWeather(Integer routeId, Map<String, Object> weatherAlert) {
        Map<String, Object> result = new HashMap<>();

        Route route = routeService.getById(routeId);
        if (route == null) {
            result.put("success", false);
            result.put("message", "路线不存在");
            return result;
        }

        String weatherType = (String) weatherAlert.get("type");

        // 根据天气类型调整路线
        List<RouteAttraction> attractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId);
        List<Map<String, Object>> indoorAlternatives = new ArrayList<>();

        for (RouteAttraction ra : attractions) {
            Attraction attraction = attractionService.getById(ra.getAttractionId());
            if (attraction != null && isOutdoorAttraction(attraction)) {
                // 查找室内替代景点
                List<Map<String, Object>> alternatives = findIndoorAlternatives(
                        attraction.getId(), route.getCity().getId());
                indoorAlternatives.addAll(alternatives);
            }
        }

        result.put("success", true);
        result.put("routeId", routeId);
        result.put("weatherType", weatherType);
        result.put("indoorAlternatives", indoorAlternatives);
        result.put("safetyTips", generateWeatherSafetyTips(weatherType));

        // 推送通知
        pushEmergencyAlert(routeId, "SEVERE_WEATHER",
                "恶劣天气预警，建议调整行程至室内景点");

        return result;
    }

    @Override
    public Map<String, Object> handleHealthEmergency(Integer routeId, Map<String, Double> currentLocation) {
        Map<String, Object> result = new HashMap<>();

        Double lat = currentLocation.get("latitude");
        Double lng = currentLocation.get("longitude");

        // 获取最近医疗点
        List<Map<String, Object>> nearestHospitals = getNearestServices(lat, lng, "hospital");
        List<Map<String, Object>> nearestPharmacies = getNearestServices(lat, lng, "pharmacy");

        result.put("success", true);
        result.put("routeId", routeId);
        result.put("currentLocation", currentLocation);
        result.put("nearestHospitals", nearestHospitals);
        result.put("nearestPharmacies", nearestPharmacies);
        result.put("emergencyContacts", Arrays.asList("120", "110"));
        result.put("sosEnabled", true);

        // 自动发送SOS
        emergencySOS(null, routeId, "HEALTH_EMERGENCY", currentLocation);

        return result;
    }

    @Override
    public Map<String, Object> generateEmergencyContacts(Integer routeId) {
        Map<String, Object> contacts = new HashMap<>();

        Route route = routeService.getById(routeId);
        if (route != null && route.getCity() != null) {
            contacts.put("city", route.getCity().getName());
        }

        // 通用紧急电话
        contacts.put("universal", Map.of(
                "police", "110",
                "ambulance", "120",
                "fire", "119",
                "traffic", "122"
        ));

        // 旅游服务热线
        contacts.put("tourism", Map.of(
                "national", "12301",
                "complaint", "12345"
        ));

        // 保险公司（示例）
        contacts.put("insurance", Arrays.asList(
                Map.of("name", "平安保险", "phone", "95511"),
                Map.of("name", "人保财险", "phone", "95518")
        ));

        return contacts;
    }

    @Override
    public List<Map<String, Object>> getNearestServices(Double latitude, Double longitude, String serviceType) {
        List<Map<String, Object>> services = new ArrayList<>();

        // 模拟最近服务点数据
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> service = new HashMap<>();
            service.put("id", i);
            service.put("name", getServiceName(serviceType) + i);
            service.put("type", serviceType);
            service.put("distance", 0.5 + i * 0.3); // 模拟距离
            service.put("latitude", latitude + (Math.random() - 0.5) * 0.01);
            service.put("longitude", longitude + (Math.random() - 0.5) * 0.01);
            service.put("phone", "010-1234567" + i);
            service.put("open", true);
            services.add(service);
        }

        // 按距离排序
        services.sort(Comparator.comparingDouble(s -> (Double) s.get("distance")));

        return services;
    }

    @Override
    public Map<String, Object> emergencySOS(Integer userId, Integer routeId, String emergencyType,
                                             Map<String, Double> location) {
        Map<String, Object> result = new HashMap<>();

        // 记录应急事件
        logEmergencyEvent(userId, routeId, emergencyType, "用户发起紧急求助");

        // 发送紧急通知
        pushEmergencyAlert(routeId, "EMERGENCY_SOS",
                "紧急求助！用户位置：" + location);

        result.put("success", true);
        result.put("sosId", UUID.randomUUID().toString());
        result.put("status", "PROCESSING");
        result.put("message", "求助信息已发送，请保持冷静，救援人员将尽快联系您");
        result.put("estimatedResponseTime", "5-10分钟");

        return result;
    }

    @Override
    public List<Map<String, Object>> getRealtimeSafetyTips(Integer routeId) {
        List<Map<String, Object>> tips = new ArrayList<>();

        Route route = routeService.getById(routeId);
        if (route == null) {
            return tips;
        }

        // 通用安全提示
        tips.add(createSafetyTip("general", "保管好个人财物，注意人身安全"));
        tips.add(createSafetyTip("traffic", "遵守交通规则，注意来往车辆"));
        tips.add(createSafetyTip("weather", "关注天气变化，做好防护措施"));

        // 根据路线特点添加特定提示
        if ("困难".equals(route.getDifficulty())) {
            tips.add(createSafetyTip("difficulty", "本路线难度较高，请量力而行"));
        }

        List<RouteAttraction> attractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId);
        for (RouteAttraction ra : attractions) {
            Attraction attraction = attractionService.getById(ra.getAttractionId());
            if (attraction != null && attraction.getDescription() != null) {
                String desc = attraction.getDescription();
                if (desc.contains("山") || desc.contains("登山")) {
                    tips.add(createSafetyTip("mountain", "登山时请注意安全，穿着合适的鞋子"));
                }
                if (desc.contains("水") || desc.contains("湖") || desc.contains("海")) {
                    tips.add(createSafetyTip("water", "水边游玩请注意安全，不要独自下水"));
                }
            }
        }

        return tips;
    }

    @Override
    public Map<String, Object> assessRouteRisk(Integer routeId) {
        Map<String, Object> assessment = new HashMap<>();

        Route route = routeService.getById(routeId);
        if (route == null) {
            assessment.put("error", "路线不存在");
            return assessment;
        }

        int riskScore = 0;
        List<String> riskFactors = new ArrayList<>();
        List<String> mitigations = new ArrayList<>();

        // 难度风险
        String difficulty = route.getDifficulty();
        if ("困难".equals(difficulty)) {
            riskScore += 30;
            riskFactors.add("路线难度较高");
            mitigations.add("建议配备专业向导");
        } else if ("中等".equals(difficulty)) {
            riskScore += 15;
        }

        // 天数风险
        Integer days = route.getDurationDays();
        if (days > 5) {
            riskScore += 10;
            riskFactors.add("行程时间较长");
            mitigations.add("注意劳逸结合");
        }

        // 景点风险
        List<RouteAttraction> attractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId);
        for (RouteAttraction ra : attractions) {
            Attraction attraction = attractionService.getById(ra.getAttractionId());
            if (attraction != null && attraction.getDescription() != null) {
                String desc = attraction.getDescription();
                if (desc.contains("山") || desc.contains("悬崖")) {
                    riskScore += 10;
                    riskFactors.add("包含山地景点");
                    mitigations.add("注意登山安全");
                }
            }
        }

        String riskLevel;
        if (riskScore >= 50) {
            riskLevel = "HIGH";
        } else if (riskScore >= 25) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }

        assessment.put("routeId", routeId);
        assessment.put("riskScore", riskScore);
        assessment.put("riskLevel", riskLevel);
        assessment.put("riskFactors", riskFactors);
        assessment.put("mitigations", mitigations);
        assessment.put("assessedAt", new Date());

        return assessment;
    }

    @Override
    public List<Map<String, Object>> generateBackupRoutes(Integer routeId) {
        List<Map<String, Object>> backupRoutes = new ArrayList<>();

        Route route = routeService.getById(routeId);
        if (route == null) {
            return backupRoutes;
        }

        // 生成3条备选路线
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> backup = new HashMap<>();
            backup.put("backupId", i);
            backup.put("name", "备选方案" + i);
            backup.put("description", "针对不同突发情况的备选路线");
            backup.put("scenario", getScenarioForBackup(i));
            backup.put("estimatedDuration", route.getDurationDays() + "天");
            backup.put("difficulty", i == 1 ? "简单" : (i == 2 ? "中等" : "困难"));
            backupRoutes.add(backup);
        }

        return backupRoutes;
    }

    @Override
    public boolean pushEmergencyAlert(Integer routeId, String alertType, String message) {
        try {
            log.info("推送应急通知: routeId={}, type={}, message={}", routeId, alertType, message);
            // 实际实现中应该调用通知服务
            return true;
        } catch (Exception e) {
            log.error("推送应急通知失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getEmergencySupplyPoints(Double latitude, Double longitude) {
        List<Map<String, Object>> supplyPoints = new ArrayList<>();

        String[] types = {"water", "food", "shelter", "medical"};
        for (int i = 0; i < 4; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("id", i + 1);
            point.put("type", types[i]);
            point.put("name", getSupplyPointName(types[i]));
            point.put("latitude", latitude + (Math.random() - 0.5) * 0.02);
            point.put("longitude", longitude + (Math.random() - 0.5) * 0.02);
            point.put("distance", 0.3 + i * 0.2);
            supplyPoints.add(point);
        }

        return supplyPoints;
    }

    @Override
    public boolean logEmergencyEvent(Integer userId, Integer routeId, String eventType, String description) {
        try {
            log.warn("应急事件记录: userId={}, routeId={}, type={}, desc={}",
                    userId, routeId, eventType, description);
            // 实际实现中应该保存到数据库
            return true;
        } catch (Exception e) {
            log.error("记录应急事件失败: {}", e.getMessage());
            return false;
        }
    }

    // 辅助方法
    private List<Map<String, Object>> findAlternativeAttractions(Integer attractionId, Integer cityId) {
        List<Map<String, Object>> alternatives = new ArrayList<>();

        // 模拟查找相似景点
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> alt = new HashMap<>();
            alt.put("attractionId", attractionId + i);
            alt.put("name", "替代景点" + i);
            alt.put("similarity", 85 - i * 5);
            alt.put("type", "similar");
            alternatives.add(alt);
        }

        return alternatives;
    }

    private List<Map<String, Object>> generateAlternativeRoutes(Integer routeId, Integer fromId, Integer toId) {
        List<Map<String, Object>> routes = new ArrayList<>();

        for (int i = 1; i <= 2; i++) {
            Map<String, Object> route = new HashMap<>();
            route.put("routeId", routeId);
            route.put("alternativeId", i);
            route.put("description", "避开拥堵的替代路线" + i);
            route.put("estimatedTime", 30 + i * 10);
            route.put("distance", 5.0 + i);
            routes.add(route);
        }

        return routes;
    }

    private Integer calculateTimeSave(List<Map<String, Object>> alternativeRoutes) {
        // 模拟计算节省时间
        return 15;
    }

    private boolean isOutdoorAttraction(Attraction attraction) {
        String desc = attraction.getDescription();
        return desc != null && (desc.contains("公园") || desc.contains("山") ||
                desc.contains("户外") || desc.contains("广场"));
    }

    private List<Map<String, Object>> findIndoorAlternatives(Integer attractionId, Integer cityId) {
        List<Map<String, Object>> alternatives = new ArrayList<>();

        Map<String, Object> alt1 = new HashMap<>();
        alt1.put("attractionId", attractionId + 100);
        alt1.put("name", "室内博物馆");
        alt1.put("type", "indoor");
        alt1.put("suitableFor", "雨天/恶劣天气");
        alternatives.add(alt1);

        Map<String, Object> alt2 = new HashMap<>();
        alt2.put("attractionId", attractionId + 101);
        alt2.put("name", "购物中心");
        alt2.put("type", "indoor");
        alt2.put("suitableFor", "雨天/恶劣天气");
        alternatives.add(alt2);

        return alternatives;
    }

    private List<String> generateWeatherSafetyTips(String weatherType) {
        List<String> tips = new ArrayList<>();

        switch (weatherType) {
            case "RAIN":
                tips.add("携带雨具，注意防滑");
                tips.add("避免在树下避雨");
                break;
            case "SNOW":
                tips.add("注意保暖，穿防滑鞋");
                tips.add("减少户外活动时间");
                break;
            case "STORM":
                tips.add("尽快寻找室内避雨");
                tips.add("远离高大建筑物和树木");
                break;
            default:
                tips.add("关注天气变化，做好防护");
        }

        return tips;
    }

    private String getServiceName(String serviceType) {
        switch (serviceType) {
            case "hospital":
                return "医院";
            case "police":
                return "派出所";
            case "pharmacy":
                return "药店";
            default:
                return "服务点";
        }
    }

    private Map<String, Object> createSafetyTip(String type, String content) {
        Map<String, Object> tip = new HashMap<>();
        tip.put("type", type);
        tip.put("content", content);
        tip.put("priority", "high".equals(type) ? "HIGH" : "NORMAL");
        return tip;
    }

    private String getScenarioForBackup(int backupId) {
        switch (backupId) {
            case 1:
                return "景点闭园/人流过大";
            case 2:
                return "交通拥堵/道路封闭";
            case 3:
                return "恶劣天气/突发事件";
            default:
                return "一般情况";
        }
    }

    private String getSupplyPointName(String type) {
        switch (type) {
            case "water":
                return "饮水点";
            case "food":
                return "食品供应点";
            case "shelter":
                return "避难所";
            case "medical":
                return "医疗点";
            default:
                return "应急点";
        }
    }
}
