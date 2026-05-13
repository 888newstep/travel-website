import apiClient from '../utils/api';

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
        return apiClient.post<RoutePlan>('/route-plan/create', data);
    },

    recommendRoutes(data: {
        userId?: number;
        cityId?: number;
        days?: number;
        preferences?: Record<string, any>;
    }) {
        return apiClient.post<RoutePlan[]>('/route-plan/recommend', data);
    },

    optimizeRoute(routeId: number, optimizationType: string) {
        return apiClient.post<RouteOptimization>(`/route-plan/optimize/${routeId}`, {
            optimizationType,
        });
    },

    evaluateRoute(routeId: number, evaluationParams: Record<string, any>) {
        return apiClient.post<RouteEvaluation>(`/route-plan/evaluate/${routeId}`, evaluationParams);
    },

    getMyRoutePlans(userId: number) {
        return apiClient.get<RoutePlan[]>('/route-plan/my', {
            params: { userId },
        });
    },

    compareRoutes(routeIds: number[]) {
        return apiClient.post<Record<string, any>>('/route-plan/compare', {
            routeIds,
        });
    },

    adjustRoute(routeId: number, data: {
        currentLocation?: Record<string, number>;
        realTimeFactors?: Record<string, any>;
    }) {
        return apiClient.post<Record<string, any>>(`/route-plan/adjust/${routeId}`, data);
    },

    getPopularRoutes(cityId: number, days: number, limit: number = 5) {
        return apiClient.get<RoutePlan[]>('/route-plan/popular', {
            params: { cityId, days, limit },
        });
    },

    getSimilarRoutes(routeId: number, limit: number = 5) {
        return apiClient.get<RoutePlan[]>(`/route-plan/similar/${routeId}`, {
            params: { limit },
        });
    },

    getSeasonalRoutes(cityId: number, season: string, days: number) {
        return apiClient.get<RoutePlan[]>('/route-plan/seasonal', {
            params: { cityId, season, days },
        });
    },

    getThemeRoutes(theme: string, cityId: number, days: number) {
        return apiClient.get<RoutePlan[]>('/route-plan/theme', {
            params: { theme, cityId, days },
        });
    },

    searchRoutes(title: string) {
        return apiClient.get<RoutePlan[]>('/route-plan/search', {
            params: { title },
        });
    },

    getRoutesByCity(cityId: number) {
        return apiClient.get<RoutePlan[]>(`/route-plan/city/${cityId}`);
    },
};

export const intelligentRouteApi = {
    recommendByPreference(userId: number, cityId: number, days: number, preferences: Record<string, any>) {
        return apiClient.post<RoutePlan[]>('/intelligent-route/recommend-by-preference', preferences, {
            params: { userId, cityId, days },
        });
    },

    compareRoutes(routeIds: number[]) {
        return apiClient.post<Record<string, any>>('/intelligent-route/compare', null, {
            params: { routeIds },
        });
    },

    getRealTimeAdjustment(routeId: number, data: {
        currentLocation?: Record<string, number>;
        realTimeFactors?: Record<string, any>;
    }) {
        return apiClient.post<Record<string, any>>(`/intelligent-route/real-time-adjustment/${routeId}`, data);
    },

    evaluateRouteQuality(routeId: number, evaluationParams: Record<string, any>) {
        return apiClient.post<RouteEvaluation>(`/intelligent-route/evaluate/${routeId}`, evaluationParams);
    },

    generatePersonalizedRoute(data: {
        userPreferences?: Record<string, any>;
        constraints?: Record<string, any>;
    }) {
        return apiClient.post<RoutePlan>('/intelligent-route/generate-personalized', data);
    },

    getPopularRoutes(cityId: number, days: number, limit: number = 5) {
        return apiClient.get<RoutePlan[]>('/intelligent-route/popular', {
            params: { cityId, days, limit },
        });
    },

    getSimilarRoutes(routeId: number, limit: number = 5) {
        return apiClient.get<RoutePlan[]>(`/intelligent-route/similar/${routeId}`, {
            params: { limit },
        });
    },

    getSeasonalRoutes(cityId: number, season: string, days: number) {
        return apiClient.get<RoutePlan[]>('/intelligent-route/seasonal', {
            params: { cityId, season, days },
        });
    },

    getThemeRoutes(theme: string, cityId: number, days: number) {
        return apiClient.get<RoutePlan[]>('/intelligent-route/theme', {
            params: { theme, cityId, days },
        });
    },

    getOptimizationSuggestions(routeId: number, optimizationType: string = 'comprehensive') {
        return apiClient.get<Record<string, any>>(`/intelligent-route/optimization-suggestions/${routeId}`, {
            params: { optimizationType },
        });
    },
};
