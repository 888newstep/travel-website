import apiClient from '../utils/api';
import { DEFAULT_LIMIT_SMALL } from '../constants';

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

    getAttractionNearby(id: number) {
        return apiClient.get<any[]>(`/attractions/${id}/nearby`);
    },

    submitReview(attractionId: number, rating: number, content: string) {
        return apiClient.post<any>(`/attractions/${attractionId}/review`, { rating, content });
    },
};
