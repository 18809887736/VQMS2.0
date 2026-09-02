<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="主体编号" prop="entityCode">
        <el-input
          v-model="queryParams.entityCode"
          placeholder="请输入主体编号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="主体名称" prop="entityName">
        <el-input
          v-model="queryParams.entityName"
          placeholder="请输入主体名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="主体类型" prop="entityType">
        <el-select v-model="queryParams.entityType" placeholder="主体类型" clearable>
          <el-option
            v-for="dict in dict.type.vqms_entity_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="主体状态" clearable>
          <el-option
            v-for="dict in dict.type.sys_normal_disable"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
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
          v-hasPermi="['vqms:entity:add']"
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
          v-hasPermi="['vqms:entity:edit']"
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
          v-hasPermi="['vqms:entity:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['vqms:entity:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="entityList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主体ID" align="center" prop="entityId" />
      <el-table-column label="主体编号" align="center" prop="entityCode" />
      <el-table-column label="主体名称" align="center" prop="entityName" :show-overflow-tooltip="true" />
      <el-table-column label="主体类型" align="center" prop="entityType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.vqms_entity_type" :value="scope.row.entityType"/>
        </template>
      </el-table-column>
      <el-table-column label="额定容量(kW)" align="center" prop="ratedCapacityKw" />
      <el-table-column label="AVC闭环" align="center" prop="avcClosedLoop">
        <template slot-scope="scope">
          <span>{{ scope.row.avcClosedLoop === 1 ? '是' : '否' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="并网生效日" align="center" prop="effectiveFrom" width="100">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.effectiveFrom, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="解列日" align="center" prop="effectiveTo" width="100">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.effectiveTo, '{y}-{m}-{d}') || '在运' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['vqms:entity:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['vqms:entity:remove']"
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

    <!-- 添加或修改并网主体对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="主体编号" prop="entityCode">
          <el-input v-model="form.entityCode" placeholder="请输入主体编号（调度口径）" />
        </el-form-item>
        <el-form-item label="主体名称" prop="entityName">
          <el-input v-model="form.entityName" placeholder="请输入主体名称" />
        </el-form-item>
        <el-form-item label="主体类型" prop="entityType">
          <el-select v-model="form.entityType" placeholder="请选择主体类型">
            <el-option
              v-for="dict in dict.type.vqms_entity_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="额定容量kW" prop="ratedCapacityKw">
          <el-input-number v-model="form.ratedCapacityKw" :precision="3" :step="10000" :min="0" :controls="false" placeholder="考核基数" style="width: 100%" />
        </el-form-item>
        <el-form-item label="AVC闭环主体" prop="avcClosedLoop">
          <el-switch v-model="form.avcClosedLoop" :active-value="1" :inactive-value="0" active-text="第26条电压考核豁免输入" />
        </el-form-item>
        <el-form-item label="并网生效日" prop="effectiveFrom">
          <el-date-picker clearable
            v-model="form.effectiveFrom"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="选择并网生效日"
            style="width: 100%"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="解列日" prop="effectiveTo">
          <el-date-picker clearable
            v-model="form.effectiveTo"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="为空表示在运"
            style="width: 100%"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.sys_normal_disable"
              :key="dict.value"
              :label="dict.value"
            >{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
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
import { listEntity, getEntity, delEntity, addEntity, updateEntity } from "@/api/vqms/entity";

export default {
  name: "Entity",
  dicts: ['vqms_entity_type', 'sys_normal_disable'],
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
      // 并网主体表格数据
      entityList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        entityCode: null,
        entityName: null,
        entityType: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        entityCode: [
          { required: true, message: "主体编号不能为空", trigger: "blur" }
        ],
        entityName: [
          { required: true, message: "主体名称不能为空", trigger: "blur" }
        ],
        entityType: [
          { required: true, message: "主体类型不能为空", trigger: "change" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询并网主体列表 */
    getList() {
      this.loading = true;
      listEntity(this.queryParams).then(response => {
        this.entityList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        entityId: null,
        entityCode: null,
        entityName: null,
        entityType: null,
        ratedCapacityKw: null,
        avcClosedLoop: 1,
        effectiveFrom: null,
        effectiveTo: null,
        status: "0",
        remark: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.entityId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加并网主体";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const entityId = row.entityId || this.ids
      getEntity(entityId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改并网主体";
      });
    },
    /** 提交按钮 */
    submitForm: function() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.entityId != null) {
            updateEntity(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addEntity(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const entityIds = row.entityId || this.ids;
      this.$modal.confirm('是否确认删除并网主体编号为"' + entityIds + '"的数据项？').then(function() {
        return delEntity(entityIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('vqms/entity/export', {
        ...this.queryParams
      }, `entity_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
