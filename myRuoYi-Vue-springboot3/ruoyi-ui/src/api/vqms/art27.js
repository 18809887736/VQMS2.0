import request from '@/utils/request'

// 第27条装置台账
export function listDevices(query) {
  return request({ url: '/vqms/art27/devices', method: 'get', params: query })
}
export function addDevice(data) {
  return request({ url: '/vqms/art27/device', method: 'post', data })
}
export function updateDevice(data) {
  return request({ url: '/vqms/art27/device', method: 'put', data })
}
export function delDevice(id) {
  return request({ url: '/vqms/art27/device/' + id, method: 'delete' })
}

// 第27条月度登记
export function listMonths(statMonth) {
  return request({ url: '/vqms/art27/months', method: 'get', params: { statMonth } })
}
export function addMonth(data) {
  return request({ url: '/vqms/art27/month', method: 'post', data })
}
export function updateMonth(data) {
  return request({ url: '/vqms/art27/month', method: 'put', data })
}
export function delMonth(id) {
  return request({ url: '/vqms/art27/month/' + id, method: 'delete' })
}

// 月度对账视图
export function reconcile27(statMonth) {
  return request({ url: '/vqms/art27/reconcile', method: 'get', params: { statMonth } })
}
