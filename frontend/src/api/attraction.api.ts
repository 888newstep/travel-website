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

export interface AMapPlaceCandidate {
    poiId?: string;
    name: string;
    type?: string;
    address?: string;
    province?: string;
    city?: string;
    district?: string;
    longitude: number;
    latitude: number;
    distanceMeters?: number;
    imageUrl?: string;
    source: 'amap';
}

export interface AMapPlaceSearchResponse {
    dataAvailable: boolean;
    source: 'amap';
    keyword: string;
    city: string;
    page: number;
    total: number;
    items: AMapPlaceCandidate[];
    message?: string;
}

export type AMapFacilityCategory = 'restaurant' | 'parking' | 'restroom' | 'transit';

export interface AMapNearbyFacilitiesResponse {
    dataAvailable: boolean;
    source: 'amap';
    attractionId: number;
    category: AMapFacilityCategory;
    categoryLabel: string;
    radiusMeters: number;
    items: AMapPlaceCandidate[];
    message?: string;
}

export interface AMapWeatherResponse {
    dataAvailable: boolean;
    source: 'amap';
    attractionId: number;
    city?: string;
    weather?: string;
    temperature?: number;
    windDirection?: string;
    windPower?: string;
    humidity?: string;
    message?: string;
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

    searchExternalPlaces(keyword: string, city: string = '', page: number = 1) {
        return apiClient.get<AMapPlaceSearchResponse>('/attractions/external-search', {
            params: { keyword, city, page },
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

    getNearbyFacilities(
        id: number,
        category: AMapFacilityCategory = 'restaurant',
        radiusMeters: number = 1000,
    ) {
        return apiClient.get<AMapNearbyFacilitiesResponse>(`/attractions/${id}/nearby-facilities`, {
            params: { category, radiusMeters },
        });
    },

    getAttractionWeather(id: number) {
        return apiClient.get<AMapWeatherResponse>(`/attractions/${id}/weather`);
    },

    submitReview(attractionId: number, rating: number, content: string) {
        return apiClient.post<any>(`/attractions/${attractionId}/review`, { rating, content });
    },
};
