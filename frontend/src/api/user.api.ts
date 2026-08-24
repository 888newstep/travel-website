import apiClient from '../utils/api';

export interface LoginRequest {
    username: string;
    password: string;
}

export interface RegisterRequest {
    username: string;
    phone: string;
    password: string;
    captcha: string;
    agreement?: boolean;
}

export interface CaptchaResponse {
    demoCode: string;
}

export interface User {
    id: number;
    username: string;
    phone: string;
    avatar: string;
    role: 'admin' | 'user';
    stats?: {
        notes: number;
        collections: number;
        shares: number;
    };
}

export const userApi = {
    async login(data: LoginRequest) {
        const response: any = await apiClient.post('/users/login', data);
        return response;
    },

    async register(data: RegisterRequest) {
        const response: any = await apiClient.post('/users/register', data);
        return response as User;
    },

    async getCurrentUser() {
        const response: any = await apiClient.get('/users/current');
        return response as User;
    },

    async updateProfile(user: Partial<User>) {
        const response: any = await apiClient.put('/users/profile', user);
        return response as User;
    },

    async logout() {
        try {
            console.log('开始退出登录...');
            const response = await apiClient.post('/users/logout');
            console.log('退出登录响应:', response);
        } catch (error) {
            console.error('退出登录请求失败:', error);
        } finally {
            console.log('清除本地token并跳转');
            localStorage.removeItem('token');
            window.location.href = '/login';
        }
    },


    async sendCaptcha(phone: string) {
        return apiClient.post<CaptchaResponse>('/users/captcha', null, {
            params: { phone },
        });
    },
};
