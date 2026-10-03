import request from '@/utils/request'

/** 获取当前商家店铺信息（未入驻时 data 为 null） */
export function getShop() {
  return request.get('/merchant/shop')
}

/** 入驻申请（提交后审核状态=待审核） */
export function applyShop(data) {
  return request.post('/merchant/shop/apply', data)
}

/** 更新店铺信息 */
export function updateShop(data) {
  return request.put('/merchant/shop', data)
}

/** 切换营业状态：{businessStatus} 0 休息 / 1 营业 */
export function updateBusinessStatus(data) {
  return request.put('/merchant/shop/business-status', data)
}