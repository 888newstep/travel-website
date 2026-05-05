// frontend/src/api/route.ts
import { apiClient } from './request';
import { API_ENDPOINTS } from './config';

export interface RoutePlan {
    id: number;
    title: string;
    destination: string;
    startDate: string;
    endDate: string;
    days: number;
    activities: number;
    status: string;
    isPublic: boolean;
    completionRate: number;
    image?: string;
    createTime?: string;
}

export interface IntelligentRouteRequest {
    destination: string;
    duration: number;
    preferences: string[];
    budget?: number;
    transportMode?: string;
}

export const routeApi = {
    getList: (userId: number) =>
        apiClient.get<RoutePlan[]>(API_ENDPOINTS.ROUTE_PLANS, { userId }),

    getDetail: (_id: number) =>
        apiClient.get<RoutePlan>(API_ENDPOINTS.ROUTE_PLAN_DETAIL(_id), { title: _id }),

    createIntelligentRoute: (data: IntelligentRouteRequest) =>
        apiClient.post<RoutePlan>(API_ENDPOINTS.INTELLIGENT_ROUTE, data),

    optimizeRoute: (routeId: number, data?: any) =>
        apiClient.post(API_ENDPOINTS.ROUTE_PLAN_OPTIMIZE(routeId), data),

    getOptimizationSuggestions: (routeId: number) =>
        apiClient.get(API_ENDPOINTS.ROUTE_OPTIMIZATION_SUGGESTIONS(routeId)),

    recommend: (data: any) =>
        apiClient.post(API_ENDPOINTS.ROUTE_PLAN_RECOMMEND, data),
};
