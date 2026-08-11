package travel.route.service;

import org.junit.jupiter.api.Test;
import travel.common.entity.travel_recommendation.Attraction;
import travel.route.dto.ai.AIAskQuestionResponse;
import travel.route.dto.ai.AIAttractionIntroResponse;
import travel.route.dto.ai.AIOptimizeRouteResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIAssistantResponseSupportTest {

    @Test
    void shouldBuildFallbackAskQuestionPayload() {
        Map<String, Object> payload = AIAssistantResponseSupport.buildFallbackAskQuestionPayload("\u4eca\u5929\u5929\u6c14\u600e\u4e48\u6837");

        AIAskQuestionResponse response = AIAssistantResponseSupport.toAskQuestionResponse(payload);

        assertEquals("fallback", response.getSource());
        assertEquals(0.70, response.getConfidence());
        assertTrue(response.getAnswer().contains("\u5929\u6c14\u9884\u62a5"));
        assertNotNull(response.getTimestamp());
    }

    @Test
    void shouldBuildOptimizationFallbackAsSuccessfulDegrade() {
        AIOptimizeRouteResponse response = AIAssistantResponseSupport.buildOptimizationFallback(12, "timeout");

        assertTrue(response.getSuccess());
        assertEquals(12, response.getRouteId());
        assertEquals("fallback", response.getSource());
        assertEquals(75, response.getOptimizedScore());
        assertEquals(1, response.getSuggestions().size());
        assertTrue(response.getMessage().contains("timeout"));
    }

    @Test
    void shouldBuildRouteNotFoundResponse() {
        AIOptimizeRouteResponse response = AIAssistantResponseSupport.buildRouteNotFound(99);

        assertFalse(response.getSuccess());
        assertEquals(99, response.getRouteId());
        assertEquals("\u8def\u7ebf\u4e0d\u5b58\u5728", response.getMessage());
    }

    @Test
    void shouldBuildAttractionIntroFallbackPayload() {
        Attraction attraction = new Attraction();
        attraction.setId(8);
        attraction.setName("West Lake");
        attraction.setDescription("\u57fa\u7840\u4ecb\u7ecd");

        Map<String, Object> payload = AIAssistantResponseSupport.buildAttractionIntroFallbackPayload(attraction, 8, "provider down");
        AIAttractionIntroResponse response = AIAssistantResponseSupport.toAttractionIntroResponse(payload);

        assertTrue(response.getSuccess());
        assertEquals(8, response.getAttractionId());
        assertEquals("fallback", response.getSource());
        assertEquals("\u57fa\u7840\u4ecb\u7ecd", response.getBriefIntro());
        assertTrue(response.getMessage().contains("provider down"));
    }
}