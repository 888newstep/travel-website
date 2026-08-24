package travel.route.algorithm;

import java.util.*;

public final class RoutePlanAlgorithm {

    private RoutePlanAlgorithm() {
    }

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

}
