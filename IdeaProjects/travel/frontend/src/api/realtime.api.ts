import apiClient from '../utils/api';
import { DEFAULT_SYNC_MINUTES } from '../constants';

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
        return apiClient.get<AttractionRealtimeStatus>(`/realtime-status/attraction/${attractionId}`);
    },

    getBatchRealtimeStatus(attractionIds: number[]) {
        return apiClient.post<AttractionRealtimeStatus[]>('/realtime-status/batch', attractionIds);
    },

    updateRealtimeStatus(status: AttractionRealtimeStatus) {
        return apiClient.post<boolean>('/realtime-status/update', status);
    },

    batchUpdateRealtimeStatus(statusList: AttractionRealtimeStatus[]) {
        return apiClient.post<boolean>('/realtime-status/batch-update', statusList);
    },

    getHistoricalAvgCrowdCount(attractionId: number) {
        return apiClient.get<number>(`/realtime-status/historical-avg/${attractionId}`);
    },

    getNeedSyncStatus(minutes: number = DEFAULT_SYNC_MINUTES) {
        return apiClient.get<AttractionRealtimeStatus[]>('/realtime-status/need-sync', {
            params: { minutes },
        });
    },

    getActiveWarns() {
        return apiClient.get<any[]>('/realtime-status/warns');
    },

    batchUpdateSyncTime(attractionIds: number[]) {
        return apiClient.post<number>('/realtime-status/sync-time', attractionIds);
    },

    get7DaysAvgCrowdCount(attractionId: number) {
        return apiClient.get<number>(`/realtime-status/7days-avg/${attractionId}`);
    },

    getCrowdedAttractions(minCrowdLevel: number) {
        return apiClient.get<AttractionRealtimeStatus[]>('/realtime-status/crowded', {
            params: { minCrowdLevel },
        });
    },

    getTrafficInfo(attractionId: number) {
        return apiClient.get<Record<string, any>>(`/realtime-status/traffic/${attractionId}`);
    },

    triggerBatchUpdate() {
        return apiClient.post<string>('/realtime-status/batch-update');
    },
};
