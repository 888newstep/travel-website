import apiClient from '../utils/api';

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

    getRouteComments(routeId: number, page: number = 1, size: number = 20) {
        return apiClient.get<RouteComment[]>(`/route-comments/route/${routeId}`, {
            params: { page, size },
        });
    },

    getUserComments(userId: number, page: number = 1, size: number = 20) {
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

    getCommentStatistics(routeId: number) {
        return apiClient.get<Record<string, any>>(`/route-comments/statistics/${routeId}`);
    },

    getCommentReplies(commentId: number, page: number = 1, size: number = 20) {
        return apiClient.get<RouteComment[]>(`/route-comments/${commentId}/replies`, {
            params: { page, size },
        });
    },

    getBatchComments(commentIds: number[]) {
        return apiClient.post<RouteComment[]>('/route-comments/batch', commentIds);
    },

    getHotComments(routeId: number, limit: number = 5) {
        return apiClient.get<RouteComment[]>(`/route-comments/hot/${routeId}`, {
            params: { limit },
        });
    },

    getLatestComments(routeId: number, limit: number = 5) {
        return apiClient.get<RouteComment[]>(`/route-comments/latest/${routeId}`, {
            params: { limit },
        });
    },

    searchComments(routeId: number, keyword: string, page: number = 1, size: number = 20) {
        return apiClient.get<RouteComment[]>('/route-comments/search', {
            params: { routeId, keyword, page, size },
        });
    },

    getHighRatingComments(routeId: number, minRating: number = 4.0, limit: number = 5) {
        return apiClient.get<RouteComment[]>(`/route-comments/high-rating/${routeId}`, {
            params: { minRating, limit },
        });
    },
};
