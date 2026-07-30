import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior() {
    return { top: 0 }
  },
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
    },
{
      path: '/attractions',
      name: 'attractions',
      component: () => import('../views/AttractionsView.vue'),
    },
{
      path: '/routes',
      name: 'routes',
      component: () => import('../views/RoutesView.vue'),
    },
{
      path: '/notes',
      name: 'notes',
      component: () => import('../views/NotesView.vue'),
    },
{
      path: '/ai-chat',
      name: 'aiChat',
      component: () => import('../views/AIChatView.vue'),
    },
{
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
    },
{
      path: '/profile',
      name: 'profile',
      component: () => import('../views/UserProfileView.vue'),
      meta: { requiresAuth: true },
    },
{
      path: '/notifications',
      name: 'notifications',
      component: () => import('../views/NotificationView.vue'),
      meta: { requiresAuth: true },
    },
{
      path: '/restaurants',
      name: 'restaurants',
      component: () => import('../views/RestaurantView.vue'),
    },
{
      path: '/feedback',
      name: 'feedback',
      component: () => import('../views/FeedbackView.vue'),
      meta: { requiresAuth: true },
    },
{
      path: '/files',
      name: 'files',
      component: () => import('../views/FileManagementView.vue'),
      meta: { requiresAuth: true },
    },
{
      path: '/share',
      name: 'share',
      component: () => import('../views/RouteShareView.vue'),
      meta: { requiresAuth: true },
    },
{
      path: '/realtime',
      name: 'realtime',
      component: () => import('../views/RealtimeStatusView.vue'),
    },
{
      path: '/optimization',
      name: 'optimization',
      component: () => import('../views/RouteOptimizationView.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

// 路由守卫：检查需要登录的页面
router.beforeEach((to, _from, next) => {
  if (to.meta.requiresAuth && !localStorage.getItem('token')) {
    next({ name: 'login' })
  } else {
    next()
  }
})

export default router