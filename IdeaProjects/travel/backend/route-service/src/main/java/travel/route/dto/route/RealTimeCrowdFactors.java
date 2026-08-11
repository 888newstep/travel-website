package travel.route.dto.route;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 实时客流因素。 */
public class RealTimeCrowdFactors {

    @Size(max = 50, message = "拥挤景点不能超过50个")
    private List<@Min(value = 1, message = "景点ID必须为正数") @Max(value = 100000000, message = "景点ID超出范围") Integer> crowdedAttractions;

    @Size(max = 20, message = "客流扩展字段不能超过20个")
    private Map<String, JsonNode> extensions;

    public RealTimeCrowdFactors() {
    }

    public List<Integer> getCrowdedAttractions() {
        return crowdedAttractions;
    }

    public void setCrowdedAttractions(List<Integer> crowdedAttractions) {
        this.crowdedAttractions = crowdedAttractions;
    }

    public Map<String, JsonNode> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, JsonNode> extensions) {
        mergeExtensions(extensions);
    }

    @JsonAnySetter
    public void addExtension(String name, JsonNode value) {
        if (extensions == null) {
            extensions = new LinkedHashMap<>();
        }
        if (!extensions.containsKey(name) && extensions.size() >= 20) {
            throw new IllegalArgumentException("客流扩展字段不能超过20个");
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
