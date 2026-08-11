package travel.route.dto.optimization;

import travel.route.algorithm.RoutePlanAlgorithm.OptimalRoute;
import java.util.List;

public class RouteRecommendationItem {
    private Integer routeId;
    private String routeName;
    private String preference;
    private Integer days;
    private Integer userId;
    private Double fitness;
    private Double totalDistance;
    private Double totalCost;
    private Double totalTime;
    private OptimalRoute route;
    private List<RouteRecommendationDayPlan> dayPlans;

    public RouteRecommendationItem() {}

    public Integer getRouteId() { return routeId; }
    public void setRouteId(Integer routeId) { this.routeId = routeId; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public String getPreference() { return preference; }
    public void setPreference(String preference) { this.preference = preference; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Double getFitness() { return fitness; }
    public void setFitness(Double fitness) { this.fitness = fitness; }
    public Double getTotalDistance() { return totalDistance; }
    public void setTotalDistance(Double totalDistance) { this.totalDistance = totalDistance; }
    public Double getTotalCost() { return totalCost; }
    public void setTotalCost(Double totalCost) { this.totalCost = totalCost; }
    public Double getTotalTime() { return totalTime; }
    public void setTotalTime(Double totalTime) { this.totalTime = totalTime; }
    public OptimalRoute getRoute() { return route; }
    public void setRoute(OptimalRoute route) { this.route = route; }
    public List<RouteRecommendationDayPlan> getDayPlans() { return dayPlans; }
    public void setDayPlans(List<RouteRecommendationDayPlan> dayPlans) { this.dayPlans = dayPlans; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final RouteRecommendationItem r = new RouteRecommendationItem();
        public Builder routeId(Integer routeId) { r.routeId = routeId; return this; }
        public Builder routeName(String routeName) { r.routeName = routeName; return this; }
        public Builder preference(String preference) { r.preference = preference; return this; }
        public Builder days(Integer days) { r.days = days; return this; }
        public Builder userId(Integer userId) { r.userId = userId; return this; }
        public Builder fitness(Double fitness) { r.fitness = fitness; return this; }
        public Builder totalDistance(Double totalDistance) { r.totalDistance = totalDistance; return this; }
        public Builder totalCost(Double totalCost) { r.totalCost = totalCost; return this; }
        public Builder totalTime(Double totalTime) { r.totalTime = totalTime; return this; }
        public Builder route(OptimalRoute route) { r.route = route; return this; }
        public Builder dayPlans(List<RouteRecommendationDayPlan> dayPlans) { r.dayPlans = dayPlans; return this; }
        public RouteRecommendationItem build() { return r; }
    }
}
