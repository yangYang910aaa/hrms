<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" class="search-form" @submit.prevent>
        <el-form-item label="姓名">
          <el-input
            v-model="query.name"
            placeholder="模糊匹配"
            clearable
            style="width: 150px"
            @keyup.enter="onSearch"
          />
        </el-form-item>
        <el-form-item label="实习部门">
          <el-select v-model="query.deptId" placeholder="全部" clearable style="width: 190px">
            <el-option v-for="d in deptOptions" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button v-permission="'hr:intern:add'" type="primary" :icon="Plus" @click="openCreate">
          新增实习生
        </el-button>
        <span class="tip">实习生是独立台账（不必是职工）；只有「已转正」才回填关联职工</span>
      </div>

      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column label="姓名" prop="name" min-width="100" />
        <el-table-column label="学校" prop="school" min-width="130" show-overflow-tooltip />
        <el-table-column label="专业" prop="major" min-width="110" show-overflow-tooltip />
        <el-table-column label="实习部门" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ deptName(row.deptId) }}</template>
        </el-table-column>
        <el-table-column label="带教导师" prop="mentor" width="110" />
        <el-table-column label="开始日期" prop="startDate" width="115" />
        <el-table-column label="结束日期" prop="endDate" width="115" />
        <el-table-column label="状态" width="92">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联职工" min-width="110">
          <template #default="{ row }">
            {{ row.status === 3 ? empName(row.employeeId) : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="76" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <span v-permission="'hr:intern:edit'">
                <el-tooltip content="修改" placement="top" :show-after="150">
                  <el-button link type="primary" :icon="Edit" aria-label="修改" @click="openEdit(row)" />
                </el-tooltip>
              </span>
              <span v-permission="'hr:intern:remove'">
                <el-tooltip content="删除" placement="top" :show-after="150">
                  <el-button link type="danger" :icon="Delete" aria-label="删除" @click="onDelete(row)" />
                </el-tooltip>
              </span>
            </div>
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
      :title="dialog.mode === 'create' ? '新增实习生' : '修改实习生'"
      width="640px"
      top="6vh"
      @closed="resetDialog"
    >
      <el-form ref="formRef" v-loading="dialog.submitting" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实习部门">
              <el-select v-model="form.deptId" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="d in deptOptions" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学校">
              <el-input v-model="form.school" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="专业">
              <el-input v-model="form.major" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="带教导师">
              <el-input v-model="form.mentor" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开始日期">
              <el-date-picker
                v-model="form.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期">
              <el-date-picker
                v-model="form.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="不得早于开始日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="关联职工">
              <el-select
                v-model="form.employeeId"
                placeholder="仅「已转正」需要选择"
                clearable
                filterable
                :disabled="form.status !== 3"
                style="width: 100%"
              >
                <el-option
                  v-for="e in employees"
                  :key="e.id"
                  :label="employeeLabel(e)"
                  :value="e.id"
                />
              </el-select>
              <div class="field-tip">
                状态不是「已转正」时不可填（后端两条互斥校验：已转正 ⟺ 关联了职工档案）。
              </div>
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
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createIntern, deleteIntern, internPage, updateIntern } from '@/api/hr'
import { deptName, deptOptions, empName, employeeLabel, employees, loadDepts, loadEmployees } from '@/utils/lookup'
import type { Intern, InternDTO, InternPageParams, PageResult } from '@/types/api'
import { confirmDelete } from '@/utils/confirm'

// ---------- 状态枚举（不在 sys_dict 里，硬编码） ----------
const STATUS_OPTIONS = [
  { value: 1, label: '实习中' },
  { value: 2, label: '已结束' },
  { value: 3, label: '已转正' }
]

function statusLabel(status: number): string {
  return STATUS_OPTIONS.find((o) => o.value === status)?.label ?? String(status)
}

function statusTag(status: number): 'success' | 'warning' | 'info' {
  if (status === 3) return 'success'
  if (status === 1) return 'warning'
  return 'info'
}

// ---------- 列表 ----------
const loading = ref(false)
const records = ref<Intern[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })
const query = reactive<{ name: string; deptId: number | null; status: number | null }>({
  name: '',
  deptId: null,
  status: null
})

async function load() {
  loading.value = true
  try {
    const params: InternPageParams = {
      current: page.current,
      size: page.size,
      name: query.name.trim() || undefined,
      deptId: query.deptId,
      status: query.status
    }
    const res = await internPage(params)
    const data = (res.data || {}) as PageResult<Intern>
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
  query.name = ''
  query.deptId = null
  query.status = null
  onSearch()
}

// ---------- 删除 ----------
async function onDelete(row: Intern) {
  try {
    await confirmDelete(`确认删除实习生【${row.name}】？`)
  } catch {
    return
  }
  await deleteIntern(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && page.current > 1) {
    page.current -= 1
  }
  load()
}

// ---------- 新增 / 修改 ----------
const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', submitting: false })
const formRef = ref<FormInstance>()

const emptyForm = (): InternDTO => ({
  id: null,
  name: '',
  school: null,
  major: null,
  deptId: null,
  mentor: null,
  startDate: null,
  endDate: null,
  status: 1,
  employeeId: null
})

const form = reactive<InternDTO>(emptyForm())

const rules: FormRules = {
  name: [{ required: true, message: '姓名不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

// 状态一旦不是「已转正」，关联职工必须清掉 —— 后端两条互斥校验，留着必被拒
watch(
  () => form.status,
  (s) => {
    if (s !== 3) form.employeeId = null
  }
)

function openCreate() {
  Object.assign(form, emptyForm())
  dialog.mode = 'create'
  dialog.visible = true
}

function openEdit(row: Intern) {
  Object.assign(form, emptyForm(), {
    id: row.id,
    name: row.name,
    school: row.school,
    major: row.major,
    deptId: row.deptId,
    mentor: row.mentor,
    startDate: row.startDate,
    endDate: row.endDate,
    status: row.status,
    employeeId: row.employeeId
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
  if (form.startDate && form.endDate && form.endDate < form.startDate) {
    ElMessage.warning('结束日期不能早于开始日期')
    return
  }
  if (form.status === 3 && !form.employeeId) {
    ElMessage.warning('「已转正」必须关联一位职工档案')
    return
  }
  dialog.submitting = true
  try {
    if (dialog.mode === 'create') {
      await createIntern(form)
      ElMessage.success('新增成功')
    } else {
      await updateIntern(form)
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
/* 骨架类（.page-container/.search-card/.search-form/.toolbar/.tip/.field-tip/.pagination/.op-btns/...）
   已在 styles/index.css 全局定义，这里不再各写一份副本（阶段 3 收敛）。 */
</style>
