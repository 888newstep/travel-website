import apiClient from '../utils/api';
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE, DEFAULT_LIMIT } from '../constants';

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
    getNotes(page: number = DEFAULT_PAGE, size: number = DEFAULT_PAGE_SIZE, filters?: Record<string, any>) {
        return apiClient.get<TravelNote[]>('/travel-notes/list', {
            params: { page, size, ...filters },
        });
    },

    getHotNotes(limit: number = DEFAULT_LIMIT) {
        return apiClient.get<TravelNote[]>('/travel-notes/hot', {
            params: { limit },
        });
    },

    getLatestNotes(limit: number = DEFAULT_LIMIT) {
        return apiClient.get<TravelNote[]>('/travel-notes/latest', {
            params: { limit },
        });
    },

    getNoteById(id: number) {
        return apiClient.get<Record<string, any>>(`/travel-notes/${id}`);
    },

    getUserTravelNotes(userId: number, page: number = DEFAULT_PAGE, size: number = DEFAULT_PAGE_SIZE) {
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

    toggleLikeNote(noteId: number, userId: number) {
        return apiClient.post<{ liked: boolean; likeCount: number }>(
            `/travel-notes/${noteId}/toggle-like`, null, { params: { userId } }
        );
    },

    toggleCollectNote(noteId: number) {
        return apiClient.post<{ collected: boolean }>(`/travel-notes/${noteId}/toggle-collect`);
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

    searchNotes(keyword: string, page: number = DEFAULT_PAGE, size: number = DEFAULT_PAGE_SIZE) {
        return apiClient.get<TravelNote[]>('/travel-notes/search', {
            params: { keyword, page, size },
        });
    },

    getComments(noteId: number, page: number = DEFAULT_PAGE, size: number = DEFAULT_PAGE_SIZE) {
        return apiClient.get<any[]>(`/travel-notes/${noteId}/comments`, {
            params: { page, size },
        });
    },

    addComment(noteId: number, content: string) {
        return apiClient.post<any>(`/travel-notes/${noteId}/comments`, { content });
    },
};
