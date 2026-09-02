import request from '@/utils/request'

// 查询主母线台账列表
export function listBusbar(query) {
  return request({
    url: '/vqms/busbar/list',
    method: 'get',
    params: query
  })
}

// 查询主母线台账详细
export function getBusbar(busbarNum) {
  return request({
    url: '/vqms/busbar/' + busbarNum,
    method: 'get'
  })
}

// 新增主母线台账
export function addBusbar(data) {
  return request({
    url: '/vqms/busbar',
    method: 'post',
    data: data
  })
}

// 修改主母线台账
export function updateBusbar(data) {
  return request({
    url: '/vqms/busbar',
    method: 'put',
    data: data
  })
}

// 删除主母线台账
export function delBusbar(busbarNum) {
  return request({
    url: '/vqms/busbar/' + busbarNum,
    method: 'delete'
  })
}
