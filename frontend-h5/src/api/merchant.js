import request from '../utils/request'

/** GET /customer/merchant/list 店铺列表（分页 + 关键词） */
export function getMerchantList(params) {
  return request.get('/customer/merchant/list', { params })
}

/** GET /customer/merchant/{id} 店铺详情 */
export function getMerchantDetail(id) {
  return request.get(`/customer/merchant/${id}`)
}