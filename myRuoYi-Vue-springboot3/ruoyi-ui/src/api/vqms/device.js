import request from '@/utils/request'

// 查询无功设备台账列表
export function listDevice(query) {
  return request({
    url: '/vqms/device/list',
    method: 'get',
    params: query
  })
}

// 查询无功设备台账详细
export function getDevice(deviceId) {
  return request({
    url: '/vqms/device/' + deviceId,
    method: 'get'
  })
}

// 新增无功设备台账
export function addDevice(data) {
  return request({
    url: '/vqms/device',
    method: 'post',
    data: data
  })
}

// 修改无功设备台账
export function updateDevice(data) {
  return request({
    url: '/vqms/device',
    method: 'put',
    data: data
  })
}

// 删除无功设备台账
export function delDevice(deviceId) {
  return request({
    url: '/vqms/device/' + deviceId,
    method: 'delete'
  })
}
