import apiClient from '../utils/api';

export interface UserStats {
    totalNotes: number;
    totalCollections: number;
    totalShares: number;
}

export const userStatsApi = {
    getCurrentUserStats() {
        return apiClient.get<UserStats>('/v1/user/stats');
    },
};
