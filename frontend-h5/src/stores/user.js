import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getMe } from '../api/auth'
import {
  clearAuth,
  getAccessToken,
  getRefreshToken,
  getStoredUser,
  setStoredUser,
  setTokens
} from '../utils/request'

/**
 * 用户 store：token 对 + 用户信息 + 登录/登出
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: getStoredUser(),
    accessToken: getAccessToken(),
    refreshToken: getRefreshToken()
  }),

  getters: {
    isLoggedIn: (state) => !!state.accessToken,
    displayName: (state) => state.userInfo?.nickname || state.userInfo?.username || '未登录'
  },

  actions: {
    /** 账号密码登录（顾客端固定 clientType=CUSTOMER） */
    async login({ username, password }) {
      const tokenVO = await loginApi({ username, password, clientType: 'CUSTOMER' })
      setTokens(tokenVO)
      this.accessToken = tokenVO.accessToken
      this.refreshToken = tokenVO.refreshToken
      await this.loadProfile()
      return tokenVO
    },

    /** 拉取当前登录用户信息 */
    async loadProfile() {
      const profile = await getMe()
      this.userInfo = profile
      setStoredUser(profile)
      return profile
    },

    /** 退出登录：先通知后端作废 refresh token，失败也要清本地 */
    async logout() {
      try {
        if (this.refreshToken) await logoutApi(this.refreshToken)
      } catch (e) {
        // 忽略：网络异常不应阻塞本地登出
      }
      this.resetAuth()
    },

    /** 仅清理本地登录态 */
    resetAuth() {
      clearAuth()
      this.userInfo = null
      this.accessToken = ''
      this.refreshToken = ''
    }
  }
})