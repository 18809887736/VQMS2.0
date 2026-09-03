<template>
  <div class="app-container">
    <el-card shadow="never" class="mb10">
      <div slot="header" class="clearfix">
        <span>AVC 考核看板</span>
        <span class="sub-title">依据：东北两个细则 附件6（投运率≥99%、调节合格率≥100%、1分=1000元）</span>
      </div>
      <el-form :inline="true" size="small">
        <el-form-item label="考核月份">
          <el-date-picker v-model="month" type="month" value-format="yyyy-MM" placeholder="选择月份" :clearable="false" @change="refresh" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-refresh" size="mini" @click="refresh">刷新</el-button>
        </el-form-item>
      </el-form>

      <el-row :gutter="12">
        <el-col :span="4"><div class="kpi"><div class="kpi-label">发令总次数</div><div class="kpi-value">{{ summary.totalCmds }}</div></div></el-col>
        <el-col :span="4"><div class="kpi"><div class="kpi-label">快速性合格率</div><div class="kpi-value">{{ pct(summary.fastRatePct) }}</div></div></el-col>
        <el-col :span="4"><div class="kpi"><div class="kpi-label">经济性合格率</div><div class="kpi-value">{{ pct(summary.econRatePct) }}</div></div></el-col>
        <el-col :span="4"><div class="kpi"><div class="kpi-label">免考点数</div><div class="kpi-value">{{ summary.exemptedTotal }}</div></div></el-col>
        <el-col :span="4"><div class="kpi"><div class="kpi-label">考核罚分</div><div class="kpi-value danger">{{ score(summary.penaltyTotal) }}</div></div></el-col>
        <el-col :span="4"><div class="kpi"><div class="kpi-label">罚款金额(元)</div><div class="kpi-value danger">{{ money(summary.penaltyTotalCny) }}</div></div></el-col>
      </el-row>
      <el-row :gutter="12" class="mt8">
        <el-col :span="4"><div class="kpi"><div class="kpi-label">投运分钟</div><div class="kpi-value">{{ runtime.inServiceMin }}</div></div></el-col>
        <el-col :span="4"><div class="kpi"><div class="kpi-label">非电网退出(扣罚)</div><div class="kpi-value">{{ runtime.exitNonGridMin }}</div></div></el-col>
        <el-col :span="4"><div class="kpi"><div class="kpi-label">电网退出(免责)</div><div class="kpi-value">{{ runtime.exitGridMin }}</div></div></el-col>
        <el-col :span="4"><div class="kpi"><div class="kpi-label">未并网(不计)</div><div class="kpi-value">{{ runtime.offlineMin }}</div></div></el-col>
        <el-col :span="4"><div class="kpi"><div class="kpi-label">投运率</div><div class="kpi-value" :class="{'danger': runtimeBad}">{{ pct(runtime.ratePct) }}</div></div></el-col>
        <el-col :span="4"><div class="kpi"><div class="kpi-label">投运罚款(元)</div><div class="kpi-value danger">{{ money(runtime.penaltyScoreCny) }}</div></div></el-col>
      </el-row>
    </el-card>

    <el-card shadow="never" class="mb10">
      <div slot="header"><span>调节合格率 · 日序列</span><span class="sub-title">点击行查看当日指令明细（三状态）</span></div>
      <el-table v-loading="loading" :data="dayRows" size="mini" highlight-current-row @row-click="showCommands" style="cursor: pointer">
        <el-table-column label="日期" align="center" prop="statPeriod" width="110" />
        <el-table-column label="发令数" align="center" prop="totalCmds" width="80" />
        <el-table-column label="快·合格" align="center">
          <template slot-scope="s">{{ s.row.counts.qualifiedFast }}</template>
        </el-table-column>
        <el-table-column label="快·不合格" align="center">
          <template slot-scope="s">{{ s.row.counts.penalizedFast }}</template>
        </el-table-column>
        <el-table-column label="快·免考" align="center">
          <template slot-scope="s">{{ s.row.counts.exemptedFast }}</template>
        </el-table-column>
        <el-table-column label="快·无效" align="center">
          <template slot-scope="s">{{ s.row.counts.invalidFast }}</template>
        </el-table-column>
        <el-table-column label="快速性合格率" align="center">
          <template slot-scope="s">{{ pct(s.row.fastRatePct) }}</template>
        </el-table-column>
        <el-table-column label="经济性合格率" align="center">
          <template slot-scope="s">{{ pct(s.row.econRatePct) }}</template>
        </el-table-column>
        <el-table-column label="免考合计" align="center" prop="exemptedTotal" width="80" />
        <el-table-column label="罚分" align="center">
          <template slot-scope="s">{{ score(s.row.penaltyTotal) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="'指令明细 ' + detailDate" :visible.sync="detailOpen" width="980px" append-to-body>
      <el-table v-loading="detailLoading" :data="detailRows" size="mini" max-height="520">
        <el-table-column label="指令时刻" align="center" prop="cmdTime" width="160" />
        <el-table-column label="目标kV" align="center" prop="targetKv" width="90" />
        <el-table-column label="响应(分)" align="center" prop="responseMinutes" width="80" />
        <el-table-column label="快速档" align="center" prop="fastState" width="110">
          <template slot-scope="s"><el-tag :type="stateTag(s.row.fastState)" size="mini">{{ stateText(s.row.fastState) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="经济档" align="center" prop="econState" width="110">
          <template slot-scope="s"><el-tag :type="stateTag(s.row.econState)" size="mini">{{ stateText(s.row.econState) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="免考源" align="center" prop="exemptSource" width="90" />
        <el-table-column label="指令原文" align="center" prop="warnTimeRaw" :show-overflow-tooltip="true" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { regulationStats, runtimeStats, commandDetail } from "@/api/vqms/stats";

export default {
  name: "VqmsDashboard",
  data() {
    return {
      month: "2026-03",
      loading: false,
      detailOpen: false,
      detailLoading: false,
      detailDate: "",
      detailRows: [],
      dayRows: [],
      monthRow: null,
      runtime: { inServiceMin: "-", exitGridMin: "-", exitNonGridMin: "-", offlineMin: "-", ratePct: null, penaltyScore: null, penaltyScoreCny: null }
    };
  },
  computed: {
    range() {
      const [y, m] = this.month.split("-").map(Number);
      const last = new Date(y, m, 0).getDate();
      return { start: `${y}-${String(m).padStart(2, "0")}-01`, end: `${y}-${String(m).padStart(2, "0")}-${String(last).padStart(2, "0")}` };
    },
    summary() {
      return this.monthRow || { totalCmds: "-", exemptedTotal: "-", fastRatePct: null, econRatePct: null, penaltyTotal: null };
    },
    runtimeBad() {
      return this.runtime.ratePct != null && Number(this.runtime.ratePct) < 99;
    }
  },
  created() {
    this.refresh();
  },
  methods: {
    refresh() {
      this.loading = true;
      regulationStats("D", this.range.start, this.range.end).then(res => {
        this.dayRows = res.data || [];
      }).finally(() => { this.loading = false; });
      regulationStats("M", this.range.start, this.range.end).then(res => {
        const rows = res.data || [];
        this.monthRow = rows.length > 0 ? rows[0] : null;
      });
      runtimeStats("M", this.range.start, this.range.end).then(res => {
        const rows = res.data || [];
        if (rows.length > 0) {
          this.runtime = rows[0];
        } else {
          this.runtime = { inServiceMin: "-", exitGridMin: "-", exitNonGridMin: "-", offlineMin: "-", ratePct: null, penaltyScore: null, penaltyScoreCny: null };
        }
      });
    },
    showCommands(row) {
      this.detailDate = row.statPeriod;
      this.detailOpen = true;
      this.detailLoading = true;
      commandDetail(row.statPeriod, row.statPeriod).then(res => {
        this.detailRows = res.data || [];
      }).finally(() => { this.detailLoading = false; });
    },
    pct(v) { return v == null ? "—" : Number(v).toFixed(2) + "%"; },
    score(v) { return v == null ? "0.000" : Number(v).toFixed(3); },
    money(v) { return v == null ? "—" : Number(v).toLocaleString(); },
    stateTag(s) {
      return { QUALIFIED: "success", PENALIZED: "danger", EXEMPTED: "warning", INVALID: "info" }[s] || "info";
    },
    stateText(s) {
      return { QUALIFIED: "合格", PENALIZED: "不合格", EXEMPTED: "免考", INVALID: "无效" }[s] || s;
    }
  }
};
</script>

<style scoped>
.mb10 { margin-bottom: 10px; }
.mt8 { margin-top: 8px; }
.sub-title { margin-left: 12px; color: #909399; font-size: 12px; }
.kpi { background: #f5f7fa; border-radius: 4px; padding: 10px 12px; text-align: center; }
.kpi-label { color: #909399; font-size: 12px; margin-bottom: 6px; }
.kpi-value { font-size: 20px; font-weight: 600; color: #303133; }
.kpi-value.danger { color: #f56c6c; }
</style>
