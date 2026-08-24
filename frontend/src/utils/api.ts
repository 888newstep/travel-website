import axios from 'axios';
import type { AxiosError, AxiosResponse } from 'axios';
import { AuthError, NetworkError } from '../lib/request';

interface BackendErrorResponse {
    message?: string;
    data?: {
        message?: string;
    };
}

const IDEMPOTENT_WRITE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

function createIdempotencyKey(): string {
    if (typeof globalThis.crypto?.randomUUID === 'function') {
        return globalThis.crypto.randomUUID();
    }
    return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
}

function getBackendErrorMessage(error: AxiosError): string | undefined {
    const response = error.response?.data as BackendErrorResponse | undefined;
    const message = response?.message || response?.data?.message;
    return typeof message === 'string' && message.trim() ? message.trim() : undefined;
}

function isAuthenticatedRequest(error: AxiosError): boolean {
    const headers = error.config?.headers as Record<string, unknown> | undefined;
    return Boolean(headers?.Authorization || headers?.authorization);
}

const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    timeout: Number(import.meta.env.VITE_API_TIMEOUT) || 30000,
    headers: {
        'Content-Type': 'application/json; charset=utf-8',
    },
});

apiClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
            const method = config.method?.toUpperCase() || '';
            // 重试沿用原请求配置时保留同一个键，服务端可直接重放首次结果。
            if (IDEMPOTENT_WRITE_METHODS.has(method) && !config.headers.get('Idempotency-Key')) {
                config.headers.set('Idempotency-Key', createIdempotencyKey());
            }
        }
        return config;
    },
    (error: AxiosError) => Promise.reject(error),
);

apiClient.interceptors.response.use(
    (response: AxiosResponse) => {
        const backendResponse = response.data as any;

        if (backendResponse.code === 200 || backendResponse.code === undefined) {
            return backendResponse.data !== undefined ? backendResponse.data : backendResponse;
        }
        console.error('API Error:', backendResponse.message);
        return Promise.reject(new Error(backendResponse.message || '请求失败'));
    },
    (error: AxiosError) => {
        if (error.response) {
            const backendMessage = getBackendErrorMessage(error);
            switch (error.response.status) {
                case 401:
                    if (isAuthenticatedRequest(error)) {
                        localStorage.removeItem('token');
                        if (window.location.pathname !== '/login') {
                            window.location.href = '/login';
                        }
                        return Promise.reject(new AuthError(backendMessage || '登录状态已失效，请重新登录'));
                    }
                    return Promise.reject(new Error(backendMessage || '认证失败'));
                case 403:
                    return Promise.reject(new Error(backendMessage || '暂无权限访问'));
                case 404:
                    return Promise.reject(new Error(backendMessage || '请求的资源不存在'));
                case 500:
                    return Promise.reject(new Error(backendMessage || '服务器内部错误，请稍后重试'));
                default:
                    return Promise.reject(new Error(backendMessage || error.message || '请求失败'));
            }
        }
        if (error.request) {
            return Promise.reject(new NetworkError('网络连接异常，请检查网络后重试'));
        }
        return Promise.reject(error);
    },
);

export default apiClient as {
    get<T>(url: string, config?: Record<string, any>): Promise<T>;
    post<T>(url: string, data?: any, config?: Record<string, any>): Promise<T>;
    put<T>(url: string, data?: any, config?: Record<string, any>): Promise<T>;
    delete<T>(url: string, config?: Record<string, any>): Promise<T>;
};
