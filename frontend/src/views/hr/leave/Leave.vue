<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" class="search-form" @submit.prevent>
        <el-form-item label="职工">
          <el-select
            v-model="query.employeeId"
            placeholder="全部"
            clearable
            filterable
            style="width: 200px"
          >
            <el-option v-for="e in employees" :key="e.id" :label="employeeLabel(e)" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.leaveType" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in leaveTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
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
        <el-button v-permission="'hr:leave:add'" type="primary" :icon="Plus" @click="openCreate">
          提交请假单
        </el-button>
        <span class="tip">
          提交即「待审批」；审批只能走专用按钮（后端通用更新接口刻意不写审批字段，防止「自己批自己」）
        </span>
      </div>

      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column label="职工" min-width="85">
          <template #default="{ row }">{{ empName(row.employeeId) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="76">
          <template #default="{ row }">{{ label('leave_type', row.leaveType) }}</template>
        </el-table-column>
        <el-table-column label="开始时间" width="145">
          <template #default="{ row }">{{ dt(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="145">
          <template #default="{ row }">{{ dt(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="天数" width="70" align="right">
          <template #default="{ row }">{{ money(row.days) }}</template>
        </el-table-column>
        <el-table-column label="事由" prop="reason" min-width="95" show-overflow-tooltip />
        <el-table-column label="状态" width="88">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批人" prop="approver" width="80" />
        <el-table-column label="审批时间" width="145">
          <template #default="{ row }">{{ dt(row.approveTime) }}</template>
        </el-table-column>
        <el-table-column label="审批意见" prop="approveRemark" min-width="90" show-overflow-tooltip />
        <el-table-column label="操作" width="104" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <span v-if="row.status === 0" v-permission="'hr:leave:edit'">
                <el-tooltip content="审批" placement="top" :show-after="150">
                  <el-button link type="success" :icon="CircleCheck" aria-label="审批" @click="openApprove(row)" />
                </el-tooltip>
              </span>
              <span v-if="row.status === 0" v-permission="'hr:leave:edit'">
                <el-tooltip content="修改" placement="top" :show-after="150">
                  <el-button link type="primary" :icon="Edit" aria-label="修改" @click="openEdit(row)" />
                </el-tooltip>
              </span>
              <span v-permission="'hr:leave:remove'">
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

    <!-- 提交 / 修改弹窗（**不含审批字段**） -->
    <el-dialog
      v-model="dialog.visible"
      :title="dialog.mode === 'create' ? '提交请假单' : '修改请假单'"
      width="620px"
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
            <el-form-item label="请假类型" prop="leaveType">
              <el-select v-model="form.leaveType" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in leaveTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="必须晚于开始时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="请假天数">
              <el-input-number
                v-model="form.days"
                :min="0"
                :precision="1"
                :value-on-clear="null"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="事由">
              <el-input v-model="form.reason" type="textarea" :rows="2" maxlength="255" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="onSubmit">保 存</el-button>
      </template>
    </el-dialog>

    <!-- 审批弹窗（专用接口） -->
    <el-dialog v-model="approveDialog.visible" title="请假审批" width="480px" @closed="resetApprove">
      <el-form ref="approveFormRef" v-loading="approveDialog.submitting" :model="approveForm" :rules="approveRules" label-width="96px">
        <el-form-item label="申请人">
          <span>{{ approveDialog.target ? `${empName(approveDialog.target.employeeId)}` : '—' }}</span>
        </el-form-item>
        <el-form-item label="审批结论" prop="status">
          <el-select v-model="approveForm.status" style="width: 100%">
            <el-option label="通过" :value="1" />
            <el-option label="驳回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approveForm.approveRemark" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
        <div class="field-tip">审批人与审批时间由服务端按当前登录用户自动写入，无需填写。</div>
      </el-form>

      <template #footer>
        <el-button @click="approveDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="approveDialog.submitting" @click="onApprove">提 交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { CircleCheck, Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { approveLeave, createLeave, deleteLeave, leavePage, updateLeave } from '@/api/hr'
import { empName, employeeLabel, employees, loadEmployees } from '@/utils/lookup'
import { useDict } from '@/utils/dict'
import { money } from '@/utils/format'
import type { Leave, LeaveApproveDTO, LeaveDTO, LeavePageParams, PageResult } from '@/types/api'

