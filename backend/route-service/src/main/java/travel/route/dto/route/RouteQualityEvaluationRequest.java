package travel.route.dto.route;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 路线质量评估的扩展配置。
 *
 * <p>当前评估算法使用固定权重，暂不虚构未实现的评分参数；兼容性配置保留在受控扩展字段中。</p>
 */
public class RouteQualityEvaluationRequest {

    @Size(max = 20, message = "评估扩展字段不能超过20个")
    private Map<String, JsonNode> extensions;

    public RouteQualityEvaluationRequest() {
    }

    public Map<String, JsonNode> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, JsonNode> extensions) {
        mergeExtensions(extensions);
    }

    /** 将历史请求中的动态参数保留在显式扩展集合中。 */
    @JsonAnySetter
    public void addExtension(String name, JsonNode value) {
        if (extensions == null) {
            extensions = new LinkedHashMap<>();
        }
        if (!extensions.containsKey(name) && extensions.size() >= 20) {
            throw new IllegalArgumentException("评估扩展字段不能超过20个");
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
