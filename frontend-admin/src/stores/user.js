import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getMe } from '@/api/auth'
import {
  getAccessToken,
  getRefreshToken,
  setTokens,
  setUserInfo,
  getUserInfo,
  clearAuth,
  setAuthResetHandler
} from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    accessToken: getAccessToken(),
    refreshToken: getRefreshToken(),
    userInfo: getUserInfo()
  }),

  getters: {
    /** 是否已登录 */
    isLogin: (state) => !!state.accessToken,
    /** 身份：1 顾客 / 2 商家 / 3 管理员 */
    userType: (state) => state.userInfo?.userType,
    /** 展示名 */
    displayName: (state) =>
      state.userInfo?.nickname || state.userInfo?.username || '',
    /** 是否为商家端 */
    isMerchant: (state) => state.userInfo?.userType === 2,
    /** 是否为管理后台 */
    isAdmin: (state) => state.userInfo?.userType === 3
  },

  actions: {
    /**
     * 登录
     * @param {{username, password, clientType}} payload clientType: MERCHANT / ADMIN
     */
    async login(payload) {
      const tokenVO = await loginApi(payload)
      this.accessToken = tokenVO.accessToken
      this.refreshToken = tokenVO.refreshToken
      setTokens(tokenVO.accessToken, tokenVO.refreshToken)
      // 登录后拉取当前用户信息，用于按 userType 挂载菜单
      await this.fetchMe()
      return this.userInfo
    },

    /** 拉取当前登录用户信息 */
    async fetchMe() {
      const me = await getMe()
      this.userInfo = me
      setUserInfo(me)
      return me
    },

    /** 退出登录（失败也强制清理本地登录态） */
    async logout() {
      try {
        await logoutApi({ refreshToken: this.refreshToken })
      } catch (e) {
        /* 退出接口异常不影响本地登出 */
      }
      this.reset()
    },

    /** 清空登录态 */
    reset() {
      this.accessToken = ''
      this.refreshToken = ''
      this.userInfo = null
      clearAuth()
    }
  }
})

/**
 * 注册登录态重置回调：
 * utils/request.js 在 401 / 40102 / 刷新失败时同步调用，清理 Pinia 中的登录态。
 * 采用回调注册而非直接 import，避免 request.js ↔ stores/user.js 循环依赖。
 */
setAuthResetHandler(() => {
  useUserStore().reset()
})