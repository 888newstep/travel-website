import apiClient from '../utils/api';
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE, DEFAULT_LIMIT_SMALL } from '../constants';

export interface RouteComment {
    id?: number;
    routeId: number;
    userId: number;
    rating?: number;
    content?: string;
    images?: string[];
    isAnonymous?: boolean;
    replyTo?: number;
    likeCount?: number;
    createTime?: string;
    updateTime?: string;
}

export const commentApi = {
    createComment(comment: Omit<RouteComment, 'id' | 'likeCount' | 'createTime' | 'updateTime'>) {
        return apiClient.post<RouteComment>('/route-comments', comment);
    },

    getRouteComments(routeId: number, page: number = DEFAULT_PAGE, size: number = DEFAULT_PAGE_SIZE) {
        return apiClient.get<RouteComment[]>(`/route-comments/route/${routeId}`, {
            params: { page, size },
        });
    },

    getCommentStatistics(routeId: number) {
        return apiClient.get<Record<string, any>>(`/route-comments/statistics/${routeId}`);
    },

    getLatestComments(routeId: number, limit: number = DEFAULT_LIMIT_SMALL) {
        return apiClient.get<RouteComment[]>(`/route-comments/latest/${routeId}`, {
            params: { limit },
        });
    },

};
