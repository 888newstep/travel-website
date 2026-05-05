// frontend/src/api/config.ts
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const API_ENDPOINTS = {
    // 系统相关
    HEALTH: '/health',
    VERSION: '/version',
    STATUS: '/status',

    // 用户相关
    USER_LOGIN: '/users/login',
    USER_REGISTER: '/users/register',
    USER_CAPTCHA: '/users/captcha',
    USER_PROFILE: '/users/current',
    USER_LOGOUT: '/users/logout',

    // 景点相关
    ATTRACTIONS: '/attractions',
    ATTRACTION_DETAIL: (id: number) => `/attractions/${id}`,
    ATTRACTIONS_BY_CITY: (cityId: number) => `/attractions/city/${cityId}`,
    ATTRACTIONS_SEARCH: '/attractions/search',
    ATTRACTIONS_RECOMMEND: '/attractions/recommend',

    // 路线规划相关
    ROUTE_PLANS: '/route-plan/my',
    ROUTE_PLAN_CREATE: '/route-plan/create',
    ROUTE_PLAN_RECOMMEND: '/route-plan/recommend',
    ROUTE_PLAN_OPTIMIZE: (routeId: number) => `/route-plan/optimize/${routeId}`,
    ROUTE_PLAN_DETAIL: (id: number) => `/route-plan/search`,
    INTELLIGENT_ROUTE: '/intelligent-route/generate-personalized',
    ROUTE_POPULAR: '/route-plan/popular',

    // 路线优化相关
    ROUTE_OPTIMIZATION: '/route-optimization/optimize',
    ROUTE_OPTIMIZATION_SUGGESTIONS: (routeId: number) => `/route-optimization/suggestions/${routeId}`,

    // 游记相关
    TRAVEL_NOTES: '/travel-notes/list',
    TRAVEL_NOTES_HOT: '/travel-notes/hot',
    TRAVEL_NOTES_LATEST: '/travel-notes/latest',
    TRAVEL_NOTE_DETAIL: (id: number) => `/travel-notes/${id}`,
    TRAVEL_NOTE_CREATE: '/travel-notes',
    TRAVEL_NOTE_UPDATE: (id: number) => `/travel-notes/${id}`,
    TRAVEL_NOTE_DELETE: (id: number) => `/travel-notes/${id}`,
    TRAVEL_NOTE_LIKE: (id: number) => `/travel-notes/${id}/like`,
    TRAVEL_NOTE_UNLIKE: (id: number) => `/travel-notes/${id}/unlike`,
    TRAVEL_NOTE_VIEW: (id: number) => `/travel-notes/${id}/view`,
    TRAVEL_NOTE_SEARCH: '/travel-notes/search',
    TRAVEL_NOTE_USER: (userId: number) => `/travel-notes/user/${userId}`,

    // 收藏相关
    COLLECTIONS: '/route-collection/list',
    COLLECTION_ADD: '/route-collection/add',
    COLLECTION_REMOVE: '/route-collection/remove',
    COLLECTION_CHECK: '/route-collection/check',
    COLLECTION_CATEGORIES: '/route-collection/categories',

    // 行程相关
    ITINERARIES: '/route-plan/my',

    // AI相关
    AI_CHAT: '/ai/advanced/chatbot',
    AI_TRANSLATE: '/ai/assistant/translate',
    AI_BUDGET: '/ai/assistant/budget',
    AI_IMAGE_ANALYSIS: '/ai/image/analyze',

    // 社区相关
    NOTE_COMMENT: (id: number) => `/route-comments/note/${id}`,

    // 分享相关
    SHARE_CODE: '/share/generate',

    // 协作相关
    COLLABORATION: '/collaboration',
    COLLABORATORS: '/collaboration/collaborators',
    TASKS: '/collaboration/tasks',

    // 资源文件相关
    RESOURCE_FILES: '/resource-files',
    FILE_CATEGORIES: '/file-categories',

    // 实时状态相关
    REALTIME_STATUS: '/realtime-status',

    // 数据分析相关
    ANALYTICS_OVERVIEW: '/analytics/overview',
    ANALYTICS_USER_ACTIVITY: '/analytics/user-activity',
} as const;
