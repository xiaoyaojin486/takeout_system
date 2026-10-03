import request from '../utils/request'

/** POST /auth/login 账号密码登录 */
export function login(data) {
  return request.post('/auth/login', data)
}

/** POST /auth/refresh 刷新令牌（正常由 request.js 拦截器自动调用，此处供特殊场景手动使用） */
export function refresh(refreshToken) {
  return request.post('/auth/refresh', { refreshToken }, { skipAuthRefresh: true })
}

/** POST /auth/logout 退出登录 */
export function logout(refreshToken) {
  return request.post('/auth/logout', { refreshToken })
}

/** GET /auth/me 当前登录用户 */
export function getMe() {
  return request.get('/auth/me')
}