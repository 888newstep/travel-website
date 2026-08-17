package travel.route.algorithm;

import lombok.RequiredArgsConstructor;
import travel.common.entity.travel_recommendation.Attraction;
import travel.route.service.AttractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
@RequiredArgsConstructor
public class RoutePlanAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(RoutePlanAlgorithm.class);

    private final AttractionService attractionService;

    /**
     * 路线染色体（遗传算法使用）
     */
    public static class RouteChromosome {
        private List<Integer> attractionIds;
        private Map<Integer, List<Integer>> dayAttractions;
        private double fitness;

        public RouteChromosome() {
            this.attractionIds = new ArrayList<>();
            this.dayAttractions = new HashMap<>();
            this.fitness = 0.0;
        }

        public List<Integer> getAttractionIds() { return attractionIds; }
        public void setAttractionIds(List<Integer> attractionIds) { this.attractionIds = attractionIds; }
        public Map<Integer, List<Integer>> getDayAttractions() { return dayAttractions; }
        public void setDayAttractions(Map<Integer, List<Integer>> dayAttractions) { this.dayAttractions = dayAttractions; }
        public double getFitness() { return fitness; }
        public void setFitness(double fitness) { this.fitness = fitness; }
    }

    /**
     * 最优路线
     */
    public static class OptimalRoute {
        private List<RouteDayPlan> dayPlans;
        private double totalDistance;
        private double totalCost;
        private double totalTime;
        private double totalFitness;

        public OptimalRoute() {
            this.dayPlans = new ArrayList<>();
        }

        public List<RouteDayPlan> getDayPlans() { return dayPlans; }
        public void setDayPlans(List<RouteDayPlan> dayPlans) { this.dayPlans = dayPlans; }
        public double getTotalDistance() { return totalDistance; }
        public void setTotalDistance(double totalDistance) { this.totalDistance = totalDistance; }
        public double getTotalCost() { return totalCost; }
        public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
        public double getTotalTime() { return totalTime; }
        public void setTotalTime(double totalTime) { this.totalTime = totalTime; }
        public double getTotalFitness() { return totalFitness; }
        public void setTotalFitness(double totalFitness) { this.totalFitness = totalFitness; }
    }

    /**
     * 每日路线计划
     */
    public static class RouteDayPlan {
        private int dayNumber;
        private List<Integer> attractionIds;
        private List<RoutePoint> points;
        private double distance;
        private double cost;
        private double time;

        public RouteDayPlan() {
            this.attractionIds = new ArrayList<>();
            this.points = new ArrayList<>();
        }

        public int getDayNumber() { return dayNumber; }
        public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }
        public List<Integer> getAttractionIds() { return attractionIds; }
        public void setAttractionIds(List<Integer> attractionIds) { this.attractionIds = attractionIds; }
        public List<RoutePoint> getPoints() { return points; }
        public void setPoints(List<RoutePoint> points) { this.points = points; }
        public double getDistance() { return distance; }
        public void setDistance(double distance) { this.distance = distance; }
        public double getCost() { return cost; }
        public void setCost(double cost) { this.cost = cost; }
        public double getTime() { return time; }
        public void setTime(double time) { this.time = time; }
    }

    /**
     * 路线点
     */
    public static class RoutePoint {
        private Integer attractionId;
        private Integer transportId;
        private Double distance;
        private Double time;

        public RoutePoint() {}

        public Integer getAttractionId() { return attractionId; }
        public void setAttractionId(Integer attractionId) { this.attractionId = attractionId; }
        public Integer getTransportId() { return transportId; }
        public void setTransportId(Integer transportId) { this.transportId = transportId; }
        public Double getDistance() { return distance; }
        public void setDistance(Double distance) { this.distance = distance; }
        public Double getTime() { return time; }
        public void setTime(Double time) { this.time = time; }
    }

    /**
     * 规划最优路线
     * @param attractionIds 景点ID列表
     * @param maxDays 最大天数
     * @param budget 预算
     * @param preference 偏好类型 (balanced, lowCost, fast, lowCarbon)
     * @return 最优路线
     */
    public OptimalRoute planOptimalRoute(List<Integer> attractionIds, int maxDays, BigDecimal budget, String preference) {
        log.info("开始规划最优路线: attractionCount={}, maxDays={}, budget={}, preference={}",
                attractionIds.size(), maxDays, budget, preference);

        OptimalRoute optimalRoute = new OptimalRoute();

        try {
            // 1. 获取景点信息
            List<Attraction> attractions = new ArrayList<>();
            for (Integer attractionId : attractionIds) {
                Attraction attraction = attractionService.getById(attractionId);
                if (attraction != null) {
                    attractions.add(attraction);
                }
            }

            if (attractions.isEmpty()) {
                throw new RuntimeException("没有有效的景点数据");
            }

            // 2. 根据偏好调整权重
            Map<String, Double> weights = calculateWeights(preference);

            // 3. 分配景点到每一天
            Map<Integer, List<Attraction>> dailyAttractions = distributeAttractionsToDays(attractions, maxDays);

            // 4. 为每一天规划最优顺序
            double totalDistance = 0.0;
            double totalCost = 0.0;
            double totalTime = 0.0;

            for (Map.Entry<Integer, List<Attraction>> entry : dailyAttractions.entrySet()) {
                int dayNumber = entry.getKey();
                List<Attraction> dayAttractionList = entry.getValue();

                // 优化当天的游览顺序
                List<Attraction> optimizedOrder = optimizeDailyOrder(dayAttractionList, weights);

                // 创建当日计划
                RouteDayPlan dayPlan = createDayPlan(dayNumber, optimizedOrder, weights);
                optimalRoute.getDayPlans().add(dayPlan);

                totalDistance += dayPlan.getDistance();
                totalCost += dayPlan.getCost();
                totalTime += dayPlan.getTime();
            }

            // 5. 计算总适应度
            double fitness = calculateFitness(totalDistance, totalCost, totalTime, budget.doubleValue(), weights);
            optimalRoute.setTotalDistance(totalDistance);
            optimalRoute.setTotalCost(totalCost);
            optimalRoute.setTotalTime(totalTime);
            optimalRoute.setTotalFitness(fitness);

            log.info("路线规划完成: days={}, totalDistance={}km, totalCost={}元, totalTime={}小时, fitness={}",
                    maxDays, totalDistance, totalCost, totalTime, fitness);

        } catch (Exception e) {
            log.error("路线规划失败: {}", e.getMessage(), e);
            throw new RuntimeException("路线规划失败: " + e.getMessage());
        }

        return optimalRoute;
    }

    /**
     * 根据偏好计算权重
     */
    private Map<String, Double> calculateWeights(String preference) {
        Map<String, Double> weights = new HashMap<>();

        switch (preference.toLowerCase()) {
            case "lowcost":
                weights.put("distance", 0.2);
                weights.put("cost", 0.6);
                weights.put("time", 0.2);
                break;
            case "fast":
                weights.put("distance", 0.3);
                weights.put("cost", 0.2);
                weights.put("time", 0.5);
                break;
            case "lowcarbon":
                weights.put("distance", 0.5);
                weights.put("cost", 0.3);
                weights.put("time", 0.2);
                break;
            case "balanced":
            default:
                weights.put("distance", 0.33);
                weights.put("cost", 0.33);
                weights.put("time", 0.34);
                break;
        }

        return weights;
    }

    /**
     * 将景点分配到每一天
     */
    private Map<Integer, List<Attraction>> distributeAttractionsToDays(List<Attraction> attractions, int maxDays) {
        Map<Integer, List<Attraction>> dailyAttractions = new HashMap<>();

        int attractionsPerDay = (int) Math.ceil((double) attractions.size() / maxDays);

        for (int day = 1; day <= maxDays; day++) {
            int startIndex = (day - 1) * attractionsPerDay;
            int endIndex = Math.min(startIndex + attractionsPerDay, attractions.size());

            if (startIndex < attractions.size()) {
                List<Attraction> dayList = attractions.subList(startIndex, endIndex);
                dailyAttractions.put(day, dayList);
            }
        }

        return dailyAttractions;
    }

    /**
     * 优化单日的游览顺序（简单的最近邻算法）
     */
    private List<Attraction> optimizeDailyOrder(List<Attraction> attractions, Map<String, Double> weights) {
        if (attractions.size() <= 1) {
            return attractions;
        }

        List<Attraction> optimized = new ArrayList<>();
        List<Attraction> remaining = new ArrayList<>(attractions);

        // 从第一个景点开始
        Attraction current = remaining.remove(0);
        optimized.add(current);

        // 贪心算法：每次选择最近的景点
        while (!remaining.isEmpty()) {
            Attraction nearest = findNearestAttraction(current, remaining);
            optimized.add(nearest);
            remaining.remove(nearest);
            current = nearest;
        }

        return optimized;
    }

    /**
     * 找到最近的景点
     */
    private Attraction findNearestAttraction(Attraction from, List<Attraction> candidates) {
        Attraction nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Attraction candidate : candidates) {
            double distance = calculateDistance(from, candidate);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = candidate;
            }
        }

        return nearest;
    }

    /**
     * 计算两个景点之间的距离（简化版，实际应调用地图API）
     */
    private double calculateDistance(Attraction from, Attraction to) {
        if (from.getLatitude() == null || from.getLongitude() == null ||
                to.getLatitude() == null || to.getLongitude() == null) {
            return 10.0; // 默认距离
        }

        // 简化的欧几里得距离计算
        double latDiff = from.getLatitude().doubleValue() - to.getLatitude().doubleValue();
        double lonDiff = from.getLongitude().doubleValue() - to.getLongitude().doubleValue();
        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff) * 111; // 转换为公里
    }

    /**
     * 创建单日计划
     */
    private RouteDayPlan createDayPlan(int dayNumber, List<Attraction> attractions, Map<String, Double> weights) {
        RouteDayPlan dayPlan = new RouteDayPlan();
        dayPlan.setDayNumber(dayNumber);

        List<Integer> attractionIds = new ArrayList<>();
        List<RoutePoint> points = new ArrayList<>();
        double totalDistance = 0.0;
        double totalCost = 0.0;
        double totalTime = 0.0;

        for (int i = 0; i < attractions.size(); i++) {
            Attraction attraction = attractions.get(i);
            attractionIds.add(attraction.getId());

            RoutePoint point = new RoutePoint();
            point.setAttractionId(attraction.getId());

            if (i > 0) {
                // 计算与前一个景点的距离和时间
                Attraction prevAttraction = attractions.get(i - 1);
                double distance = calculateDistance(prevAttraction, attraction);
                double time = estimateTravelTime(distance);
                double cost = estimateTravelCost(distance);

                point.setDistance(distance);
                point.setTime(time);

                totalDistance += distance;
                totalTime += time;
                totalCost += cost;
            }

            points.add(point);
        }

        dayPlan.setAttractionIds(attractionIds);
        dayPlan.setPoints(points);
        dayPlan.setDistance(totalDistance);
        dayPlan.setTime(totalTime);
        dayPlan.setCost(totalCost);

        return dayPlan;
    }

    /**
     * 估算旅行时间（小时）
     */
    private double estimateTravelTime(double distanceKm) {
        // 假设平均速度为30km/h
        return distanceKm / 30.0;
    }

    /**
     * 估算旅行成本（元）
     */
    private double estimateTravelCost(double distanceKm) {
        // 假设每公里成本为2元
        return distanceKm * 2.0;
    }

    /**
     * 计算适应度分数
     */
    private double calculateFitness(double distance, double cost, double time, double budget, Map<String, Double> weights) {
        // 归一化处理
        double distanceScore = 1.0 / (1.0 + distance);
        double costScore = 1.0 / (1.0 + cost / budget);
        double timeScore = 1.0 / (1.0 + time);

        // 加权计算
        double fitness = distanceScore * weights.get("distance") +
                costScore * weights.get("cost") +
                timeScore * weights.get("time");

        return fitness;
    }
}
