package travel.route.dto.route;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class PersonalizedRouteRequest {

    @Valid
    @NotNull(message = "userPreferences is required")
    private PersonalizedRoutePreferences userPreferences;

    @Valid
    private PersonalizedRouteConstraints constraints;

    public PersonalizedRouteRequest() {}

    public PersonalizedRoutePreferences getUserPreferences() { return userPreferences; }
    public void setUserPreferences(PersonalizedRoutePreferences userPreferences) { this.userPreferences = userPreferences; }
    public PersonalizedRouteConstraints getConstraints() { return constraints; }
    public void setConstraints(PersonalizedRouteConstraints constraints) { this.constraints = constraints; }
}
