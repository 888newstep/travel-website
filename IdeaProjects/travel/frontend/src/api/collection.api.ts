import apiClient from '../utils/api';
import { DEFAULT_PAGE, DEFAULT_PAGE_ZERO, DEFAULT_PAGE_SIZE_SMALL } from '../constants';

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

    updateCollectionNotes(collectionId: number, notes: string) {
        return apiClient.put<boolean>(`/v1/route-collections/${collectionId}/notes`, { notes });
    },

    updatePublicStatus(collectionId: number, isPublic: boolean) {
        return apiClient.put<boolean>(`/v1/route-collections/${collectionId}/public-status`, { isPublic });
    },

    getPublicCollections(page: number = DEFAULT_PAGE, size: number = DEFAULT_PAGE_SIZE_SMALL) {
        return apiClient.get<RouteCollection[]>('/v1/route-collections/public', {
            params: { page, size },
        });
    },

    removeCollection(routeId: number) {
        return apiClient.delete('/v1/route-collections/remove', {
            params: { routeId },
        });
    },

    getCollectionCategories(userId: number) {
        return apiClient.get<string[]>(`/v1/route-collections/categories/${userId}`);
    },

    getCollectionsByCategory(
        userId: number,
        category: string,
        page: number = DEFAULT_PAGE_ZERO,
        size: number = DEFAULT_PAGE_SIZE_SMALL
    ) {
        return apiClient.get<RouteCollection[]>(
            `/v1/route-collections/category/${userId}/${category}`,
            {
                params: { page, size },
            }
        );
    },

    batchRemoveCollections(ids: number[]) {
        return apiClient.delete('/v1/route-collections/batch-remove', {
            data: ids,
        });
    },
};
