package travel.route.dto.route;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class RouteQualityEvaluation {
    private Integer routeId;
    private Double qualityScore;
    private Double diversityScore;
    private Double reasonablenessScore;
    private Double costPerformanceScore;
    private Double overallScore;
    private Map<String, JsonNode> details;

    public RouteQualityEvaluation() {}

    public Integer getRouteId() { return routeId; }
    public void setRouteId(Integer routeId) { this.routeId = routeId; }
    public Double getQualityScore() { return qualityScore; }
    public void setQualityScore(Double qualityScore) { this.qualityScore = qualityScore; }
    public Double getDiversityScore() { return diversityScore; }
    public void setDiversityScore(Double diversityScore) { this.diversityScore = diversityScore; }
    public Double getReasonablenessScore() { return reasonablenessScore; }
    public void setReasonablenessScore(Double reasonablenessScore) { this.reasonablenessScore = reasonablenessScore; }
    public Double getCostPerformanceScore() { return costPerformanceScore; }
    public void setCostPerformanceScore(Double costPerformanceScore) { this.costPerformanceScore = costPerformanceScore; }
    public Double getOverallScore() { return overallScore; }
    public void setOverallScore(Double overallScore) { this.overallScore = overallScore; }
    public Map<String, JsonNode> getDetails() { return details; }
    public void setDetails(Map<String, JsonNode> details) { this.details = details; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final RouteQualityEvaluation e = new RouteQualityEvaluation();
        public Builder routeId(Integer routeId) { e.routeId = routeId; return this; }
        public Builder qualityScore(Double qualityScore) { e.qualityScore = qualityScore; return this; }
        public Builder diversityScore(Double diversityScore) { e.diversityScore = diversityScore; return this; }
        public Builder reasonablenessScore(Double reasonablenessScore) { e.reasonablenessScore = reasonablenessScore; return this; }
        public Builder costPerformanceScore(Double costPerformanceScore) { e.costPerformanceScore = costPerformanceScore; return this; }
        public Builder overallScore(Double overallScore) { e.overallScore = overallScore; return this; }
        public Builder details(Map<String, JsonNode> details) { e.details = details; return this; }
        public RouteQualityEvaluation build() { return e; }
    }
}
