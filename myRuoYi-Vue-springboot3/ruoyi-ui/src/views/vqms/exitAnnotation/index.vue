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
      <el-form-item label="退出时段起" prop="periodStart">
        <el-date-picker clearable
          v-model="queryParams.periodStart"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择退出时段起">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="退出时段止" prop="periodEnd">
        <el-date-picker clearable
          v-model="queryParams.periodEnd"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择退出时段止">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="GRID=电网原因" prop="exitReason">
        <el-input
          v-model="queryParams.exitReason"
          placeholder="请输入GRID=电网原因"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="来源：AUTO_YC=yc521/522 三态点自动 / MANUAL=人工标注" prop="source">
        <el-input
          v-model="queryParams.source"
          placeholder="请输入来源：AUTO_YC=yc521/522 三态点自动 / MANUAL=人工标注"
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
          v-hasPermi="['vqms:exitAnnotation:add']"
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
          v-hasPermi="['vqms:exitAnnotation:edit']"
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
          v-hasPermi="['vqms:exitAnnotation:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['vqms:exitAnnotation:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="exitAnnotationList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键" align="center" prop="annotationId" />
      <el-table-column label="考核主体" align="center" prop="entityId" />
      <el-table-column label="退出时段起" align="center" prop="periodStart" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.periodStart, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="退出时段止" align="center" prop="periodEnd" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.periodEnd, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="GRID=电网原因" align="center" prop="exitReason" />
      <el-table-column label="来源：AUTO_YC=yc521/522 三态点自动 / MANUAL=人工标注" align="center" prop="source" />
      <el-table-column label="依据" align="center" prop="evidence" />
      <el-table-column label="状态：0=有效, 1=撤销" align="center" prop="status" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['vqms:exitAnnotation:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['vqms:exitAnnotation:remove']"
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

    <!-- 添加或修改AVC退出原因标注对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="考核主体" prop="entityId">
              <el-input v-model="form.entityId" placeholder="请输入考核主体" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="退出时段起" prop="periodStart">
              <el-date-picker clearable
                v-model="form.periodStart"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择退出时段起">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="退出时段止" prop="periodEnd">
              <el-date-picker clearable
                v-model="form.periodEnd"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择退出时段止">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="GRID=电网原因" prop="exitReason">
              <el-input v-model="form.exitReason" placeholder="请输入GRID=电网原因" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="来源：AUTO_YC=yc521/522 三态点自动 / MANUAL=人工标注" prop="source">
              <el-input v-model="form.source" placeholder="请输入来源：AUTO_YC=yc521/522 三态点自动 / MANUAL=人工标注" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="依据" prop="evidence">
              <el-input v-model="form.evidence" type="textarea" placeholder="请输入内容" />
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
import { listExitAnnotation, getExitAnnotation, delExitAnnotation, addExitAnnotation, updateExitAnnotation } from "@/api/vqms/exitAnnotation"

export default {
  name: "ExitAnnotation",
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
      // AVC退出原因标注表格数据
      exitAnnotationList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        entityId: null,
        periodStart: null,
        periodEnd: null,
        exitReason: null,
        source: null,
        evidence: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        entityId: [
          { required: true, message: "考核主体不能为空", trigger: "blur" }
        ],
        periodStart: [
          { required: true, message: "退出时段起不能为空", trigger: "blur" }
        ],
        periodEnd: [
          { required: true, message: "退出时段止不能为空", trigger: "blur" }
        ],
        exitReason: [
          { required: true, message: "GRID=电网原因不能为空", trigger: "blur" }
        ],
        source: [
          { required: true, message: "来源：AUTO_YC=yc521/522 三态点自动 / MANUAL=人工标注不能为空", trigger: "blur" }
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
    /** 查询AVC退出原因标注列表 */
    getList() {
      this.loading = true
      listExitAnnotation(this.queryParams).then(response => {
        this.exitAnnotationList = response.rows
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
        periodStart: null,
        periodEnd: null,
        exitReason: null,
        source: null,
        evidence: null,
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
      this.title = "添加AVC退出原因标注"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const annotationId = row.annotationId || this.ids
      getExitAnnotation(annotationId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改AVC退出原因标注"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.annotationId != null) {
            updateExitAnnotation(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addExitAnnotation(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除AVC退出原因标注编号为"' + annotationIds + '"的数据项？').then(function() {
        return delExitAnnotation(annotationIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('vqms/exitAnnotation/export', {
        ...this.queryParams
      }, `exitAnnotation_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
