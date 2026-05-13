import apiClient from '../utils/api';

export interface AttractionRealtimeStatus {
    id?: number;
    attractionId: number;
    crowdCount?: number;
    waitTime?: number;
    status?: string;
    temperature?: number;
    weather?: string;
    openStatus?: boolean;
    lastUpdateTime?: string;
    syncTime?: string;
}

export const realtimeApi = {
    getAttractionRealtimeStatus(attractionId: number) {
        return apiClient.get<AttractionRealtimeStatus>(`/api/realtime-status/attraction/${attractionId}`);
    },

    getBatchRealtimeStatus(attractionIds: number[]) {
        return apiClient.post<AttractionRealtimeStatus[]>('/api/realtime-status/batch', attractionIds);
    },

    updateRealtimeStatus(status: AttractionRealtimeStatus) {
        return apiClient.post<boolean>('/api/realtime-status/update', status);
    },

    batchUpdateRealtimeStatus(statusList: AttractionRealtimeStatus[]) {
        return apiClient.post<boolean>('/api/realtime-status/batch-update', statusList);
    },

    getHistoricalAvgCrowdCount(attractionId: number) {
        return apiClient.get<number>(`/api/realtime-status/historical-avg/${attractionId}`);
    },

    getNeedSyncStatus(minutes: number = 60) {
        return apiClient.get<AttractionRealtimeStatus[]>('/api/realtime-status/need-sync', {
            params: { minutes },
        });
    },

    getActiveWarns() {
        return apiClient.get<any[]>('/api/realtime-status/warns');
    },

    batchUpdateSyncTime(attractionIds: number[]) {
        return apiClient.post<number>('/api/realtime-status/sync-time', attractionIds);
    },

    get7DaysAvgCrowdCount(attractionId: number) {
        return apiClient.get<number>(`/api/realtime-status/7days-avg/${attractionId}`);
    },
};
