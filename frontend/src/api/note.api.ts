import apiClient from '../utils/api';
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE, DEFAULT_LIMIT } from '../constants';

export interface TravelNote {
    id: number;
    title: string;
    author: string;
    image: string;
    likes: number;
    excerpt: string;
    content?: string;
    isLiked?: boolean;
    isCollected?: boolean;
    createTime?: string;
}

function normalizeTravelNote(value: unknown): TravelNote {
    const envelope = value && typeof value === 'object' ? value as Record<string, any> : {};
    const source = envelope.travelNote && typeof envelope.travelNote === 'object'
        ? envelope.travelNote as Record<string, any>
        : envelope;
    const content = typeof source.content === 'string' ? source.content : '';

    return {
        id: Number(source.id) || 0,
        title: typeof source.title === 'string' ? source.title : '',
        author: String(source.author || source.user?.nickname || source.user?.username || ''),
        image: String(source.image || source.coverImage || ''),
        likes: Number(source.likes ?? source.likesCount) || 0,
        excerpt: String(source.excerpt || content),
        content,
        isLiked: Boolean(source.isLiked),
        isCollected: Boolean(source.isCollected),
        createTime: source.createTime || source.createdAt,
    };
}

function normalizeTravelNotes(value: unknown): TravelNote[] {
    return Array.isArray(value) ? value.map(normalizeTravelNote) : [];
}

export const noteApi = {
    getNotes(page: number = DEFAULT_PAGE, size: number = DEFAULT_PAGE_SIZE, filters?: Record<string, any>) {
        return apiClient.get<TravelNote[]>('/travel-notes/list', {
            params: { page, size, ...filters },
        }).then(normalizeTravelNotes);
    },

    getHotNotes(limit: number = DEFAULT_LIMIT) {
        return apiClient.get<TravelNote[]>('/travel-notes/hot', {
            params: { limit },
        }).then(normalizeTravelNotes);
    },

    getLatestNotes(limit: number = DEFAULT_LIMIT) {
        return apiClient.get<TravelNote[]>('/travel-notes/latest', {
            params: { limit },
        }).then(normalizeTravelNotes);
    },

    getNoteById(id: number) {
        return apiClient.get<unknown>(`/travel-notes/${id}`).then(normalizeTravelNote);
    },

    getUserTravelNotes(userId: number, page: number = DEFAULT_PAGE, size: number = DEFAULT_PAGE_SIZE) {
        return apiClient.get<TravelNote[]>(`/travel-notes/user/${userId}`, {
            params: { page, size },
        }).then(normalizeTravelNotes);
    },

    searchNotes(keyword: string, page: number = DEFAULT_PAGE, size: number = DEFAULT_PAGE_SIZE) {
        return apiClient.get<TravelNote[]>('/travel-notes/search', {
            params: { keyword, page, size },
        }).then(normalizeTravelNotes);
    },
};
