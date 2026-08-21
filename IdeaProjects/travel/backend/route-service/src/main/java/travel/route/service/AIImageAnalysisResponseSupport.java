package travel.route.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AIImageAnalysisResponseSupport {

    private AIImageAnalysisResponseSupport() {
    }

    /**
     * 将第三方 AI 原始字段转换为只读动态 JSON，并移除内部状态字段。
     */
    public static Map<String, JsonNode> toDynamicDetails(Map<String, Object> result, ObjectMapper objectMapper) {
        if (result == null || result.isEmpty()) {
            return Map.of();
        }
        ObjectNode root = objectMapper.valueToTree(result);
        root.remove("success");
        root.remove("error");

        Map<String, JsonNode> details = new LinkedHashMap<>();
        root.fields().forEachRemaining(entry -> details.put(entry.getKey(), entry.getValue()));
        return Collections.unmodifiableMap(details);
    }
}
