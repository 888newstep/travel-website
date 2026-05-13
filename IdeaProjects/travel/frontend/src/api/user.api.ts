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

export interface ChangePasswordRequest {
    oldPassword: string;
    newPassword: string;
}

export interface ResetPasswordRequest {
    phone: string;
    captcha: string;
    newPassword: string;
}

export interface RefreshTokenRequest {
    oldToken: string;
}

export interface RefreshTokenResponse {
    token: string;
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

    async getUserById(id: number) {
        const response: any = await apiClient.get(`/users/${id}`);
        return response as User;
    },

    async updateUser(user: Partial<User>) {
        const response: any = await apiClient.put('/users', user);
        return response as boolean;
    },

    async deleteUser(id: number) {
        const response: any = await apiClient.delete(`/users/${id}`);
        return response as boolean;
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
        const response: any = await apiClient.post('/users/captcha', null, {
            params: { phone },
        });
        return response as boolean;
    },

    async changePassword(data: ChangePasswordRequest) {
        const response: any = await apiClient.post('/users/change-password', data);
        return response as boolean;
    },

    async resetPassword(data: ResetPasswordRequest) {
        const response: any = await apiClient.post('/users/reset-password', data);
        return response as boolean;
    },

    async refreshToken(data: RefreshTokenRequest) {
        const response: any = await apiClient.post('/users/refresh-token', data);
        return response as RefreshTokenResponse;
    },

    async getNotifications(page: number = 1, size: number = 20) {
        const response: any = await apiClient.get('/users/notifications', {
            params: { page, size },
        });
        return response;
    },
};
