import apiClient from '../utils/api';

export interface ResourceFile {
    id?: number;
    fileName: string;
    category?: string;
    description?: string;
    size?: string;
    uploadTime?: string;
    version?: string;
    url?: string;
    tags?: string[];
}

export interface FileCategory {
    id?: number;
    tagName: string;
    parentId?: number | null;
}

export const fileApi = {
    uploadFile(formData: FormData) {
        return apiClient.post<ResourceFile>('/resource-file/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
        });
    },

    deleteFile(id: number) {
        return apiClient.delete<boolean>(`/resource-file/delete/${id}`);
    },

    getFileList(params?: Record<string, any>) {
        return apiClient.get<ResourceFile[]>('/resource-file/list', { params });
    },

    searchFiles(keyword: string) {
        return apiClient.get<ResourceFile[]>('/resource-file/search', { params: { keyword } });
    },

    getFileStatistics() {
        return apiClient.get<any>('/resource-file/statistics');
    },

    // 分类管理
    getCategories() {
        return apiClient.get<any[]>('/resource-file/category/list');
    },

    createCategory(category: Partial<FileCategory>) {
        return apiClient.post<any>('/resource-file/category/create', category);
    },

    deleteCategory(id: string) {
        return apiClient.delete<boolean>(`/resource-file/category/delete/${id}`);
    },

    // 版本管理
    getFileVersions(fileId: string) {
        return apiClient.get<any[]>(`/resource-file/version/list/${fileId}`);
    },

    compareVersions(version1Id: string, version2Id: string) {
        return apiClient.post<any>('/resource-file/version/compare', null, {
            params: { version1Id, version2Id },
        });
    },
};
