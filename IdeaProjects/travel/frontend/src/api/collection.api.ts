import apiClient from '../utils/api';

export interface RouteCollection {
    id: number;
    userId: number;
    routeId: number;
    note?: string;
    category?: string;
    createTime?: string;
}

export const collectionApi = {
    addCollection(collection: Omit<RouteCollection, 'id' | 'createTime'>) {
        return apiClient.post<RouteCollection>('/route-collection/add', collection);
    },

    removeCollection(userId: number, routeId: number) {
        return apiClient.delete('/route-collection/remove', {
            params: { userId, routeId },
        });
    },

    getUserCollections(userId: number, page: number = 0, size: number = 10) {
        return apiClient.get<RouteCollection[]>(`/route-collection/list/${userId}`, {
            params: { page, size },
        });
    },

    checkCollected(userId: number, routeId: number) {
        return apiClient.get<boolean>('/route-collection/check', {
            params: { userId, routeId },
        });
    },

    updateCollectionNote(id: number, note: string) {
        return apiClient.put('/route-collection/update-note', null, {
            params: { id, note },
        });
    },

    getCollectionCategories(userId: number) {
        return apiClient.get<string[]>(`/route-collection/categories/${userId}`);
    },

    getCollectionsByCategory(
        userId: number,
        category: string,
        page: number = 0,
        size: number = 10
    ) {
        return apiClient.get<RouteCollection[]>(
            `/route-collection/category/${userId}/${category}`,
            {
                params: { page, size },
            }
        );
    },

    batchRemoveCollections(ids: number[]) {
        return apiClient.delete('/route-collection/batch-remove', {
            data: ids,
        });
    },
};
