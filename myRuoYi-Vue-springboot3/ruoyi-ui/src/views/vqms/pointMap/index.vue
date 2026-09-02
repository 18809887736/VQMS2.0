<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="C=遥测 yc / X=遥信 yx" prop="pointKind">
        <el-input
          v-model="queryParams.pointKind"
          placeholder="请输入C=遥测 yc / X=遥信 yx"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="语义名称" prop="pointName">
        <el-input
          v-model="queryParams.pointName"
          placeholder="请输入语义名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="归属主体" prop="entityId">
        <el-input
          v-model="queryParams.entityId"
          placeholder="请输入归属主体"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="关联母线" prop="busbarNum">
        <el-input
          v-model="queryParams.busbarNum"
          placeholder="请输入关联母线"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="单位" prop="unit">
        <el-input
          v-model="queryParams.unit"
          placeholder="请输入单位"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="yx 值=1 语义" prop="state1Label">
        <el-input
          v-model="queryParams.state1Label"
          placeholder="请输入yx 值=1 语义"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="yx 值=0 语义" prop="state0Label">
        <el-input
          v-model="queryParams.state0Label"
          placeholder="请输入yx 值=0 语义"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否启用为考核门控：1=启用；真实环境默认0，现场核对后置1" prop="gateEnabled">
        <el-input
          v-model="queryParams.gateEnabled"
          placeholder="请输入是否启用为考核门控：1=启用；真实环境默认0，现场核对后置1"
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
          v-hasPermi="['vqms:pointMap:add']"
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
          v-hasPermi="['vqms:pointMap:edit']"
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
          v-hasPermi="['vqms:pointMap:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['vqms:pointMap:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="pointMapList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="点号" align="center" prop="pointNum" />
      <el-table-column label="C=遥测 yc / X=遥信 yx" align="center" prop="pointKind" />
      <el-table-column label="语义名称" align="center" prop="pointName" />
      <el-table-column label="busbar_id=主母线号 / voltage=电压 / power=有功 / reactive=无功 / yx=开关量 / analog=编码量" align="center" prop="pointType" />
      <el-table-column label="归属主体" align="center" prop="entityId" />
      <el-table-column label="关联母线" align="center" prop="busbarNum" />
      <el-table-column label="单位" align="center" prop="unit" />
      <el-table-column label="yx 值=1 语义" align="center" prop="state1Label" />
      <el-table-column label="yx 值=0 语义" align="center" prop="state0Label" />
      <el-table-column label="是否启用为考核门控：1=启用；真实环境默认0，现场核对后置1" align="center" prop="gateEnabled" />
      <el-table-column label="状态：0=正常, 1=停用" align="center" prop="status" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['vqms:pointMap:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['vqms:pointMap:remove']"
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

    <!-- 添加或修改点号语义注册对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="C=遥测 yc / X=遥信 yx" prop="pointKind">
              <el-input v-model="form.pointKind" placeholder="请输入C=遥测 yc / X=遥信 yx" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="语义名称" prop="pointName">
              <el-input v-model="form.pointName" placeholder="请输入语义名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="归属主体" prop="entityId">
              <el-input v-model="form.entityId" placeholder="请输入归属主体" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="关联母线" prop="busbarNum">
              <el-input v-model="form.busbarNum" placeholder="请输入关联母线" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="form.unit" placeholder="请输入单位" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="yx 值=1 语义" prop="state1Label">
              <el-input v-model="form.state1Label" placeholder="请输入yx 值=1 语义" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="yx 值=0 语义" prop="state0Label">
              <el-input v-model="form.state0Label" placeholder="请输入yx 值=0 语义" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否启用为考核门控：1=启用；真实环境默认0，现场核对后置1" prop="gateEnabled">
              <el-input v-model="form.gateEnabled" placeholder="请输入是否启用为考核门控：1=启用；真实环境默认0，现场核对后置1" />
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
import { listPointMap, getPointMap, delPointMap, addPointMap, updatePointMap } from "@/api/vqms/pointMap"

export default {
  name: "PointMap",
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
      // 点号语义注册表格数据
      pointMapList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        pointKind: null,
        pointName: null,
        pointType: null,
        entityId: null,
        busbarNum: null,
        unit: null,
        state1Label: null,
        state0Label: null,
        gateEnabled: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        pointKind: [
          { required: true, message: "C=遥测 yc / X=遥信 yx不能为空", trigger: "blur" }
        ],
        pointName: [
          { required: true, message: "语义名称不能为空", trigger: "blur" }
        ],
        gateEnabled: [
          { required: true, message: "是否启用为考核门控：1=启用；真实环境默认0，现场核对后置1不能为空", trigger: "blur" }
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
    /** 查询点号语义注册列表 */
    getList() {
      this.loading = true
      listPointMap(this.queryParams).then(response => {
        this.pointMapList = response.rows
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
        pointNum: null,
        pointKind: null,
        pointName: null,
        pointType: null,
        entityId: null,
        busbarNum: null,
        unit: null,
        state1Label: null,
        state0Label: null,
        gateEnabled: null,
        status: null,
        createTime: null,
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
      this.ids = selection.map(item => item.pointNum)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加点号语义注册"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const pointNum = row.pointNum || this.ids
      getPointMap(pointNum).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改点号语义注册"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.pointNum != null) {
            updatePointMap(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addPointMap(this.form).then(response => {
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
      const pointNums = row.pointNum || this.ids
      this.$modal.confirm('是否确认删除点号语义注册编号为"' + pointNums + '"的数据项？').then(function() {
        return delPointMap(pointNums)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('vqms/pointMap/export', {
        ...this.queryParams
      }, `pointMap_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
