package travel.route.dto.optimization;

public class RouteAlternative {
    private Integer originalRouteId;
    private RouteAlternativeData routeData;

    public RouteAlternative() {}

    public Integer getOriginalRouteId() { return originalRouteId; }
    public void setOriginalRouteId(Integer originalRouteId) { this.originalRouteId = originalRouteId; }
    public RouteAlternativeData getRouteData() { return routeData; }
    public void setRouteData(RouteAlternativeData routeData) { this.routeData = routeData; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final RouteAlternative a = new RouteAlternative();
        public Builder originalRouteId(Integer originalRouteId) { a.originalRouteId = originalRouteId; return this; }
        public Builder routeData(RouteAlternativeData routeData) { a.routeData = routeData; return this; }
        public RouteAlternative build() { return a; }
    }
}
