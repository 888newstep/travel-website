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

export interface RouteEvaluation {
    [key: string]: any;
}

export const routeApi = {
    createRoutePlan(data: {
        preferences?: Record<string, any>;
        constraints?: Record<string, any>;
    }) {
        return apiClient.post<RoutePlan>('/routes/smart/generate-personalized', {
            userPreferences: data.preferences,
            constraints: data.constraints,
        });
    },

    recommendRoutes(data: {
        userId?: number;
        cityId?: number;
        days?: number;
        preferences?: Record<string, any>;
    }) {
        return apiClient.post<RoutePlan[]>('/routes/smart/recommend-by-preference', data.preferences || {}, {
            params: { userId: data.userId, cityId: data.cityId, days: data.days },
        });
    },

    optimizeRoute(routeId: number, optimizationType: string) {
        return apiClient.post<RouteOptimization>('/routes/smart/optimize', null, {
            params: { routeId },
        });
    },

    applyOptimization(data: { routeId: number; suggestion?: Record<string, any> }) {
        return apiClient.post<boolean>('/route-optimization/apply', data);
    },

    getOptimizationHistory(routeId: number) {
        return apiClient.get<Record<string, any>[]>(`/route-optimization/history/${routeId}`);
    },

    getOptimizationSuggestions(routeId: number) {
        return apiClient.get(`/route-optimization/suggestions/${routeId}`);
    },

    evaluateRoute(routeId: number, evaluationParams: Record<string, any>) {
        return apiClient.post<RouteEvaluation>(`/routes/smart/evaluate/${routeId}`, evaluationParams);
    },

    getMyRoutePlans(userId: number) {
        return apiClient.get<RoutePlan[]>('/routes/my', {
            params: { userId },
        });
    },

    compareRoutes(routeIds: number[]) {
        return apiClient.post<Record<string, any>>('/routes/smart/compare', null, {
            params: { routeIds },
        });
    },

    adjustRoute(routeId: number, data: {
        currentLocation?: Record<string, number>;
        realTimeFactors?: Record<string, any>;
    }) {
        return apiClient.post<Record<string, any>>(`/routes/smart/real-time-adjustment/${routeId}`, data);
    },

    getPopularRoutes(cityId: number, days: number, limit: number = DEFAULT_LIMIT_SMALL) {
        return apiClient.get<RoutePlan[]>('/routes/smart/popular', {
            params: { cityId, days, limit },
        });
    },

    getSimilarRoutes(routeId: number, limit: number = DEFAULT_LIMIT_SMALL) {
        return apiClient.get<RoutePlan[]>(`/routes/smart/similar/${routeId}`, {
            params: { limit },
        });
    },

    getSeasonalRoutes(cityId: number, season: string, days: number) {
        return apiClient.get<RoutePlan[]>('/routes/smart/seasonal', {
            params: { cityId, season, days },
        });
    },

    getThemeRoutes(theme: string, cityId: number, days: number) {
        return apiClient.get<RoutePlan[]>('/routes/smart/theme', {
            params: { theme, cityId, days },
        });
    },

    searchRoutes(title: string) {
        return apiClient.get<RoutePlan[]>('/routes/search', {
            params: { title },
        });
    },

    getRoutesByCity(cityId: number) {
        return apiClient.get<RoutePlan[]>(`/routes/city/${cityId}`);
    },
};

export const intelligentRouteApi = {
    recommendByPreference(userId: number, cityId: number, days: number, preferences: Record<string, any>) {
        return apiClient.post<RoutePlan[]>('/routes/smart/recommend-by-preference', preferences, {
            params: { userId, cityId, days },
        });
    },

    compareRoutes(routeIds: number[]) {
        return apiClient.post<Record<string, any>>('/routes/smart/compare', null, {
            params: { routeIds },
        });
    },

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
        return apiClient.get<RoutePlan[]>('/routes/smart/popular', {
            params: { cityId, days, limit },
        });
    },

    getSimilarRoutes(routeId: number, limit: number = DEFAULT_LIMIT_SMALL) {
        return apiClient.get<RoutePlan[]>(`/routes/smart/similar/${routeId}`, {
            params: { limit },
        });
    },

    getSeasonalRoutes(cityId: number, season: string, days: number) {
        return apiClient.get<RoutePlan[]>('/routes/smart/seasonal', {
            params: { cityId, season, days },
        });
    },

    getThemeRoutes(theme: string, cityId: number, days: number) {
        return apiClient.get<RoutePlan[]>('/routes/smart/theme', {
            params: { theme, cityId, days },
        });
    },

    getOptimizationSuggestions(routeId: number, optimizationType: string = 'comprehensive') {
        return apiClient.get<Record<string, any>>(`/routes/smart/optimization-suggestions/${routeId}`, {
            params: { optimizationType },
        });
    },

    getSmartRouteList(params: { type: string; cityId: number; days: number; limit?: number; season?: string; theme?: string }) {
        return apiClient.get<RoutePlan[]>('/routes/smart/list', { params });
    },

    recommendByPreference(preferences: Record<string, any>, params: { userId: number; cityId: number; days: number }) {
        return apiClient.post<RoutePlan[]>('/routes/smart/recommend-by-preference', { preferences }, { params });
    },

    compareRoutes(routeIds: number[]) {
        return apiClient.post<Record<string, any>>('/routes/smart/compare', { routeIds });
    },

    optimizeRoute(routeId: number) {
        return apiClient.post<Record<string, any>>(`/routes/smart/optimize`, null, { params: { routeId } });
    },

    getOptimizationSuggestionsForRoute(routeId: number) {
        return apiClient.get<any[]>(`/route-optimization/suggestions/${routeId}`);
    },

    getOptimizationHistory(routeId: number) {
        return apiClient.get<any[]>(`/route-optimization/history/${routeId}`);
    },

    applyOptimizationSuggestion(routeId: number, suggestionId: number, suggestion: Record<string, any>) {
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
