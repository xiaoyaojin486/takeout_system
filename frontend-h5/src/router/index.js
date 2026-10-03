import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由表
 * meta.public   = true 表示免登录可访问
 * meta.tabbar   = true 表示展示底部导航
 */
const routes = [
  { path: '/', redirect: '/home' },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { public: true, tabbar: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { public: true, tabbar: false }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { public: true, tabbar: true }
  },
  {
    path: '/merchant/:id',
    name: 'MerchantDetail',
    component: () => import('../views/MerchantDetail.vue'),
    meta: { tabbar: false }
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('../views/Cart.vue'),
    meta: { tabbar: true }
  },
  {
    path: '/order/confirm',
    name: 'OrderConfirm',
    component: () => import('../views/OrderConfirm.vue'),
    meta: { tabbar: false }
  },
  {
    path: '/orders',
    name: 'OrderList',
    component: () => import('../views/OrderList.vue'),
    meta: { tabbar: true }
  },
  {
    path: '/order/:id',
    name: 'OrderDetail',
    component: () => import('../views/OrderDetail.vue'),
    meta: { tabbar: false }
  },
  {
    path: '/address',
    name: 'AddressList',
    component: () => import('../views/AddressList.vue'),
    meta: { tabbar: false }
  },
  {
    path: '/address/edit/:id?',
    name: 'AddressEdit',
    component: () => import('../views/AddressEdit.vue'),
    meta: { tabbar: false }
  },
  {
    path: '/mine',
    name: 'Mine',
    component: () => import('../views/Mine.vue'),
    meta: { tabbar: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { tabbar: false }
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 登录守卫：未登录访问受保护页面时跳转登录页，并记录回跳地址
router.beforeEach((to) => {
  const logged = !!localStorage.getItem('h5_access_token')
  if (!to.meta.public && !logged) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 已登录再访问登录/注册页，直接回首页
  if (logged && (to.path === '/login' || to.path === '/register')) {
    return { path: '/home' }
  }
  return true
})

export default router