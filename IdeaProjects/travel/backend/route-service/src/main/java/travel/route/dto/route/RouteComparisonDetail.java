package travel.route.dto.route;

public class RouteComparisonDetail {
    private Integer routeId;
    private String routeName;
    private Integer durationDays;
    private Integer totalAttractions;
    private Double totalDistance;
    private Double estimatedCost;
    private Double estimatedTime;
    private Double averageRating;
    private Integer viewCount;
    private Integer likeCount;

    public RouteComparisonDetail() {}

    public Integer getRouteId() { return routeId; }
    public void setRouteId(Integer routeId) { this.routeId = routeId; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
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
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final RouteComparisonDetail d = new RouteComparisonDetail();
        public Builder routeId(Integer routeId) { d.routeId = routeId; return this; }
        public Builder routeName(String routeName) { d.routeName = routeName; return this; }
        public Builder durationDays(Integer durationDays) { d.durationDays = durationDays; return this; }
        public Builder totalAttractions(Integer totalAttractions) { d.totalAttractions = totalAttractions; return this; }
        public Builder totalDistance(Double totalDistance) { d.totalDistance = totalDistance; return this; }
        public Builder estimatedCost(Double estimatedCost) { d.estimatedCost = estimatedCost; return this; }
        public Builder estimatedTime(Double estimatedTime) { d.estimatedTime = estimatedTime; return this; }
        public Builder averageRating(Double averageRating) { d.averageRating = averageRating; return this; }
        public Builder viewCount(Integer viewCount) { d.viewCount = viewCount; return this; }
        public Builder likeCount(Integer likeCount) { d.likeCount = likeCount; return this; }
        public RouteComparisonDetail build() { return d; }
    }
}
