import apiClient from '../utils/api';
import { DEFAULT_LIMIT_SMALL, DEFAULT_RADIUS } from '../constants';

export interface Attraction {
    id?: number;
    name: string;
    cityId?: number;
    description?: string;
    address?: string;
    rating?: number;
    price?: number;
    images?: string[];
    openingHours?: string;
    contactInfo?: string;
    tags?: string[];
    createTime?: string;
    updateTime?: string;
}

export const attractionApi = {
    getAttractions() {
        return apiClient.get<Attraction[]>('/attractions');
    },

    getAttractionById(id: number) {
        return apiClient.get<Attraction>(`/attractions/${id}`);
    },

    getAttractionsByCity(cityId: number) {
        return apiClient.get<Attraction[]>(`/attractions/city/${cityId}`);
    },

    searchAttractions(keyword: string) {
        return apiClient.get<Attraction[]>('/attractions/search', {
            params: { keyword },
        });
    },

    getRecommendations(cityId: number, limit: number = DEFAULT_LIMIT_SMALL) {
        return apiClient.get<Attraction[]>('/attractions/recommend', {
            params: { cityId, limit },
        });
    },

    createAttraction(attraction: Omit<Attraction, 'id' | 'createTime' | 'updateTime'>) {
        return apiClient.post<Attraction>('/attractions', attraction);
    },

    updateAttraction(id: number, attraction: Partial<Attraction>) {
        return apiClient.put<Attraction>(`/attractions/${id}`, attraction);
    },

    deleteAttraction(id: number) {
        return apiClient.delete(`/attractions/${id}`);
    },

    getAttractionDetail(id: number) {
        return apiClient.get<any>(`/attractions/detail/${id}`);
    },

    getAttractionImages(id: number) {
        return apiClient.get<string[]>(`/attractions/images/${id}`);
    },

    getAttractionReviews(id: number, page?: number, size?: number) {
        return apiClient.get<any[]>(`/attractions/reviews/${id}`, {
            params: { page, size },
        });
    },

    getAttractionRatingStats(id: number) {
        return apiClient.get<any>(`/attractions/rating-statistics/${id}`);
    },

    getSimilarAttractions(id: number, limit?: number) {
        return apiClient.get<any[]>(`/attractions/similar/${id}`, {
            params: { limit },
        });
    },

    getAttractionNearby(id: number, radius: number = DEFAULT_RADIUS) {
        return apiClient.get<any[]>(`/attractions/${id}/nearby`, {
            params: { radius },
        });
    },

    submitReview(attractionId: number, rating: number, content: string, userId?: number) {
        return apiClient.post<any>(`/attractions/${attractionId}/review`, { rating, content, userId });
    },
};