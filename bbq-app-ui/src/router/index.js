import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Home', component: () => import('../views/Home.vue') },
  { path: '/cart', name: 'Cart', component: () => import('../views/Cart.vue') },
  { path: '/order-result', name: 'OrderResult', component: () => import('../views/OrderResult.vue') },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  {
    path: '/user',
    name: 'UserHome',
    component: () => import('../views/UserHome.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/Admin.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/m-admin',
    name: 'MobileAdmin',
    component: () => import('../views/MobileAdmin.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/flash-sale',
    name: 'FlashSale',
    component: () => import('../views/FlashSale.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫：未登录跳登录页
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth) {
    const token = localStorage.getItem('token')
    if (!token) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }
  next()
})

export default router
