import request from '@/utils/request'

/** 登录：{username, password, clientType} → TokenVO */
export function login(data) {
  return request.post('/auth/login', data)
}

/** 刷新令牌：{refreshToken} → TokenVO（旧 refresh 立即作废） */
export function refreshToken(data) {
  return request.post('/auth/refresh', data)
}

/** 退出登录：{refreshToken} 需带 accessToken */
export function logout(data) {
  return request.post('/auth/logout', data)
}

/** 当前登录用户信息 */
export function getMe() {
  return request.get('/auth/me')
}