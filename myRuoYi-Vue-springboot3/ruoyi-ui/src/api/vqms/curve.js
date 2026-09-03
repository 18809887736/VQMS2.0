import request from '@/utils/request'

// 电压曲线查询（逐分钟 high/low，外部源 his_curve_sv；复核免考指令时段用）
export function listCurve(query) {
  return request({
    url: '/vqms/curve/list',
    method: 'get',
    params: query
  })
}
