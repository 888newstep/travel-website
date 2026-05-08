import apiClient from '../utils/api';

export interface ShareCode {
    code: string;
    expireTime: string;
    itemId: number;
    itemType: string;
}

export const shareApi = {
    generateShareCode(itemId: number, itemType: string) {
        return apiClient.post<ShareCode>('/route-share/generate', {
            itemId,
            itemType,
        });
    },

    getShareInfo(code: string) {
        return apiClient.get<any>(`/route-share/info/${code}`);
    },

    validateShareCode(code: string) {
        return apiClient.get<boolean>('/route-share/validate', {
            params: { code },
        });
    },

    getFileShareCode(fileId: number) {
        return apiClient.post<ShareCode>('/file-share/generate', {
            fileId,
        });
    },
};
