package travel.route.dto.route;

import java.util.List;

public class RouteRecommendationReason {
    private Integer routeId;
    private Integer userId;
    private List<String> reasons;

    public RouteRecommendationReason() {}

    public RouteRecommendationReason(Integer routeId, Integer userId, List<String> reasons) {
        this.routeId = routeId;
        this.userId = userId;
        this.reasons = reasons;
    }

    public Integer getRouteId() { return routeId; }
    public void setRouteId(Integer routeId) { this.routeId = routeId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer routeId;
        private Integer userId;
        private List<String> reasons;
        public Builder routeId(Integer routeId) { this.routeId = routeId; return this; }
        public Builder userId(Integer userId) { this.userId = userId; return this; }
        public Builder reasons(List<String> reasons) { this.reasons = reasons; return this; }
        public RouteRecommendationReason build() { return new RouteRecommendationReason(routeId, userId, reasons); }
    }
}
