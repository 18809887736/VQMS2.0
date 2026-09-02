import request from '@/utils/request'

// 查询AVC退出原因标注列表
export function listExitAnnotation(query) {
  return request({
    url: '/vqms/exitAnnotation/list',
    method: 'get',
    params: query
  })
}

// 查询AVC退出原因标注详细
export function getExitAnnotation(annotationId) {
  return request({
    url: '/vqms/exitAnnotation/' + annotationId,
    method: 'get'
  })
}

// 新增AVC退出原因标注
export function addExitAnnotation(data) {
  return request({
    url: '/vqms/exitAnnotation',
    method: 'post',
    data: data
  })
}

// 修改AVC退出原因标注
export function updateExitAnnotation(data) {
  return request({
    url: '/vqms/exitAnnotation',
    method: 'put',
    data: data
  })
}

// 删除AVC退出原因标注
export function delExitAnnotation(annotationId) {
  return request({
    url: '/vqms/exitAnnotation/' + annotationId,
    method: 'delete'
  })
}
