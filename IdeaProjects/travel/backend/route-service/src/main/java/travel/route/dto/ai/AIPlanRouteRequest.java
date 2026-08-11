package travel.route.dto.ai;

import jakarta.validation.Valid;

public class AIPlanRouteRequest {

    @Valid
    private AIPlanRoutePreferences preferences;

    @Valid
    private AIPlanRouteConstraints constraints;

    public AIPlanRouteRequest() {
    }

    public AIPlanRouteRequest(AIPlanRoutePreferences preferences, AIPlanRouteConstraints constraints) {
        this.preferences = preferences;
        this.constraints = constraints;
    }

    public AIPlanRoutePreferences getPreferences() {
        return preferences;
    }

    public void setPreferences(AIPlanRoutePreferences preferences) {
        this.preferences = preferences;
    }

    public AIPlanRouteConstraints getConstraints() {
        return constraints;
    }

    public void setConstraints(AIPlanRouteConstraints constraints) {
        this.constraints = constraints;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AIPlanRoutePreferences preferences;
        private AIPlanRouteConstraints constraints;

        public Builder preferences(AIPlanRoutePreferences preferences) {
            this.preferences = preferences;
            return this;
        }

        public Builder constraints(AIPlanRouteConstraints constraints) {
            this.constraints = constraints;
            return this;
        }

        public AIPlanRouteRequest build() {
            return new AIPlanRouteRequest(preferences, constraints);
        }
    }

}
