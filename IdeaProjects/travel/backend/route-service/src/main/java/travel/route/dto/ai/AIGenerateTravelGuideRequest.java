package travel.route.dto.ai;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public class AIGenerateTravelGuideRequest {

    @NotNull(message = "cityId不能为空")
    @Min(value = 1, message = "cityId必须为正数")
    private Integer cityId;

    @NotNull(message = "days不能为空")
    @Min(value = 1, message = "天数至少为1天")
    @Max(value = 30, message = "天数不能超过30天")
    private Integer days;

    /**
     * 偏好字段允许扩展，但使用 JsonNode 保留 JSON 类型，避免 Object 在服务链路中无约束传播。
     */
    @Size(max = 20, message = "偏好最多支持20个字段")
    private Map<String, JsonNode> preferences;

    public AIGenerateTravelGuideRequest() {
    }

    public AIGenerateTravelGuideRequest(Integer cityId, Integer days, Map<String, JsonNode> preferences) {
        this.cityId = cityId;
        this.days = days;
        this.preferences = preferences;
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
        private Integer cityId;
        private Integer days;
        private Map<String, JsonNode> preferences;

        public Builder cityId(Integer cityId) {
            this.cityId = cityId;
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

        public AIGenerateTravelGuideRequest build() {
            return new AIGenerateTravelGuideRequest(cityId, days, preferences);
        }
    }

}
