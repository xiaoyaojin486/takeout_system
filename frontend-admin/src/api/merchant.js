import request from '@/utils/request'

/* --------------------------- 管理后台 · 商家管理 --------------------------- */

/** 商家分页：pageNum, pageSize, keyword?, auditStatus?, businessStatus? */
export function getMerchantPage(params) {
  return request.get('/admin/merchant', { params })
}

/** 审核商家：{auditStatus, auditRemark?} 1 通过 / 2 驳回 */
export function auditMerchant(id, data) {
  return request.put(`/admin/merchant/${id}/audit`, data)
}

/** 强制打烊 / 恢复营业：{businessStatus} */
export function updateMerchantBusinessStatus(id, data) {
  return request.put(`/admin/merchant/${id}/business-status`, data)
}