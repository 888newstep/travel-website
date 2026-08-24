import apiClient from '../utils/api';

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
};
