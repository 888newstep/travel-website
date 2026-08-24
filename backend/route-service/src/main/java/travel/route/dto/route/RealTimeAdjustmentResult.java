package travel.route.dto.route;

import java.util.List;

public class RealTimeAdjustmentResult {
    private Integer routeId;
    private String routeName;
    private List<String> adjustments;
    private List<RealTimeAlternativeAttraction> alternativeAttractions;
    private RealTimeLocation currentLocation;
    private RealTimeFactors realTimeFactors;

    public RealTimeAdjustmentResult() {}

    public Integer getRouteId() { return routeId; }
    public void setRouteId(Integer routeId) { this.routeId = routeId; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public List<String> getAdjustments() { return adjustments; }
    public void setAdjustments(List<String> adjustments) { this.adjustments = adjustments; }
    public List<RealTimeAlternativeAttraction> getAlternativeAttractions() { return alternativeAttractions; }
    public void setAlternativeAttractions(List<RealTimeAlternativeAttraction> alternativeAttractions) { this.alternativeAttractions = alternativeAttractions; }
    public RealTimeLocation getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(RealTimeLocation currentLocation) { this.currentLocation = currentLocation; }
    public RealTimeFactors getRealTimeFactors() { return realTimeFactors; }
    public void setRealTimeFactors(RealTimeFactors realTimeFactors) { this.realTimeFactors = realTimeFactors; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final RealTimeAdjustmentResult r = new RealTimeAdjustmentResult();
        public Builder routeId(Integer routeId) { r.routeId = routeId; return this; }
        public Builder routeName(String routeName) { r.routeName = routeName; return this; }
        public Builder adjustments(List<String> adjustments) { r.adjustments = adjustments; return this; }
        public Builder alternativeAttractions(List<RealTimeAlternativeAttraction> alternativeAttractions) { r.alternativeAttractions = alternativeAttractions; return this; }
        public Builder currentLocation(RealTimeLocation currentLocation) { r.currentLocation = currentLocation; return this; }
        public Builder realTimeFactors(RealTimeFactors realTimeFactors) { r.realTimeFactors = realTimeFactors; return this; }
        public RealTimeAdjustmentResult build() { return r; }
    }
}
