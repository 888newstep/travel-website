// frontend/src/api/request.ts
import axios, {type AxiosInstance, AxiosError, type InternalAxiosRequestConfig, type AxiosRequestConfig } from 'axios';
import { API_BASE_URL } from './config';

class ApiClient {
    private instance: AxiosInstance;

    constructor(baseURL: string) {
        this.instance = axios.create({
            baseURL,
            timeout: 30000,
            headers: {
                'Content-Type': 'application/json',
            },
        });

        this.setupInterceptors();
    }

    private setupInterceptors() {
        // 请求拦截器
        this.instance.interceptors.request.use(
            (config: InternalAxiosRequestConfig) => {
                const token = localStorage.getItem('token');
                if (token && config.headers) {
                    config.headers.Authorization = `Bearer ${token}`;
                }
                return config;
            },
            (error: AxiosError) => {
                return Promise.reject(error);
            }
        );

        // 响应拦截器 - 处理后端 Result<T> 格式
        this.instance.interceptors.response.use(
            (response) => {
                const data = response.data;

                // 如果响应包含 Result 格式（code, message, data），则提取 data
                if (data && typeof data === 'object' && 'code' in data) {
                    if (data.code === 200 || data.success === true) {
                        return data.data !== undefined ? data.data : data;
                    } else {
                        const error = new Error(data.message || '请求失败');
                        (error as any).code = data.code;
                        return Promise.reject(error);
                    }
                }

                // 否则直接返回数据
                return data;
            },
            (error: AxiosError) => {
                console.error('API Error:', error.message);

                if (error.response) {
                    switch (error.response.status) {
                        case 401:
                            localStorage.removeItem('token');
                            window.location.href = '/';
                            break;
                        case 403:
                            console.error('没有权限访问');
                            break;
                        case 404:
                            console.error('请求的资源不存在');
                            break;
                        case 500:
                            console.error('服务器内部错误');
                            break;
                    }
                }

                return Promise.reject(error);
            }
        );
    }

    async get<T = any>(url: string, params?: any): Promise<T> {
        return this.instance.get(url, { params });
    }

    async post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
        return this.instance.post(url, data, config);
    }

    async put<T = any>(url: string, data?: any): Promise<T> {
        return this.instance.put(url, data);
    }

    async delete<T = any>(url: string, params?: any): Promise<T> {
        return this.instance.delete(url, { params });
    }
}

export const apiClient = new ApiClient(API_BASE_URL);
