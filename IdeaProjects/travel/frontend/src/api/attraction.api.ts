import apiClient from '../utils/api';

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

    getRecommendations(cityId: number, limit: number = 5) {
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
};
