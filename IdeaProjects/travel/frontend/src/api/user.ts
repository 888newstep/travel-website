// frontend/src/api/user.ts
import { apiClient } from './request';
import { API_ENDPOINTS } from './config';

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

export interface LoginResponse {
    token: string;
}

export const userApi = {
    login: (data: LoginRequest) =>
        apiClient.post<LoginResponse>(API_ENDPOINTS.USER_LOGIN, data),

    register: (data: RegisterRequest) =>
        apiClient.post<User>(API_ENDPOINTS.USER_REGISTER, data),

    sendCaptcha: (phone: string) =>
        apiClient.post<{ captcha: string }>(`${API_ENDPOINTS.USER_CAPTCHA}?phone=${phone}`),

    getCurrentUser: () =>
        apiClient.get<User>(API_ENDPOINTS.USER_PROFILE),

    updateProfile: (data: Partial<User>) =>
        apiClient.put<User>(API_ENDPOINTS.USER_PROFILE, data),

    logout: () =>
        apiClient.post(API_ENDPOINTS.USER_LOGOUT),
};
