import request from '@/utils/request'

// 查询数据不可用策略参数列表
export function listPolicyParam(query) {
  return request({
    url: '/vqms/policyParam/list',
    method: 'get',
    params: query
  })
}

// 查询数据不可用策略参数详细
export function getPolicyParam(paramId) {
  return request({
    url: '/vqms/policyParam/' + paramId,
    method: 'get'
  })
}

// 新增数据不可用策略参数
export function addPolicyParam(data) {
  return request({
    url: '/vqms/policyParam',
    method: 'post',
    data: data
  })
}

// 修改数据不可用策略参数
export function updatePolicyParam(data) {
  return request({
    url: '/vqms/policyParam',
    method: 'put',
    data: data
  })
}

// 删除数据不可用策略参数
export function delPolicyParam(paramId) {
  return request({
    url: '/vqms/policyParam/' + paramId,
    method: 'delete'
  })
}
