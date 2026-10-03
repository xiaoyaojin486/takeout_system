import request from '@/utils/request'

/** 分类列表 */
export function getCategoryList() {
  return request.get('/merchant/category')
}

/** 新增分类：{name, sort, status} */
export function createCategory(data) {
  return request.post('/merchant/category', data)
}

/** 修改分类 */
export function updateCategory(id, data) {
  return request.put(`/merchant/category/${id}`, data)
}

/** 删除分类 */
export function deleteCategory(id) {
  return request.delete(`/merchant/category/${id}`)
}