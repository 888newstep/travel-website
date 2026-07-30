import apiClient from '../utils/api';
import { DEFAULT_LIMIT } from '../constants';

export interface Restaurant {
    id: number;
    name: string;
    cityId: number;
    address: string;
    latitude: number;
    longitude: number;
    rating: number;
    priceLevel: string;
    averageCost: number;
    cuisineType: string;
    feature: string;
    phone: string;
    openingHours: string;
    imageUrl: string;
    description: string;
    createdAt: string;
    updatedAt: string;
}

export const restaurantApi = {
    getByCity(cityId: number) {
        return apiClient.get<Restaurant[]>(`/restaurants/city/${cityId}`);
    },

    search(cityId: number, keyword: string) {
        return apiClient.get<Restaurant[]>('/restaurants/search', {
            params: { cityId, keyword },
        });
    },

    getTopRated(cityId: number, limit: number = DEFAULT_LIMIT) {
        return apiClient.get<Restaurant[]>(`/restaurants/top-rated/${cityId}`, {
            params: { limit },
        });
    },

    getById(id: number) {
        return apiClient.get<Record<string, any>>(`/restaurants/${id}`);
    },
};