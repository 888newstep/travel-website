import apiClient from '../utils/api';
import { DEFAULT_PAGE, DEFAULT_PAGE_ZERO, DEFAULT_PAGE_SIZE_SMALL } from '../constants';

export interface RouteCollection {
    id?: number;
    userId: number;
    routeId: number;
    note?: string;
    category?: string;
    isPublic?: boolean;
    createTime?: string;
}

export interface RouteCollectionVO extends RouteCollection {
    routeTitle?: string;
    routeDescription?: string;
    routeImage?: string;
}

export const collectionApi = {
    collectRoute(routeId: number, userId: number) {
        return apiClient.post<boolean>('/v1/route-collections/collect', { routeId, userId });
    },

    uncollectRoute(routeId: number, userId: number) {
        return apiClient.delete<boolean>('/v1/route-collections/uncollect', {
            data: { routeId, userId },
        });
    },

    toggleCollection(routeId: number, userId: number) {
        return apiClient.post<{ collected: boolean }>('/v1/route-collections/toggle', { routeId, userId });
    },

    getUserCollections(userId: number, page: number = DEFAULT_PAGE, size: number = DEFAULT_PAGE_SIZE_SMALL) {
        return apiClient.get<RouteCollectionVO[]>(`/v1/route-collections/list/${userId}`, {
            params: { page, size },
        });
    },

    checkCollected(userId: number, routeId: number) {
        return apiClient.get<boolean>('/v1/route-collections/check', {
            params: { userId, routeId },
        });
    },

    updateCollectionNotes(collectionId: number, userId: number, notes: string) {
        return apiClient.put<boolean>(`/v1/route-collections/${collectionId}/notes`, { userId, notes });
    },

    updatePublicStatus(collectionId: number, userId: number, isPublic: boolean) {
        return apiClient.put<boolean>(`/v1/route-collections/${collectionId}/public-status`, { userId, isPublic });
    },

    getPublicCollections(page: number = DEFAULT_PAGE, size: number = DEFAULT_PAGE_SIZE_SMALL) {
        return apiClient.get<RouteCollection[]>('/v1/route-collections/public', {
            params: { page, size },
        });
    },

    addCollection(collection: Omit<RouteCollection, 'id' | 'createTime'>) {
        return apiClient.post<RouteCollection>('/v1/route-collections/add', collection);
    },

    removeCollection(userId: number, routeId: number) {
        return apiClient.delete('/v1/route-collections/remove', {
            params: { userId, routeId },
        });
    },

    updateCollectionNote(id: number, note: string) {
        return apiClient.put('/v1/route-collections/update-note', null, {
            params: { id, note },
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
