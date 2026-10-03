import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import {
  getAccessToken,
  getRefreshToken,
  setTokens,
  clearAuth,
  notifyAuthReset
} from '@/utils/auth'

const baseURL = import.meta.env.VITE_API_BASE || '/api'

const service = axios.create({
  baseURL,
  timeout: 15000
})

/* -------------------------------------------------------------------------- */
/*                              双 token 自动续期                              */
/* -------------------------------------------------------------------------- */

/** 刷新令牌请求的并发去重：同一时刻只会真正发出一次 /auth/refresh */
let refreshPromise = null
/** 防止登录过期提示 / 跳转重复触发 */
let expiredHandled = false

/** 判断是否为刷新令牌接口本身（刷新接口不参与重试） */
function isRefreshRequest(config) {
  return String(config?.url || '').includes('/auth/refresh')
}

/**
 * 清空登录态并回到登录页
 * 说明：先清 localStorage 再同步重置 Pinia（通过 auth.js 注册的回调，避免循环依赖），
 * 路由守卫以 localStorage 为准判断登录态，因此这里不会出现竞态。
 */
function handleLoginExpired() {
  clearAuth()
  notifyAuthReset()

  if (router.currentRoute.value.path === '/login') {
    return
  }
  if (!expiredHandled) {
    expiredHandled = true
    ElMessage.error('登录已过期')
    setTimeout(() => {
      expiredHandled = false
    }, 1500)
  }
  router.replace({
    path: '/login',
    query: { redirect: router.currentRoute.value.fullPath }
  })
}

/**
 * 执行刷新令牌请求
 * 用独立的 axios 实例发送，完全不经过下面的拦截器，
 * 因此刷新接口自身绝不会再次触发刷新（避免死循环）。
 */
async function doRefreshToken() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new Error('缺少 refreshToken')
  }
  const response = await axios.post(
    `${baseURL}/auth/refresh`,
    { refreshToken },
    { headers: { 'Content-Type': 'application/json' } }
  )
  const body = response.data
  if (!body || body.code !== 200 || !body.data) {
    throw new Error(body?.msg || '刷新令牌失败')
  }
  const { accessToken, refreshToken: newRefreshToken } = body.data
  setTokens(accessToken, newRefreshToken)
  return body.data
}

/** 获取（或复用）刷新 Promise —— 并发去重的核心 */
function ensureRefresh() {
  if (!refreshPromise) {
    refreshPromise = doRefreshToken().finally(() => {
      // 无论成功失败都要释放，下一次 401 才能重新发起刷新
      refreshPromise = null
    })
  }
  return refreshPromise
}

/* -------------------------------------------------------------------------- */
/*                                  拦截器                                     */
/* -------------------------------------------------------------------------- */

// 请求拦截器：统一带 Authorization
service.interceptors.request.use(
  (config) => {
    const token = getAccessToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：剥壳 + 双 token 续期
service.interceptors.response.use(
  async (response) => {
    const res = response.data

    // 非统一响应体（如文件流）直接返回
    if (!res || typeof res.code === 'undefined') {
      return res
    }

    const { code, msg, data } = res

    // 成功：剥壳
    if (code === 200) {
      return data
    }

    // accessToken 过期 → 静默刷新后重放原请求
    if (code === 40101) {
      const config = response.config
      // 刷新接口自身 / 已重放过的请求再次 40101：说明刷新无效，直接登出，避免死循环
      if (isRefreshRequest(config) || config.__isRetryRequest) {
        handleLoginExpired()
        return Promise.reject(new Error(msg || '登录已过期'))
      }
      try {
        await ensureRefresh()
      } catch (e) {
        handleLoginExpired()
        return Promise.reject(new Error(msg || '登录已过期'))
      }
      // 标记为重放请求，并用新 token 重放
      config.__isRetryRequest = true
      const newToken = getAccessToken()
      if (newToken) {
        config.headers.Authorization = `Bearer ${newToken}`
      }
      return service(config)
    }

    // refreshToken 失效 / 未认证 → 清空登录态并跳登录页
    if (code === 40102 || code === 401) {
      handleLoginExpired()
      return Promise.reject(new Error(msg || '登录已过期'))
    }

    // 已登录但无权访问（跨端调用 / 越权）→ 只提示，不跳登录页
    if (code === 403) {
      ElMessage.error(msg || '无权访问该操作')
      return Promise.reject(new Error(msg || '无权访问该操作'))
    }

    // 其它业务错误
    ElMessage.error(msg || '请求失败')
    return Promise.reject(new Error(msg || '请求失败'))
  },
  async (error) => {
    // HTTP 层错误
    const response = error.response
    const status = response?.status
    const body = response?.data
    const bizCode = body?.code
    const bizMsg = body?.msg

    // 【关键】后端在 accessToken 过期时返回的是「HTTP 401 + body.code=40101」，
    // 业务码在响应体里而不在 HTTP 状态码上。
    // 若在这里一见 401 就跳登录页，自动续期将永远不会触发（令牌一过期就被踢出去）。
    // 因此必须先看业务码，只有确认不是「可续期」的情况才当作未登录处理。
    if (bizCode === 40101) {
      const config = error.config
      if (!config || isRefreshRequest(config) || config.__isRetryRequest) {
        handleLoginExpired()
        return Promise.reject(error)
      }
      try {
        await ensureRefresh()
      } catch (e) {
        handleLoginExpired()
        return Promise.reject(error)
      }
      config.__isRetryRequest = true
      const newToken = getAccessToken()
      if (newToken) {
        config.headers = config.headers || {}
        config.headers.Authorization = `Bearer ${newToken}`
      }
      return service(config)
    }

    // refreshToken 失效 / 未认证 / 令牌非法
    if (status === 401 || bizCode === 40102 || bizCode === 401) {
      handleLoginExpired()
      return Promise.reject(error)
    }
    if (status === 403 || bizCode === 403) {
      ElMessage.error(bizMsg || '无权访问该操作')
      return Promise.reject(error)
    }
    if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
      return Promise.reject(error)
    }
    ElMessage.error(bizMsg || error.message || '网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

export default service