import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { useUserStore } from '@/stores/user'
import { getAccessToken } from '@/utils/auth'
import { APP_TITLE, CLIENT_TYPE_TITLE } from '@/utils/constants'

/**
 * 商家端菜单（userType = 2）
 */
export const merchantRoutes = {
  path: '/merchant',
  component: AdminLayout,
  redirect: '/merchant/dashboard',
  meta: { title: '商家端', requiredUserType: 2 },
  children: [
    {
      path: 'dashboard',
      name: 'MerchantDashboard',
      component: () => import('@/views/merchant/Dashboard.vue'),
      meta: { title: '工作台', icon: 'HomeFilled' }
    },
    {
      path: 'shop',
      name: 'MerchantShop',
      component: () => import('@/views/merchant/Shop.vue'),
      meta: { title: '店铺设置', icon: 'Shop' }
    },
    {
      path: 'category',
      name: 'MerchantCategory',
      component: () => import('@/views/merchant/Category.vue'),
      meta: { title: '分类管理', icon: 'Menu' }
    },
    {
      path: 'dish',
      name: 'MerchantDish',
      component: () => import('@/views/merchant/Dish.vue'),
      meta: { title: '菜品管理', icon: 'Dish' }
    },
    {
      path: 'order',
      name: 'MerchantOrder',
      component: () => import('@/views/merchant/Order.vue'),
      meta: { title: '订单管理', icon: 'List' }
    },
    {
      path: 'stats',
      name: 'MerchantStats',
      component: () => import('@/views/merchant/Stats.vue'),
      meta: { title: '经营统计', icon: 'TrendCharts' }
    }
  ]
}

/**
 * 管理后台菜单（userType = 3）
 */
export const adminRoutes = {
  path: '/admin',
  component: AdminLayout,
  redirect: '/admin/dashboard',
  meta: { title: '管理后台', requiredUserType: 3 },
  children: [
    {
      path: 'dashboard',
      name: 'AdminDashboard',
      component: () => import('@/views/admin/Dashboard.vue'),
      meta: { title: '平台看板', icon: 'DataLine' }
    },
    {
      path: 'merchant-audit',
      name: 'AdminMerchantAudit',
      component: () => import('@/views/admin/MerchantAudit.vue'),
      meta: { title: '商家审核', icon: 'Stamp' }
    },
    {
      path: 'merchant-list',
      name: 'AdminMerchantList',
      component: () => import('@/views/admin/MerchantList.vue'),
      meta: { title: '商家列表', icon: 'Shop' }
    },
    {
      path: 'user-list',
      name: 'AdminUserList',
      component: () => import('@/views/admin/UserList.vue'),
      meta: { title: '用户管理', icon: 'User' }
    },
    {
      path: 'order-list',
      name: 'AdminOrderList',
      component: () => import('@/views/admin/OrderList.vue'),
      meta: { title: '订单总览', icon: 'Tickets' }
    }
  ]
}

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  merchantRoutes,
  adminRoutes,
  { path: '/', redirect: '/login' },
  { path: '/:pathMatch(.*)*', redirect: '/', meta: { title: '页面不存在' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

/** 根据 userType 计算首页地址 */
export function homePath(userType) {
  return userType === 3 ? '/admin/dashboard' : '/merchant/dashboard'
}

/** 根据 userType 取当前角色分组标题 */
export function getRoleTitle(userType) {
  if (userType === 3) return CLIENT_TYPE_TITLE.ADMIN
  if (userType === 2) return CLIENT_TYPE_TITLE.MERCHANT
  return APP_TITLE
}

/**
 * 路由守卫：
 * 1. 未登录且访问非登录页 → 跳登录页（带 redirect）
 * 2. 已登录且访问登录页 → 跳各自首页
 * 3. 校验 meta.requiredUserType，防止商家手动改 URL 进管理页面
 */
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  // 登录态以 localStorage 为准（token 被拦截器清掉后能立即生效）
  const hasToken = !!getAccessToken() || userStore.isLogin

  if (to.path === '/login') {
    if (hasToken && userStore.userInfo?.userType) {
      return next(homePath(userStore.userInfo.userType))
    }
    document.title = `${CLIENT_TYPE_TITLE.MERCHANT} / ${CLIENT_TYPE_TITLE.ADMIN} - ${APP_TITLE}`
    return next()
  }

  if (!hasToken) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  // token 存在但缺少用户信息（异常场景）→ 补拉一次
  if (!userStore.userInfo) {
    try {
      await userStore.fetchMe()
    } catch (e) {
      userStore.reset()
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }
  }

  // 角色校验：商家不能进管理后台，管理员不能进商家端
  const required = to.meta?.requiredUserType
  if (required && required !== userStore.userInfo?.userType) {
    ElMessage.error('无权访问该页面')
    return next(homePath(userStore.userInfo?.userType))
  }

  document.title = to.meta?.title
    ? `${to.meta.title} - ${getRoleTitle(userStore.userInfo?.userType)} - ${APP_TITLE}`
    : APP_TITLE
  next()
})

export default router