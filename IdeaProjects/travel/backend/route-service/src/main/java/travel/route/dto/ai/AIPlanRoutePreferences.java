package travel.route.dto.ai;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 路线规划的用户偏好。
 *
 * <p>核心字段显式建模，动态字段只能放在 extensions 中，避免业务代码依赖裸 Map。</p>
 */
public class AIPlanRoutePreferences {

    @Size(max = 100, message = "目的地长度不能超过100个字符")
    private String destination;

    @Min(value = 1, message = "出行天数至少为1天")
    @Max(value = 30, message = "出行天数不能超过30天")
    private Integer days;

    @Size(max = 32, message = "出行风格长度不能超过32个字符")
    private String travelStyle;

    @Size(max = 32, message = "交通偏好长度不能超过32个字符")
    private String transportPreference;

    @Min(value = 0, message = "预算不能为负数")
    @Max(value = 1000000, message = "预算不能超过1000000元")
    private Integer budget;

    @Size(max = 20, message = "扩展字段不能超过20个")
    private Map<String, JsonNode> extensions;

    public AIPlanRoutePreferences() {
    }

    public AIPlanRoutePreferences(String destination, Integer days, String travelStyle,
                                  String transportPreference, Integer budget,
                                  Map<String, JsonNode> extensions) {
        this.destination = destination;
        this.days = days;
        this.travelStyle = travelStyle;
        this.transportPreference = transportPreference;
        this.budget = budget;
        this.extensions = extensions;
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

    public String getTravelStyle() {
        return travelStyle;
    }

    public void setTravelStyle(String travelStyle) {
        this.travelStyle = travelStyle;
    }

    public String getTransportPreference() {
        return transportPreference;
    }

    public void setTransportPreference(String transportPreference) {
        this.transportPreference = transportPreference;
    }

    public Integer getBudget() {
        return budget;
    }

    public void setBudget(Integer budget) {
        this.budget = budget;
    }

    public Map<String, JsonNode> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, JsonNode> extensions) {
        mergeExtensions(extensions);
    }

    /**
     * 兼容历史请求中的额外字段，但不让它们重新污染核心业务字段模型。
     */
    @JsonAnySetter
    public void addExtension(String name, JsonNode value) {
        if (extensions == null) {
            extensions = new LinkedHashMap<>();
        }
        if (!extensions.containsKey(name) && extensions.size() >= 20) {
            throw new IllegalArgumentException("扩展字段不能超过20个");
        }
        extensions.put(name, value);
    }

    private void mergeExtensions(Map<String, JsonNode> additionalExtensions) {
        if (additionalExtensions == null || additionalExtensions.isEmpty()) {
            return;
        }
        if (extensions == null) {
            extensions = new LinkedHashMap<>();
        }
        for (Map.Entry<String, JsonNode> entry : additionalExtensions.entrySet()) {
            addExtension(entry.getKey(), entry.getValue());
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String destination;
        private Integer days;
        private String travelStyle;
        private String transportPreference;
        private Integer budget;
        private Map<String, JsonNode> extensions;

        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public Builder days(Integer days) {
            this.days = days;
            return this;
        }

        public Builder travelStyle(String travelStyle) {
            this.travelStyle = travelStyle;
            return this;
        }

        public Builder transportPreference(String transportPreference) {
            this.transportPreference = transportPreference;
            return this;
        }

        public Builder budget(Integer budget) {
            this.budget = budget;
            return this;
        }

        public Builder extensions(Map<String, JsonNode> extensions) {
            this.extensions = extensions;
            return this;
        }

        public AIPlanRoutePreferences build() {
            return new AIPlanRoutePreferences(destination, days, travelStyle,
                    transportPreference, budget, extensions);
        }
    }
}
