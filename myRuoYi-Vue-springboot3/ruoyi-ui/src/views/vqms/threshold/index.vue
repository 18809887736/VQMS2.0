<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="母线编号" prop="busbarNum">
        <el-input
          v-model="queryParams.busbarNum"
          placeholder="请输入母线编号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="AVC 容差 kV：220kV=1.000, 500kV=1.500；66kV及以下按 ±1% 在判定层由 nominal_kv 折算，本列可空" prop="toleranceV">
        <el-input
          v-model="queryParams.toleranceV"
          placeholder="请输入AVC 容差 kV：220kV=1.000, 500kV=1.500；66kV及以下按 ±1% 在判定层由 nominal_kv 折算，本列可空"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="生效起始日" prop="effectiveFrom">
        <el-date-picker clearable
          v-model="queryParams.effectiveFrom"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择生效起始日">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="生效结束日" prop="effectiveTo">
        <el-date-picker clearable
          v-model="queryParams.effectiveTo"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择生效结束日">
        </el-date-picker>
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
          v-hasPermi="['vqms:threshold:add']"
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
          v-hasPermi="['vqms:threshold:edit']"
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
          v-hasPermi="['vqms:threshold:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['vqms:threshold:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="thresholdList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键" align="center" prop="thresholdId" />
      <el-table-column label="母线编号" align="center" prop="busbarNum" />
      <el-table-column label="口径：AVC=附件6 合格区间 / GB=国标；附件6：500kV±1.5kV、220kV±1kV、66kV及以下±1%额定" align="center" prop="criterionType" />
      <el-table-column label="AVC 容差 kV：220kV=1.000, 500kV=1.500；66kV及以下按 ±1% 在判定层由 nominal_kv 折算，本列可空" align="center" prop="toleranceV" />
      <el-table-column label="生效起始日" align="center" prop="effectiveFrom" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.effectiveFrom, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="生效结束日" align="center" prop="effectiveTo" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.effectiveTo, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['vqms:threshold:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['vqms:threshold:remove']"
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

    <!-- 添加或修改母线电压阈值对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="母线编号" prop="busbarNum">
              <el-input v-model="form.busbarNum" placeholder="请输入母线编号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="AVC 容差 kV：220kV=1.000, 500kV=1.500；66kV及以下按 ±1% 在判定层由 nominal_kv 折算，本列可空" prop="toleranceV">
              <el-input v-model="form.toleranceV" placeholder="请输入AVC 容差 kV：220kV=1.000, 500kV=1.500；66kV及以下按 ±1% 在判定层由 nominal_kv 折算，本列可空" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="生效起始日" prop="effectiveFrom">
              <el-date-picker clearable
                v-model="form.effectiveFrom"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择生效起始日">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="生效结束日" prop="effectiveTo">
              <el-date-picker clearable
                v-model="form.effectiveTo"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择生效结束日">
              </el-date-picker>
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
import { listThreshold, getThreshold, delThreshold, addThreshold, updateThreshold } from "@/api/vqms/threshold"

export default {
  name: "Threshold",
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
      // 母线电压阈值表格数据
      thresholdList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        busbarNum: null,
        criterionType: null,
        toleranceV: null,
        effectiveFrom: null,
        effectiveTo: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        busbarNum: [
          { required: true, message: "母线编号不能为空", trigger: "blur" }
        ],
        criterionType: [
          { required: true, message: "口径：AVC=附件6 合格区间 / GB=国标；附件6：500kV±1.5kV、220kV±1kV、66kV及以下±1%额定不能为空", trigger: "change" }
        ],
        effectiveFrom: [
          { required: true, message: "生效起始日不能为空", trigger: "blur" }
        ],
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询母线电压阈值列表 */
    getList() {
      this.loading = true
      listThreshold(this.queryParams).then(response => {
        this.thresholdList = response.rows
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
        thresholdId: null,
        busbarNum: null,
        criterionType: null,
        toleranceV: null,
        effectiveFrom: null,
        effectiveTo: null,
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
      this.ids = selection.map(item => item.thresholdId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加母线电压阈值"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const thresholdId = row.thresholdId || this.ids
      getThreshold(thresholdId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改母线电压阈值"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.thresholdId != null) {
            updateThreshold(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addThreshold(this.form).then(response => {
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
      const thresholdIds = row.thresholdId || this.ids
      this.$modal.confirm('是否确认删除母线电压阈值编号为"' + thresholdIds + '"的数据项？').then(function() {
        return delThreshold(thresholdIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('vqms/threshold/export', {
        ...this.queryParams
      }, `threshold_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
