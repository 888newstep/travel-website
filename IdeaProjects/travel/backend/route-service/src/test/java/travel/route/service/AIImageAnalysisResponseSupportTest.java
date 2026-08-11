package travel.route.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import travel.route.dto.ai.AIImageAnalysisResponse;
import travel.route.dto.ai.AIRecognizeAttractionResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIImageAnalysisResponseSupportTest {

    @Test
    void shouldBuildAnalysisFailureResponse() {
        AIImageAnalysisResponse response = AIImageAnalysisResponseSupport.buildAnalysisFailure("io error");

        assertFalse(response.getSuccess());
        assertEquals("io error", response.getError());
    }

    @Test
    void shouldConvertDynamicImageDetailsToJsonNodesAndDropMetadata() {
        Map<String, Object> payload = Map.of(
                "success", true,
                "error", "ignored",
                "score", 0.92,
                "labels", List.of("mountain", "lake"),
                "nested", Map.of("name", "West Lake")
        );

        Map<String, JsonNode> details = AIImageAnalysisResponseSupport.toDynamicDetails(
                payload, new ObjectMapper());

        assertEquals(3, details.size());
        assertFalse(details.containsKey("success"));
        assertFalse(details.containsKey("error"));
        assertEquals(0.92, details.get("score").asDouble());
        assertEquals("mountain", details.get("labels").get(0).asText());
        assertEquals("West Lake", details.get("nested").get("name").asText());
    }

    @Test
    void shouldMapImageAnalysisPayloadToDto() {
        Map<String, Object> payload = Map.of(
                "success", true,
                "analysisType", "comprehensive",
                "timestamp", LocalDateTime.now(),
                "contentAnalysis", Map.of(
                        "scene", "nature",
                        "objects", List.of("mountain", "lake"),
                        "colors", List.of("blue")
                ),
                "qualityAnalysis", Map.of(
                        "sharpness", 0.9,
                        "brightness", 0.8,
                        "contrast", 0.7,
                        "composition", 0.85,
                        "overallQuality", 0.83
                ),
                "recommendations", List.of(Map.of(
                        "type", "route",
                        "name", "nature-route",
                        "items", List.of("trail"),
                        "tips", List.of("bring water")
                )),
                "confidence", 0.91
        );

        AIImageAnalysisResponse response = AIImageAnalysisResponseSupport.toImageAnalysisResponse(payload);

        assertTrue(response.getSuccess());
        assertEquals("comprehensive", response.getAnalysisType());
        assertEquals("nature", response.getContentAnalysis().getMainSubject());
        assertEquals("nature", response.getContentAnalysis().getSceneType());
        assertEquals(List.of("blue"), response.getContentAnalysis().getDominantColors());
        assertEquals(0.83, response.getQualityAnalysis().getOverallQuality());
        assertEquals(1, response.getRecommendations().size());
        assertEquals("nature-route", response.getRecommendations().get(0).getName());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void shouldBuildRecognizeFailureResponse() {
        AIRecognizeAttractionResponse response = AIImageAnalysisResponseSupport.buildRecognizeFailure("bad image");

        assertFalse(response.getSuccess());
        assertEquals("bad image", response.getError());
    }

    @Test
    void shouldMapRecognizePayloadToDto() {
        Map<String, Object> topMatch = Map.of(
                "name", "West Lake",
                "confidence", 0.97,
                "location", "Hangzhou",
                "description", "classic",
                "rating", 4.9
        );
        Map<String, Object> payload = Map.of(
                "success", true,
                "type", "landmark-recognition",
                "timestamp", LocalDateTime.now(),
                "attractions", List.of(topMatch),
                "topMatch", topMatch
        );

        AIRecognizeAttractionResponse response = AIImageAnalysisResponseSupport.toRecognizeAttractionResponse(payload);

        assertTrue(response.getSuccess());
        assertEquals("landmark-recognition", response.getType());
        assertEquals(1, response.getAttractions().size());
        assertEquals("West Lake", response.getTopMatch().getName());
        assertEquals(0.97, response.getTopMatch().getConfidence());
    }
}
