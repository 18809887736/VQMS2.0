import request from '@/utils/request'

// 查询母线组列表
export function listBusbarGroup(query) {
  return request({
    url: '/vqms/busbarGroup/list',
    method: 'get',
    params: query
  })
}

// 查询母线组详细
export function getBusbarGroup(groupNum) {
  return request({
    url: '/vqms/busbarGroup/' + groupNum,
    method: 'get'
  })
}

// 新增母线组
export function addBusbarGroup(data) {
  return request({
    url: '/vqms/busbarGroup',
    method: 'post',
    data: data
  })
}

// 修改母线组
export function updateBusbarGroup(data) {
  return request({
    url: '/vqms/busbarGroup',
    method: 'put',
    data: data
  })
}

// 删除母线组
export function delBusbarGroup(groupNum) {
  return request({
    url: '/vqms/busbarGroup/' + groupNum,
    method: 'delete'
  })
}
