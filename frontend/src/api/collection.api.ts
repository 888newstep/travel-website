import apiClient from '../utils/api';
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE_SMALL } from '../constants';

export interface RouteCollection {
    id?: number;
    userId: number;
    routeId: number;
    notes?: string;
    category?: string;
    isPublic?: boolean;
    collectionTime?: string;
}

export interface RouteCollectionVO extends RouteCollection {
    routeTitle?: string;
    routeCoverImage?: string;
    routeDurationDays?: number;
    routeDifficulty?: string;
}

export const collectionApi = {
    toggleCollection(routeId: number) {
        return apiClient.post<{ collected: boolean }>('/v1/route-collections/toggle', { routeId });
    },

    getUserCollections(userId: number, page: number = DEFAULT_PAGE, size: number = DEFAULT_PAGE_SIZE_SMALL) {
        return apiClient.get<RouteCollectionVO[]>(`/v1/route-collections/list/${userId}`, {
            params: { page, size },
        });
    },

    checkCollected(routeId: number) {
        return apiClient.get<boolean>('/v1/route-collections/check', {
            params: { routeId },
        });
    },

    removeCollection(routeId: number) {
        return apiClient.delete('/v1/route-collections/remove', {
            params: { routeId },
        });
    },

};
