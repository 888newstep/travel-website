import apiClient from '../utils/api';
import { DEFAULT_LIMIT_SMALL } from '../constants';

export interface RoutePlan {
    id?: number;
    title: string;
    userId?: number;
    cityId?: number;
    durationDays?: number;
    preferences?: Record<string, any>;
    constraints?: Record<string, any>;
    createTime?: string;
}

export interface RouteOptimizationSuggestion {
    id: number;
    title: string;
    description: string;
    type: 'distance';
    message?: string;
}

export interface RouteOptimizationHistory {
    routeId: number;
    optimizationType: 'distance';
    description: string;
    appliedAt: string;
}

// 后端智能路线接口返回 SmartRouteItem，主键字段为 routeId；前端统一映射为 id
function normalizeSmartRoute<T>(item: T): T {
  if (item && typeof item === 'object' && !('id' in item) && 'routeId' in item) {
    const record = item as Record<string, unknown>
    return { ...record, id: record.routeId } as T
  }
  return item
}

export const intelligentRouteApi = {
    getPopularRoutes(cityId: number, days: number, limit: number = DEFAULT_LIMIT_SMALL) {
        return apiClient.get<RoutePlan[]>('/routes/smart/list', {
            params: { type: 'popular', cityId, days, limit },
        }).then((list) => (Array.isArray(list) ? list.map(normalizeSmartRoute) : list));
    },

    getSimilarRoutes(routeId: number, limit: number = DEFAULT_LIMIT_SMALL) {
        return apiClient.get<RoutePlan[]>(`/routes/smart/similar/${routeId}`, {
            params: { limit },
        });
    },

    getOptimizationSuggestions(routeId: number) {
        return apiClient.get<RouteOptimizationSuggestion[]>(`/route-optimization/suggestions/${routeId}`);
    },

    getOptimizationHistory(routeId: number) {
        return apiClient.get<RouteOptimizationHistory[]>(`/route-optimization/history/${routeId}`);
    },

    applyOptimizationSuggestion(routeId: number, suggestionId: number, suggestion: RouteOptimizationSuggestion) {
        return apiClient.post('/route-optimization/apply', { routeId, suggestionId, suggestion });
    },

};

export interface Route {
    id?: number;
    title: string;
    description?: string;
    cityId?: number;
    durationDays?: number;
    difficulty?: string;
    coverImage?: string;
    userId?: number;
    viewCount?: number;
    likeCount?: number;
    isPublic?: boolean;
    createdAt?: string;
    updatedAt?: string;
}

export const routeCrudApi = {
    getRoute(id: number) {
        return apiClient.get<Route>(`/routes/${id}`);
    },

    getMyRoutes(userId: number) {
        return apiClient.get<Route[]>('/routes/my', {
            params: { userId },
        });
    },

    searchRoutes(title: string) {
        return apiClient.get<Route[]>('/routes/search', {
            params: { title },
        });
    },

    getRoutesByCity(cityId: number) {
        return apiClient.get<Route[]>(`/routes/city/${cityId}`);
    },

};
