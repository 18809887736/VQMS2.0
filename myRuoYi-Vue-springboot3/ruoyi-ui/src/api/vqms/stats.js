import request from '@/utils/request'

// 调节合格率报表（计数+率/罚款重算）
export function regulationStats(grain, start, end) {
  return request({
    url: '/vqms/stats/regulation',
    method: 'get',
    params: { grain, start, end }
  })
}

// 投运率报表（快照随行）
export function runtimeStats(grain, start, end) {
  return request({
    url: '/vqms/stats/runtime',
    method: 'get',
    params: { grain, start, end }
  })
}

// 指令级明细（看板钻取，单日）
export function commandDetail(start, end) {
  return request({
    url: '/vqms/stats/commands',
    method: 'get',
    params: { start, end }
  })
}
