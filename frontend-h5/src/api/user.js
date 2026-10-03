import request from '../utils/request'

/** POST /customer/user/register 注册（免登录） */
export function register(data) {
  return request.post('/customer/user/register', data)
}

/** GET /customer/user/profile 个人资料 */
export function getProfile() {
  return request.get('/customer/user/profile')
}

/** PUT /customer/user/profile 修改个人资料 */
export function updateProfile(data) {
  return request.put('/customer/user/profile', data)
}

/** PUT /customer/user/password 修改密码 */
export function changePassword(data) {
  return request.put('/customer/user/password', data)
}