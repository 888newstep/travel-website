import apiClient from '../utils/api';

export interface TravelNote {
    id: number;
    title: string;
    author: string;
    image: string;
    likes: number;
    comments: number;
    excerpt: string;
    content?: string;
    isLiked?: boolean;
    isCollected?: boolean;
    commentList?: any[];
    createTime?: string;
}

export const noteApi = {
    getNotes(page: number = 1, size: number = 20, filters?: Record<string, any>) {
        return apiClient.get<TravelNote[]>('/travel-notes/list', {
            params: { page, size, ...filters },
        });
    },

    getHotNotes(limit: number = 10) {
        return apiClient.get<TravelNote[]>('/travel-notes/hot', {
            params: { limit },
        });
    },

    getLatestNotes(limit: number = 10) {
        return apiClient.get<TravelNote[]>('/travel-notes/latest', {
            params: { limit },
        });
    },

    getNoteById(id: number) {
        return apiClient.get<Record<string, any>>(`/travel-notes/${id}`);
    },

    getUserTravelNotes(userId: number, page: number = 1, size: number = 20) {
        return apiClient.get<TravelNote[]>(`/travel-notes/user/${userId}`, {
            params: { page, size },
        });
    },

    createNote(data: {
        travelNote: Omit<TravelNote, 'id' | 'likes' | 'comments' | 'createTime'>;
        tags?: string[];
    }) {
        return apiClient.post<TravelNote>('/travel-notes', data);
    },

    updateNote(id: number, data: {
        travelNote: Partial<TravelNote>;
        tags?: string[];
    }) {
        return apiClient.put<TravelNote>(`/travel-notes/${id}`, data);
    },

    deleteNote(id: number, userId: number) {
        return apiClient.delete<boolean>(`/travel-notes/${id}`, {
            params: { userId },
        });
    },

    likeNote(noteId: number, userId: number) {
        return apiClient.post<boolean>(`/travel-notes/${noteId}/like`, null, {
            params: { userId },
        });
    },

    unlikeNote(noteId: number, userId: number) {
        return apiClient.post<boolean>(`/travel-notes/${noteId}/unlike`, null, {
            params: { userId },
        });
    },

    collectNote(noteId: number) {
        return apiClient.post<boolean>(`/travel-notes/${noteId}/collect`);
    },

    uncollectNote(noteId: number) {
        return apiClient.post<boolean>(`/travel-notes/${noteId}/uncollect`);
    },

    incrementViews(noteId: number) {
        return apiClient.post<boolean>(`/travel-notes/${noteId}/view`);
    },

    searchNotes(keyword: string, page: number = 1, size: number = 20) {
        return apiClient.get<TravelNote[]>('/travel-notes/search', {
            params: { keyword, page, size },
        });
    },
};
