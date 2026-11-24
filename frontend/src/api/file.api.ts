import apiClient from '../utils/api'

export interface ResourceFile {
  id?: number
  fileName: string
  category?: string
  description?: string
  size?: string
  uploadTime?: string
  version?: string
  url?: string
  tags?: string[]
}

export interface FileCategory {
  id?: number
  tagName: string
  parentId?: number | null
}

export interface FileStatistics {
  totalFiles?: number
  totalCategories?: number
}

export interface FileVersion {
  id?: number | string
  version?: string
  uploadTime?: string
  createTime?: string
  description?: string
}

export interface FileVersionCompare {
  version1?: string
  version2?: string
  diff?: string
  message?: string
}

export const fileApi = {
  uploadFile(formData: FormData) {
    return apiClient.post<ResourceFile>('/resource-file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  deleteFile(id: number) {
    return apiClient.delete<boolean>(`/resource-file/delete/${id}`)
  },

  getFileList(params?: Record<string, unknown>) {
    return apiClient.get<ResourceFile[]>('/resource-file/list', { params })
  },

  searchFiles(keyword: string) {
    return apiClient.get<ResourceFile[]>('/resource-file/search', { params: { keyword } })
  },

  getFileStatistics() {
    return apiClient.get<FileStatistics>('/resource-file/statistics')
  },

  // 分类管理
  getCategories() {
    return apiClient.get<FileCategory[]>('/resource-file/category/list')
  },

  createCategory(category: Partial<FileCategory>) {
    return apiClient.post<FileCategory>('/resource-file/category/create', category)
  },

  deleteCategory(id: string) {
    return apiClient.delete<boolean>(`/resource-file/category/delete/${id}`)
  },

  // 版本管理
  getFileVersions(fileId: string) {
    return apiClient.get<FileVersion[]>(`/resource-file/version/list/${fileId}`)
  },

  compareVersions(version1Id: string, version2Id: string) {
    return apiClient.post<FileVersionCompare>('/resource-file/version/compare', null, {
      params: { version1Id, version2Id },
    })
  },
}
