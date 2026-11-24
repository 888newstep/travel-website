import apiClient from '../utils/api'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  phone: string
  password: string
  captcha: string
  agreement?: boolean
}

export interface CaptchaResponse {
  demoCode: string
}

export interface User {
  id: number
  username: string
  phone: string
  avatar: string
  role: 'admin' | 'user'
  stats?: {
    notes: number
    collections: number
    shares: number
  }
}

export const userApi = {
  login(data: LoginRequest) {
    // 登录返回结构由后端决定（token/accessToken 等），交给 persistAuth 做兼容解析
    return apiClient.post<unknown>('/users/login', data)
  },

  register(data: RegisterRequest) {
    return apiClient.post<User>('/users/register', data)
  },

  getCurrentUser() {
    return apiClient.get<User>('/users/current')
  },

  updateProfile(user: Partial<User>) {
    return apiClient.put<User>('/users/profile', user)
  },

  async logout() {
    try {
      console.log('开始退出登录...')
      const response = await apiClient.post('/users/logout')
      console.log('退出登录响应:', response)
    } catch (error) {
      console.error('退出登录请求失败:', error)
    } finally {
      console.log('清除本地token并跳转')
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
  },

  async sendCaptcha(phone: string) {
    return apiClient.post<CaptchaResponse>('/users/captcha', null, {
      params: { phone },
    })
  },
}
