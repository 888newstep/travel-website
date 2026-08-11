package travel.route.dto.route;

import java.util.List;

public class RouteComparisonResult {
    private List<RouteComparisonDetail> routes;
    private RouteComparisonDetail bestRoute;
    private Integer totalRoutes;

    public RouteComparisonResult() {}

    public RouteComparisonResult(List<RouteComparisonDetail> routes, RouteComparisonDetail bestRoute, Integer totalRoutes) {
        this.routes = routes;
        this.bestRoute = bestRoute;
        this.totalRoutes = totalRoutes;
    }

    public List<RouteComparisonDetail> getRoutes() { return routes; }
    public void setRoutes(List<RouteComparisonDetail> routes) { this.routes = routes; }
    public RouteComparisonDetail getBestRoute() { return bestRoute; }
    public void setBestRoute(RouteComparisonDetail bestRoute) { this.bestRoute = bestRoute; }
    public Integer getTotalRoutes() { return totalRoutes; }
    public void setTotalRoutes(Integer totalRoutes) { this.totalRoutes = totalRoutes; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private List<RouteComparisonDetail> routes;
        private RouteComparisonDetail bestRoute;
        private Integer totalRoutes;
        public Builder routes(List<RouteComparisonDetail> routes) { this.routes = routes; return this; }
        public Builder bestRoute(RouteComparisonDetail bestRoute) { this.bestRoute = bestRoute; return this; }
        public Builder totalRoutes(Integer totalRoutes) { this.totalRoutes = totalRoutes; return this; }
        public RouteComparisonResult build() { return new RouteComparisonResult(routes, bestRoute, totalRoutes); }
    }
}
