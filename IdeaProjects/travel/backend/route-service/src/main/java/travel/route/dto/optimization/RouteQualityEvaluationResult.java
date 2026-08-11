package travel.route.dto.optimization;

public class RouteQualityEvaluationResult {
    private Integer routeId;
    private String routeName;
    private Double averageRating;
    private Integer totalAttractions;
    private Double attractionsPerDay;
    private Double diversityScore;
    private Double qualityScore;
    private String recommendationLevel;

    public RouteQualityEvaluationResult() {}

    public Integer getRouteId() { return routeId; }
    public void setRouteId(Integer routeId) { this.routeId = routeId; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
    public Integer getTotalAttractions() { return totalAttractions; }
    public void setTotalAttractions(Integer totalAttractions) { this.totalAttractions = totalAttractions; }
    public Double getAttractionsPerDay() { return attractionsPerDay; }
    public void setAttractionsPerDay(Double attractionsPerDay) { this.attractionsPerDay = attractionsPerDay; }
    public Double getDiversityScore() { return diversityScore; }
    public void setDiversityScore(Double diversityScore) { this.diversityScore = diversityScore; }
    public Double getQualityScore() { return qualityScore; }
    public void setQualityScore(Double qualityScore) { this.qualityScore = qualityScore; }
    public String getRecommendationLevel() { return recommendationLevel; }
    public void setRecommendationLevel(String recommendationLevel) { this.recommendationLevel = recommendationLevel; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final RouteQualityEvaluationResult e = new RouteQualityEvaluationResult();
        public Builder routeId(Integer routeId) { e.routeId = routeId; return this; }
        public Builder routeName(String routeName) { e.routeName = routeName; return this; }
        public Builder averageRating(Double averageRating) { e.averageRating = averageRating; return this; }
        public Builder totalAttractions(Integer totalAttractions) { e.totalAttractions = totalAttractions; return this; }
        public Builder attractionsPerDay(Double attractionsPerDay) { e.attractionsPerDay = attractionsPerDay; return this; }
        public Builder diversityScore(Double diversityScore) { e.diversityScore = diversityScore; return this; }
        public Builder qualityScore(Double qualityScore) { e.qualityScore = qualityScore; return this; }
        public Builder recommendationLevel(String recommendationLevel) { e.recommendationLevel = recommendationLevel; return this; }
        public RouteQualityEvaluationResult build() { return e; }
    }
}
