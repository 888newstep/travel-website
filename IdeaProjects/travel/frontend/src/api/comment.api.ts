import apiClient from '../utils/api';
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE, DEFAULT_LIMIT, DEFAULT_LIMIT_SMALL } from '../constants';

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

    getUserComments(userId: number, page: number = DEFAULT_PAGE, size: number = DEFAULT_PAGE_SIZE) {
        return apiClient.get<RouteComment[]>(`/route-comments/user/${userId}`, {
            params: { page, size },
        });
    },

    likeComment(commentId: number, userId: number) {
        return apiClient.post<boolean>(`/route-comments/${commentId}/like`, null, {
            params: { userId },
        });
    },

    unlikeComment(commentId: number, userId: number) {
        return apiClient.post<boolean>(`/route-comments/${commentId}/unlike`, null, {
            params: { userId },
        });
    },

    deleteComment(commentId: number, userId: number) {
        return apiClient.delete<boolean>(`/route-comments/${commentId}`, {
            params: { userId },
        });
    },

    toggleLikeComment(commentId: number, userId: number) {
        return apiClient.post<{ liked: boolean; likeCount: number }>(
            `/route-comments/${commentId}/toggle-like`,
            null,
            { params: { userId } }
        );
    },

    getCommentStatistics(routeId: number) {
        return apiClient.get<Record<string, any>>(`/route-comments/statistics/${routeId}`);
    },

    getCommentReplies(commentId: number, page: number = DEFAULT_PAGE, size: number = DEFAULT_PAGE_SIZE) {
        return apiClient.get<RouteComment[]>(`/route-comments/${commentId}/replies`, {
            params: { page, size },
        });
    },

    getBatchComments(commentIds: number[]) {
        return apiClient.post<RouteComment[]>('/route-comments/batch', commentIds);
    },

    getHotComments(routeId: number, limit: number = DEFAULT_LIMIT_SMALL) {
        return apiClient.get<RouteComment[]>(`/route-comments/hot/${routeId}`, {
            params: { limit },
        });
    },

    getLatestComments(routeId: number, limit: number = DEFAULT_LIMIT_SMALL) {
        return apiClient.get<RouteComment[]>(`/route-comments/latest/${routeId}`, {
            params: { limit },
        });
    },

    searchComments(routeId: number, keyword: string, page: number = DEFAULT_PAGE, size: number = DEFAULT_PAGE_SIZE) {
        return apiClient.get<RouteComment[]>('/route-comments/search', {
            params: { routeId, keyword, page, size },
        });
    },

    getHighRatingComments(routeId: number, minRating: number = 4.0, limit: number = DEFAULT_LIMIT_SMALL) {
        return apiClient.get<RouteComment[]>(`/route-comments/high-rating/${routeId}`, {
            params: { minRating, limit },
        });
    },
};
