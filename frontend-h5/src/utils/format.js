/**
 * 通用格式化与字典工具
 */
const API_BASE = import.meta.env.VITE_API_BASE || '/api'

/** 订单状态字典（与接口文档第八章一致） */
export const ORDER_STATUS = {
  10: '待支付',
  20: '待接单',
  30: '备餐中',
  40: '配送中',
  50: '已完成',
  60: '已取消',
  70: '已退款'
}

/** 订单列表 tab 配置 */
export const ORDER_TABS = [
  { name: 'all', title: '全部' },
  { name: '10', title: '待支付' },
  { name: '20', title: '待接单' },
  { name: '30', title: '备餐中' },
  { name: '40', title: '配送中' },
  { name: '50', title: '已完成' },
  { name: '60', title: '已取消' }
]

export function orderStatusText(status, fallback) {
  return fallback || ORDER_STATUS[status] || '未知状态'
}

/** 状态标签配色 */
export function orderStatusTagType(status) {
  switch (Number(status)) {
    case 10:
      return 'warning'
    case 20:
      return 'primary'
    case 30:
      return 'primary'
    case 40:
      return 'success'
    case 50:
      return 'success'
    case 60:
      return 'default'
    default:
      return 'default'
  }
}

/**
 * 图片地址处理
 * - 本地存储：后端返回 /upload/2026/09/24/xxx.jpg，需拼 /api 前缀
 * - 对象存储：后端返回 /files/2026/09/24/xxx.jpg，由 Nginx 的 /files/ 在根路径直接反代，
 *   不能再拼 /api（拼了会打到后端 /api/files/**，那里没有静态处理器 → 404）
 * 空值返回空串，由 Vant 占位样式兜底。
 */
export function resolveImage(url) {
  if (!url) return ''
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:')) return url
  if (url.startsWith(API_BASE)) return url
  // 对象存储（MinIO）产物：/files/ 由 Nginx 根路径反代，原样返回
  if (url.startsWith('/files')) return url
  return `${API_BASE}${url.startsWith('/') ? '' : '/'}${url}`
}

/** 金额展示，保留两位小数 */
export function formatAmount(amount) {
  const num = Number(amount)
  if (Number.isNaN(num)) return '0.00'
  return num.toFixed(2)
}

/** 一次性幂等 token（优先使用浏览器原生 randomUUID） */
export function uuid() {
  if (typeof window !== 'undefined' && window.crypto && typeof window.crypto.randomUUID === 'function') {
    return window.crypto.randomUUID()
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}