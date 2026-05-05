package travel.service.impl.route_planning;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.route_planning.Transport;
import travel.entity.travel_recommendation.Attraction;
import travel.service.route_planning.MultiTransportService;
import travel.service.route_planning.TransportService;
import travel.service.travel_recommendation.AttractionService;
import travel.utils.CacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiTransportServiceImpl implements MultiTransportService {

    @Autowired
    private TransportService transportService;

    @Autowired
    private AttractionService attractionService;

    @Autowired
    private CacheUtil cacheUtil;

    private static final String TRANSPORT_COMPARE_PREFIX = "transport:compare:";
    private static final String TRAFFIC_CONDITION_PREFIX = "traffic:condition:";

    // 碳排放系数 (kg CO2/km) - 使用ConcurrentHashMap保证线程安全
    private static final Map<String, Double> CARBON_EMISSION_RATES = new ConcurrentHashMap<>();

    static {
        CARBON_EMISSION_RATES.put("walking", 0.0);
        CARBON_EMISSION_RATES.put("bicycle", 0.0);
        CARBON_EMISSION_RATES.put("bus", 0.12);
        CARBON_EMISSION_RATES.put("subway", 0.08);
        CARBON_EMISSION_RATES.put("taxi", 0.15);
        CARBON_EMISSION_RATES.put("car", 0.18);
        CARBON_EMISSION_RATES.put("train", 0.05);
        CARBON_EMISSION_RATES.put("plane", 0.25);
        CARBON_EMISSION_RATES.put("boat", 0.03);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> compareTransportOptions(Integer fromAttractionId, Integer toAttractionId,
                                                             Integer travelerCount, String preference) {
        String cacheKey = TRANSPORT_COMPARE_PREFIX + fromAttractionId + ":" + toAttractionId + ":" + travelerCount;
        
        List<Map<String, Object>> cachedOptions = cacheUtil.get(cacheKey, List.class);
        if (cachedOptions != null) {
            return cachedOptions;
        }

        Attraction fromAttraction = attractionService.getById(fromAttractionId);
        Attraction toAttraction = attractionService.getById(toAttractionId);
        
        if (fromAttraction == null || toAttraction == null) {
            return Collections.emptyList();
        }

        double distance = calculateDistance(fromAttraction.getLatitude().doubleValue(), fromAttraction.getLongitude().doubleValue(),
                                          toAttraction.getLatitude().doubleValue(), toAttraction.getLongitude().doubleValue());

        List<Map<String, Object>> options = new ArrayList<>();
        
        // 公共交通方案
        options.add(createTransportOption("bus", "公交", distance, travelerCount, preference));
        options.add(createTransportOption("subway", "地铁", distance, travelerCount, preference));
        
        // 出租车/网约车方案
        options.add(createTransportOption("taxi", "出租车", distance, travelerCount, preference));
        
        // 自驾方案
        options.add(createTransportOption("car", "自驾", distance, travelerCount, preference));
        
        // 步行方案（短距离）
        if (distance <= 3.0) {
            options.add(createTransportOption("walking", "步行", distance, travelerCount, preference));
        }
        
        // 骑行方案（中短距离）
        if (distance <= 10.0) {
            options.add(createTransportOption("bicycle", "骑行", distance, travelerCount, preference));
        }

        // 根据偏好排序
        sortOptionsByPreference(options, preference);
        
        cacheUtil.set(cacheKey, options, 15, java.util.concurrent.TimeUnit.MINUTES);
        
        return options;
    }

    @Override
    public Map<String, Object> getOptimalTransportCombination(Integer routeId, Integer travelerCount, String preference) {
        // 获取路线的所有交通段
        Map<String, Object> combination = new HashMap<>();
        
        combination.put("routeId", routeId);
        combination.put("travelerCount", travelerCount);
        combination.put("preference", preference);
        combination.put("recommendation", generateCombinationRecommendation(preference));
        combination.put("estimatedTotalCost", BigDecimal.ZERO);
        combination.put("estimatedTotalTime", 0);
        combination.put("carbonEmission", 0.0);
        
        return combination;
    }

    @Override
    public BigDecimal calculateTransportCost(String transportType, Double distance, Integer travelerCount) {
        Transport transport = getTransportByType(transportType);
        if (transport == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal costPerKm = transport.getCostPerKm() != null ? transport.getCostPerKm() : BigDecimal.ZERO;
        BigDecimal baseCost = new BigDecimal("10"); // 基础费用
        
        BigDecimal distanceCost = costPerKm.multiply(new BigDecimal(distance));
        BigDecimal totalCost = baseCost.add(distanceCost).multiply(new BigDecimal(travelerCount));
        
        return totalCost.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public Integer estimateTransportTime(String transportType, Double distance, String trafficCondition) {
        Transport transport = getTransportByType(transportType);
        if (transport == null) {
            return 0;
        }

        BigDecimal avgSpeed = transport.getAvgSpeedKmh() != null ? transport.getAvgSpeedKmh() : new BigDecimal("30");
        
        // 根据交通状况调整速度
        double speedFactor = 1.0;
        switch (trafficCondition) {
            case "congested":
                speedFactor = 0.5;
                break;
            case "moderate":
                speedFactor = 0.8;
                break;
            default:
                speedFactor = 1.0;
        }
        
        double adjustedSpeed = avgSpeed.doubleValue() * speedFactor;
        int timeInMinutes = (int) ((distance / adjustedSpeed) * 60);
        
        // 添加等待时间
        if ("bus".equals(transportType) || "subway".equals(transportType)) {
            timeInMinutes += 10; // 等车时间
        } else if ("taxi".equals(transportType)) {
            timeInMinutes += 5; // 叫车时间
        }
        
        return timeInMinutes;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getRealtimeTrafficCondition(Double fromLat, Double fromLng, 
                                                           Double toLat, Double toLng) {
        String cacheKey = TRAFFIC_CONDITION_PREFIX + fromLat + ":" + fromLng + ":" + toLat + ":" + toLng;
        
        Map<String, Object> cachedCondition = cacheUtil.get(cacheKey, Map.class);
        if (cachedCondition != null) {
            return cachedCondition;
        }

        // 模拟实时交通数据
        Map<String, Object> condition = new HashMap<>();
        condition.put("status", getRandomTrafficStatus());
        condition.put("congestionLevel", new Random().nextInt(100));
        condition.put("averageSpeed", 20 + new Random().nextInt(40));
        condition.put("estimatedDelay", new Random().nextInt(30));
        condition.put("lastUpdate", new Date());
        
        cacheUtil.set(cacheKey, condition, 5, java.util.concurrent.TimeUnit.MINUTES);
        
        return condition;
    }

    @Override
    public Map<String, Object> recommendBestDepartureTime(Integer fromAttractionId, Integer toAttractionId, 
                                                          String transportType) {
        Map<String, Object> recommendation = new HashMap<>();
        
        // 根据交通类型推荐最佳出发时间
        List<Map<String, Object>> timeSlots = new ArrayList<>();
        
        // 早高峰避开建议
        timeSlots.add(createTimeSlot("07:00-09:00", "早高峰", "congested", "不建议"));
        timeSlots.add(createTimeSlot("09:00-11:00", "上午", "smooth", "推荐"));
        timeSlots.add(createTimeSlot("11:00-14:00", "中午", "moderate", "一般"));
        timeSlots.add(createTimeSlot("14:00-17:00", "下午", "smooth", "推荐"));
        timeSlots.add(createTimeSlot("17:00-19:00", "晚高峰", "congested", "不建议"));
        timeSlots.add(createTimeSlot("19:00-22:00", "晚上", "smooth", "推荐"));
        
        recommendation.put("timeSlots", timeSlots);
        recommendation.put("bestTime", "09:00-11:00 或 14:00-17:00");
        recommendation.put("transportType", transportType);
        
        return recommendation;
    }

    @Override
    public Map<String, Double> getCarbonEmissionComparison(List<String> transportTypes, Double distance) {
        Map<String, Double> emissions = new HashMap<>();
        
        for (String type : transportTypes) {
            Double rate = CARBON_EMISSION_RATES.getOrDefault(type, 0.15);
            emissions.put(type, rate * distance);
        }
        
        return emissions;
    }

    @Override
    public Map<String, Object> recommendTransportMode(Double distance, Integer travelerCount, 
                                                      BigDecimal budget, Integer timeConstraint) {
        Map<String, Object> recommendation = new HashMap<>();
        
        String recommendedMode;
        String reason;
        
        if (distance <= 2.0) {
            recommendedMode = "walking";
            reason = "距离较近，步行最环保且无需等待";
        } else if (distance <= 5.0 && travelerCount <= 2) {
            recommendedMode = "bicycle";
            reason = "距离适中，骑行快速便捷";
        } else if (distance <= 15.0 && budget.doubleValue() < 50 * travelerCount) {
            recommendedMode = "bus";
            reason = "经济实惠，覆盖范围广";
        } else if (timeConstraint != null && timeConstraint < 30) {
            recommendedMode = "taxi";
            reason = "时间紧迫，出租车最快捷";
        } else if (travelerCount >= 4) {
            recommendedMode = "car";
            reason = "人数较多，自驾分摊成本更划算";
        } else {
            recommendedMode = "subway";
            reason = "快速准时，不受拥堵影响";
        }
        
        recommendation.put("recommendedMode", recommendedMode);
        recommendation.put("reason", reason);
        recommendation.put("alternatives", getAlternativeModes(recommendedMode));
        
        return recommendation;
    }

    // 辅助方法
    private Map<String, Object> createTransportOption(String type, String name, double distance, 
                                                      Integer travelerCount, String preference) {
        Map<String, Object> option = new HashMap<>();
        
        BigDecimal cost = calculateTransportCost(type, distance, travelerCount);
        Integer time = estimateTransportTime(type, distance, "moderate");
        Double carbonEmission = CARBON_EMISSION_RATES.getOrDefault(type, 0.15) * distance;
        
        option.put("type", type);
        option.put("name", name);
        option.put("distance", distance);
        option.put("cost", cost);
        option.put("time", time);
        option.put("carbonEmission", carbonEmission);
        option.put("ecoFriendly", carbonEmission < 0.05);
        option.put("description", generateOptionDescription(type, distance, time));
        
        return option;
    }

    private String generateOptionDescription(String type, double distance, int time) {
        StringBuilder desc = new StringBuilder();
        
        switch (type) {
            case "walking":
                desc.append("步行约").append(time).append("分钟，");
                desc.append("距离").append(String.format("%.1f", distance)).append("公里");
                break;
            case "bicycle":
                desc.append("骑行约").append(time).append("分钟，");
                desc.append("健康环保，沿途风景优美");
                break;
            case "bus":
                desc.append("公交约").append(time).append("分钟，");
                desc.append("经济实惠，覆盖范围广");
                break;
            case "subway":
                desc.append("地铁约").append(time).append("分钟，");
                desc.append("快速准时，不受拥堵影响");
                break;
            case "taxi":
                desc.append("出租车约").append(time).append("分钟，");
                desc.append("门到门服务，舒适便捷");
                break;
            case "car":
                desc.append("自驾约").append(time).append("分钟，");
                desc.append("自由灵活，适合多人出行");
                break;
            default:
                desc.append("约").append(time).append("分钟");
        }
        
        return desc.toString();
    }

    private void sortOptionsByPreference(List<Map<String, Object>> options, String preference) {
        switch (preference) {
            case "cost":
                options.sort(Comparator.comparing(o -> ((BigDecimal) o.get("cost"))));
                break;
            case "time":
                options.sort(Comparator.comparing(o -> ((Integer) o.get("time"))));
                break;
            case "eco":
                options.sort(Comparator.comparing(o -> ((Double) o.get("carbonEmission"))));
                break;
            default:
                // 综合评分排序
                options.sort((o1, o2) -> {
                    double score1 = calculateCompositeScore(o1);
                    double score2 = calculateCompositeScore(o2);
                    return Double.compare(score2, score1);
                });
        }
    }

    private double calculateCompositeScore(Map<String, Object> option) {
        BigDecimal cost = (BigDecimal) option.get("cost");
        Integer time = (Integer) option.get("time");
        Double carbonEmission = (Double) option.get("carbonEmission");
        
        // 归一化评分（越低越好）
        double costScore = cost.doubleValue() / 100.0;
        double timeScore = time / 60.0;
        double ecoScore = carbonEmission * 10;
        
        return 100 - (costScore * 0.4 + timeScore * 0.4 + ecoScore * 0.2);
    }

    private double calculateDistance(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return 0.0;
        }
        
        // 使用Haversine公式计算距离
        double R = 6371; // 地球半径（公里）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    private Transport getTransportByType(String type) {
        return transportService.list().stream()
                .filter(t -> type.equalsIgnoreCase(String.valueOf(t.getTransportType())))
                .findFirst()
                .orElse(null);
    }

    private String getRandomTrafficStatus() {
        String[] statuses = {"smooth", "moderate", "congested"};
        return statuses[new Random().nextInt(statuses.length)];
    }

    private Map<String, Object> createTimeSlot(String timeRange, String period, String traffic, String recommendation) {
        Map<String, Object> slot = new HashMap<>();
        slot.put("timeRange", timeRange);
        slot.put("period", period);
        slot.put("trafficCondition", traffic);
        slot.put("recommendation", recommendation);
        return slot;
    }

    private String generateCombinationRecommendation(String preference) {
        switch (preference) {
            case "cost":
                return "推荐以公共交通为主，短距离步行，平衡成本与效率";
            case "time":
                return "推荐地铁+出租车组合，最大限度节省时间";
            case "comfort":
                return "推荐全程专车服务，享受舒适出行体验";
            case "eco":
                return "推荐公共交通+骑行组合，低碳环保出行";
            default:
                return "推荐混合交通方式，根据实际情况灵活选择";
        }
    }

    private List<String> getAlternativeModes(String recommendedMode) {
        List<String> alternatives = new ArrayList<>();
        
        switch (recommendedMode) {
            case "walking":
                alternatives.add("bicycle");
                alternatives.add("bus");
                break;
            case "bicycle":
                alternatives.add("bus");
                alternatives.add("subway");
                break;
            case "bus":
                alternatives.add("subway");
                alternatives.add("taxi");
                break;
            case "subway":
                alternatives.add("bus");
                alternatives.add("taxi");
                break;
            case "taxi":
                alternatives.add("subway");
                alternatives.add("car");
                break;
            case "car":
                alternatives.add("taxi");
                alternatives.add("subway");
                break;
            default:
                alternatives.add("bus");
                alternatives.add("subway");
        }
        
        return alternatives;
    }
}
