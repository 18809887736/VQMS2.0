import request from '@/utils/request'

// 配置库整定预览（上传 .db）
export function tuningPreview(formData) {
  return request({
    url: '/vqms/tuning/preview',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

// 确认执行整定语句
export function tuningApply(sqls) {
  return request({
    url: '/vqms/tuning/apply',
    method: 'post',
    data: sqls,
    timeout: 120000
  })
}
