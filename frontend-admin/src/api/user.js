import request from '@/utils/request'

/* --------------------------- 管理后台 · 用户管理 --------------------------- */

/** 用户分页：pageNum, pageSize, keyword?, userType?, status? */
export function getUserPage(params) {
  return request.get('/admin/user', { params })
}

/** 启用 / 停用：{status} 1 正常 / 0 停用 */
export function updateUserStatus(id, data) {
  return request.put(`/admin/user/${id}/status`, data)
}

/** 重置密码：{newPassword} */
export function resetUserPassword(id, data) {
  return request.put(`/admin/user/${id}/reset-password`, data)
}