package travel.route.dto.ai;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public class AIItineraryGenerateRequest {

        @NotBlank(message = "目的地不能为空")
        @Size(max = 100, message = "目的地长度不能超过100个字符")
    private String destination;

        @NotNull(message = "天数不能为空")
        @Min(value = 1, message = "天数至少为1天")
        @Max(value = 30, message = "天数不能超过30天")
    private Integer days;

    @Size(max = 20, message = "偏好最多支持20个字段")
    private Map<String, JsonNode> preferences;

    @Min(value = 0, message = "预算不能为负数")
    private Integer budget;

    public AIItineraryGenerateRequest() {
    }

    public AIItineraryGenerateRequest(String destination, Integer days, Map<String, JsonNode> preferences, Integer budget) {
        this.destination = destination;
        this.days = days;
        this.preferences = preferences;
        this.budget = budget;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Map<String, JsonNode> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<String, JsonNode> preferences) {
        this.preferences = preferences;
    }

    public Integer getBudget() {
        return budget;
    }

    public void setBudget(Integer budget) {
        this.budget = budget;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String destination;
        private Integer days;
        private Map<String, JsonNode> preferences;
        private Integer budget;

        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public Builder days(Integer days) {
            this.days = days;
            return this;
        }

        public Builder preferences(Map<String, JsonNode> preferences) {
            this.preferences = preferences;
            return this;
        }

        public Builder budget(Integer budget) {
            this.budget = budget;
            return this;
        }

        public AIItineraryGenerateRequest build() {
            return new AIItineraryGenerateRequest(destination, days, preferences, budget);
        }
    }

}
