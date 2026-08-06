import axios from 'axios';
import type { AxiosResponse, AxiosError } from 'axios';
import { AuthError, NetworkError } from '../lib/request';

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
        }
        return config;
    },
    (error: AxiosError) => {
        return Promise.reject(error);
    }
);

apiClient.interceptors.response.use(
    (response: AxiosResponse) => {
        const backendResponse = response.data as any;

        if (backendResponse.code === 200 || backendResponse.code === undefined) {
            return backendResponse.data !== undefined ? backendResponse.data : backendResponse;
        } else {
            console.error('API Error:', backendResponse.message);
            return Promise.reject(new Error(backendResponse.message || '请求失败'));
        }
    },
    (error: AxiosError) => {
        if (error.response) {
            switch (error.response.status) {
                case 401:
                    console.error('登录状态已失效');
                    localStorage.removeItem('token');
                    window.location.href = '/';
                    return Promise.reject(new AuthError('登录状态已失效，请重新登录'));
                case 403:
                    console.error('没有访问权限');
                    return Promise.reject(new Error('暂无权限访问'));
                case 404:
                    console.error('接口资源不存在');
                    return Promise.reject(new Error('请求的资源不存在'));
                case 500:
                    console.error('服务器内部错误');
                    return Promise.reject(new Error('服务器开小差了，请稍后重试'));
                default:
                    console.error('接口请求失败:', error.message);
                    return Promise.reject(new Error(error.message || '请求失败'));
            }
        } else if (error.request) {
            console.error('网络请求未收到响应');
            return Promise.reject(new NetworkError('网络连接异常，请检查后重试'));
        }
        return Promise.reject(error);
    }
);

export default apiClient as {
    get<T>(url: string, config?: Record<string, any>): Promise<T>;
    post<T>(url: string, data?: any, config?: Record<string, any>): Promise<T>;
    put<T>(url: string, data?: any, config?: Record<string, any>): Promise<T>;
    delete<T>(url: string, config?: Record<string, any>): Promise<T>;
};
