import apiClient from '../utils/api';
import { DEFAULT_SYNC_MINUTES } from '../constants';

export interface AttractionRealtimeStatus {
    id?: number;
    attractionId: number;
    crowdCount?: number;
    waitTime?: number;
    crowdLevel?: number;
    status?: string;
    temperature?: number;
    weather?: string;
    openStatus?: boolean;
    lastUpdateTime?: string;
    syncTime?: string;
}

export interface AttractionWarning {
    warnId: string;
    attractionId: number;
    warnType: string;
    warnLevel: '严重' | '较高' | '一般';
    warnMessage: string;
    createTime?: string;
    status: 'active';
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

    getNeedSyncStatus(minutes: number = DEFAULT_SYNC_MINUTES) {
        return apiClient.get<AttractionRealtimeStatus[]>('/realtime-status/need-sync', {
            params: { minutes },
        });
    },

    getActiveWarns() {
        return apiClient.get<AttractionWarning[]>('/realtime-status/warns');
    },

    batchUpdateSyncTime(attractionIds: number[]) {
        return apiClient.post<number>('/realtime-status/sync-time', attractionIds);
    },

    getCrowdedAttractions(minCrowdLevel: number) {
        return apiClient.get<AttractionRealtimeStatus[]>('/realtime-status/crowded', {
            params: { minCrowdLevel },
        });
    },

};
