/**
 * 订单状态字典（与接口文档第八章一致）
 * 10 待支付 / 20 待接单 / 30 备餐中 / 40 配送中 / 50 已完成 / 60 已取消 / 70 已退款
 * tag 对应 el-tag 的 type
 */
export const ORDER_STATUS = {
  10: { text: '待支付', tag: 'info' },
  20: { text: '待接单', tag: 'warning' },
  30: { text: '备餐中', tag: 'primary' },
  40: { text: '配送中', tag: 'primary' },
  50: { text: '已完成', tag: 'success' },
  60: { text: '已取消', tag: 'danger' },
  70: { text: '已退款', tag: 'danger' }
}

/** 下拉/选项卡用列表 */
export const ORDER_STATUS_OPTIONS = Object.keys(ORDER_STATUS).map((key) => ({
  value: Number(key),
  label: ORDER_STATUS[key].text
}))

export function orderStatusText(status) {
  return ORDER_STATUS[status]?.text || '未知状态'
}

export function orderStatusTag(status) {
  return ORDER_STATUS[status]?.tag || 'info'
}

/** 商家审核状态 */
export const AUDIT_STATUS = {
  0: { text: '待审核', tag: 'warning' },
  1: { text: '已通过', tag: 'success' },
  2: { text: '已驳回', tag: 'danger' }
}

export const AUDIT_STATUS_OPTIONS = [
  { value: 0, label: '待审核' },
  { value: 1, label: '已通过' },
  { value: 2, label: '已驳回' }
]

export function auditStatusText(status) {
  return AUDIT_STATUS[status]?.text || '未知'
}

export function auditStatusTag(status) {
  return AUDIT_STATUS[status]?.tag || 'info'
}

/** 营业状态：0 休息 / 1 营业 */
export const BUSINESS_STATUS = {
  0: { text: '休息中', tag: 'info' },
  1: { text: '营业中', tag: 'success' }
}

export function businessStatusText(status) {
  return BUSINESS_STATUS[status]?.text || '未知'
}

export function businessStatusTag(status) {
  return BUSINESS_STATUS[status]?.tag || 'info'
}

/** 菜品上下架状态：0 下架 / 1 上架 */
export const DISH_STATUS = {
  0: { text: '已下架', tag: 'info' },
  1: { text: '售卖中', tag: 'success' }
}

/** 用户身份：1 顾客 / 2 商家 / 3 管理员 */
export const USER_TYPE = {
  1: { text: '顾客', tag: 'info' },
  2: { text: '商家', tag: 'warning' },
  3: { text: '管理员', tag: 'danger' }
}

export const USER_TYPE_OPTIONS = [
  { value: 1, label: '顾客' },
  { value: 2, label: '商家' },
  { value: 3, label: '管理员' }
]

export function userTypeText(userType) {
  return USER_TYPE[userType]?.text || '未知'
}

export function userTypeTag(userType) {
  return USER_TYPE[userType]?.tag || 'info'
}

/** 用户状态：1 正常 / 0 停用 */
export const USER_STATUS = {
  0: { text: '已停用', tag: 'danger' },
  1: { text: '正常', tag: 'success' }
}

export function userStatusText(status) {
  return USER_STATUS[status]?.text || '未知'
}

export function userStatusTag(status) {
  return USER_STATUS[status]?.tag || 'info'
}

/** 登录端标识 */
export const CLIENT_TYPE = {
  MERCHANT: 'MERCHANT',
  ADMIN: 'ADMIN'
}

/** 端标识 → 应用标题 */
export const CLIENT_TYPE_TITLE = {
  MERCHANT: '商家端',
  ADMIN: '管理后台'
}

export const APP_TITLE = '外卖系统'