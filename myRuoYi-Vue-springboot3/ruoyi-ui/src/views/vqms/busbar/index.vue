<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="母线名称" prop="busbarName">
        <el-input
          v-model="queryParams.busbarName"
          placeholder="请输入母线名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="电压等级编码，对齐字典 vqms_v_grade：0=500kV, 1=220kV, 2=66kV及以下(预留)" prop="vGrade">
        <el-input
          v-model="queryParams.vGrade"
          placeholder="请输入电压等级编码，对齐字典 vqms_v_grade：0=500kV, 1=220kV, 2=66kV及以下(预留)"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="所属母线组" prop="groupNum">
        <el-input
          v-model="queryParams.groupNum"
          placeholder="请输入所属母线组"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="标称电压 kV" prop="nominalKv">
        <el-input
          v-model="queryParams.nominalKv"
          placeholder="请输入标称电压 kV"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="该母线 t0 实时电压 yc 点" prop="realtimeYcNum">
        <el-input
          v-model="queryParams.realtimeYcNum"
          placeholder="请输入该母线 t0 实时电压 yc 点"
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
          v-hasPermi="['vqms:busbar:add']"
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
          v-hasPermi="['vqms:busbar:edit']"
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
          v-hasPermi="['vqms:busbar:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['vqms:busbar:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="busbarList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主母线编号，对齐 his_curve_sv.busbar_num" align="center" prop="busbarNum" />
      <el-table-column label="母线名称" align="center" prop="busbarName" />
      <el-table-column label="电压等级编码，对齐字典 vqms_v_grade：0=500kV, 1=220kV, 2=66kV及以下(预留)" align="center" prop="vGrade">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.vqms_v_grade" :value="scope.row.vGrade"/>
        </template>
      </el-table-column>
      <el-table-column label="所属母线组" align="center" prop="groupNum" />
      <el-table-column label="标称电压 kV" align="center" prop="nominalKv" />
      <el-table-column label="该母线 t0 实时电压 yc 点" align="center" prop="realtimeYcNum" />
      <el-table-column label="状态：0=正常, 1=停用" align="center" prop="status" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['vqms:busbar:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['vqms:busbar:remove']"
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

    <!-- 添加或修改主母线台账对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="母线名称" prop="busbarName">
              <el-input v-model="form.busbarName" placeholder="请输入母线名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="电压等级编码，对齐字典 vqms_v_grade：0=500kV, 1=220kV, 2=66kV及以下(预留)" prop="vGrade">
              <el-input v-model="form.vGrade" placeholder="请输入电压等级编码，对齐字典 vqms_v_grade：0=500kV, 1=220kV, 2=66kV及以下(预留)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所属母线组" prop="groupNum">
              <el-input v-model="form.groupNum" placeholder="请输入所属母线组" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="标称电压 kV" prop="nominalKv">
              <el-input v-model="form.nominalKv" placeholder="请输入标称电压 kV" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="该母线 t0 实时电压 yc 点" prop="realtimeYcNum">
              <el-input v-model="form.realtimeYcNum" placeholder="请输入该母线 t0 实时电压 yc 点" />
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
import { listBusbar, getBusbar, delBusbar, addBusbar, updateBusbar } from "@/api/vqms/busbar"

export default {
  name: "Busbar",
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
      // 主母线台账表格数据
      busbarList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        busbarName: null,
        vGrade: null,
        groupNum: null,
        nominalKv: null,
        realtimeYcNum: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        busbarName: [
          { required: true, message: "母线名称不能为空", trigger: "blur" }
        ],
        vGrade: [
          { required: true, message: "电压等级编码，对齐字典 vqms_v_grade：0=500kV, 1=220kV, 2=66kV及以下(预留)不能为空", trigger: "blur" }
        ],
        nominalKv: [
          { required: true, message: "标称电压 kV不能为空", trigger: "blur" }
        ],
        status: [
          { required: true, message: "状态：0=正常, 1=停用不能为空", trigger: "change" }
        ],
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询主母线台账列表 */
    getList() {
      this.loading = true
      listBusbar(this.queryParams).then(response => {
        this.busbarList = response.rows
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
        busbarNum: null,
        busbarName: null,
        vGrade: null,
        groupNum: null,
        nominalKv: null,
        realtimeYcNum: null,
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
      this.ids = selection.map(item => item.busbarNum)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加主母线台账"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const busbarNum = row.busbarNum || this.ids
      getBusbar(busbarNum).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改主母线台账"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.busbarNum != null) {
            updateBusbar(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addBusbar(this.form).then(response => {
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
      const busbarNums = row.busbarNum || this.ids
      this.$modal.confirm('是否确认删除主母线台账编号为"' + busbarNums + '"的数据项？').then(function() {
        return delBusbar(busbarNums)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('vqms/busbar/export', {
        ...this.queryParams
      }, `busbar_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
