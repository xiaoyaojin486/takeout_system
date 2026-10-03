import request from '@/utils/request'

/** 菜品分页：pageNum, pageSize, keyword?, categoryId?, status? */
export function getDishPage(params) {
  return request.get('/merchant/dish', { params })
}

/** 新增菜品 */
export function createDish(data) {
  return request.post('/merchant/dish', data)
}

/** 修改菜品 */
export function updateDish(id, data) {
  return request.put(`/merchant/dish/${id}`, data)
}

/** 删除菜品 */
export function deleteDish(id) {
  return request.delete(`/merchant/dish/${id}`)
}

/** 上下架：{status} 0 下架 / 1 上架 */
export function updateDishStatus(id, status) {
  return request.put(`/merchant/dish/${id}/status`, { status })
}