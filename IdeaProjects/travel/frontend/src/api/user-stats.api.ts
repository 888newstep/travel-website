import apiClient from '../utils/api';

export interface UserStats {
    totalNotes?: number;
    totalCollections?: number;
    totalShares?: number;
    totalLikes?: number;
    totalViews?: number;
    followers?: number;
    following?: number;
    [key: string]: any;
}

export const userStatsApi = {
    getCurrentUserStats() {
        return apiClient.get<UserStats>('/v1/user/stats');
    },

    getUserStatsById(userId: number) {
        return apiClient.get<UserStats>(`/v1/user/stats/${userId}`);
    },
};
