<template>
  <div class="hr-certificate-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" class="search-form" @submit.prevent>
        <el-form-item label="职工">
          <el-select
            v-model="query.employeeId"
            placeholder="全部"
            clearable
            filterable
            style="width: 210px"
          >
            <el-option v-for="e in employees" :key="e.id" :label="employeeLabel(e)" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="证书名称">
          <el-input
            v-model="query.certName"
            placeholder="模糊匹配"
            clearable
            style="width: 170px"
            @keyup.enter="onSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <div class="toolbar">
        <el-button
          v-permission="'hr:certificate:add'"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          新增证书
        </el-button>
      </div>

      <el-table :data="records" v-loading="loading" border>
        <el-table-column label="职工" min-width="110">
          <template #default="{ row }">{{ empName(row.employeeId) }}</template>
        </el-table-column>
        <el-table-column label="证书名称" prop="certName" min-width="140" show-overflow-tooltip />
        <el-table-column label="证书编号" prop="certNo" min-width="120" show-overflow-tooltip />
        <el-table-column label="发证机构" prop="issueOrg" min-width="140" show-overflow-tooltip />
        <el-table-column label="发证日期" prop="issueDate" width="115" />
        <el-table-column label="有效期至" prop="expireDate" width="115" />
        <el-table-column label="扫描件" width="90">
          <template #default="{ row }">
            <el-link v-if="row.fileUrl" type="primary" :href="row.fileUrl" target="_blank">查看</el-link>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'hr:certificate:edit'" link type="primary" @click="openEdit(row)">
              修改
            </el-button>
            <el-button
              v-permission="'hr:certificate:remove'"
              link
              type="danger"
              @click="onDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>暂无数据</template>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page.current"
          v-model:page-size="page.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="load"
          @current-change="load"
        />
      </div>
    </el-card>

    <!-- 新增 / 修改弹窗 -->
    <el-dialog
      v-model="dialog.visible"
      :title="dialog.mode === 'create' ? '新增证书' : '修改证书'"
      width="640px"
      top="6vh"
      @closed="resetDialog"
    >
      <el-form ref="formRef" v-loading="dialog.submitting" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="职工" prop="employeeId">
              <el-select v-model="form.employeeId" placeholder="请选择" filterable style="width: 100%">
                <el-option
                  v-for="e in employees"
                  :key="e.id"
                  :label="employeeLabel(e)"
                  :value="e.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证书名称" prop="certName">
              <el-input v-model="form.certName" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证书编号">
              <el-input v-model="form.certNo" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发证机构">
              <el-input v-model="form.issueOrg" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发证日期">
              <el-date-picker
                v-model="form.issueDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="有效期至">
              <el-date-picker
                v-model="form.expireDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="不得早于发证日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="扫描件地址">
              <el-input v-model="form.fileUrl" maxlength="255" placeholder="如 /uploads/202610/xxx.png" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="onSubmit">保 存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { certificatePage, createCertificate, deleteCertificate, updateCertificate } from '@/api/hr'
import { empName, employeeLabel, employees, loadEmployees } from '@/utils/lookup'
import type {
  Certificate,
  CertificateDTO,
  CertificatePageParams,
  PageResult
} from '@/types/api'

// ---------- 列表 ----------
const loading = ref(false)
const records = ref<Certificate[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })
const query = reactive<{ employeeId: number | null; certName: string }>({
  employeeId: null,
  certName: ''
})

async function load() {
  loading.value = true
  try {
    const params: CertificatePageParams = {
      current: page.current,
      size: page.size,
      employeeId: query.employeeId,
      certName: query.certName.trim() || undefined
    }
    const res = await certificatePage(params)
    const data = (res.data || {}) as PageResult<Certificate>
    records.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.current = 1
  load()
}

function onReset() {
  query.employeeId = null
  query.certName = ''
  onSearch()
}

// ---------- 删除 ----------
async function onDelete(row: Certificate) {
  try {
    await ElMessageBox.confirm(
      `确认删除职工【${empName(row.employeeId)}】的证书【${row.certName}】？`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await deleteCertificate(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && page.current > 1) {
    page.current -= 1
  }
  load()
}

// ---------- 新增 / 修改 ----------
const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', submitting: false })
const formRef = ref<FormInstance>()

const emptyForm = (): CertificateDTO => ({
  id: null,
  employeeId: null,
  certName: '',
  certNo: null,
  issueOrg: null,
  issueDate: null,
  expireDate: null,
  fileUrl: null
})

const form = reactive<CertificateDTO>(emptyForm())

const rules: FormRules = {
  employeeId: [{ required: true, message: '请选择职工', trigger: 'change' }],
  certName: [{ required: true, message: '证书名称不能为空', trigger: 'blur' }]
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialog.mode = 'create'
  dialog.visible = true
}

function openEdit(row: Certificate) {
  Object.assign(form, emptyForm(), {
    id: row.id,
    employeeId: row.employeeId,
    certName: row.certName,
    certNo: row.certNo,
    issueOrg: row.issueOrg,
    issueDate: row.issueDate,
    expireDate: row.expireDate,
    fileUrl: row.fileUrl
  })
  dialog.mode = 'edit'
  dialog.visible = true
}

function resetDialog() {
  formRef.value?.clearValidate()
  Object.assign(form, emptyForm())
}

async function onSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  // 与后端同口径：两者都填时才比较（字符串直接比即可，YYYY-MM-DD 字典序=时间序）
  if (form.issueDate && form.expireDate && form.expireDate < form.issueDate) {
    ElMessage.warning('有效期不能早于发证日期')
    return
  }
  dialog.submitting = true
  try {
    if (dialog.mode === 'create') {
      await createCertificate(form)
      ElMessage.success('新增成功')
    } else {
      await updateCertificate(form)
      ElMessage.success('修改成功')
    }
    dialog.visible = false
    load()
  } catch {
    // 失败消息由 request 拦截器统一弹出
  } finally {
    dialog.submitting = false
  }
}

onMounted(() => {
  loadEmployees()
  load()
})
</script>

<style scoped>
.search-card {
  margin-bottom: 12px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.toolbar {
  margin-bottom: 12px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
