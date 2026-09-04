<template>
  <div class="app-container">
    <el-card shadow="never" class="mb10">
      <div slot="header">
        <span>对端 AVC 配置库整定</span>
        <span class="sub-tip">上传 QHeatAvcRtdb.db → diff 预览 → 人工确认执行（幂等；容量/Q 额定差异只警示不落库）</span>
      </div>
      <el-upload
        ref="upload"
        drag
        action=""
        :auto-upload="false"
        :limit="1"
        :on-change="onFile"
        accept=".db"
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">拖拽配置库文件到此处，或<em>点击选择</em>（QHeatAvcRtdb.db）</div>
      </el-upload>
      <div style="margin-top: 10px">
        <el-button type="primary" icon="el-icon-search" size="small" :disabled="!file" :loading="loading" @click="doPreview">diff 预览</el-button>
        <el-button type="danger" icon="el-icon-check" size="small" :disabled="!sqls.length" :loading="applying" @click="doApply">确认执行（{{ sqls.length }} 项）</el-button>
      </div>
    </el-card>

    <el-card v-if="preview" shadow="never" class="mb10">
      <div slot="header">
        <span>diff 结果：换号 {{ preview.changes }} 项 / 警示 {{ preview.warnings }} 项（{{ preview.dbInfo }}）</span>
      </div>
      <el-table :data="preview.rows" size="mini" border>
        <el-table-column label="类别" prop="section" width="70" align="center" />
        <el-table-column label="项" prop="what" width="200" />
        <el-table-column label="现值" prop="current" width="110" align="center" />
        <el-table-column label="配置库值" width="110" align="center">
          <template slot-scope="s"><b>{{ s.row.config }}</b></template>
        </el-table-column>
        <el-table-column label="执行" width="70" align="center">
          <template slot-scope="s">
            <el-tag v-if="s.row.executable" size="mini" type="success">可执行</el-tag>
            <el-tag v-else size="mini" type="warning">警示</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="说明" prop="note" :show-overflow-tooltip="true" />
        <el-table-column label="勾选" width="60" align="center">
          <template slot-scope="s">
            <el-checkbox v-if="s.row.executable" v-model="s.row.checked" />
          </template>
        </el-table-column>
      </el-table>
      <el-alert v-if="preview.warnings > 0" type="warning" :closable="false" style="margin-top: 8px"
                title="警示项（容量/Q 额定/台账缺行）不自动落库：容量以监管确认为准（核实单 §1）、Q 额定走 P-Q 曲线换版流程" />
    </el-card>
  </div>
</template>

<script>
import { tuningPreview, tuningApply } from "@/api/vqms/tuning"

export default {
  name: "VqmsTuning",
  data() {
    return {
      file: null,
      loading: false,
      applying: false,
      preview: null
    }
  },
  computed: {
    sqls() {
      if (!this.preview) return []
      return this.preview.rows.filter(r => r.executable && r.checked).map(r => r.sql)
    }
  },
  methods: {
    onFile(file) {
      this.file = file.raw
      this.preview = null
    },
    doPreview() {
      const fd = new FormData()
      fd.append("file", this.file)
      this.loading = true
      tuningPreview(fd).then(res => {
        this.preview = res.data
        this.preview.rows.forEach(r => { this.$set(r, "checked", !!r.executable) })
        this.$modal.msgSuccess("预览完成：换号 " + this.preview.changes + " 项")
      }).finally(() => { this.loading = false })
    },
    doApply() {
      this.$modal.confirm('确认执行 ' + this.sqls.length + ' 条整定语句？执行后请重算判定验证等价。').then(() => {
        this.applying = true
        return tuningApply(this.sqls)
      }).then(res => {
        this.$modal.msgSuccess(res.msg || "整定完成")
        this.preview = null
      }).catch(() => {}).finally(() => { this.applying = false })
    }
  }
}
</script>

<style scoped>
.mb10 { margin-bottom: 10px; }
.sub-tip { float: right; color: #909399; font-size: 12px; }
</style>
