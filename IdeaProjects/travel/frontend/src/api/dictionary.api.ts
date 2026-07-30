import apiClient from '../utils/api';

export interface DictItem {
    key: string;
    value: string;
    label: string;
}

export const dictionaryApi = {
    /** 获取指定类型的字典项 */
    getByType(dictType: string) {
        return apiClient.get<DictItem[]>(`/dictionary/${dictType}`);
    },

    /** 批量获取字典：types 用逗号分隔，如 "nav_menu,ai_tabs" */
    getBatch(types: string) {
        return apiClient.get<Record<string, DictItem[]>>('/dictionary/batch', {
            params: { types },
        });
    },
};
