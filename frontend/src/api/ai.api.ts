import apiClient from '../utils/api';

const AI_BASE = '/ai';
const AI_ADVANCED_BASE = `${AI_BASE}/advanced`;

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
        return apiClient.post<AIChatResponse>(`${AI_BASE}/chat`, data);
    },

    getTravelRecommendation(data: {
        userId?: number;
        location?: string;
        preferences?: Record<string, any>;
        budget?: number;
        duration?: number;
    }) {
        return apiClient.post<any>(`${AI_BASE}/recommend`, data);
    },

    analyzeImage(data: AIImageAnalysis) {
        return apiClient.post<any>(`${AI_BASE}/image-analysis`, data);
    },

    getImageAnalysisTypes() {
        return apiClient.get<{ value: string; label: string }[]>(`${AI_BASE}/image-analysis/types`);
    },

    generateItinerary(data: {
        destination: string;
        days: number;
        preferences?: Record<string, any>;
        budget?: number;
    }) {
        return apiClient.post<any>(`${AI_BASE}/itinerary/generate`, data);
    },

    smartAssistant(query: string, context?: Record<string, any>) {
        return apiClient.post<AIChatResponse>(`${AI_BASE}/assistant/chat`, {
            query,
            context,
        });
    },

    planSmartRoute(data: Record<string, any>) {
        return apiClient.post<any>(`${AI_ADVANCED_BASE}/plan`, data);
    },
};
