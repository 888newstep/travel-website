import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import ProfileView from '../views/ProfileView.vue';
import AttractionsView from '../views/AttractionsView.vue';
import ItinerariesView from '../views/ItinerariesView.vue';
import CommunityView from '../views/CommunityView.vue';
import PlanningView from '../views/PlanningView.vue';

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            name: 'home',
            component: HomeView,
        },
        {
            path: '/profile',
            name: 'profile',
            component: ProfileView,
            meta: { requiresAuth: true }
        },
        {
            path: '/attractions',
            name: 'attractions',
            component: AttractionsView,
        },
        {
            path: '/itineraries',
            name: 'itineraries',
            component: ItinerariesView,
        },
        {
            path: '/community',
            name: 'community',
            component: CommunityView,
        },
        {
            path: '/planning',
            name: 'planning',
            component: PlanningView,
        },
    ],
});

// 路由守卫 - 检查认证状态
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token');

    if (to.meta.requiresAuth && !token) {
        // 如果需要认证但没有登录，重定向到首页
        next('/');
    } else {
        next();
    }
});

export default router;
