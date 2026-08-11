package travel.route.service;

import travel.route.dto.ai.AIAskQuestionResponse;
import travel.route.dto.ai.AIAttractionIntroResponse;
import travel.route.dto.ai.AIOptimizeRouteResponse;

public interface AIAssistantService {

    AIAskQuestionResponse askQuestion(String question, Integer userId);

    AIOptimizeRouteResponse optimizeRouteByAI(Integer routeId);

    AIAttractionIntroResponse getAttractionIntro(Integer attractionId);
}