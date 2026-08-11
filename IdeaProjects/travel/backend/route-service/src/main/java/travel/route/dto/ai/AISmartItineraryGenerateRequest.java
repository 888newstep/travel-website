package travel.route.dto.ai;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.Map;

public class AISmartItineraryGenerateRequest {

    private Integer userId;

        @NotNull(message = "cityId不能为空")
    private Integer cityId;

    @Min(value = 1, message = "days must be at least 1")
    @Max(value = 30, message = "days must not exceed 30")
    private Integer days;

    private Double budget;

    @Size(max = 20, message = "preferences must not contain more than 20 entries")
    private Map<String, JsonNode> preferences;

    public AISmartItineraryGenerateRequest() {
    }

    public AISmartItineraryGenerateRequest(Integer userId, Integer cityId, Integer days, Double budget, Map<String, JsonNode> preferences) {
        this.userId = userId;
        this.cityId = cityId;
        this.days = days;
        this.budget = budget;
        this.preferences = preferences;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getCityId() {
        return cityId;
    }

    public void setCityId(Integer cityId) {
        this.cityId = cityId;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Double getBudget() {
        return budget;
    }

    public void setBudget(Double budget) {
        this.budget = budget;
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
        private Integer userId;
        private Integer cityId;
        private Integer days;
        private Double budget;
        private Map<String, JsonNode> preferences;

        public Builder userId(Integer userId) {
            this.userId = userId;
            return this;
        }

        public Builder cityId(Integer cityId) {
            this.cityId = cityId;
            return this;
        }

        public Builder days(Integer days) {
            this.days = days;
            return this;
        }

        public Builder budget(Double budget) {
            this.budget = budget;
            return this;
        }

        public Builder preferences(Map<String, JsonNode> preferences) {
            this.preferences = preferences;
            return this;
        }

        public AISmartItineraryGenerateRequest build() {
            return new AISmartItineraryGenerateRequest(userId, cityId, days, budget, preferences);
        }
    }

}
