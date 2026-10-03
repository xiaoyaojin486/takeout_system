import request from '../utils/request'

/** GET /customer/address 收货地址列表（默认地址最前） */
export function getAddressList() {
  return request.get('/customer/address')
}

/** POST /customer/address 新增地址（第一条自动设为默认） */
export function addAddress(data) {
  return request.post('/customer/address', data)
}

/** PUT /customer/address/{id} 修改地址 */
export function updateAddress(id, data) {
  return request.put(`/customer/address/${id}`, data)
}

/** DELETE /customer/address/{id} 删除地址 */
export function deleteAddress(id) {
  return request.delete(`/customer/address/${id}`)
}

/** PUT /customer/address/{id}/default 设为默认地址 */
export function setDefaultAddress(id) {
  return request.put(`/customer/address/${id}/default`)
}