<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="所属并网主体" prop="entityId">
        <el-input
          v-model="queryParams.entityId"
          placeholder="请输入所属并网主体"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="组名" prop="groupName">
        <el-input
          v-model="queryParams.groupName"
          placeholder="请输入组名"
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
      <el-form-item label="该组当前主母线号指示点" prop="mainIndicatorYcNum">
        <el-input
          v-model="queryParams.mainIndicatorYcNum"
          placeholder="请输入该组当前主母线号指示点"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="指示点不可用兜底主母线号；NULL=不兜底→该组该分钟无主母线" prop="defaultMainBusbarNum">
        <el-input
          v-model="queryParams.defaultMainBusbarNum"
          placeholder="请输入指示点不可用兜底主母线号；NULL=不兜底→该组该分钟无主母线"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="指示点陈旧窗口(分钟)" prop="maxStalenessMinutes">
        <el-input
          v-model="queryParams.maxStalenessMinutes"
          placeholder="请输入指示点陈旧窗口(分钟)"
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
          v-hasPermi="['vqms:busbarGroup:add']"
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
          v-hasPermi="['vqms:busbarGroup:edit']"
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
          v-hasPermi="['vqms:busbarGroup:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['vqms:busbarGroup:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="busbarGroupList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="母线组编号" align="center" prop="groupNum" />
      <el-table-column label="所属并网主体" align="center" prop="entityId" />
      <el-table-column label="组名" align="center" prop="groupName" />
      <el-table-column label="电压等级编码，对齐字典 vqms_v_grade：0=500kV, 1=220kV, 2=66kV及以下(预留)" align="center" prop="vGrade">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.vqms_v_grade" :value="scope.row.vGrade"/>
        </template>
      </el-table-column>
      <el-table-column label="该组当前主母线号指示点" align="center" prop="mainIndicatorYcNum" />
      <el-table-column label="指示点不可用兜底主母线号；NULL=不兜底→该组该分钟无主母线" align="center" prop="defaultMainBusbarNum" />
      <el-table-column label="指示点陈旧窗口(分钟)" align="center" prop="maxStalenessMinutes" />
      <el-table-column label="状态：0=正常, 1=停用" align="center" prop="status" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['vqms:busbarGroup:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['vqms:busbarGroup:remove']"
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

    <!-- 添加或修改母线组对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="所属并网主体" prop="entityId">
              <el-input v-model="form.entityId" placeholder="请输入所属并网主体" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="组名" prop="groupName">
              <el-input v-model="form.groupName" placeholder="请输入组名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="电压等级编码，对齐字典 vqms_v_grade：0=500kV, 1=220kV, 2=66kV及以下(预留)" prop="vGrade">
              <el-input v-model="form.vGrade" placeholder="请输入电压等级编码，对齐字典 vqms_v_grade：0=500kV, 1=220kV, 2=66kV及以下(预留)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="该组当前主母线号指示点" prop="mainIndicatorYcNum">
              <el-input v-model="form.mainIndicatorYcNum" placeholder="请输入该组当前主母线号指示点" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="指示点不可用兜底主母线号；NULL=不兜底→该组该分钟无主母线" prop="defaultMainBusbarNum">
              <el-input v-model="form.defaultMainBusbarNum" placeholder="请输入指示点不可用兜底主母线号；NULL=不兜底→该组该分钟无主母线" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="指示点陈旧窗口(分钟)" prop="maxStalenessMinutes">
              <el-input v-model="form.maxStalenessMinutes" placeholder="请输入指示点陈旧窗口(分钟)" />
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
import { listBusbarGroup, getBusbarGroup, delBusbarGroup, addBusbarGroup, updateBusbarGroup } from "@/api/vqms/busbarGroup"

export default {
  name: "BusbarGroup",
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
      // 母线组表格数据
      busbarGroupList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        entityId: null,
        groupName: null,
        vGrade: null,
        mainIndicatorYcNum: null,
        defaultMainBusbarNum: null,
        maxStalenessMinutes: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        entityId: [
          { required: true, message: "所属并网主体不能为空", trigger: "blur" }
        ],
        groupName: [
          { required: true, message: "组名不能为空", trigger: "blur" }
        ],
        vGrade: [
          { required: true, message: "电压等级编码，对齐字典 vqms_v_grade：0=500kV, 1=220kV, 2=66kV及以下(预留)不能为空", trigger: "blur" }
        ],
        maxStalenessMinutes: [
          { required: true, message: "指示点陈旧窗口(分钟)不能为空", trigger: "blur" }
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
    /** 查询母线组列表 */
    getList() {
      this.loading = true
      listBusbarGroup(this.queryParams).then(response => {
        this.busbarGroupList = response.rows
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
        groupNum: null,
        entityId: null,
        groupName: null,
        vGrade: null,
        mainIndicatorYcNum: null,
        defaultMainBusbarNum: null,
        maxStalenessMinutes: null,
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
      this.ids = selection.map(item => item.groupNum)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加母线组"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const groupNum = row.groupNum || this.ids
      getBusbarGroup(groupNum).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改母线组"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.groupNum != null) {
            updateBusbarGroup(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addBusbarGroup(this.form).then(response => {
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
      const groupNums = row.groupNum || this.ids
      this.$modal.confirm('是否确认删除母线组编号为"' + groupNums + '"的数据项？').then(function() {
        return delBusbarGroup(groupNums)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('vqms/busbarGroup/export', {
        ...this.queryParams
      }, `busbarGroup_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
