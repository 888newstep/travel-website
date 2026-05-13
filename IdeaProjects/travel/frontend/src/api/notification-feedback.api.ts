import apiClient from '../utils/api';

export interface Notification {
    id?: number;
    userId?: number;
    type: string;
    title: string;
    content: string;
    isRead?: boolean;
    createTime?: string;
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
    getNotifications(page: number = 1, size: number = 20) {
        return apiClient.get<Notification[]>('/users/notifications', {
            params: { page, size },
        });
    },

    markAsRead(notificationId: number) {
        return apiClient.put<boolean>(`/users/notifications/${notificationId}/read`);
    },

    deleteNotification(notificationId: number) {
        return apiClient.delete<boolean>(`/users/notifications/${notificationId}`);
    },

    getUnreadCount() {
        return apiClient.get<number>('/users/notifications/unread-count');
    },

    markAllAsRead() {
        return apiClient.post<boolean>('/users/notifications/mark-all-read');
    },
};

export const feedbackApi = {
    submitFeedback(data: FeedbackRequest) {
        return apiClient.post<Feedback>('/users/feedback', data);
    },

    getFeedbackList(page: number = 1, size: number = 20) {
        return apiClient.get<Feedback[]>('/users/feedback/list', {
            params: { page, size },
        });
    },
};
