package travel.route.dto.ai;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.Map;

public class AIRecommendRequest {

    private Integer userId;

    @Size(max = 100, message = "目的地长度不能超过100个字符")
    private String location;

    @Size(max = 20, message = "偏好最多支持20个字段")
    private Map<String, JsonNode> preferences;

    @Min(value = 0, message = "预算不能为负数")
    @Max(value = 10000000, message = "预算不能超过10000000")
    private Integer budget;

    @Min(value = 1, message = "时长至少为1天")
    @Max(value = 30, message = "时长不能超过30天")
    private Integer duration;

    private Integer cityId;

    public AIRecommendRequest() {
    }

    public AIRecommendRequest(Integer userId, String location, Map<String, JsonNode> preferences, Integer budget, Integer duration, Integer cityId) {
        this.userId = userId;
        this.location = location;
        this.preferences = preferences;
        this.budget = budget;
        this.duration = duration;
        this.cityId = cityId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getCityId() {
        return cityId;
    }

    public void setCityId(Integer cityId) {
        this.cityId = cityId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer userId;
        private String location;
        private Map<String, JsonNode> preferences;
        private Integer budget;
        private Integer duration;
        private Integer cityId;

        public Builder userId(Integer userId) {
            this.userId = userId;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
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

        public Builder duration(Integer duration) {
            this.duration = duration;
            return this;
        }

        public Builder cityId(Integer cityId) {
            this.cityId = cityId;
            return this;
        }

        public AIRecommendRequest build() {
            return new AIRecommendRequest(userId, location, preferences, budget, duration, cityId);
        }
    }

}
