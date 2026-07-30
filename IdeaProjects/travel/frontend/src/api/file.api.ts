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

    getFile(id: number) {
        return apiClient.get<ResourceFile>(`/resource-file/${id}`);
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

    getVersionHistory(fileId: string, page = 0, size = 10) {
        return apiClient.get<any[]>(`/resource-file/version/history/${fileId}`, { params: { page, size } });
    },

    compareVersions(version1Id: string, version2Id: string) {
        return apiClient.post<any>('/resource-file/version/compare', null, {
            params: { version1Id, version2Id },
        });
    },
};

export const fileCategoryApi = {
    createCategory(category: Partial<FileCategory>) {
        return apiClient.post<FileCategory>('/resource-file/category/create', category);
    },

    getCategoryList() {
        return apiClient.get<FileCategory[]>('/resource-file/category/list');
    },

    getCategoryTree() {
        return apiClient.get<FileCategory[]>('/resource-file/category/tree');
    },

    updateCategory(id: number, category: Partial<FileCategory>) {
        return apiClient.put<FileCategory>(`/resource-file/category/update/${id}`, category);
    },

    deleteCategory(id: number) {
        return apiClient.delete<boolean>(`/resource-file/category/delete/${id}`);
    },
};
