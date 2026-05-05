import { apiClient } from './request';
import { API_ENDPOINTS } from './config';

export interface HealthInfo {
    status: string;
    timestamp: string;
    service: string;
    version: string;
}

export interface SystemStatus {
    system: string;
    timestamp: string;
    components: {
        database: string;
        redis: string;
        aiService: string;
        cache: string;
    };
    uptime: string;
}

export const systemApi = {
    healthCheck: () =>
        apiClient.get<HealthInfo>(API_ENDPOINTS.HEALTH),

    getVersion: () =>
        apiClient.get<{ version: string; features: string[] }>(API_ENDPOINTS.VERSION),

    getStatus: () =>
        apiClient.get<SystemStatus>(API_ENDPOINTS.STATUS),
};
