package travel.route.dto.route;

import java.util.List;
import java.util.Map;

public class RouteOptimizationSuggestionResult {
    private Integer routeId;
    private String routeName;
    private String optimizationType;
    private List<String> suggestions;
    private Integer attractionCount;
    private Integer durationDays;

    public RouteOptimizationSuggestionResult() {}

    public Integer getRouteId() { return routeId; }
    public void setRouteId(Integer routeId) { this.routeId = routeId; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public String getOptimizationType() { return optimizationType; }
    public void setOptimizationType(String optimizationType) { this.optimizationType = optimizationType; }
    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    public Integer getAttractionCount() { return attractionCount; }
    public void setAttractionCount(Integer attractionCount) { this.attractionCount = attractionCount; }
    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final RouteOptimizationSuggestionResult r = new RouteOptimizationSuggestionResult();
        public Builder routeId(Integer routeId) { r.routeId = routeId; return this; }
        public Builder routeName(String routeName) { r.routeName = routeName; return this; }
        public Builder optimizationType(String optimizationType) { r.optimizationType = optimizationType; return this; }
        public Builder suggestions(List<String> suggestions) { r.suggestions = suggestions; return this; }
        public Builder attractionCount(Integer attractionCount) { r.attractionCount = attractionCount; return this; }
        public Builder durationDays(Integer durationDays) { r.durationDays = durationDays; return this; }
        public RouteOptimizationSuggestionResult build() { return r; }
    }
}
