<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="考核主体" prop="entityId">
        <el-input
          v-model="queryParams.entityId"
          placeholder="请输入考核主体"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="指向指令" prop="warnTimeRaw">
        <el-input
          v-model="queryParams.warnTimeRaw"
          placeholder="请输入指向指令"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="毫秒原文" prop="millisecond">
        <el-input
          v-model="queryParams.millisecond"
          placeholder="请输入毫秒原文"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="对象编号" prop="objNum">
        <el-input
          v-model="queryParams.objNum"
          placeholder="请输入对象编号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="免考档：FAST/ECON/BOTH" prop="tier">
        <el-input
          v-model="queryParams.tier"
          placeholder="请输入免考档：FAST/ECON/BOTH"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="免考依据" prop="exemptReason">
        <el-input
          v-model="queryParams.exemptReason"
          placeholder="请输入免考依据"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="复核人" prop="reviewBy">
        <el-input
          v-model="queryParams.reviewBy"
          placeholder="请输入复核人"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="复核时间" prop="reviewTime">
        <el-date-picker clearable
          v-model="queryParams.reviewTime"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择复核时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="复核意见" prop="reviewOpinion">
        <el-input
          v-model="queryParams.reviewOpinion"
          placeholder="请输入复核意见"
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
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['vqms:exemptAnnotation:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['vqms:exemptAnnotation:remove']"
        >删除</el-button>
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
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键" align="center" prop="annotationId" />
      <el-table-column label="考核主体" align="center" prop="entityId" />
      <el-table-column label="指向指令" align="center" prop="warnTimeRaw" />
      <el-table-column label="毫秒原文" align="center" prop="millisecond" />
      <el-table-column label="对象编号" align="center" prop="objNum" />
      <el-table-column label="免考档：FAST/ECON/BOTH" align="center" prop="tier" />
      <el-table-column label="免考依据" align="center" prop="exemptReason" />
      <el-table-column label="佐证材料描述" align="center" prop="evidence" />
      <el-table-column label="复核状态：PENDING=待复核 / APPROVED=已批准" align="center" prop="reviewStatus" />
      <el-table-column label="复核人" align="center" prop="reviewBy" />
      <el-table-column label="复核时间" align="center" prop="reviewTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.reviewTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="复核意见" align="center" prop="reviewOpinion" />
      <el-table-column label="状态：0=有效, 1=撤销" align="center" prop="status" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['vqms:exemptAnnotation:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
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

    <!-- 添加或修改调节免考标注对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="考核主体" prop="entityId">
              <el-input v-model="form.entityId" placeholder="请输入考核主体" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="指向指令" prop="warnTimeRaw">
              <el-input v-model="form.warnTimeRaw" placeholder="请输入指向指令" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="毫秒原文" prop="millisecond">
              <el-input v-model="form.millisecond" placeholder="请输入毫秒原文" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="对象编号" prop="objNum">
              <el-input v-model="form.objNum" placeholder="请输入对象编号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="免考档：FAST/ECON/BOTH" prop="tier">
              <el-input v-model="form.tier" placeholder="请输入免考档：FAST/ECON/BOTH" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="免考依据" prop="exemptReason">
              <el-input v-model="form.exemptReason" placeholder="请输入免考依据" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="佐证材料描述" prop="evidence">
              <el-input v-model="form.evidence" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="复核人" prop="reviewBy">
              <el-input v-model="form.reviewBy" placeholder="请输入复核人" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="复核时间" prop="reviewTime">
              <el-date-picker clearable
                v-model="form.reviewTime"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择复核时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="复核意见" prop="reviewOpinion">
              <el-input v-model="form.reviewOpinion" placeholder="请输入复核意见" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listExemptAnnotation, getExemptAnnotation, delExemptAnnotation, addExemptAnnotation, updateExemptAnnotation } from "@/api/vqms/exemptAnnotation"

export default {
  name: "ExemptAnnotation",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
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
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        entityId: null,
        warnTimeRaw: null,
        millisecond: null,
        objNum: null,
        tier: null,
        exemptReason: null,
        evidence: null,
        reviewStatus: null,
        reviewBy: null,
        reviewTime: null,
        reviewOpinion: null,
        status: null,
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
          { required: true, message: "免考档：FAST/ECON/BOTH不能为空", trigger: "blur" }
        ],
        exemptReason: [
          { required: true, message: "免考依据不能为空", trigger: "blur" }
        ],
        reviewStatus: [
          { required: true, message: "复核状态：PENDING=待复核 / APPROVED=已批准不能为空", trigger: "change" }
        ],
        status: [
          { required: true, message: "状态：0=有效, 1=撤销不能为空", trigger: "change" }
        ],
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
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        annotationId: null,
        entityId: null,
        warnTimeRaw: null,
        millisecond: null,
        objNum: null,
        tier: null,
        exemptReason: null,
        evidence: null,
        reviewStatus: null,
        reviewBy: null,
        reviewTime: null,
        reviewOpinion: null,
        status: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
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
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加调节免考标注"
    },
    /** 修改按钮操作 */
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
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
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
