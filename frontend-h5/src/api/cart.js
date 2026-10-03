import request from '../utils/request'

/** GET /customer/cart 购物车（按店铺分组） */
export function getCart() {
  return request.get('/customer/cart')
}

/** GET /customer/cart/count 购物车件数（角标） */
export function getCartCount() {
  return request.get('/customer/cart/count')
}

/** POST /customer/cart 加入购物车（同菜品累加） */
export function addToCart(data) {
  return request.post('/customer/cart', data)
}

/** PUT /customer/cart/{id} 修改数量，quantity 传 0 视为移除 */
export function updateCartItem(id, quantity) {
  return request.put(`/customer/cart/${id}`, { quantity })
}

/** DELETE /customer/cart/{id} 移除单项 */
export function removeCartItem(id) {
  return request.delete(`/customer/cart/${id}`)
}

/** DELETE /customer/cart 清空购物车 */
export function clearCart() {
  return request.delete('/customer/cart')
}