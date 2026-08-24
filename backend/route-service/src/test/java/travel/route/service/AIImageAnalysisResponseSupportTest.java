package travel.route.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AIImageAnalysisResponseSupportTest {

    @Test
    void shouldExposeProviderDetailsWithoutInternalStatusFields() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", true);
        payload.put("label", "mountain");
        payload.put("confidence", 0.91);
        payload.put("error", null);

        Map<String, JsonNode> details = AIImageAnalysisResponseSupport.toDynamicDetails(
                payload, new ObjectMapper());

        assertFalse(details.containsKey("success"));
        assertFalse(details.containsKey("error"));
        assertEquals("mountain", details.get("label").asText());
        assertEquals(0.91, details.get("confidence").asDouble());
        assertThrows(UnsupportedOperationException.class,
                () -> details.put("unexpected", new ObjectMapper().nullNode()));
    }
}
