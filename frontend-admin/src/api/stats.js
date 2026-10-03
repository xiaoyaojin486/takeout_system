import request from '@/utils/request'

/* ------------------------------- 商家端统计 ------------------------------- */

/** 商家经营概览 */
export function getMerchantOverview() {
  return request.get('/merchant/stats/overview')
}

/** 营业额/订单数趋势（后端已补零） */
export function getMerchantTrend(days = 7) {
  return request.get('/merchant/stats/trend', { params: { days } })
}

/** 热销菜品 TOP N */
export function getMerchantHotDish(days = 30, limit = 5) {
  return request.get('/merchant/stats/hot-dish', { params: { days, limit } })
}

/* ------------------------------- 平台看板 ------------------------------- */

/** 平台概览 */
export function getAdminOverview() {
  return request.get('/admin/stats/overview')
}

/** 商家成交排行 */
export function getAdminMerchantRank(days = 30, limit = 10) {
  return request.get('/admin/stats/merchant-rank', { params: { days, limit } })
}