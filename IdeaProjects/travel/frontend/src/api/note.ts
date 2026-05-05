// frontend/src/api/note.ts
import { apiClient } from './request';
import { API_ENDPOINTS } from './config';

export interface TravelNote {
    id: number;
    title: string;
    content: string;
    coverImage?: string;
    userId?: number;
    author?: {
        id: number;
        username: string;
        avatar?: string;
    };
    tags?: string[];
    likeCount?: number;
    commentCount?: number;
    viewCount?: number;
    isLiked?: boolean;
    createTime?: string;
    updateTime?: string;
}

export interface CreateNoteRequest {
    travelNote: {
        userId: number;
        title: string;
        content: string;
        coverImage?: string;
    };
    tags?: string[];
}

export const noteApi = {
    // 获取热门游记
    getHotNotes: async (limit: number = 10) => {
        return apiClient.get<TravelNote[]>(API_ENDPOINTS.TRAVEL_NOTES_HOT, { limit });
    },

    // 获取最新游记
    getLatestNotes: async (limit: number = 10) => {
        return apiClient.get<TravelNote[]>(API_ENDPOINTS.TRAVEL_NOTES_LATEST, { limit });
    },

    // 获取游记列表（分页）
    getList: async (page: number = 1, size: number = 12) => {
        return apiClient.get<TravelNote[]>(API_ENDPOINTS.TRAVEL_NOTES, { page, size });
    },

    // 搜索游记
    search: async (keyword: string, page: number = 1, size: number = 12) => {
        return apiClient.get<TravelNote[]>(API_ENDPOINTS.TRAVEL_NOTE_SEARCH, { keyword, page, size });
    },

    // 获取游记详情
    getDetail: async (id: number) => {
        return apiClient.get<TravelNote>(API_ENDPOINTS.TRAVEL_NOTE_DETAIL(id));
    },

    // 创建游记
    create: async (data: CreateNoteRequest) => {
        return apiClient.post<TravelNote>(API_ENDPOINTS.TRAVEL_NOTE_CREATE, data);
    },

    // 更新游记
    update: async (id: number, data: Partial<TravelNote>) => {
        return apiClient.put<TravelNote>(API_ENDPOINTS.TRAVEL_NOTE_UPDATE(id), data);
    },

    // 删除游记
    delete: async (id: number, userId: number) => {
        return apiClient.delete<boolean>(API_ENDPOINTS.TRAVEL_NOTE_DELETE(id), { userId });
    },

    // 点赞游记
    likeNote: async (id: number, userId: number) => {
        return apiClient.post<boolean>(API_ENDPOINTS.TRAVEL_NOTE_LIKE(id), {}, { params: { userId } });
    },

    // 取消点赞
    unlikeNote: async (id: number, userId: number) => {
        return apiClient.post<boolean>(API_ENDPOINTS.TRAVEL_NOTE_UNLIKE(id), {}, { params: { userId } });
    },

    // 增加浏览量
    incrementView: async (id: number) => {
        return apiClient.post<boolean>(API_ENDPOINTS.TRAVEL_NOTE_VIEW(id));
    },

    // 获取用户游记
    getUserNotes: async (userId: number, page: number = 1, size: number = 12) => {
        return apiClient.get<TravelNote[]>(API_ENDPOINTS.TRAVEL_NOTE_USER(userId), { page, size });
    },
};
