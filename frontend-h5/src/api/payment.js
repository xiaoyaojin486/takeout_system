import request from '../utils/request'

/** POST /customer/payment/{orderNo}/pay 发起支付（一期固定模拟支付） */
export function payOrder(orderNo, data) {
  return request.post(`/customer/payment/${orderNo}/pay`, data || {})
}

/** GET /customer/payment/{orderNo} 查询支付单（不存在时 data 为 null） */
export function getPayment(orderNo) {
  return request.get(`/customer/payment/${orderNo}`)
}