package travel.route.dto.ai;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Map;

public class AISmartItineraryOptimizeRequest {

        @NotNull(message = "routeId不能为空")
    @Positive(message = "routeId must be positive")
    private Integer routeId;

    @Size(max = 20, message = "preferences must not contain more than 20 entries")
    private Map<String, JsonNode> preferences;

    public AISmartItineraryOptimizeRequest() {
    }

    public AISmartItineraryOptimizeRequest(Integer routeId, Map<String, JsonNode> preferences) {
        this.routeId = routeId;
        this.preferences = preferences;
    }

    public Integer getRouteId() {
        return routeId;
    }

    public void setRouteId(Integer routeId) {
        this.routeId = routeId;
    }

    public Map<String, JsonNode> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<String, JsonNode> preferences) {
        this.preferences = preferences;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer routeId;
        private Map<String, JsonNode> preferences;

        public Builder routeId(Integer routeId) {
            this.routeId = routeId;
            return this;
        }

        public Builder preferences(Map<String, JsonNode> preferences) {
            this.preferences = preferences;
            return this;
        }

        public AISmartItineraryOptimizeRequest build() {
            return new AISmartItineraryOptimizeRequest(routeId, preferences);
        }
    }

}
