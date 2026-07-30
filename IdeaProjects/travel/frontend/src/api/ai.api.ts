import apiClient from '../utils/api';

export interface AIChatRequest {
    message: string;
    context?: Record<string, any>;
}

export interface AIChatResponse {
    response: string;
    suggestions?: string[];
}

export interface AIImageAnalysis {
    imageUrl: string;
    analysisType?: string;
}

export const aiApi = {
    chat(data: AIChatRequest) {
        return apiClient.post<AIChatResponse>('/ai/chat', data);
    },

    getTravelRecommendation(data: {
        userId?: number;
        location?: string;
        preferences?: Record<string, any>;
        budget?: number;
        duration?: number;
    }) {
        return apiClient.post<any>('/ai/recommend', data);
    },

    analyzeImage(data: AIImageAnalysis) {
        return apiClient.post<any>('/ai/image-analysis', data);
    },

    getImageAnalysisTypes() {
        return apiClient.get<{ value: string; label: string }[]>('/ai/image-analysis/types');
    },

    generateItinerary(data: {
        destination: string;
        days: number;
        preferences?: Record<string, any>;
        budget?: number;
    }) {
        return apiClient.post<any>('/ai/itinerary/generate', data);
    },

    multimodalQuery(data: {
        text?: string;
        image?: string;
        audio?: string;
        context?: Record<string, any>;
    }) {
        return apiClient.post<any>('/ai/multimodal/query', data);
    },

    smartAssistant(query: string, context?: Record<string, any>) {
        return apiClient.post<AIChatResponse>('/ai/assistant/chat', {
            query,
            context,
        });
    },

    advancedChatbot(message: string, conversationId?: string) {
        return apiClient.post<AIChatResponse>('/ai/advanced/chat', {
            message,
            conversationId,
        });
    },

    getBudgetEstimation(data: Record<string, any>) {
        return apiClient.post<any>('/ai/advanced/budget', data);
    },

    planSmartRoute(data: Record<string, any>) {
        return apiClient.post<any>('/ai/advanced/plan', data);
    },

    getSafetyAdvice(cityId: number) {
        return apiClient.get<any>(`/ai/advanced/safety/${cityId}`);
    },

    processVoice(data: { audioData: string | null; text: string }) {
        return apiClient.post<any>('/ai/advanced/voice', data);
    },
};
