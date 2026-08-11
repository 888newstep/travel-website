package travel.route.dto.route;

import travel.route.algorithm.RoutePlanAlgorithm.OptimalRoute;

public class UserPreferenceRecommendation {
    private String preference;
    private OptimalRoute route;
    private Integer attractionCount;

    public UserPreferenceRecommendation() {}

    public UserPreferenceRecommendation(String preference, OptimalRoute route, Integer attractionCount) {
        this.preference = preference;
        this.route = route;
        this.attractionCount = attractionCount;
    }

    public String getPreference() { return preference; }
    public void setPreference(String preference) { this.preference = preference; }
    public OptimalRoute getRoute() { return route; }
    public void setRoute(OptimalRoute route) { this.route = route; }
    public Integer getAttractionCount() { return attractionCount; }
    public void setAttractionCount(Integer attractionCount) { this.attractionCount = attractionCount; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String preference;
        private OptimalRoute route;
        private Integer attractionCount;
        public Builder preference(String preference) { this.preference = preference; return this; }
        public Builder route(OptimalRoute route) { this.route = route; return this; }
        public Builder attractionCount(Integer attractionCount) { this.attractionCount = attractionCount; return this; }
        public UserPreferenceRecommendation build() { return new UserPreferenceRecommendation(preference, route, attractionCount); }
    }
}
