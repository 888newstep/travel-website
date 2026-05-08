import axios from 'axios';
import type { AxiosInstance, AxiosResponse, AxiosError } from 'axios';
const apiClient: AxiosInstance = axios.create({
    baseURL: '/api',
    timeout: 30000,
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
                    console.error('未授权，请重新登录');
                    localStorage.removeItem('token');
                    window.location.href = '/login';
                    break;
                case 403:
                    console.error('拒绝访问');
                    break;
                case 404:
                    console.error('请求的资源不存在');
                    break;
                case 500:
                    console.error('服务器错误');
                    break;
                default:
                    console.error('请求失败:', error.message);
            }
        } else if (error.request) {
            console.error('网络错误，请检查后端服务是否启动');
        }
        return Promise.reject(error);
    }
);

export default apiClient;
