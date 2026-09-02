import request from '@/utils/request'

// 查询调节免考标注列表
export function listExemptAnnotation(query) {
  return request({
    url: '/vqms/exemptAnnotation/list',
    method: 'get',
    params: query
  })
}

// 查询调节免考标注详细
export function getExemptAnnotation(annotationId) {
  return request({
    url: '/vqms/exemptAnnotation/' + annotationId,
    method: 'get'
  })
}

// 新增调节免考标注
export function addExemptAnnotation(data) {
  return request({
    url: '/vqms/exemptAnnotation',
    method: 'post',
    data: data
  })
}

// 修改调节免考标注
export function updateExemptAnnotation(data) {
  return request({
    url: '/vqms/exemptAnnotation',
    method: 'put',
    data: data
  })
}

// 删除调节免考标注
export function delExemptAnnotation(annotationId) {
  return request({
    url: '/vqms/exemptAnnotation/' + annotationId,
    method: 'delete'
  })
}
