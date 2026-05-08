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

    async updateProfile(user: Partial<User>) {
        const response: any = await apiClient.put('/users/profile', user);
        return response as User;
    },

    async logout() {
        await apiClient.post('/users/logout');
    },

    async sendCaptcha(phone: string) {
        const response: any = await apiClient.post('/users/captcha', null, {
            params: { phone },
        });
        return response;
    },

    async changePassword(oldPassword: string, newPassword: string) {
        const response: any = await apiClient.post('/users/change-password', {
            oldPassword,
            newPassword,
        });
        return response as boolean;
    },

    async getNotifications(page: number = 0, size: number = 20) {
        const response: any = await apiClient.get('/users/notifications', {
            params: { page, size },
        });
        return response;
    },
};
