/**
 * 图片地址处理
 * - 本地存储：后端返回 /upload/2026/09/24/xxx.jpg，需拼 /api 前缀（即 /api/upload/...）
 * - 对象存储：后端返回 /files/2026/09/24/xxx.jpg，由 Nginx 的 /files/ 在根路径直接反代，
 *   不能再拼 /api（拼了会打到后端 /api/files/**，那里没有静态处理器 → 404）
 */
export function resolveImageUrl(url) {
  if (!url) return ''
  // 已是完整外链
  if (/^https?:\/\//i.test(url)) return url
  // 已带 /api 前缀
  if (url.startsWith('/api')) return url
  // 对象存储（MinIO）产物：/files/ 由 Nginx 根路径反代，原样返回
  if (url.startsWith('/files')) return url
  return `/api${url.startsWith('/') ? '' : '/'}${url}`
}

export default resolveImageUrl