import request from '../utils/request'

/**
 * POST /customer/order 提交订单
 * 请求体只传 { addressId, remark, items:[{dishId, quantity}] }，金额由后端重算
 * @param {object} data 订单数据
 * @param {string} idempotentToken 一次性幂等 token，放请求头 X-Idempotent-Token
 */
export function createOrder(data, idempotentToken) {
  return request.post('/customer/order', data, {
    headers: { 'X-Idempotent-Token': idempotentToken }
  })
}

/** GET /customer/order 订单列表（分页 + 状态筛选） */
export function getOrderList(params) {
  return request.get('/customer/order', { params })
}

/** GET /customer/order/{id} 订单详情（含状态时间轴） */
export function getOrderDetail(id) {
  return request.get(`/customer/order/${id}`)
}

/** PUT /customer/order/{id}/cancel 取消订单 */
export function cancelOrder(id, reason) {
  return request.put(`/customer/order/${id}/cancel`, { reason })
}

/** PUT /customer/order/{id}/confirm 确认收货 */
export function confirmOrder(id) {
  return request.put(`/customer/order/${id}/confirm`)
}