package travel.route.dto.ai;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 路线规划约束条件。
 */
public class AIPlanRouteConstraints {

    @Min(value = 1, message = "每日可用时长至少为1小时")
    @Max(value = 24, message = "每日可用时长不能超过24小时")
    private Integer maxDailyHours;

    @Size(max = 50, message = "必游景点不能超过50个")
    private List<@NotBlank(message = "必游景点名称不能为空") @Size(max = 100, message = "景点名称不能超过100个字符") String> mustVisitAttractions;

    @Size(max = 50, message = "避开景点不能超过50个")
    private List<@NotBlank(message = "避开景点名称不能为空") @Size(max = 100, message = "景点名称不能超过100个字符") String> avoidAttractions;

    @Valid
    @Size(max = 20, message = "固定时间窗口不能超过20个")
    private List<@Valid AITimeWindow> fixedTimeWindows;

    @Size(max = 20, message = "扩展字段不能超过20个")
    private Map<String, JsonNode> extensions;

    public AIPlanRouteConstraints() {
    }

    public AIPlanRouteConstraints(Integer maxDailyHours, List<String> mustVisitAttractions,
                                  List<String> avoidAttractions, List<AITimeWindow> fixedTimeWindows,
                                  Map<String, JsonNode> extensions) {
        this.maxDailyHours = maxDailyHours;
        this.mustVisitAttractions = mustVisitAttractions;
        this.avoidAttractions = avoidAttractions;
        this.fixedTimeWindows = fixedTimeWindows;
        this.extensions = extensions;
    }

    public Integer getMaxDailyHours() {
        return maxDailyHours;
    }

    public void setMaxDailyHours(Integer maxDailyHours) {
        this.maxDailyHours = maxDailyHours;
    }

    public List<String> getMustVisitAttractions() {
        return mustVisitAttractions;
    }

    public void setMustVisitAttractions(List<String> mustVisitAttractions) {
        this.mustVisitAttractions = mustVisitAttractions;
    }

    public List<String> getAvoidAttractions() {
        return avoidAttractions;
    }

    public void setAvoidAttractions(List<String> avoidAttractions) {
        this.avoidAttractions = avoidAttractions;
    }

    public List<AITimeWindow> getFixedTimeWindows() {
        return fixedTimeWindows;
    }

    public void setFixedTimeWindows(List<AITimeWindow> fixedTimeWindows) {
        this.fixedTimeWindows = fixedTimeWindows;
    }

    public Map<String, JsonNode> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, JsonNode> extensions) {
        mergeExtensions(extensions);
    }

    /**
     * 将未建模的约束保留在受控扩展集合中，避免 Jackson 静默丢弃客户端字段。
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
        private Integer maxDailyHours;
        private List<String> mustVisitAttractions;
        private List<String> avoidAttractions;
        private List<AITimeWindow> fixedTimeWindows;
        private Map<String, JsonNode> extensions;

        public Builder maxDailyHours(Integer maxDailyHours) {
            this.maxDailyHours = maxDailyHours;
            return this;
        }

        public Builder mustVisitAttractions(List<String> mustVisitAttractions) {
            this.mustVisitAttractions = mustVisitAttractions;
            return this;
        }

        public Builder avoidAttractions(List<String> avoidAttractions) {
            this.avoidAttractions = avoidAttractions;
            return this;
        }

        public Builder fixedTimeWindows(List<AITimeWindow> fixedTimeWindows) {
            this.fixedTimeWindows = fixedTimeWindows;
            return this;
        }

        public Builder extensions(Map<String, JsonNode> extensions) {
            this.extensions = extensions;
            return this;
        }

        public AIPlanRouteConstraints build() {
            return new AIPlanRouteConstraints(maxDailyHours, mustVisitAttractions,
                    avoidAttractions, fixedTimeWindows, extensions);
        }
    }
}
