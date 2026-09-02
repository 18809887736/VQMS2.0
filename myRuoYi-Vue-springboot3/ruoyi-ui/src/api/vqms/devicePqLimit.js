import request from '@/utils/request'

// 查询设备P-Q极限曲线列表
export function listDevicePqLimit(query) {
  return request({
    url: '/vqms/devicePqLimit/list',
    method: 'get',
    params: query
  })
}

// 查询设备P-Q极限曲线详细
export function getDevicePqLimit(id) {
  return request({
    url: '/vqms/devicePqLimit/' + id,
    method: 'get'
  })
}

// 新增设备P-Q极限曲线
export function addDevicePqLimit(data) {
  return request({
    url: '/vqms/devicePqLimit',
    method: 'post',
    data: data
  })
}

// 修改设备P-Q极限曲线
export function updateDevicePqLimit(data) {
  return request({
    url: '/vqms/devicePqLimit',
    method: 'put',
    data: data
  })
}

// 删除设备P-Q极限曲线
export function delDevicePqLimit(id) {
  return request({
    url: '/vqms/devicePqLimit/' + id,
    method: 'delete'
  })
}
