<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="80px">
      <el-form-item label="时间范围" prop="timeRange">
        <el-date-picker
          v-model="queryParams.timeRange"
          value-format="yyyy-MM-dd HH:mm:ss"
          type="datetimerange"
          range-separator="-"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          style="width: 380px"
          :default-time="['00:00:00', '23:59:59']"
        />
      </el-form-item>
      <el-form-item label="电压等级" prop="vGrade">
        <el-select v-model="queryParams.vGrade" placeholder="电压等级" clearable style="width: 150px" @change="handleVGradeChange">
          <el-option v-for="d in vGradeOptions" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="母线" prop="busbarNum">
        <el-select v-model="queryParams.busbarNum" placeholder="母线" style="width: 180px">
          <el-option v-for="b in busbarOptions" :key="b.busbarNum" :label="b.busbarName + ' (' + b.busbarNum + ')'" :value="b.busbarNum" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">查询</el-button>
      </el-form-item>
    </el-form>

    <el-card shadow="never">
      <div slot="header">
        <span>电压曲线（high_SV / low_SV，逐分钟观测极值）</span>
        <span v-if="total > 0" style="float: right; color: #909399; font-size: 12px">共 {{ total }} 分钟</span>
      </div>
      <div ref="chart" v-loading="loading" style="height: 480px"></div>
      <div v-if="!loading && !hasData" class="empty-tip">暂无数据（选择母线与时间范围后查询）</div>
      <div v-if="truncated" class="empty-tip">窗口超过 {{ pageSize }} 分钟，仅显示前 {{ pageSize }} 个点（复核指令时段建议查小时级窗口；完整回查请缩小时间范围）</div>
    </el-card>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { listCurve } from '@/api/vqms/curve'
import { listBusbar } from '@/api/vqms/busbar'

export default {
  name: "VqmsCurve",
  data() {
    return {
      loading: false,
      hasData: false,
      truncated: false,
      total: 0,
      pageSize: 500,
      busbarList: [],
      vGradeOptions: [
        { value: 0, label: '500kV' },
        { value: 1, label: '220kV' },
        { value: 2, label: '66kV及以下' }
      ],
      queryParams: {
        timeRange: [],
        vGrade: 1,
        busbarNum: undefined
      },
      chart: null
    }
  },
  computed: {
    busbarOptions() {
      return this.queryParams.vGrade === null || this.queryParams.vGrade === undefined || this.queryParams.vGrade === ''
        ? this.busbarList
        : this.busbarList.filter(b => b.vGrade === this.queryParams.vGrade)
    }
  },
  mounted() {
    listBusbar().then(response => {
      this.busbarList = (response.rows || []).map(b => ({
        busbarNum: b.busbarNum,
        busbarName: b.busbarName,
        vGrade: b.vGrade
      }))
      if (!this.queryParams.busbarNum && this.busbarList.length) {
        this.queryParams.busbarNum = this.busbarList[0].busbarNum
      }
    })
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    if (this.chart) {
      this.chart.dispose()
      this.chart = null
    }
  },
  methods: {
    handleVGradeChange() {
      if (this.queryParams.busbarNum !== undefined
        && !this.busbarOptions.some(b => b.busbarNum === this.queryParams.busbarNum)) {
        this.queryParams.busbarNum = this.busbarOptions.length ? this.busbarOptions[0].busbarNum : undefined
      }
    },
    handleQuery() {
      if (!this.queryParams.timeRange || !this.queryParams.timeRange[0] || !this.queryParams.timeRange[1]) {
        this.$modal.msgWarning("请选择时间范围")
        return
      }
      if (this.queryParams.busbarNum === undefined || this.queryParams.busbarNum === null) {
        this.$modal.msgWarning("请选择母线")
        return
      }
      this.loading = true
      listCurve({
        startTime: this.queryParams.timeRange[0],
        endTime: this.queryParams.timeRange[1],
        busbarNum: this.queryParams.busbarNum,
        pageNum: 1,
        pageSize: this.pageSize
      }).then(response => {
        const rows = response.rows || []
        this.hasData = rows.length > 0
        this.total = response.total || 0
        this.truncated = this.total > rows.length
        if (this.hasData) {
          this.renderChart(rows)
        }
      }).catch(() => {
        this.hasData = false
      }).finally(() => {
        this.loading = false
      })
    },
    renderChart(rows) {
      if (!this.$refs.chart) return
      if (!this.chart) {
        this.chart = echarts.init(this.$refs.chart)
      }
      this.chart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['high_SV', 'low_SV'] },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: rows.map(r => r.saveTime) },
        yAxis: { type: 'value', name: 'kV', scale: true },
        series: [
          { name: 'high_SV', type: 'line', data: rows.map(r => r.highSV), smooth: true, symbol: 'none' },
          { name: 'low_SV', type: 'line', data: rows.map(r => r.lowSV), smooth: true, symbol: 'none' }
        ]
      })
    },
    handleResize() {
      this.chart && this.chart.resize()
    }
  }
}
</script>

<style scoped>
.empty-tip {
  text-align: center;
  color: #999;
  padding: 40px 0;
}
</style>
