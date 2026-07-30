import apiClient from '../utils/api';
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE } from '../constants';

export interface Notification {
    id?: number;
    userId?: number;
    type: string;
    title: string;
    content: string;
    isRead?: boolean;
    createdAt?: string;
}

export interface Feedback {
    id?: number;
    userId?: number;
    type: string;
    content: string;
    contactInfo?: string;
    status?: string;
    createTime?: string;
    replyContent?: string;
    replyTime?: string;
}

export interface FeedbackRequest {
    type: string;
    content: string;
    contactInfo?: string;
}

export const notificationApi = {
    getNotifications(page: number = DEFAULT_PAGE, size: number = DEFAULT_PAGE_SIZE) {
        return apiClient.get<Notification[]>('/v1/notifications', {
            params: { page, size },
        });
    },

    markAsRead(notificationId: number) {
        return apiClient.put<boolean>(`/v1/notifications/${notificationId}/read`);
    },

    deleteNotification(notificationId: number) {
        return apiClient.delete<boolean>(`/v1/notifications/${notificationId}`);
    },

    getUnreadCount() {
        return apiClient.get<number>('/v1/notifications/unread-count');
    },

    markAllAsRead() {
        return apiClient.put<boolean>('/v1/notifications/read-all');
    },
};

export const feedbackApi = {
    submitFeedback(data: FeedbackRequest) {
        return apiClient.post<Feedback>('/feedback/submit', data);
    },

    getFeedbackList(userId: number, page: number = DEFAULT_PAGE, size: number = DEFAULT_PAGE_SIZE) {
        return apiClient.get<Feedback[]>(`/feedback/list/${userId}`, {
            params: { page, size },
        });
    },

    getFeedbackTypes() {
        return apiClient.get<{ value: string; label: string }[]>('/feedback/types');
    },
};
