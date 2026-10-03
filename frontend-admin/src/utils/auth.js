/**
 * 登录态本地存储工具
 * token 统一存 localStorage，key 固定为：
 *  - admin_access_token
 *  - admin_refresh_token
 *  - admin_user
 */
const ACCESS_TOKEN_KEY = 'admin_access_token'
const REFRESH_TOKEN_KEY = 'admin_refresh_token'
const USER_INFO_KEY = 'admin_user'

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY) || ''
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY) || ''
}

export function getUserInfo() {
  const raw = localStorage.getItem(USER_INFO_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (e) {
    return null
  }
}

/** 写入双 token（旧 refreshToken 会被立即作废，需整体覆盖） */
export function setTokens(accessToken, refreshToken) {
  if (accessToken) {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  }
  if (refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  }
}

export function setUserInfo(user) {
  if (user) {
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(user))
  } else {
    localStorage.removeItem(USER_INFO_KEY)
  }
}

/** 清空登录态 */
export function clearAuth() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_INFO_KEY)
}

/**
 * 登录态重置回调注册（由 Pinia store 注册，request.js 在 token 失效时同步调用）
 * 目的：让 request.js 与 stores/user.js 之间不产生循环依赖。
 */
let authResetHandler = null

export function setAuthResetHandler(handler) {
  authResetHandler = handler
}

export function notifyAuthReset() {
  if (typeof authResetHandler === 'function') {
    try {
      authResetHandler()
    } catch (e) {
      /* 无 Pinia 实例时忽略 */
    }
  }
}