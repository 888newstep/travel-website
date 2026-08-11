package travel.route.dto.route;

import travel.route.algorithm.RoutePlanAlgorithm.OptimalRoute;

public class PersonalizedRouteResult {
    private OptimalRoute route;
    private PersonalizedRoutePreferences userPreferences;
    private PersonalizedRouteConstraints constraints;
    private Integer attractionCount;
    private Integer cityId;
    private Integer days;
    private String transportPreference;

    public PersonalizedRouteResult() {}

    public OptimalRoute getRoute() { return route; }
    public void setRoute(OptimalRoute route) { this.route = route; }
    public PersonalizedRoutePreferences getUserPreferences() { return userPreferences; }
    public void setUserPreferences(PersonalizedRoutePreferences userPreferences) { this.userPreferences = userPreferences; }
    public PersonalizedRouteConstraints getConstraints() { return constraints; }
    public void setConstraints(PersonalizedRouteConstraints constraints) { this.constraints = constraints; }
    public Integer getAttractionCount() { return attractionCount; }
    public void setAttractionCount(Integer attractionCount) { this.attractionCount = attractionCount; }
    public Integer getCityId() { return cityId; }
    public void setCityId(Integer cityId) { this.cityId = cityId; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public String getTransportPreference() { return transportPreference; }
    public void setTransportPreference(String transportPreference) { this.transportPreference = transportPreference; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final PersonalizedRouteResult r = new PersonalizedRouteResult();
        public Builder route(OptimalRoute route) { r.route = route; return this; }
        public Builder userPreferences(PersonalizedRoutePreferences userPreferences) { r.userPreferences = userPreferences; return this; }
        public Builder constraints(PersonalizedRouteConstraints constraints) { r.constraints = constraints; return this; }
        public Builder attractionCount(Integer attractionCount) { r.attractionCount = attractionCount; return this; }
        public Builder cityId(Integer cityId) { r.cityId = cityId; return this; }
        public Builder days(Integer days) { r.days = days; return this; }
        public Builder transportPreference(String transportPreference) { r.transportPreference = transportPreference; return this; }
        public PersonalizedRouteResult build() { return r; }
    }
}
