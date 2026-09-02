import request from '@/utils/request'

// 查询判定整定参数列表
export function listJudgeParam(query) {
  return request({
    url: '/vqms/judgeParam/list',
    method: 'get',
    params: query
  })
}

// 查询判定整定参数详细
export function getJudgeParam(paramId) {
  return request({
    url: '/vqms/judgeParam/' + paramId,
    method: 'get'
  })
}

// 新增判定整定参数
export function addJudgeParam(data) {
  return request({
    url: '/vqms/judgeParam',
    method: 'post',
    data: data
  })
}

// 修改判定整定参数
export function updateJudgeParam(data) {
  return request({
    url: '/vqms/judgeParam',
    method: 'put',
    data: data
  })
}

// 删除判定整定参数
export function delJudgeParam(paramId) {
  return request({
    url: '/vqms/judgeParam/' + paramId,
    method: 'delete'
  })
}
