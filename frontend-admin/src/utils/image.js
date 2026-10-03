/**
 * 图片地址处理
 * 后端 /file/upload 返回的 url 形如 /upload/2026/09/24/xxx.jpg，
 * 展示时必须拼上 /api 前缀（即 /api/upload/...）。
 */
export function resolveImageUrl(url) {
  if (!url) return ''
  // 已是完整外链
  if (/^https?:\/\//i.test(url)) return url
  // 已带 /api 前缀
  if (url.startsWith('/api')) return url
  return `/api${url.startsWith('/') ? '' : '/'}${url}`
}

export default resolveImageUrl