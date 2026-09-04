<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="复核状态" prop="reviewStatus">
        <el-select v-model="queryParams.reviewStatus" placeholder="请选择复核状态" clearable style="width: 160px">
          <el-option v-for="(label, key) in reviewStatusMap" :key="key" :label="label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="指向指令" prop="warnTimeRaw">
        <el-input
          v-model="queryParams.warnTimeRaw"
          placeholder="请输入指向指令时间原文"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="免考档" prop="tier">
        <el-select v-model="queryParams.tier" placeholder="请选择免考档" clearable style="width: 140px">
          <el-option v-for="t in tierOptions" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="复核人" prop="reviewBy">
        <el-input
          v-model="queryParams.reviewBy"
          placeholder="请输入复核人"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['vqms:exemptAnnotation:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['vqms:exemptAnnotation:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="exemptAnnotationList" @selection-change="handleSelectionChange">
      <el-table-column label="主键" align="center" prop="annotationId" width="60" />
      <el-table-column label="指向指令" align="center" prop="warnTimeRaw" width="170" />
      <el-table-column label="毫秒原文" align="center" prop="millisecond" width="140" />
      <el-table-column label="对象编号" align="center" prop="objNum" width="70" />
      <el-table-column label="免考档" align="center" prop="tier" width="70">
        <template slot-scope="scope">
          <el-tag size="small">{{ scope.row.tier }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="免考依据" align="center" prop="exemptReason" :show-overflow-tooltip="true" />
      <el-table-column label="复核状态" align="center" prop="reviewStatus" width="90">
        <template slot-scope="scope">
          <el-tag size="small" :type="reviewTagType[scope.row.reviewStatus] || 'info'">
            {{ reviewStatusMap[scope.row.reviewStatus] || scope.row.reviewStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标注人" align="center" prop="createBy" width="90" />
      <el-table-column label="复核人" align="center" prop="reviewBy" width="90" />
      <el-table-column label="复核时间" align="center" prop="reviewTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.reviewTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="复核意见" align="center" prop="reviewOpinion" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="260">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            v-if="scope.row.reviewStatus === 'PENDING'"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['vqms:exemptAnnotation:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-check"
            v-if="scope.row.reviewStatus === 'PENDING'"
            @click="handleApprove(scope.row)"
            v-hasPermi="['vqms:exemptAnnotation:edit']"
          >批准</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-close"
            v-if="scope.row.reviewStatus === 'PENDING'"
            @click="handleReject(scope.row)"
            v-hasPermi="['vqms:exemptAnnotation:edit']"
          >驳回</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            v-if="scope.row.reviewStatus === 'PENDING'"
            @click="handleDelete(scope.row)"
            v-hasPermi="['vqms:exemptAnnotation:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改调节免考标注对话框（复核字段不在此录入——批准/驳回走列表复核按钮） -->
    <el-dialog :title="title" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="考核主体" prop="entityId">
          <el-input-number v-model="form.entityId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="指向指令" prop="warnTimeRaw">
          <el-input v-model="form.warnTimeRaw" placeholder="warn_info.warn_time 原文（如 2026-04-03 10:00:00）" />
        </el-form-item>
        <el-form-item label="毫秒原文" prop="millisecond">
          <el-input v-model="form.millisecond" placeholder="溯源键成分（与指令行一致）" />
        </el-form-item>
        <el-form-item label="对象编号" prop="objNum">
          <el-input-number v-model="form.objNum" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="免考档" prop="tier">
          <el-select v-model="form.tier" style="width: 200px">
            <el-option v-for="t in tierOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="免考依据" prop="exemptReason">
          <el-input v-model="form.exemptReason" placeholder="附件6§三：全部闭环无功设备正确方向顶满仍不达标 等" />
        </el-form-item>
        <el-form-item label="佐证材料" prop="evidence">
          <el-input v-model="form.evidence" type="textarea" placeholder="设备Q曲线截图/调度电话记录等" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listExemptAnnotation, getExemptAnnotation, delExemptAnnotation, addExemptAnnotation, updateExemptAnnotation, reviewExemptAnnotation } from "@/api/vqms/exemptAnnotation"

export default {
  name: "ExemptAnnotation",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 调节免考标注表格数据
      exemptAnnotationList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 复核状态映射
      reviewStatusMap: { PENDING: "待复核", APPROVED: "已批准", REJECTED: "已驳回" },
      reviewTagType: { PENDING: "warning", APPROVED: "success", REJECTED: "danger" },
      tierOptions: [
        { value: "FAST", label: "FAST 快速性档" },
        { value: "ECON", label: "ECON 经济性档" },
        { value: "BOTH", label: "BOTH 两档" }
      ],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        reviewStatus: null,
        warnTimeRaw: null,
        tier: null,
        reviewBy: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        entityId: [
          { required: true, message: "考核主体不能为空", trigger: "blur" }
        ],
        warnTimeRaw: [
          { required: true, message: "指向指令不能为空", trigger: "blur" }
        ],
        tier: [
          { required: true, message: "免考档不能为空", trigger: "change" }
        ],
        exemptReason: [
          { required: true, message: "免考依据不能为空", trigger: "blur" }
        ]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询调节免考标注列表 */
    getList() {
      this.loading = true
      listExemptAnnotation(this.queryParams).then(response => {
        this.exemptAnnotationList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 单账户复核（Leo 2026-09-04 拍板简化：标注人可自批，一人走通全流程）
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置（review_status/status 走库默认：PENDING / 0）
    reset() {
      this.form = {
        annotationId: null,
        entityId: 1,
        warnTimeRaw: null,
        millisecond: null,
        objNum: 0,
        tier: "BOTH",
        exemptReason: null,
        evidence: null,
        remark: null
      }
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.annotationId)
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加调节免考标注"
    },
    /** 修改按钮操作（仅待复核可改） */
    handleUpdate(row) {
      this.reset()
      const annotationId = row.annotationId || this.ids
      getExemptAnnotation(annotationId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改调节免考标注"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.annotationId != null) {
            updateExemptAnnotation(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addExemptAnnotation(this.form).then(response => {
              this.$modal.msgSuccess("新增成功（待复核）")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 批准（生效需重算判定） */
    handleApprove(row) {
      this.$modal.confirm('确认批准标注 #' + row.annotationId + ' 免考（' + row.tier + '档）？批准后需重算判定生效。').then(() => {
        return reviewExemptAnnotation({ annotationId: row.annotationId, reviewStatus: "APPROVED" })
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("已批准；重算判定后按 MANUAL 免考生效")
      }).catch(() => {})
    },
    /** 驳回（必填意见） */
    handleReject(row) {
      this.$prompt('请输入驳回原因（必填）', '驳回标注 #' + row.annotationId, {
        confirmButtonText: '确定驳回',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputValidator: v => (v && v.trim().length > 0) || '驳回原因不能为空'
      }).then(({ value }) => {
        return reviewExemptAnnotation({ annotationId: row.annotationId, reviewStatus: "REJECTED", reviewOpinion: value })
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("已驳回")
      }).catch(() => {})
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const annotationIds = row.annotationId || this.ids
      this.$modal.confirm('是否确认删除调节免考标注编号为"' + annotationIds + '"的数据项？').then(function() {
        return delExemptAnnotation(annotationIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('vqms/exemptAnnotation/export', {
        ...this.queryParams
      }, `exemptAnnotation_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
