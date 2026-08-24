package travel.route.dto.optimization;

import java.time.LocalDateTime;

public class OptimizationHistoryItem {
    private Integer routeId;
    private String optimizationType;
    private String description;
    private LocalDateTime appliedAt;

    public OptimizationHistoryItem() {}
    public OptimizationHistoryItem(Integer routeId, String optimizationType, String description, LocalDateTime appliedAt) {
        this.routeId = routeId;
        this.optimizationType = optimizationType;
        this.description = description;
        this.appliedAt = appliedAt;
    }
    public Integer getRouteId() { return routeId; }
    public void setRouteId(Integer routeId) { this.routeId = routeId; }
    public String getOptimizationType() { return optimizationType; }
    public void setOptimizationType(String optimizationType) { this.optimizationType = optimizationType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Integer routeId;
        private String optimizationType;
        private String description;
        private LocalDateTime appliedAt;
        public Builder routeId(Integer routeId) { this.routeId = routeId; return this; }
        public Builder optimizationType(String optimizationType) { this.optimizationType = optimizationType; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder appliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; return this; }
        public OptimizationHistoryItem build() { return new OptimizationHistoryItem(routeId, optimizationType, description, appliedAt); }
    }
}