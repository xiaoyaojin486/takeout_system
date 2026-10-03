import request from '../utils/request'

/** GET /customer/dish/menu 店铺菜单（按分类分组，只含上架菜品） */
export function getMenu(merchantId) {
  return request.get('/customer/dish/menu', { params: { merchantId } })
}