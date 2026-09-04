<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <el-form :inline="true" size="small">
      <el-form-item label="季度">
        <el-select v-model="quarter" style="width: 120px">
          <el-option v-for="q in quarterOptions" :key="q" :label="q" :value="q" />
        </el-select>
      </el-form-item>
      <el-form-item label="母线">
        <el-select v-model="busbarNum" style="width: 180px">
          <el-option v-for="b in busbarOptions" :key="b.busbarNum" :label="b.busbarName + ' (' + b.busbarNum + ')'" :value="b.busbarNum" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="loadAll">查询</el-button>
        <el-button type="success" icon="el-icon-s-data" size="mini" :disabled="!hasCurve" @click="doReconcile" v-hasPermi="['vqms:art26:list']">对账</el-button>
        <el-button type="warning" icon="el-icon-download" size="mini" :disabled="!rec" @click="doExport" v-hasPermi="['vqms:art26:export']">对账单导出</el-button>
      </el-form-item>
    </el-form>

    <!-- 对账汇总 -->
    <el-card v-if="rec" shadow="never" class="mb10">
      <div slot="header">
        <span>第26条对账汇总 · {{ rec.quarter }} 母线 {{ rec.busbarNum }}（{{ rec.start }} ~ {{ rec.end }}）</span>
        <span class="sub-tip">依据：季度电压曲线；AVC 主站闭环调节期间免考核（第 3 款）</span>
      </div>
      <el-row :gutter="12">
        <el-col :span="4"><div class="kpi"><div class="kpi-label">考核分钟</div><div class="kpi-value">{{ rec.totalMinutes }}</div></div></el-col>
        <el-col :span="4"><div class="kpi"><div class="kpi-label">合格分钟</div><div class="kpi-value ok">{{ rec.qualified }}</div></div></el-col>
        <el-col :span="4"><div class="kpi"><div class="kpi-label">不合格·AVC闭环(免考)</div><div class="kpi-value warn">{{ rec.exemptClosedLoop }}</div></div></el-col>
        <el-col :span="4"><div class="kpi"><div class="kpi-label">不合格·AVC退出(待判)</div><div class="kpi-value danger">{{ rec.violationExitAvc }}</div></div></el-col>
        <el-col :span="4"><div class="kpi"><div class="kpi-label">无曲线覆盖</div><div class="kpi-value">{{ rec.noCurve }}</div></div></el-col>
        <el-col :span="4"><div class="kpi"><div class="kpi-label">无实测</div><div class="kpi-value">{{ rec.noData }}</div></div></el-col>
      </el-row>
    </el-card>

    <!-- 逐日明细 -->
    <el-card v-if="rec" shadow="never" class="mb10">
      <div slot="header"><span>逐日对账明细（分钟）</span></div>
      <el-table :data="rec.days" size="mini" border max-height="360">
        <el-table-column label="日期" prop="date" width="110" align="center" />
        <el-table-column label="考核分钟" prop="total" width="90" align="center" />
        <el-table-column label="合格" prop="qualified" width="80" align="center" />
        <el-table-column label="不合格·AVC闭环(免考)" align="center">
          <template slot-scope="s"><span :class="s.row.exemptClosedLoop > 0 ? 'warn' : ''">{{ s.row.exemptClosedLoop }}</span></template>
        </el-table-column>
        <el-table-column label="不合格·AVC退出(待判)" align="center">
          <template slot-scope="s"><span :class="s.row.violationExitAvc > 0 ? 'danger' : ''">{{ s.row.violationExitAvc }}</span></template>
        </el-table-column>
        <el-table-column label="无曲线" prop="noCurve" width="80" align="center" />
        <el-table-column label="无实测" prop="noData" width="80" align="center" />
      </el-table>
    </el-card>

    <!-- 季度曲线登记 -->
    <el-card shadow="never">
      <div slot="header">
        <span>季度考核曲线（调度下发登记）</span>
        <el-button-group style="float: right">
          <el-button type="primary" size="mini" icon="el-icon-plus" @click="handleAdd" v-hasPermi="['vqms:art26:add']">登记</el-button>
          <el-button type="info" size="mini" icon="el-icon-upload2" @click="importOpen = true" v-hasPermi="['vqms:art26:add']">CSV 导入</el-button>
        </el-button-group>
      </div>
      <el-table v-loading="curveLoading" :data="curves" size="mini" border>
        <el-table-column label="时段起" align="center" width="160">
          <template slot-scope="s">{{ fmt(s.row.periodStart) }}</template>
        </el-table-column>
        <el-table-column label="时段止" align="center" width="160">
          <template slot-scope="s">{{ fmt(s.row.periodEnd) }}</template>
        </el-table-column>
        <el-table-column label="上限 kV" prop="limitUpKv" width="90" align="center" />
        <el-table-column label="下限 kV" prop="limitDownKv" width="90" align="center" />
        <el-table-column label="来源" prop="source" :show-overflow-tooltip="true" />
        <el-table-column label="备注" prop="remark" :show-overflow-tooltip="true" />
        <el-table-column label="操作" align="center" width="130">
          <template slot-scope="s">
            <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(s.row)" v-hasPermi="['vqms:art26:edit']">修改</el-button>
            <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(s.row)" v-hasPermi="['vqms:art26:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 登记对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="520px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="母线" prop="busbarNum">
          <el-select v-model="form.busbarNum" style="width: 220px">
            <el-option v-for="b in busbarOptions" :key="b.busbarNum" :label="b.busbarName + ' (' + b.busbarNum + ')'" :value="b.busbarNum" />
          </el-select>
        </el-form-item>
        <el-form-item label="季度" prop="quarter">
          <el-select v-model="form.quarter" style="width: 220px">
            <el-option v-for="q in quarterOptions" :key="q" :label="q" :value="q" />
          </el-select>
        </el-form-item>
        <el-form-item label="时段" prop="periodRange">
          <el-date-picker v-model="form.periodRange" type="datetimerange" value-format="yyyy-MM-dd HH:mm:ss"
                          range-separator="-" style="width: 360px" />
        </el-form-item>
        <el-form-item label="上限 kV" prop="limitUpKv">
          <el-input-number v-model="form.limitUpKv" :precision="3" :step="0.5" controls-position="right" />
        </el-form-item>
        <el-form-item label="下限 kV" prop="limitDownKv">
          <el-input-number v-model="form.limitDownKv" :precision="3" :step="0.5" controls-position="right" />
        </el-form-item>
        <el-form-item label="来源" prop="source">
          <el-input v-model="form.source" placeholder="下发文件名/通知单号" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="备注" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- CSV 导入对话框 -->
    <el-dialog title="CSV 导入考核曲线" :visible.sync="importOpen" width="640px" append-to-body>
      <el-alert type="info" :closable="false" style="margin-bottom: 10px"
                title="每行 5 列：母线号, 时段起, 时段止, 上限kV, 下限kV（时间 yyyy-MM-dd HH:mm:ss；首行表头自动跳过）。季度与来源统一按当前查询条件写入。" />
      <el-input v-model="importText" type="textarea" :rows="10" placeholder="0, 2026-01-01 00:00:00, 2026-03-31 23:59:59, 232.000, 224.000" />
      <div slot="footer">
        <el-button type="primary" @click="doImport">导 入</el-button>
        <el-button @click="importOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listCurve, addCurve, updateCurve, delCurve, importCurve, reconcile } from "@/api/vqms/art26"
import { listBusbar } from "@/api/vqms/busbar"

function quarters() {
  const out = []
  const now = new Date()
  for (let i = 0; i < 6; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i * 3, 1)
    out.push(d.getFullYear() + "Q" + (Math.floor(d.getMonth() / 3) + 1))
  }
  return out
}

