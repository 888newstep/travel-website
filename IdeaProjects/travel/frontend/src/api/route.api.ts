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

export interface RouteOptimization {
    optimizationType: string;
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

export interface RouteEvaluation {
    [key: string]: any;
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
    getRealTimeAdjustment(routeId: number, data: {
        currentLocation?: Record<string, number>;
        realTimeFactors?: Record<string, any>;
    }) {
        return apiClient.post<Record<string, any>>(`/routes/smart/real-time-adjustment/${routeId}`, data);
    },

    evaluateRouteQuality(routeId: number, evaluationParams: Record<string, any>) {
        return apiClient.post<RouteEvaluation>(`/routes/smart/evaluate/${routeId}`, evaluationParams);
    },

    generatePersonalizedRoute(data: {
        userPreferences?: Record<string, any>;
        constraints?: Record<string, any>;
    }) {
        return apiClient.post<RoutePlan>('/routes/smart/generate-personalized', data);
    },

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

    getSeasonalRoutes(cityId: number, season: string, days: number) {
        return apiClient.get<RoutePlan[]>('/routes/smart/list', {
            params: { type: 'seasonal', cityId, season, days },
        }).then((list) => (Array.isArray(list) ? list.map(normalizeSmartRoute) : list));
    },

    getThemeRoutes(theme: string, cityId: number, days: number) {
        return apiClient.get<RoutePlan[]>('/routes/smart/list', {
            params: { type: 'theme', theme, cityId, days },
        }).then((list) => (Array.isArray(list) ? list.map(normalizeSmartRoute) : list));
    },

    getSmartRouteList(params: { type: string; cityId: number; days: number; limit?: number; season?: string; theme?: string }) {
        return apiClient.get<RoutePlan[]>('/routes/smart/list', { params }).then((list) =>
            Array.isArray(list) ? list.map(normalizeSmartRoute) : list,
        );
    },

    recommendByPreference(preferences: Record<string, any>, params: { userId: number; cityId: number; days: number }) {
        return apiClient.post<RoutePlan[]>('/routes/smart/recommend-by-preference', preferences, { params });
    },

    compareRoutes(routeIds: number[]) {
        return apiClient.post<Record<string, any>>('/routes/smart/compare', null, {
            params: { routeIds },
        });
    },

    optimizeRoute(routeId: number) {
        return apiClient.post<Record<string, any>>(`/routes/smart/optimize`, null, { params: { routeId } });
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

    getRouteThemes() {
        return apiClient.get<{ value: string; label: string }[]>('/routes/smart/themes');
    },

    getRouteSeasons() {
        return apiClient.get<{ value: string; label: string }[]>('/routes/smart/seasons');
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
    createRoute(route: Partial<Route>) {
        return apiClient.post<Route>('/routes', route);
    },

    getRoute(id: number) {
        return apiClient.get<Route>(`/routes/${id}`);
    },

    updateRoute(id: number, route: Partial<Route>) {
        return apiClient.put<Route>(`/routes/${id}`, route);
    },

    deleteRoute(id: number, userId: number) {
        return apiClient.delete<boolean>(`/routes/${id}`, {
            params: { userId },
        });
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

    getRouteCount(userId: number) {
        return apiClient.get<number>(`/routes/count/${userId}`);
    },

    batchGetRoutes(routeIds: number[]) {
        return apiClient.post<Route[]>('/routes/batch', routeIds);
    },

    copyRoute(id: number, userId: number) {
        return apiClient.post<Route>(`/routes/${id}/copy`, null, {
            params: { userId },
        });
    },

    setRouteVisibility(id: number, userId: number, isPublic: boolean) {
        return apiClient.put<boolean>(`/routes/${id}/visibility`, null, {
            params: { userId, isPublic },
        });
    },
};
