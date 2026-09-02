import request from '@/utils/request'

// 查询并网主体列表
export function listEntity(query) {
  return request({
    url: '/vqms/entity/list',
    method: 'get',
    params: query
  })
}

// 查询并网主体详细
export function getEntity(entityId) {
  return request({
    url: '/vqms/entity/' + entityId,
    method: 'get'
  })
}

// 新增并网主体
export function addEntity(data) {
  return request({
    url: '/vqms/entity',
    method: 'post',
    data: data
  })
}

// 修改并网主体
export function updateEntity(data) {
  return request({
    url: '/vqms/entity',
    method: 'put',
    data: data
  })
}

// 删除并网主体
export function delEntity(entityId) {
  return request({
    url: '/vqms/entity/' + entityId,
    method: 'delete'
  })
}
