import request from '@/utils/request'

// 查询点号语义注册列表
export function listPointMap(query) {
  return request({
    url: '/vqms/pointMap/list',
    method: 'get',
    params: query
  })
}

// 查询点号语义注册详细
export function getPointMap(pointNum) {
  return request({
    url: '/vqms/pointMap/' + pointNum,
    method: 'get'
  })
}

// 新增点号语义注册
export function addPointMap(data) {
  return request({
    url: '/vqms/pointMap',
    method: 'post',
    data: data
  })
}

// 修改点号语义注册
export function updatePointMap(data) {
  return request({
    url: '/vqms/pointMap',
    method: 'put',
    data: data
  })
}

// 删除点号语义注册
export function delPointMap(pointNum) {
  return request({
    url: '/vqms/pointMap/' + pointNum,
    method: 'delete'
  })
}
