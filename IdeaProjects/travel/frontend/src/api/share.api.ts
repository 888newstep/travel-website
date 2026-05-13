import apiClient from '../utils/api';

export interface ShareCode {
    code: string;
    expireTime: string;
    itemId: number;
    itemType: string;
}

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

export const shareApi = {
    createRouteShare(share: Omit<RouteShare, 'id' | 'shareCode' | 'visitCount' | 'createTime'>) {
        return apiClient.post<RouteShare>('/route-share/create', share);
    },

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

    getShareInfo(shareCode: string) {
        return apiClient.get<RouteShare>(`/route-share/info/${shareCode}`);
    },

    accessShareRoute(shareCode: string) {
        return apiClient.get<Record<string, any>>(`/route-share/access/${shareCode}`);
    },

    getUserShares(userId: number, page: number = 0, size: number = 10) {
        return apiClient.get<RouteShare[]>(`/route-share/user/${userId}`, {
            params: { page, size },
        });
    },

    cancelShare(id: number) {
        return apiClient.delete<boolean>(`/route-share/cancel/${id}`);
    },

    updateShareSettings(id: number, settings: Record<string, any>) {
        return apiClient.put<boolean>(`/route-share/update/${id}`, settings);
    },

    increaseVisitCount(shareCode: string) {
        return apiClient.post<boolean>(`/route-share/visit/${shareCode}`);
    },

    getShareStatistics(id: number) {
        return apiClient.get<Record<string, any>>(`/route-share/statistics/${id}`);
    },

    getPopularShares(limit: number = 10) {
        return apiClient.get<RouteShare[]>('/route-share/popular', {
            params: { limit },
        });
    },

    createFileShare(share: Omit<RouteShare, 'id' | 'shareCode' | 'visitCount' | 'createTime'>) {
        return apiClient.post<RouteShare>('/file-share/create', share);
    },

    generateFileShareCode(fileId: number) {
        return apiClient.post<RouteShare>('/file-share/generate', {
            fileId,
        });
    },

    getFileShareInfo(shareCode: string) {
        return apiClient.get<RouteShare>(`/file-share/info/${shareCode}`);
    },

    accessShareFile(shareCode: string, password?: string) {
        return apiClient.get<string>(`/file-share/access/${shareCode}`, {
            params: { password },
        });
    },

    getUserFileShares(userId: number, page: number = 0, size: number = 10) {
        return apiClient.get<RouteShare[]>(`/file-share/user/${userId}`, {
            params: { page, size },
        });
    },

    cancelFileShare(id: number) {
        return apiClient.delete<boolean>(`/file-share/cancel/${id}`);
    },

    updateFileShareSettings(id: number, settings: Record<string, any>) {
        return apiClient.put<boolean>(`/file-share/update/${id}`, settings);
    },

    getFileShareStatistics(id: number) {
        return apiClient.get<Record<string, any>>(`/file-share/statistics/${id}`);
    },

    batchCancelShares(ids: number[]) {
        return apiClient.post<number>('/file-share/batch-cancel', ids);
    },
};
