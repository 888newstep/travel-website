package travel.route.dto.route;

import travel.route.algorithm.RoutePlanAlgorithm.OptimalRoute;
import java.util.Map;

public class MultiDayRouteResult {
    private OptimalRoute route;
    private Integer cityId;
    private String startDate;
    private String endDate;
    private Map<String, Object> userPreferences;
    private Integer attractionCount;

    public MultiDayRouteResult() {}

    public OptimalRoute getRoute() { return route; }
    public void setRoute(OptimalRoute route) { this.route = route; }
    public Integer getCityId() { return cityId; }
    public void setCityId(Integer cityId) { this.cityId = cityId; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public Map<String, Object> getUserPreferences() { return userPreferences; }
    public void setUserPreferences(Map<String, Object> userPreferences) { this.userPreferences = userPreferences; }
    public Integer getAttractionCount() { return attractionCount; }
    public void setAttractionCount(Integer attractionCount) { this.attractionCount = attractionCount; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final MultiDayRouteResult r = new MultiDayRouteResult();
        public Builder route(OptimalRoute route) { r.route = route; return this; }
        public Builder cityId(Integer cityId) { r.cityId = cityId; return this; }
        public Builder startDate(String startDate) { r.startDate = startDate; return this; }
        public Builder endDate(String endDate) { r.endDate = endDate; return this; }
        public Builder userPreferences(Map<String, Object> userPreferences) { r.userPreferences = userPreferences; return this; }
        public Builder attractionCount(Integer attractionCount) { r.attractionCount = attractionCount; return this; }
        public MultiDayRouteResult build() { return r; }
    }
}
