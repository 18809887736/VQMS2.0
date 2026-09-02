<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="所属主体" prop="entityId">
        <el-input
          v-model="queryParams.entityId"
          placeholder="请输入所属主体"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="设备编号" prop="deviceCode">
        <el-input
          v-model="queryParams.deviceCode"
          placeholder="请输入设备编号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="设备名称" prop="deviceName">
        <el-input
          v-model="queryParams.deviceName"
          placeholder="请输入设备名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="1=同步发电机/调相机 2=逆变器型(风/光/储) 3=SVC/STATCOM 4=电容器组 5=电抗器" prop="deviceType">
        <el-select v-model="queryParams.deviceType" placeholder="请选择1=同步发电机/调相机 2=逆变器型(风/光/储) 3=SVC/STATCOM 4=电容器组 5=电抗器" clearable>
          <el-option
            v-for="dict in dict.type.vqms_device_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="是否纳入 AVC 闭环控制" prop="inAvcLoop">
        <el-input
          v-model="queryParams.inAvcLoop"
          placeholder="请输入是否纳入 AVC 闭环控制"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="视在容量 kVA" prop="ratedSKva">
        <el-input
          v-model="queryParams.ratedSKva"
          placeholder="请输入视在容量 kVA"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="发出上限 kvar" prop="ratedQUpKvar">
        <el-input
          v-model="queryParams.ratedQUpKvar"
          placeholder="请输入发出上限 kvar"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="吸收下限 kvar" prop="ratedQDownKvar">
        <el-input
          v-model="queryParams.ratedQDownKvar"
          placeholder="请输入吸收下限 kvar"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="无功遥测点号" prop="qYcNum">
        <el-input
          v-model="queryParams.qYcNum"
          placeholder="请输入无功遥测点号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="有功遥测点号" prop="pYcNum">
        <el-input
          v-model="queryParams.pYcNum"
          placeholder="请输入有功遥测点号"
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
          v-hasPermi="['vqms:device:add']"
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
          v-hasPermi="['vqms:device:edit']"
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
          v-hasPermi="['vqms:device:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['vqms:device:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="deviceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键" align="center" prop="deviceId" />
      <el-table-column label="所属主体" align="center" prop="entityId" />
      <el-table-column label="设备编号" align="center" prop="deviceCode" />
      <el-table-column label="设备名称" align="center" prop="deviceName" />
      <el-table-column label="1=同步发电机/调相机 2=逆变器型(风/光/储) 3=SVC/STATCOM 4=电容器组 5=电抗器" align="center" prop="deviceType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.vqms_device_type" :value="scope.row.deviceType"/>
        </template>
      </el-table-column>
      <el-table-column label="是否纳入 AVC 闭环控制" align="center" prop="inAvcLoop" />
      <el-table-column label="视在容量 kVA" align="center" prop="ratedSKva" />
      <el-table-column label="发出上限 kvar" align="center" prop="ratedQUpKvar" />
      <el-table-column label="吸收下限 kvar" align="center" prop="ratedQDownKvar" />
      <el-table-column label="无功遥测点号" align="center" prop="qYcNum" />
      <el-table-column label="有功遥测点号" align="center" prop="pYcNum" />
      <el-table-column label="状态：0=正常, 1=停用" align="center" prop="status" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['vqms:device:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['vqms:device:remove']"
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

    <!-- 添加或修改无功设备台账对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="所属主体" prop="entityId">
              <el-input v-model="form.entityId" placeholder="请输入所属主体" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="设备编号" prop="deviceCode">
              <el-input v-model="form.deviceCode" placeholder="请输入设备编号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="设备名称" prop="deviceName">
              <el-input v-model="form.deviceName" placeholder="请输入设备名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="1=同步发电机/调相机 2=逆变器型(风/光/储) 3=SVC/STATCOM 4=电容器组 5=电抗器" prop="deviceType">
              <el-select v-model="form.deviceType" placeholder="请选择1=同步发电机/调相机 2=逆变器型(风/光/储) 3=SVC/STATCOM 4=电容器组 5=电抗器">
                <el-option
                  v-for="dict in dict.type.vqms_device_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="parseInt(dict.value)"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否纳入 AVC 闭环控制" prop="inAvcLoop">
              <el-input v-model="form.inAvcLoop" placeholder="请输入是否纳入 AVC 闭环控制" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="视在容量 kVA" prop="ratedSKva">
              <el-input v-model="form.ratedSKva" placeholder="请输入视在容量 kVA" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="发出上限 kvar" prop="ratedQUpKvar">
              <el-input v-model="form.ratedQUpKvar" placeholder="请输入发出上限 kvar" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="吸收下限 kvar" prop="ratedQDownKvar">
              <el-input v-model="form.ratedQDownKvar" placeholder="请输入吸收下限 kvar" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="无功遥测点号" prop="qYcNum">
              <el-input v-model="form.qYcNum" placeholder="请输入无功遥测点号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="有功遥测点号" prop="pYcNum">
              <el-input v-model="form.pYcNum" placeholder="请输入有功遥测点号" />
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
import { listDevice, getDevice, delDevice, addDevice, updateDevice } from "@/api/vqms/device"

export default {
  name: "Device",
  dicts: ['vqms_device_type'],
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
      // 无功设备台账表格数据
      deviceList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        entityId: null,
        deviceCode: null,
        deviceName: null,
        deviceType: null,
        inAvcLoop: null,
        ratedSKva: null,
        ratedQUpKvar: null,
        ratedQDownKvar: null,
        qYcNum: null,
        pYcNum: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        entityId: [
          { required: true, message: "所属主体不能为空", trigger: "blur" }
        ],
        deviceCode: [
          { required: true, message: "设备编号不能为空", trigger: "blur" }
        ],
        deviceName: [
          { required: true, message: "设备名称不能为空", trigger: "blur" }
        ],
        deviceType: [
          { required: true, message: "1=同步发电机/调相机 2=逆变器型(风/光/储) 3=SVC/STATCOM 4=电容器组 5=电抗器不能为空", trigger: "change" }
        ],
        inAvcLoop: [
          { required: true, message: "是否纳入 AVC 闭环控制不能为空", trigger: "blur" }
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
    /** 查询无功设备台账列表 */
    getList() {
      this.loading = true
      listDevice(this.queryParams).then(response => {
        this.deviceList = response.rows
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
        deviceId: null,
        entityId: null,
        deviceCode: null,
        deviceName: null,
        deviceType: null,
        inAvcLoop: null,
        ratedSKva: null,
        ratedQUpKvar: null,
        ratedQDownKvar: null,
        qYcNum: null,
        pYcNum: null,
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
      this.ids = selection.map(item => item.deviceId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加无功设备台账"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const deviceId = row.deviceId || this.ids
      getDevice(deviceId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改无功设备台账"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.deviceId != null) {
            updateDevice(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addDevice(this.form).then(response => {
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
      const deviceIds = row.deviceId || this.ids
      this.$modal.confirm('是否确认删除无功设备台账编号为"' + deviceIds + '"的数据项？').then(function() {
        return delDevice(deviceIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('vqms/device/export', {
        ...this.queryParams
      }, `device_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
