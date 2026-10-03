/**
 * axios 封装 + 双 token 自动续期
 *
 * 后端统一响应体：{ code, msg, data }
 *  - 200   → resolve(data)（剥壳）
 *  - 40101 → accessToken 过期：静默刷新后【重放】原请求
 *  - 40102 / 401 / HTTP 401 → 清空登录态 → 跳登录页
 *  - 403   → 仅提示（重登也没用）
 *
 * 注意：后端在 accessToken 过期时返回的是 HTTP 401 + body.code=40101，
 * 因此错误分支（error）里必须优先看 body.code，不能一见 HTTP 401 就登出。
 */
import axios from 'axios'
import { showToast } from 'vant'
import router from '../router'

/** 统一错误码 */
const CODE_SUCCESS = 200
const CODE_ACCESS_EXPIRED = 40101 // accessToken 过期
const CODE_REFRESH_INVALID = 40102 // refreshToken 失效
const CODE_UNAUTHORIZED = 401 // 未认证 / 令牌非法
const CODE_FORBIDDEN = 403 // 无权限

/** localStorage key 约定 */
export const ACCESS_TOKEN_KEY = 'h5_access_token'
export const REFRESH_TOKEN_KEY = 'h5_refresh_token'
export const USER_KEY = 'h5_user'

const API_BASE = import.meta.env.VITE_API_BASE || '/api'

/* ------------------------- 本地登录态存取 ------------------------- */

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY) || ''
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY) || ''
}

export function getStoredUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (e) {
    return null
  }
}

/** 写入 token 对（登录成功 / 刷新成功都会走这里） */
export function setTokens(tokenVO) {
  if (!tokenVO) return
  if (tokenVO.accessToken) localStorage.setItem(ACCESS_TOKEN_KEY, tokenVO.accessToken)
  if (tokenVO.refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, tokenVO.refreshToken)
}

export function setStoredUser(user) {
  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
  else localStorage.removeItem(USER_KEY)
}

/** 清空登录态 */
export function clearAuth() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

/** 登录失效统一出口：清空登录态 + 提示 + 跳登录页（保留回跳地址） */
let redirecting = false
export function redirectToLogin(message) {
  clearAuth()
  if (message) showToast(message)
  const current = router.currentRoute.value
  if (current.path === '/login' || redirecting) return
  redirecting = true
  router
    .replace({ path: '/login', query: { redirect: current.fullPath } })
    .catch(() => {})
    .finally(() => {
      redirecting = false
    })
}

/* ------------------------- axios 实例 ------------------------- */

const service = axios.create({
  baseURL: API_BASE,
  timeout: 15000
})

/**
 * 并发去重：同一时刻多个 40101 只发一次 /auth/refresh，其余请求 await 同一个 Promise
 */
let refreshPromise = null

/**
 * 静默续期：换取新 token 对并写入本地
 * @returns {Promise<string>} 新的 accessToken
 */
function requestNewToken() {
  if (refreshPromise) return refreshPromise

  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    return Promise.reject(new Error('缺少刷新令牌'))
  }

  refreshPromise = service
    // skipAuthRefresh 标记：刷新接口自身不携带旧 accessToken、响应也不触发续期
    .post('/auth/refresh', { refreshToken }, { skipAuthRefresh: true })
    .then((tokenVO) => {
      setTokens(tokenVO)
      return tokenVO.accessToken
    })
    .finally(() => {
      // 无论成功失败都释放，避免后续请求永远复用已结束的 Promise
      refreshPromise = null
    })

  return refreshPromise
}

/**
 * 续期成功 → 用新 token 重放被中断的请求
 * 重放请求带 _retried 标记，若仍返回 40101 则直接登出，避免死循环
 */
function retryAfterRefresh(config) {
  if (!config) {
    redirectToLogin('登录已过期')
    return Promise.reject(new Error('登录已过期'))
  }
  if (config._retried) {
    redirectToLogin('登录已过期')
    return Promise.reject(new Error('登录已过期'))
  }
  config._retried = true

  return requestNewToken()
    .then((accessToken) => {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${accessToken}`
      return service(config)
    })
    .catch((err) => {
      // 刷新失败（如 refreshToken 失效）：清空 token → 跳登录页
      redirectToLogin('登录已过期，请重新登录')
      return Promise.reject(err)
    })
}

/* ------------------------- 拦截器 ------------------------- */

// 请求拦截器：注入 Authorization
service.interceptors.request.use(
  (config) => {
    if (!config.skipAuthRefresh) {
      const token = getAccessToken()
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一剥壳 + 错误码分流
service.interceptors.response.use(
  (response) => {
    const body = response.data

    // 非统一响应结构（如文件流）原样返回
    if (!body || typeof body !== 'object' || !('code' in body)) return body

    // 刷新接口自身不参与续期/重放，只做剥壳
    if (response.config && response.config.skipAuthRefresh) {
      if (body.code === CODE_SUCCESS) return body.data
      return Promise.reject(new Error(body.msg || '刷新令牌失败'))
    }

    const { code, msg, data } = body

    if (code === CODE_SUCCESS) return data

    if (code === CODE_ACCESS_EXPIRED) {
      return retryAfterRefresh(response.config)
    }

    if (code === CODE_REFRESH_INVALID || code === CODE_UNAUTHORIZED) {
      redirectToLogin('登录已过期')
      return Promise.reject(new Error(msg || '登录已过期'))
    }

    if (code === CODE_FORBIDDEN) {
      // 无权访问：重登也没用，只提示
      showToast(msg || '没有操作权限')
      return Promise.reject(new Error(msg || '没有操作权限'))
    }

    showToast(msg || '请求失败')
    return Promise.reject(new Error(msg || '请求失败'))
  },
  (error) => {
    const response = error.response
    const body = response && response.data
    const code = body && typeof body === 'object' ? body.code : undefined

    // 后端用 HTTP 401 + code=40101 表达 accessToken 过期，必须优先续期
    if (code === CODE_ACCESS_EXPIRED) {
      return retryAfterRefresh(error.config)
    }

    if (
      (response && response.status === 401) ||
      code === CODE_REFRESH_INVALID ||
      code === CODE_UNAUTHORIZED
    ) {
      redirectToLogin('登录已过期')
      return Promise.reject(error)
    }

    if ((response && response.status === 403) || code === CODE_FORBIDDEN) {
      showToast((body && body.msg) || '没有操作权限')
      return Promise.reject(error)
    }

    showToast((body && body.msg) || error.message || '网络异常，请稍后再试')
    return Promise.reject(error)
  }
)

export default service