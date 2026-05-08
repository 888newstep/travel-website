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
    getNotes(page: number = 0, size: number = 10) {
        return apiClient.get<TravelNote[]>('/travel-notes/list', {
            params: { page, size },
        });
    },

    getHotNotes(limit: number = 3) {
        return apiClient.get<TravelNote[]>('/travel-notes/hot', {
            params: { limit },
        });
    },

    getNoteById(id: number) {
        return apiClient.get<TravelNote>(`/travel-notes/${id}`);
    },

    createNote(note: Omit<TravelNote, 'id' | 'likes' | 'comments' | 'createTime'>) {
        return apiClient.post<TravelNote>('/travel-notes', note);
    },

    updateNote(id: number, note: Partial<TravelNote>) {
        return apiClient.put<TravelNote>(`/travel-notes/${id}`, note);
    },

    deleteNote(id: number) {
        return apiClient.delete(`/travel-notes/${id}`);
    },

    likeNote(noteId: number) {
        return apiClient.post(`/travel-notes/${noteId}/like`);
    },

    unlikeNote(noteId: number) {
        return apiClient.post(`/travel-notes/${noteId}/unlike`);
    },

    collectNote(noteId: number) {
        return apiClient.post(`/travel-notes/${noteId}/collect`);
    },

    uncollectNote(noteId: number) {
        return apiClient.post(`/travel-notes/${noteId}/uncollect`);
    },
};
