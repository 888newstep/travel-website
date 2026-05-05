// frontend/src/api/attraction.ts
import { apiClient } from './request';
import { API_ENDPOINTS } from './config';

export interface Attraction {
    id: number;
    name: string;
    cityId: number;
    cityName?: string;
    description: string;
    image: string;
    rating: number;
    price?: string;
    category?: string;
    address?: string;
    openingHours?: string;
}

export const attractionApi = {
    getList: () =>
        apiClient.get<Attraction[]>(API_ENDPOINTS.ATTRACTIONS),

    getDetail: (id: number) =>
        apiClient.get<Attraction>(API_ENDPOINTS.ATTRACTION_DETAIL(id)),

    getByCity: (cityId: number) =>
        apiClient.get<Attraction[]>(API_ENDPOINTS.ATTRACTIONS_BY_CITY(cityId)),

    search: (keyword: string) =>
        apiClient.get<Attraction[]>(API_ENDPOINTS.ATTRACTIONS_SEARCH, { keyword }),

    getRecommendations: (cityId: number, limit: number = 5) =>
        apiClient.get<Attraction[]>(API_ENDPOINTS.ATTRACTIONS_RECOMMEND, { cityId, limit }),
};
