package travel.route.dto.optimization;

import java.util.List;
import java.util.Map;

public class RouteAnalysisResult {
    private Integer routeId;
    private String routeName;
    private Integer totalAttractions;
    private Double totalDistance;
    private Double estimatedCost;
    private Double estimatedTime;
    private Double averageRating;
    private Map<String, Integer> attractionTypeDistribution;
    private List<String> recommendations;

    public RouteAnalysisResult() {}

    public Integer getRouteId() { return routeId; }
    public void setRouteId(Integer routeId) { this.routeId = routeId; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public Integer getTotalAttractions() { return totalAttractions; }
    public void setTotalAttractions(Integer totalAttractions) { this.totalAttractions = totalAttractions; }
    public Double getTotalDistance() { return totalDistance; }
    public void setTotalDistance(Double totalDistance) { this.totalDistance = totalDistance; }
    public Double getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(Double estimatedCost) { this.estimatedCost = estimatedCost; }
    public Double getEstimatedTime() { return estimatedTime; }
    public void setEstimatedTime(Double estimatedTime) { this.estimatedTime = estimatedTime; }
    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
    public Map<String, Integer> getAttractionTypeDistribution() { return attractionTypeDistribution; }
    public void setAttractionTypeDistribution(Map<String, Integer> attractionTypeDistribution) { this.attractionTypeDistribution = attractionTypeDistribution; }
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final RouteAnalysisResult a = new RouteAnalysisResult();
        public Builder routeId(Integer routeId) { a.routeId = routeId; return this; }
        public Builder routeName(String routeName) { a.routeName = routeName; return this; }
        public Builder totalAttractions(Integer totalAttractions) { a.totalAttractions = totalAttractions; return this; }
        public Builder totalDistance(Double totalDistance) { a.totalDistance = totalDistance; return this; }
        public Builder estimatedCost(Double estimatedCost) { a.estimatedCost = estimatedCost; return this; }
        public Builder estimatedTime(Double estimatedTime) { a.estimatedTime = estimatedTime; return this; }
        public Builder averageRating(Double averageRating) { a.averageRating = averageRating; return this; }
        public Builder attractionTypeDistribution(Map<String, Integer> attractionTypeDistribution) { a.attractionTypeDistribution = attractionTypeDistribution; return this; }
        public Builder recommendations(List<String> recommendations) { a.recommendations = recommendations; return this; }
        public RouteAnalysisResult build() { return a; }
    }
}