// ---------- 字典与枚举 ----------
// 字典值是字符串、DTO 里是整数 → Number() 转换
const { dictMap, load: loadDict, label } = useDict(['leave_type'])
const leaveTypeOptions = computed(() =>
  (dictMap.value.leave_type || []).map((o) => ({ label: o.label, value: Number(o.value) }))
)

const STATUS_OPTIONS = [
  { value: 0, label: '待审批' },
  { value: 1, label: '已通过' },
  { value: 2, label: '已驳回' }
]

function statusLabel(status: number): string {
  return STATUS_OPTIONS.find((o) => o.value === status)?.label ?? String(status)
}

function statusTag(status: number): 'success' | 'warning' | 'danger' {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'warning'
}

/** 后端返回的是 ISO 时间（2026-10-13T09:00:00）→ 显示成「2026-10-13 09:00」 */
function dt(value: string | null): string {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

// ---------- 列表 ----------
const loading = ref(false)
const records = ref<Leave[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })
const query = reactive<{ employeeId: number | null; status: number | null; leaveType: number | null }>({
  employeeId: null,
  status: null,
  leaveType: null
})

async function load() {
  loading.value = true
  try {
    const params: LeavePageParams = {
      current: page.current,
      size: page.size,
      employeeId: query.employeeId,
      status: query.status,
      leaveType: query.leaveType
    }
    const res = await leavePage(params)
    const data = (res.data || {}) as PageResult<Leave>
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
  query.status = null
  query.leaveType = null
  onSearch()
}

// ---------- 删除 ----------
async function onDelete(row: Leave) {
  const extra = row.status === 0 ? '' : `（该单已是「${statusLabel(row.status)}」）`
  try {
    await ElMessageBox.confirm(
      `确认删除职工【${empName(row.employeeId)}】的请假单${extra}？`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await deleteLeave(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && page.current > 1) {
    page.current -= 1
  }
  load()
}

// ---------- 提交 / 修改 ----------
const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', submitting: false })
const formRef = ref<FormInstance>()

const emptyForm = (): LeaveDTO => ({
  id: null,
  employeeId: null,
  leaveType: null,
  startTime: null,
  endTime: null,
  days: null,
  reason: null
})

const form = reactive<LeaveDTO>(emptyForm())

const rules: FormRules = {
  employeeId: [{ required: true, message: '请选择职工', trigger: 'change' }],
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialog.mode = 'create'
  dialog.visible = true
}

function openEdit(row: Leave) {
  Object.assign(form, emptyForm(), {
    id: row.id,
    employeeId: row.employeeId,
    leaveType: row.leaveType,
    startTime: row.startTime,
    endTime: row.endTime,
    days: row.days,
    reason: row.reason
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
  // 与后端 assertTimeRange 同口径。ISO 串字典序 = 时间序，直接比即可
  if (form.startTime && form.endTime && form.endTime <= form.startTime) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  dialog.submitting = true
  try {
    if (dialog.mode === 'create') {
      await createLeave(form)
      ElMessage.success('提交成功')
    } else {
      await updateLeave(form)
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

// ---------- 审批（专用接口） ----------
const approveDialog = reactive({
  visible: false,
  submitting: false,
  target: null as Leave | null
})
const approveFormRef = ref<FormInstance>()
const approveForm = reactive<LeaveApproveDTO>({ status: 1, approveRemark: null })

const approveRules: FormRules = {
  status: [{ required: true, message: '请选择审批结论', trigger: 'change' }]
}

function openApprove(row: Leave) {
  approveDialog.target = row
  approveForm.status = 1
  approveForm.approveRemark = null
  approveDialog.visible = true
}

function resetApprove() {
  approveFormRef.value?.clearValidate()
  approveDialog.target = null
  approveForm.status = 1
  approveForm.approveRemark = null
}

async function onApprove() {
  if (!approveDialog.target) return
  if (!approveFormRef.value) return
  try {
    await approveFormRef.value.validate()
  } catch {
    return
  }
  approveDialog.submitting = true
  try {
    await approveLeave(approveDialog.target.id, approveForm)
    ElMessage.success('审批完成')
    approveDialog.visible = false
    load()
  } catch {
    // 失败消息由 request 拦截器统一弹出
  } finally {
    approveDialog.submitting = false
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
