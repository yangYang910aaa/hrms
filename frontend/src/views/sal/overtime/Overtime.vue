<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" class="search-form" @submit.prevent>
        <el-form-item label="职工">
          <el-select v-model="query.employeeId" placeholder="全部" clearable filterable style="width: 200px">
            <el-option v-for="e in employees" :key="e.id" :label="employeeLabel(e)" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属月份">
          <el-date-picker
            v-model="query.month"
            type="month"
            value-format="YYYY-MM"
            placeholder="全部"
            clearable
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item label="加班类型">
          <el-select v-model="query.overtimeType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" />
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
        <el-button v-permission="'sal:overtime:add'" type="primary" :icon="Plus" @click="openCreate">
          新增加班登记
        </el-button>
        <span class="tip">
          金额 = 时薪 × 倍率 × 小时，其中时薪 = 基本工资 ÷ 21.75 ÷ 8（服务端算，前端只填小时）；
          倍率取「该职工薪酬级别 + 该加班类型」在加班标准里的配置，同月可登记多条
        </span>
      </div>

      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column label="职工" min-width="120">
          <template #default="{ row }">{{ empName(row.employeeId) }}</template>
        </el-table-column>
        <el-table-column label="所属月份" prop="month" width="100" />
        <el-table-column label="加班类型" width="130">
          <template #default="{ row }">{{ label('overtime_type', row.overtimeType) }}</template>
        </el-table-column>
        <el-table-column label="加班小时" width="110" align="right">
          <template #default="{ row }">{{ money(row.hours) }}</template>
        </el-table-column>
        <el-table-column label="倍率（快照）" width="120" align="right">
          <template #default="{ row }">{{ money(row.rate) }}</template>
        </el-table-column>
        <el-table-column label="加班金额" width="130" align="right">
          <template #default="{ row }">
            <strong>{{ money(row.amount) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="76" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <span v-permission="'sal:overtime:edit'">
                <el-tooltip content="修改" placement="top" :show-after="150">
                  <el-button link type="primary" :icon="Edit" aria-label="修改" @click="openEdit(row)" />
                </el-tooltip>
              </span>
              <span v-permission="'sal:overtime:remove'">
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
      :title="dialog.mode === 'create' ? '新增加班登记' : '修改加班登记'"
      width="600px"
      top="6vh"
      @closed="resetDialog"
    >
      <el-form ref="formRef" v-loading="dialog.submitting" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="职工" prop="employeeId">
              <el-select v-model="form.employeeId" placeholder="请选择" filterable style="width: 100%">
                <el-option v-for="e in employees" :key="e.id" :label="employeeLabel(e)" :value="e.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属月份" prop="month">
              <el-date-picker
                v-model="form.month"
                type="month"
                value-format="YYYY-MM"
                placeholder="YYYY-MM"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="加班类型" prop="overtimeType">
              <el-select v-model="form.overtimeType" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="加班小时">
              <el-input-number
                v-model="form.hours"
                :min="0"
                :max="99999.9"
                :step="0.5"
                :precision="1"
                :value-on-clear="null"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <div class="field-tip">
              倍率与金额由服务端计算并落库（倍率同时是"当时倍率"的快照，之后改标准不影响已登记金额）。
              若该职工没有薪酬级别、或该级别 + 该类型没配加班标准，保存会被拒绝并提示先去配置 ——
              这是有意的，不会静默按 0 计。
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
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createOvertime, deleteOvertime, overtimePage, updateOvertime } from '@/api/sal'
import { empName, employeeLabel, employees, loadEmployees } from '@/utils/lookup'
import { useDict } from '@/utils/dict'
import { money } from '@/utils/format'
import type {
  PageResult,
  SalOvertimeRecord,
  SalOvertimeRecordDTO,
  SalOvertimeRecordPageParams
} from '@/types/api'

// ---------- 字典 ----------
const { dictMap, load: loadDict, label } = useDict(['overtime_type'])
const typeOptions = computed(() =>
  (dictMap.value.overtime_type || []).map((o) => ({ label: o.label, value: Number(o.value) }))
)

// ---------- 列表 ----------
const loading = ref(false)
const records = ref<SalOvertimeRecord[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })
const query = reactive<{ employeeId: number | null; month: string | null; overtimeType: number | null }>({
  employeeId: null,
  month: null,
  overtimeType: null
})

async function load() {
  loading.value = true
  try {
    const params: SalOvertimeRecordPageParams = {
      current: page.current,
      size: page.size,
      employeeId: query.employeeId,
      month: query.month,
      overtimeType: query.overtimeType
    }
    const res = await overtimePage(params)
    const data = (res.data || {}) as PageResult<SalOvertimeRecord>
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
  query.month = null
  query.overtimeType = null
  onSearch()
}

// ---------- 删除 ----------
async function onDelete(row: SalOvertimeRecord) {
  try {
    await ElMessageBox.confirm(
      `确认删除职工【${empName(row.employeeId)}】${row.month} 的这条${label('overtime_type', row.overtimeType)}记录？`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await deleteOvertime(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && page.current > 1) {
    page.current -= 1
  }
  load()
}

// ---------- 新增 / 修改 ----------
const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', submitting: false })
const formRef = ref<FormInstance>()

const emptyForm = (): SalOvertimeRecordDTO => ({
  id: null,
  employeeId: null,
  month: '',
  overtimeType: null,
  hours: null
})

const form = reactive<SalOvertimeRecordDTO>(emptyForm())

const rules: FormRules = {
  employeeId: [{ required: true, message: '请选择职工', trigger: 'change' }],
  month: [{ required: true, message: '请选择所属月份', trigger: 'change' }],
  overtimeType: [{ required: true, message: '请选择加班类型', trigger: 'change' }]
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialog.mode = 'create'
  dialog.visible = true
}

function openEdit(row: SalOvertimeRecord) {
  Object.assign(form, emptyForm(), {
    id: row.id,
    employeeId: row.employeeId,
    month: row.month,
    overtimeType: row.overtimeType,
    hours: row.hours
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
  dialog.submitting = true
  try {
    if (dialog.mode === 'create') {
      await createOvertime(form)
      ElMessage.success('新增成功')
    } else {
      await updateOvertime(form)
      ElMessage.success('修改成功')
    }
    dialog.visible = false
    load()
  } catch {
    // 失败消息由 request 拦截器统一弹出（如"没有薪酬级别/未配加班标准"）
  } finally {
    dialog.submitting = false
  }
}

onMounted(() => {
  loadDict()
  loadEmployees()
  load()
})
</script>

<style scoped>
/* 骨架类（.page-container/.search-card/.search-form/.toolbar/.tip/.field-tip/.pagination/.op-btns/...）
   已在 styles/index.css 全局定义，这里不再各写一份副本（阶段 3 收敛）。 */
</style>
