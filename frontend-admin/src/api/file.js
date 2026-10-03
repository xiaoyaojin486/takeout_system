import request from '@/utils/request'

/**
 * 上传图片 / 文件
 * @param {File} file 文件对象
 * @returns {Promise<{url, name, size}>} url 形如 /upload/2026/09/24/xxx.jpg（展示需拼 /api）
 */
export function uploadFile(file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (event) => {
      if (onProgress && event.total) {
        onProgress(Math.round((event.loaded * 100) / event.total))
      }
    }
  })
}