<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 投运率报表 -->
      <el-tab-pane label="投运率报表" name="runtime">
        <el-form :inline="true" size="small">
          <el-form-item label="粒度">
            <el-radio-group v-model="runtimeQuery.grain" size="mini" @change="loadRuntime">
              <el-radio-button label="D">日</el-radio-button>
              <el-radio-button label="M">月</el-radio-button>
              <el-radio-button label="Y">年</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="期间">
            <el-date-picker
              v-model="runtimeQuery.range"
              type="daterange"
              value-format="yyyy-MM-dd"
              range-separator="-"
              start-placeholder="开始日"
              end-placeholder="结束日"
              style="width: 260px"
              :clearable="false"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="loadRuntime">查询</el-button>
            <el-button type="warning" icon="el-icon-download" size="mini" @click="exportRuntime">导出</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="runtimeLoading" :data="runtimeRows" size="mini" border>
          <el-table-column label="统计期间" prop="statPeriod" width="110" align="center" />
          <el-table-column label="并网运行(分)" align="center">
            <template slot-scope="s">{{ gridMinutes(s.row) }}</template>
          </el-table-column>
          <el-table-column label="投运(分)" prop="inServiceMin" width="95" align="center" />
          <el-table-column label="电网退出(分)" prop="exitGridMin" width="105" align="center" />
          <el-table-column label="非电网退出(分)" prop="exitNonGridMin" width="115" align="center" />
          <el-table-column label="投运率(%)" align="center" width="100">
            <template slot-scope="s">
              <span :style="rateStyle(s.row.ratePct, 99)"> {{ s.row.ratePct == null ? '-' : s.row.ratePct }}</span>
            </template>
          </el-table-column>
          <el-table-column label="缺额(百分点)" prop="shortfallPct" width="105" align="center" />
          <el-table-column label="罚款(分)" prop="penaltyScore" width="90" align="center" />
          <el-table-column label="罚款(元)" align="center" width="110">
            <template slot-scope="s">{{ s.row.penaltyScore == null ? '-' : (s.row.penaltyScore * 1000).toLocaleString() }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 调节合格率报表 -->
      <el-tab-pane label="调节合格率报表" name="regulation">
        <el-form :inline="true" size="small">
          <el-form-item label="粒度">
            <el-radio-group v-model="regQuery.grain" size="mini" @change="loadRegulation">
              <el-radio-button label="D">日</el-radio-button>
              <el-radio-button label="M">月</el-radio-button>
              <el-radio-button label="Y">年</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="期间">
            <el-date-picker
              v-model="regQuery.range"
              type="daterange"
              value-format="yyyy-MM-dd"
              range-separator="-"
              start-placeholder="开始日"
              end-placeholder="结束日"
              style="width: 260px"
              :clearable="false"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="loadRegulation">查询</el-button>
            <el-button type="warning" icon="el-icon-download" size="mini" @click="exportRegulation">导出</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="regLoading" :data="regRows" size="mini" border>
          <el-table-column label="统计期间" prop="statPeriod" width="110" align="center" />
          <el-table-column label="发令次数" prop="totalCmds" width="85" align="center" />
          <el-table-column label="快·合格" align="center" width="80">
            <template slot-scope="s">{{ s.row.counts.qualifiedFast }}</template>
          </el-table-column>
          <el-table-column label="快·免考" align="center" width="80">
            <template slot-scope="s">{{ s.row.counts.exemptedFast }}</template>
          </el-table-column>
          <el-table-column label="快·无效" align="center" width="80">
            <template slot-scope="s">{{ s.row.counts.invalidFast }}</template>
          </el-table-column>
          <el-table-column label="快速性合格率(%)" align="center" width="130">
            <template slot-scope="s">
              <span :style="rateStyle(s.row.fastRatePct, 100)">{{ s.row.fastRatePct == null ? '-' : s.row.fastRatePct }}</span>
            </template>
          </el-table-column>
          <el-table-column label="快罚款(分)" prop="fastPenaltyScore" width="95" align="center" />
          <el-table-column label="经·合格" align="center" width="80">
            <template slot-scope="s">{{ s.row.counts.qualifiedEcon }}</template>
          </el-table-column>
          <el-table-column label="经·免考" align="center" width="80">
            <template slot-scope="s">{{ s.row.counts.exemptedEcon }}</template>
          </el-table-column>
          <el-table-column label="经·无效" align="center" width="80">
            <template slot-scope="s">{{ s.row.counts.invalidEcon }}</template>
          </el-table-column>
          <el-table-column label="经济性合格率(%)" align="center" width="130">
            <template slot-scope="s">
              <span :style="rateStyle(s.row.econRatePct, 100)">{{ s.row.econRatePct == null ? '-' : s.row.econRatePct }}</span>
            </template>
          </el-table-column>
          <el-table-column label="经罚款(分)" prop="econPenaltyScore" width="95" align="center" />
          <el-table-column label="总罚款(分)" align="center" width="95">
            <template slot-scope="s">{{ s.row.penaltyTotal == null ? '-' : s.row.penaltyTotal }}</template>
          </el-table-column>
          <el-table-column label="总罚款(元)" align="center" width="110">
            <template slot-scope="s">{{ s.row.penaltyTotal == null ? '-' : (s.row.penaltyTotal * 1000).toLocaleString() }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { regulationStats, runtimeStats } from "@/api/vqms/stats"

function defaultRange() {
  const end = new Date()
  const start = new Date(end.getTime() - 90 * 24 * 3600 * 1000)
  const f = d => d.toISOString().slice(0, 10)
  return [f(start), f(end)]
}

export default {
  name: "VqmsReports",
  data() {
    return {
      activeTab: "runtime",
      runtimeLoading: false,
      regLoading: false,
      runtimeRows: [],
      regRows: [],
      runtimeQuery: { grain: "M", range: defaultRange() },
      regQuery: { grain: "M", range: defaultRange() }
    }
  },
  created() {
    this.loadRuntime()
    this.loadRegulation()
  },
  methods: {
    gridMinutes(r) {
      return (r.inServiceMin || 0) + (r.exitGridMin || 0) + (r.exitNonGridMin || 0)
    },
    rateStyle(rate, line) {
      if (rate == null) return {}
      return Number(rate) < line ? { color: "#F56C6C", fontWeight: "bold" } : { color: "#67C23A" }
    },
    loadRuntime() {
      const [s, e] = this.runtimeQuery.range
      if (!s || !e) return
      this.runtimeLoading = true
      runtimeStats(this.runtimeQuery.grain, s, e).then(res => {
        this.runtimeRows = res.data || []
      }).finally(() => { this.runtimeLoading = false })
    },
    loadRegulation() {
      const [s, e] = this.regQuery.range
      if (!s || !e) return
      this.regLoading = true
      regulationStats(this.regQuery.grain, s, e).then(res => {
        this.regRows = res.data || []
      }).finally(() => { this.regLoading = false })
    },
    exportRuntime() {
      const [s, e] = this.runtimeQuery.range
      this.download('vqms/stats/runtime/export', { grain: this.runtimeQuery.grain, start: s, end: e },
        `投运率报表_${this.runtimeQuery.grain}_${s}_${e}.xlsx`)
    },
    exportRegulation() {
      const [s, e] = this.regQuery.range
      this.download('vqms/stats/regulation/export', { grain: this.regQuery.grain, start: s, end: e },
        `调节合格率报表_${this.regQuery.grain}_${s}_${e}.xlsx`)
    }
  }
}
</script>
