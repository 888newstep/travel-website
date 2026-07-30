import apiClient from '../utils/api';

export interface City {
    id: number;
    name: string;
    country: string;
    province: string;
    latitude: number;
    longitude: number;
    description: string;
    coverImage: string;
}

export const cityApi = {
    getAllCities() {
        return apiClient.get<City[]>('/cities');
    },

    getCityById(id: number) {
        return apiClient.get<City>(`/cities/${id}`);
    },
};
