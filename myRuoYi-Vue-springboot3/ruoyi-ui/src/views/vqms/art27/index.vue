<template>
  <div class="app-container">
    <el-form :inline="true" size="small">
      <el-form-item label="统计月">
        <el-date-picker v-model="statMonth" type="month" value-format="yyyy-MM" :clearable="false" style="width: 130px" @change="loadAll" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="loadAll">查询</el-button>
        <el-button type="success" icon="el-icon-s-data" size="mini" @click="doReconcile" v-hasPermi="['vqms:art27:list']">对账</el-button>
        <el-button type="warning" icon="el-icon-download" size="mini" :disabled="!rec" @click="doExport" v-hasPermi="['vqms:art27:export']">对账单导出</el-button>
      </el-form-item>
    </el-form>

    <el-alert v-if="rec" type="info" :closable="false" class="mb10"
              :title="'月度合计：本方考核分 ' + rec.totalPenalty + ' ｜ 监管上报考核分 ' + (rec.totalRegulatorPenalty || 0) + '（1 分 = 1000 元）'" />

    <!-- 对账表 -->
    <el-card v-if="rec" shadow="never" class="mb10">
      <div slot="header"><span>第27条月度对账（合格线 99%；缺额每 1pp × 容量(万千瓦) × 0.1 分；合计上限容量 × 5 分）</span></div>
      <el-table :data="rec.rows" size="mini" border>
        <el-table-column label="装置" align="center" width="150">
          <template slot-scope="s">{{ s.row.deviceCode }}<br /><span class="sub">{{ s.row.deviceName }}</span></template>
        </el-table-column>
        <el-table-column label="容量 kW" prop="ratedCapacityKw" width="90" align="center" />
        <el-table-column label="自动可用(h)" prop="autoHours" width="95" align="center">
          <template slot-scope="s">{{ s.row.registered ? s.row.autoHours : '—' }}</template>
        </el-table-column>
        <el-table-column label="带电(h)" prop="energizedHours" width="80" align="center">
          <template slot-scope="s">{{ s.row.registered ? s.row.energizedHours : '未登记' }}</template>
        </el-table-column>
        <el-table-column label="可用率(%)" align="center" width="90">
          <template slot-scope="s">
            <span v-if="s.row.availabilityPct != null" :class="Number(s.row.availabilityPct) < 99 ? 'danger' : 'ok'">{{ s.row.availabilityPct }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="缺额pp" prop="shortfallPct" width="75" align="center" />
        <el-table-column label="速率/铭牌天数" align="center" width="100">
          <template slot-scope="s">{{ s.row.ratePenaltyDays || 0 }} / {{ s.row.nameplateDays || 0 }}</template>
        </el-table-column>
        <el-table-column label="合计罚分" align="center" width="110">
          <template slot-scope="s">
            <span v-if="s.row.totalPenalty != null" :class="Number(s.row.totalPenalty) > 0 ? 'danger' : 'ok'">{{ s.row.totalPenalty }}{{ s.row.capped ? '(封顶)' : '' }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="罚款(元)" align="center" width="100">
          <template slot-scope="s">{{ s.row.penaltyCny != null ? Number(s.row.penaltyCny).toLocaleString() : '—' }}</template>
        </el-table-column>
        <el-table-column label="监管上报率(%)" prop="regulatorRate" width="100" align="center" />
        <el-table-column label="监管考核分" prop="regulatorPenalty" width="90" align="center" />
      </el-table>
    </el-card>

    <!-- 月度登记 -->
    <el-card shadow="never" class="mb10">
      <div slot="header">
        <span>月度登记（{{ statMonth }}）</span>
        <el-button type="primary" size="mini" icon="el-icon-plus" style="float: right" @click="handleMonthAdd" v-hasPermi="['vqms:art27:add']">登记</el-button>
      </div>
      <el-table v-loading="monthLoading" :data="months" size="mini" border>
        <el-table-column label="装置" align="center" width="150">
          <template slot-scope="s">{{ deviceLabel(s.row.deviceId) }}</template>
        </el-table-column>
        <el-table-column label="自动可用(h)" prop="autoHours" width="95" align="center" />
        <el-table-column label="带电(h)" prop="energizedHours" width="80" align="center" />
        <el-table-column label="速率天数" prop="ratePenaltyDays" width="80" align="center" />
        <el-table-column label="铭牌天数" prop="nameplateDays" width="80" align="center" />
        <el-table-column label="监管上报率(%)" prop="regulatorRate" width="100" align="center" />
        <el-table-column label="监管考核分" prop="regulatorPenalty" width="90" align="center" />
        <el-table-column label="备注" prop="remark" :show-overflow-tooltip="true" />
        <el-table-column label="操作" align="center" width="130">
          <template slot-scope="s">
            <el-button size="mini" type="text" icon="el-icon-edit" @click="handleMonthUpdate(s.row)" v-hasPermi="['vqms:art27:edit']">修改</el-button>
            <el-button size="mini" type="text" icon="el-icon-delete" @click="handleMonthDelete(s.row)" v-hasPermi="['vqms:art27:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 装置台账 -->
    <el-card shadow="never">
      <div slot="header">
        <span>动态无功补偿装置台账（SVG/SVC/调相机；风光储主体适用）</span>
        <el-button type="primary" size="mini" icon="el-icon-plus" style="float: right" @click="handleDeviceAdd" v-hasPermi="['vqms:art27:add']">登记</el-button>
      </div>
      <el-table v-loading="deviceLoading" :data="devices" size="mini" border>
        <el-table-column label="编号" prop="deviceCode" width="110" align="center" />
        <el-table-column label="名称" prop="deviceName" width="160" align="center" />
        <el-table-column label="类型" align="center" width="90">
          <template slot-scope="s">{{ typeText(s.row.deviceType) }}</template>
        </el-table-column>
        <el-table-column label="额定容量 kW" prop="ratedCapacityKw" width="110" align="center" />
        <el-table-column label="自动投运信号点" align="center" width="110">
          <template slot-scope="s">{{ s.row.autoYxNum || '未接入' }}</template>
        </el-table-column>
        <el-table-column label="升压变带电信号点" align="center" width="115">
          <template slot-scope="s">{{ s.row.energizedYxNum || '未接入' }}</template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" :show-overflow-tooltip="true" />
        <el-table-column label="操作" align="center" width="130">
          <template slot-scope="s">
            <el-button size="mini" type="text" icon="el-icon-edit" @click="handleDeviceUpdate(s.row)" v-hasPermi="['vqms:art27:edit']">修改</el-button>
            <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDeviceDelete(s.row)" v-hasPermi="['vqms:art27:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 月度登记对话框 -->
    <el-dialog :title="monthTitle" :visible.sync="monthOpen" width="480px" append-to-body>
      <el-form ref="monthForm" :model="monthForm" :rules="monthRules" label-width="110px">
        <el-form-item label="装置" prop="deviceId">
          <el-select v-model="monthForm.deviceId" style="width: 280px">
            <el-option v-for="d in devices" :key="d.deviceId" :label="d.deviceCode + ' ' + d.deviceName" :value="d.deviceId" />
          </el-select>
        </el-form-item>
        <el-form-item label="自动可用小时" prop="autoHours">
          <el-input-number v-model="monthForm.autoHours" :precision="3" :min="0" :max="744" controls-position="right" />
        </el-form-item>
        <el-form-item label="升压变带电小时" prop="energizedHours">
          <el-input-number v-model="monthForm.energizedHours" :precision="3" :min="0" :max="744" controls-position="right" />
        </el-form-item>
        <el-form-item label="速率不符天数">
          <el-input-number v-model="monthForm.ratePenaltyDays" :min="0" :max="31" controls-position="right" />
        </el-form-item>
        <el-form-item label="铭牌不符天数">
          <el-input-number v-model="monthForm.nameplateDays" :min="0" :max="31" controls-position="right" />
        </el-form-item>
        <el-form-item label="监管上报率(%)">
          <el-input-number v-model="monthForm.regulatorRate" :precision="3" :min="0" :max="100" controls-position="right" />
        </el-form-item>
        <el-form-item label="监管考核分">
          <el-input-number v-model="monthForm.regulatorPenalty" :precision="3" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="monthForm.remark" placeholder="数据来源/依据" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submitMonth">确 定</el-button>
        <el-button @click="monthOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 装置登记对话框 -->
    <el-dialog :title="deviceTitle" :visible.sync="deviceOpen" width="480px" append-to-body>
      <el-form ref="deviceForm" :model="deviceForm" :rules="deviceRules" label-width="110px">
        <el-form-item label="装置编号" prop="deviceCode">
          <el-input v-model="deviceForm.deviceCode" placeholder="如 SVG_01" style="width: 220px" />
        </el-form-item>
        <el-form-item label="装置名称" prop="deviceName">
          <el-input v-model="deviceForm.deviceName" placeholder="如 35kV SVG 补偿装置" />
        </el-form-item>
        <el-form-item label="类型" prop="deviceType">
          <el-select v-model="deviceForm.deviceType" style="width: 200px">
            <el-option label="SVG" :value="1" />
            <el-option label="SVC" :value="2" />
            <el-option label="调相机" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="额定容量 kW" prop="ratedCapacityKw">
          <el-input-number v-model="deviceForm.ratedCapacityKw" :precision="3" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="自动投运信号点">
          <el-input-number v-model="deviceForm.autoYxNum" :min="1" controls-position="right" placeholder="接入后填" />
        </el-form-item>
        <el-form-item label="升压变带电信号点">
          <el-input-number v-model="deviceForm.energizedYxNum" :min="1" controls-position="right" placeholder="接入后填" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="deviceForm.remark" placeholder="备注" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submitDevice">确 定</el-button>
        <el-button @click="deviceOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listDevices, addDevice, updateDevice, delDevice, listMonths, addMonth, updateMonth, delMonth, reconcile27 } from "@/api/vqms/art27"

export default {
  name: "VqmsArt27",
  data() {
    const now = new Date()
    return {
      statMonth: now.getFullYear() + "-" + String(now.getMonth() + 1).padStart(2, "0"),
      devices: [],
      months: [],
      deviceLoading: false,
      monthLoading: false,
      rec: null,
      deviceOpen: false,
      monthOpen: false,
      deviceTitle: "",
      monthTitle: "",
      deviceForm: {},
      monthForm: {},
      deviceRules: {
        deviceCode: [{ required: true, message: "编号必填", trigger: "blur" }],
        deviceName: [{ required: true, message: "名称必填", trigger: "blur" }],
        deviceType: [{ required: true, message: "类型必选", trigger: "change" }],
        ratedCapacityKw: [{ required: true, message: "容量必填", trigger: "blur" }]
      },
      monthRules: {
        deviceId: [{ required: true, message: "装置必选", trigger: "change" }],
        energizedHours: [{ required: true, message: "带电小时必填", trigger: "blur" }],
        autoHours: [{ required: true, message: "可用小时必填", trigger: "blur" }]
      }
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    typeText(t) { return { 1: "SVG", 2: "SVC", 3: "调相机" }[t] || t },
    deviceLabel(id) {
      const d = this.devices.find(x => x.deviceId === id)
      return d ? d.deviceCode + " " + d.deviceName : id
    },
    loadAll() {
      this.deviceLoading = true
      listDevices({}).then(res => { this.devices = res.rows || [] }).finally(() => { this.deviceLoading = false })
      this.monthLoading = true
      listMonths(this.statMonth).then(res => { this.months = res.rows || [] }).finally(() => { this.monthLoading = false })
      this.rec = null
    },
    doReconcile() {
      reconcile27(this.statMonth).then(res => { this.rec = res.data })
    },
    doExport() {
      this.download('vqms/art27/reconcile/export', { statMonth: this.statMonth }, `第27条对账_${this.statMonth}.xlsx`)
    },
    handleDeviceAdd() {
      this.deviceForm = { entityId: 1, deviceCode: "", deviceName: "", deviceType: 1, ratedCapacityKw: undefined, autoYxNum: undefined, energizedYxNum: undefined, status: "0", remark: "" }
      this.deviceOpen = true
      this.deviceTitle = "登记装置"
    },
    handleDeviceUpdate(row) {
      this.deviceForm = { ...row }
      this.deviceOpen = true
      this.deviceTitle = "修改装置"
    },
    submitDevice() {
      this.$refs["deviceForm"].validate(valid => {
        if (!valid) return
        const action = this.deviceForm.deviceId ? updateDevice : addDevice
        action(this.deviceForm).then(() => {
          this.$modal.msgSuccess("保存成功")
          this.deviceOpen = false
          this.loadAll()
        })
      })
    },
    handleDeviceDelete(row) {
      this.$modal.confirm('确认删除装置 ' + row.deviceCode + '（其月度登记将保留孤儿行）？')
        .then(() => delDevice(row.deviceId))
        .then(() => { this.loadAll(); this.$modal.msgSuccess("删除成功") })
        .catch(() => {})
    },
    handleMonthAdd() {
      this.monthForm = { statMonth: this.statMonth, deviceId: undefined, autoHours: undefined, energizedHours: undefined, ratePenaltyDays: 0, nameplateDays: 0, regulatorRate: undefined, regulatorPenalty: undefined, remark: "" }
      this.monthOpen = true
      this.monthTitle = "登记月度数据"
    },
    handleMonthUpdate(row) {
      this.monthForm = { ...row }
      this.monthOpen = true
      this.monthTitle = "修改月度数据"
    },
    submitMonth() {
      this.$refs["monthForm"].validate(valid => {
        if (!valid) return
        const action = this.monthForm.id ? updateMonth : addMonth
        action(this.monthForm).then(() => {
          this.$modal.msgSuccess("保存成功")
          this.monthOpen = false
          this.loadAll()
        })
      })
    },
    handleMonthDelete(row) {
      this.$modal.confirm('确认删除该月度登记？')
        .then(() => delMonth(row.id))
        .then(() => { this.loadAll(); this.$modal.msgSuccess("删除成功") })
        .catch(() => {})
    }
  }
}
</script>

<style scoped>
.mb10 { margin-bottom: 10px; }
.sub { font-size: 11px; color: #909399; }
.danger { color: #F56C6C; font-weight: bold; }
.ok { color: #67C23A; }
</style>
