import apiClient from '../utils/api';
import { DEFAULT_PAGE_ZERO, DEFAULT_PAGE_SIZE_SMALL, DEFAULT_LIMIT } from '../constants';

export interface RouteShare {
    id?: number;
    userId?: number;
    routeId?: number;
    itemId?: number;
    itemType?: string;
    fileName?: string;
    shareCode?: string;
    visitCount?: number;
    isPublic?: boolean;
    password?: string;
    expireTime?: string;
    createTime?: string;
}

export interface ShareStatistics {
    shareId: number;
    shareCode: string;
    shareTitle?: string;
    shareCount: number;
    visitCount: number;
    createdAt: string;
    expireTime?: string;
    isActive: boolean;
    isExpired: boolean;
}

export const shareApi = {
    generateShareCode(itemId: number, itemType: string = 'route') {
        return apiClient.post<RouteShare>('/route-share/generate', {
            itemId,
            itemType,
        });
    },

    validateShareCode(code: string) {
        return apiClient.get<boolean>('/route-share/validate', {
            params: { code },
        });
    },

    getUserShares(userId: number, page: number = DEFAULT_PAGE_ZERO, size: number = DEFAULT_PAGE_SIZE_SMALL) {
        return apiClient.get<RouteShare[]>(`/route-share/user/${userId}`, {
            params: { page, size },
        });
    },

    cancelShare(id: number) {
        return apiClient.delete<boolean>(`/route-share/cancel/${id}`);
    },

    getShareStatistics(id: number) {
        return apiClient.get<ShareStatistics>(`/route-share/statistics/${id}`);
    },

    getPopularShares(limit: number = DEFAULT_LIMIT) {
        return apiClient.get<RouteShare[]>('/route-share/popular', {
            params: { limit },
        });
    },

    batchCancelShares(ids: number[]) {
        return apiClient.post<number>('/route-share/batch-cancel', ids);
    },
};
