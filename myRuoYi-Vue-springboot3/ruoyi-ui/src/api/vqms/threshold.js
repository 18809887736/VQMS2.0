import request from '@/utils/request'

// 查询母线电压阈值列表
export function listThreshold(query) {
  return request({
    url: '/vqms/threshold/list',
    method: 'get',
    params: query
  })
}

// 查询母线电压阈值详细
export function getThreshold(thresholdId) {
  return request({
    url: '/vqms/threshold/' + thresholdId,
    method: 'get'
  })
}

// 新增母线电压阈值
export function addThreshold(data) {
  return request({
    url: '/vqms/threshold',
    method: 'post',
    data: data
  })
}

// 修改母线电压阈值
export function updateThreshold(data) {
  return request({
    url: '/vqms/threshold',
    method: 'put',
    data: data
  })
}

// 删除母线电压阈值
export function delThreshold(thresholdId) {
  return request({
    url: '/vqms/threshold/' + thresholdId,
    method: 'delete'
  })
}
