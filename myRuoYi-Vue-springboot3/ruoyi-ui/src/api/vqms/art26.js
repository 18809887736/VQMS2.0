import request from '@/utils/request'

// 第26条考核曲线行（按母线+季度）
export function listCurve(query) {
  return request({
    url: '/vqms/art26/list',
    method: 'get',
    params: query
  })
}

export function addCurve(data) {
  return request({
    url: '/vqms/art26',
    method: 'post',
    data: data
  })
}

export function updateCurve(data) {
  return request({
    url: '/vqms/art26',
    method: 'put',
    data: data
  })
}

export function delCurve(curveId) {
  return request({
    url: '/vqms/art26/' + curveId,
    method: 'delete'
  })
}

// CSV 批量导入：busbar_num,period_start,period_end,limit_up_kv,limit_down_kv
export function importCurve(quarter, source, csv) {
  return request({
    url: '/vqms/art26/import',
    method: 'post',
    params: { quarter, source },
    data: csv,
    headers: { 'Content-Type': 'application/json' }
  })
}

// 三桶对账
export function reconcile(quarter, busbarNum) {
  return request({
    url: '/vqms/art26/reconcile',
    method: 'get',
    params: { quarter, busbarNum }
  })
}
