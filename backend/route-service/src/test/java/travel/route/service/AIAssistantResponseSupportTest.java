package travel.route.service;

import org.junit.jupiter.api.Test;
import travel.route.dto.ai.AIAskQuestionResponse;
import travel.route.dto.ai.AIAttractionIntroResponse;
import travel.route.dto.ai.AIOptimizeRouteResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIAssistantResponseSupportTest {

    @Test
    void shouldNotInventQuestionConfidence() {
        AIAskQuestionResponse response = AIAssistantResponseSupport.toAskQuestionResponse(
                AIAssistantResponseSupport.buildAskQuestionPayload("question", "answer", "qwen"));

        assertEquals("answer", response.getAnswer());
        assertEquals("qwen", response.getSource());
        assertNull(response.getConfidence());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void shouldNotInventOptimizationScore() {
        AIOptimizeRouteResponse response = AIAssistantResponseSupport.buildOptimizationSuccess(
                12, "Use metro", "qwen");

        assertTrue(response.getSuccess());
        assertEquals(12, response.getRouteId());
        assertEquals("Use metro", response.getSuggestions().get(0).getDescription());
        assertNull(response.getOptimizedScore());
    }

    @Test
    void shouldLeaveUnstructuredAttractionFieldsEmpty() {
        Map<String, Object> payload = AIAssistantResponseSupport.buildAttractionIntroPayload(
                8, "West Lake", "provider intro", "qwen");
        AIAttractionIntroResponse response = AIAssistantResponseSupport.toAttractionIntroResponse(payload);

        assertTrue(response.getSuccess());
        assertEquals("provider intro", response.getDetailedIntro());
        assertNull(response.getBriefIntro());
        assertNull(response.getFunFacts());
        assertNull(response.getBestVisitTime());
        assertNull(response.getEstimatedDuration());
    }
}
