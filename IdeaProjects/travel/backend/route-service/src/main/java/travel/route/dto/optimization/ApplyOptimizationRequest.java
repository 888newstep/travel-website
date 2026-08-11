package travel.route.dto.optimization;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApplyOptimizationRequest {
    private static final int MAX_EXTENSION_FIELDS = 20;

    @NotNull(message = "routeId不能为空")
    @Positive(message = "routeId必须为正数")
    private Integer routeId;
    @Positive(message = "suggestionId必须为正数")
    private Integer suggestionId;
    private String optimizationType;
    private Map<String, JsonNode> parameters;
    private Map<String, JsonNode> suggestion;
    @Size(max = MAX_EXTENSION_FIELDS, message = "extensions must contain at most 20 fields")
    private Map<String, JsonNode> extensions;

    public ApplyOptimizationRequest() {}
    public ApplyOptimizationRequest(Integer routeId, String optimizationType, Map<String, JsonNode> parameters) {
        this.routeId = routeId;
        this.optimizationType = optimizationType;
        this.parameters = parameters;
    }
    public Integer getRouteId() { return routeId; }
    public void setRouteId(Integer routeId) { this.routeId = routeId; }
    public String getOptimizationType() { return optimizationType; }
    public void setOptimizationType(String optimizationType) { this.optimizationType = optimizationType; }
    public Integer getSuggestionId() { return suggestionId; }
    public void setSuggestionId(Integer suggestionId) { this.suggestionId = suggestionId; }
    public Map<String, JsonNode> getParameters() { return parameters; }
    public void setParameters(Map<String, JsonNode> parameters) { this.parameters = parameters; }
    public Map<String, JsonNode> getSuggestion() { return suggestion; }
    public void setSuggestion(Map<String, JsonNode> suggestion) { this.suggestion = suggestion; }
    public Map<String, JsonNode> getExtensions() { return extensions; }
    public void setExtensions(Map<String, JsonNode> extensions) { mergeExtensions(extensions); }

    @JsonAnySetter
    public void addExtension(String name, JsonNode value) {
        if (extensions == null) {
            extensions = new LinkedHashMap<>();
        }
        if (!extensions.containsKey(name) && extensions.size() >= MAX_EXTENSION_FIELDS) {
            throw new IllegalArgumentException("extensions must contain at most 20 fields");
        }
        extensions.put(name, value);
    }

    private void mergeExtensions(Map<String, JsonNode> additionalExtensions) {
        if (additionalExtensions == null || additionalExtensions.isEmpty()) {
            return;
        }
        for (Map.Entry<String, JsonNode> entry : additionalExtensions.entrySet()) {
            addExtension(entry.getKey(), entry.getValue());
        }
    }
}
