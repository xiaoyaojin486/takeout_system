import request from '@/utils/request'

/* ------------------------------- 商家端订单 ------------------------------- */

/** 商家订单分页：pageNum, pageSize, status? */
export function getMerchantOrderPage(params) {
  return request.get('/merchant/order', { params })
}

/** 商家侧订单详情 */
export function getMerchantOrderDetail(id) {
  return request.get(`/merchant/order/${id}`)
}

/** 接单：待接单 → 备餐中 */
export function acceptOrder(id) {
  return request.put(`/merchant/order/${id}/accept`)
}

/** 拒单：{reason} 待接单 → 已取消 */
export function rejectOrder(id, data) {
  return request.put(`/merchant/order/${id}/reject`, data)
}

/** 商家取消：{reason?} 待接单/备餐中 → 已取消 */
export function cancelOrder(id, data) {
  return request.put(`/merchant/order/${id}/cancel`, data)
}

/** 出餐：备餐中 → 配送中 */
export function deliverOrder(id) {
  return request.put(`/merchant/order/${id}/deliver`)
}

/** 送达：配送中 → 已完成 */
export function finishOrder(id) {
  return request.put(`/merchant/order/${id}/finish`)
}

/* ------------------------------- 管理端订单 ------------------------------- */

/** 全平台订单分页：同商家端 + merchantId? */
export function getAdminOrderPage(params) {
  return request.get('/admin/order', { params })
}

/** 管理端订单详情（含状态时间轴） */
export function getAdminOrderDetail(id) {
  return request.get(`/admin/order/${id}`)
}