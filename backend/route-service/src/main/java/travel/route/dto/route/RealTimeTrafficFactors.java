package travel.route.dto.route;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 实时交通因素。 */
public class RealTimeTrafficFactors {

    @Size(max = 50, message = "拥堵路段不能超过50个")
    private List<@NotBlank(message = "拥堵路段名称不能为空") @Size(max = 100, message = "拥堵路段名称不能超过100个字符") String> congestedRoutes;

    @Size(max = 20, message = "交通扩展字段不能超过20个")
    private Map<String, JsonNode> extensions;

    public RealTimeTrafficFactors() {
    }

    public List<String> getCongestedRoutes() {
        return congestedRoutes;
    }

    public void setCongestedRoutes(List<String> congestedRoutes) {
        this.congestedRoutes = congestedRoutes;
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
            throw new IllegalArgumentException("交通扩展字段不能超过20个");
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