export default {
  name: "VqmsArt26",
  data() {
    return {
      quarter: quarters()[1] || quarters()[0],
      quarterOptions: quarters(),
      busbarNum: 0,
      busbarOptions: [],
      curveLoading: false,
      curves: [],
      hasCurve: false,
      rec: null,
      open: false,
      importOpen: false,
      importText: "",
      title: "",
      form: {},
      rules: {
        busbarNum: [{ required: true, message: "母线必选", trigger: "change" }],
        quarter: [{ required: true, message: "季度必选", trigger: "change" }],
        periodRange: [{ required: true, message: "时段必选", trigger: "change" }],
        limitUpKv: [{ required: true, message: "上限必填", trigger: "blur" }],
        limitDownKv: [{ required: true, message: "下限必填", trigger: "blur" }]
      }
    }
  },
  created() {
    listBusbar().then(res => {
      this.busbarOptions = (res.rows || []).map(b => ({ busbarNum: b.busbarNum, busbarName: b.busbarName }))
      if (this.busbarOptions.length && !this.busbarOptions.some(b => b.busbarNum === this.busbarNum)) {
        this.busbarNum = this.busbarOptions[0].busbarNum
      }
      this.loadCurves()
    })
  },
  methods: {
    fmt(v) { return v ? String(v).replace("T", " ").slice(0, 19) : "-" },
    loadCurves() {
      this.curveLoading = true
      listCurve({ busbarNum: this.busbarNum, quarter: this.quarter }).then(res => {
        this.curves = res.rows || []
        this.hasCurve = this.curves.length > 0
        this.rec = null
      }).finally(() => { this.curveLoading = false })
    },
    loadAll() { this.loadCurves() },
    doReconcile() {
      this.$modal.loading("对账计算中（季度逐分钟）…")
      reconcile(this.quarter, this.busbarNum).then(res => {
        this.rec = res.data
      }).finally(() => { this.$modal.closeLoading() })
    },
    doExport() {
      this.download('vqms/art26/reconcile/export', { quarter: this.quarter, busbarNum: this.busbarNum },
        `第26条对账_${this.quarter}_母线${this.busbarNum}.xlsx`)
    },
    handleAdd() {
      this.form = { busbarNum: this.busbarNum, quarter: this.quarter, periodRange: null, limitUpKv: 232, limitDownKv: 224, source: "", remark: "" }
      this.open = true
      this.title = "登记考核曲线"
    },
    handleUpdate(row) {
      this.form = { curveId: row.curveId, busbarNum: row.busbarNum, quarter: row.quarter,
        periodRange: [this.fmt(row.periodStart), this.fmt(row.periodEnd)],
        limitUpKv: Number(row.limitUpKv), limitDownKv: Number(row.limitDownKv),
        source: row.source, remark: row.remark }
      this.open = true
      this.title = "修改考核曲线"
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) return
        const data = { ...this.form,
          periodStart: this.form.periodRange[0], periodEnd: this.form.periodRange[1] }
        delete data.periodRange
        const action = data.curveId ? updateCurve : addCurve
        action(data).then(() => {
          this.$modal.msgSuccess(data.curveId ? "修改成功" : "登记成功")
          this.open = false
          this.loadCurves()
        })
      })
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除该曲线行（' + this.fmt(row.periodStart) + ' ~ ' + this.fmt(row.periodEnd) + '）？')
        .then(() => delCurve(row.curveId))
        .then(() => { this.loadCurves(); this.$modal.msgSuccess("删除成功") })
        .catch(() => {})
    },
    doImport() {
      importCurve(this.quarter, "CSV导入", this.importText).then(res => {
        this.$modal.msgSuccess(res.msg || "导入成功")
        this.importOpen = false
        this.importText = ""
        this.loadCurves()
      })
    },
    cancel() { this.open = false }
  }
}
</script>

<style scoped>
.mb10 { margin-bottom: 10px; }
.sub-tip { float: right; color: #909399; font-size: 12px; }
.kpi { text-align: center; padding: 6px 0; border: 1px solid #ebeef5; border-radius: 4px; }
.kpi-label { font-size: 12px; color: #909399; }
.kpi-value { font-size: 20px; font-weight: bold; margin-top: 4px; }
.kpi-value.ok { color: #67C23A; }
.kpi-value.warn { color: #E6A23C; }
.kpi-value.danger { color: #F56C6C; }
.danger { color: #F56C6C; font-weight: bold; }
.warn { color: #E6A23C; font-weight: bold; }
</style>
