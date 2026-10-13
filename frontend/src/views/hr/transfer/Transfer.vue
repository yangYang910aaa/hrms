<template>
  <div class="hr-transfer-page">
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
            <el-option v-for="e in employees" :key="e.id" :label="`${e.empNo} ${e.name}`" :value="e.id" />
          </el-select>
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
        <el-button v-permission="'hr:transfer:add'" type="primary" :icon="Plus" @click="openCreate">
          新增调动
        </el-button>
        <span class="tip">
          原部门/原岗位留空时由系统按职工主档快照带出；本表只登记，不会自动改写职工档案
        </span>
      </div>

      <el-table :data="records" v-loading="loading" border>
        <el-table-column label="职工" min-width="110">
          <template #default="{ row }">{{ empName(row.employeeId) }}</template>
        </el-table-column>
        <el-table-column label="原部门" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ deptName(row.fromDeptId) }}</template>
        </el-table-column>
        <el-table-column label="原岗位" prop="fromPosition" min-width="110" show-overflow-tooltip />
        <el-table-column label="新部门" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ deptName(row.toDeptId) }}</template>
        </el-table-column>
        <el-table-column label="新岗位" prop="toPosition" min-width="110" show-overflow-tooltip />
        <el-table-column label="调动日期" prop="transferDate" width="115" />
        <el-table-column label="调动原因" prop="reason" min-width="150" show-overflow-tooltip />
        <el-table-column label="审批人" prop="approver" width="100" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'hr:transfer:edit'" link type="primary" @click="openEdit(row)">
              修改
            </el-button>
            <el-button v-permission="'hr:transfer:remove'" link type="danger" @click="onDelete(row)">
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
      :title="dialog.mode === 'create' ? '新增调动' : '修改调动'"
      width="660px"
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
                  :label="`${e.empNo} ${e.name}`"
                  :value="e.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="调动日期">
              <el-date-picker
                v-model="form.transferDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原部门">
              <el-select v-model="form.fromDeptId" placeholder="留空取档案" clearable style="width: 100%">
                <el-option v-for="d in deptOptions" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原岗位">
              <el-input v-model="form.fromPosition" maxlength="50" placeholder="留空取档案" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="新部门">
              <el-select v-model="form.toDeptId" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="d in deptOptions" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="新岗位">
              <el-input v-model="form.toPosition" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="审批人">
              <el-input v-model="form.approver" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="调动原因">
              <el-input v-model="form.reason" type="textarea" :rows="2" maxlength="255" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <div class="field-tip">
              「原部门 + 原岗位」与「新部门 + 新岗位」不能完全相同，否则后端会拒绝。
              编辑时页面会把已保存的原部门/原岗位原样回传，以保住历史快照。
            </div>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createTransfer, deleteTransfer, transferPage, updateTransfer } from '@/api/hr'
import { deptName, depts, empName, employees, loadDepts, loadEmployees } from '@/utils/lookup'
import { flattenTree } from '@/utils/tree'
import type { PageResult, Transfer, TransferDTO, TransferPageParams } from '@/types/api'

// ---------- 下拉数据 ----------
/** 部门树拍平成下拉（缩进用全角空格，复用系统设置页的工具） */
const deptOptions = computed(() => flattenTree(depts.value, (n) => n.deptName))

// ---------- 列表 ----------
const loading = ref(false)
const records = ref<Transfer[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })
const query = reactive<{ employeeId: number | null }>({ employeeId: null })

async function load() {
  loading.value = true
  try {
    const params: TransferPageParams = {
      current: page.current,
      size: page.size,
      employeeId: query.employeeId
    }
    const res = await transferPage(params)
    const data = (res.data || {}) as PageResult<Transfer>
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
  onSearch()
}

// ---------- 删除 ----------
async function onDelete(row: Transfer) {
  try {
    await ElMessageBox.confirm(`确认删除职工【${empName(row.employeeId)}】的这条调动记录？`, '提示', {
      type: 'warning'
    })
  } catch {
    return
  }
  await deleteTransfer(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && page.current > 1) {
    page.current -= 1
  }
  load()
}

// ---------- 新增 / 修改 ----------
const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', submitting: false })
const formRef = ref<FormInstance>()

const emptyForm = (): TransferDTO => ({
  id: null,
  employeeId: null,
  fromDeptId: null,
  toDeptId: null,
  fromPosition: null,
  toPosition: null,
  transferDate: null,
  reason: null,
  approver: null
})

const form = reactive<TransferDTO>(emptyForm())

const rules: FormRules = {
  employeeId: [{ required: true, message: '请选择职工', trigger: 'change' }]
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialog.mode = 'create'
  dialog.visible = true
}

function openEdit(row: Transfer) {
  // from* 是历史快照，必须原样回传（后端回退链：入参 → 本条已有值 → 主档当前值）
  Object.assign(form, emptyForm(), {
    id: row.id,
    employeeId: row.employeeId,
    fromDeptId: row.fromDeptId,
    toDeptId: row.toDeptId,
    fromPosition: row.fromPosition,
    toPosition: row.toPosition,
    transferDate: row.transferDate,
    reason: row.reason,
    approver: row.approver
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
  // 与后端 assertNotSame 同口径：前后部门与岗位完全相同则拒绝（前端先提示，省一次往返）
  if (
    form.fromDeptId === form.toDeptId &&
    (form.fromPosition || '') === (form.toPosition || '')
  ) {
    ElMessage.warning('调动前后的部门与岗位完全相同，请检查')
    return
  }
  dialog.submitting = true
  try {
    if (dialog.mode === 'create') {
      await createTransfer(form)
      ElMessage.success('新增成功')
    } else {
      await updateTransfer(form)
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
  loadDepts()
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
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.tip,
.field-tip {
  font-size: 12px;
  color: #909399;
}

.field-tip {
  line-height: 1.4;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
