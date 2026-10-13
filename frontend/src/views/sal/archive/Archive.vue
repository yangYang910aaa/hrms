<template>
  <div class="sal-archive-page">
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
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
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
    <el-card shadow="never">
      <div class="toolbar">
        <el-button
          v-permission="'sal:archive:calc'"
          type="primary"
          :icon="MagicStick"
          @click="openCalc"
        >
          批量计算 / 重算
        </el-button>
        <span class="tip">
          实发 = 基本 + 加班 + 计件 + 计时 + 奖罚 − 社保 − 水电 − 其他扣款 − 个税，全部由服务端算；
          档案是快照，之后改标准/比例都不影响已生成的档案；重算只覆盖草稿，已确认/已发放不可改
        </span>
      </div>

      <el-table :data="records" v-loading="loading" border>
        <el-table-column label="职工" width="140" fixed="left" show-overflow-tooltip>
          <template #default="{ row }">{{ row.empNo }} {{ row.employeeName }}</template>
        </el-table-column>
        <el-table-column label="月份" prop="month" width="90" fixed="left" />
        <el-table-column label="级别" prop="levelName" width="120" show-overflow-tooltip />
        <el-table-column label="基本工资" width="105" align="right">
          <template #default="{ row }">{{ money(row.baseSalary) }}</template>
        </el-table-column>
        <el-table-column label="加班" width="95" align="right">
          <template #default="{ row }">{{ money(row.overtimeAmount) }}</template>
        </el-table-column>
        <el-table-column label="计件" width="95" align="right">
          <template #default="{ row }">{{ money(row.pieceAmount) }}</template>
        </el-table-column>
        <el-table-column label="计时" width="95" align="right">
          <template #default="{ row }">{{ money(row.timeAmount) }}</template>
        </el-table-column>
        <el-table-column label="奖罚" width="95" align="right">
          <template #default="{ row }">{{ money(row.rewardAmount) }}</template>
        </el-table-column>
        <el-table-column label="社保" width="95" align="right">
          <template #default="{ row }">{{ money(row.socialSecurity) }}</template>
        </el-table-column>
        <el-table-column label="水电" width="95" align="right">
          <template #default="{ row }">{{ money(row.utilityFee) }}</template>
        </el-table-column>
        <el-table-column label="其他扣款" width="100" align="right">
          <template #default="{ row }">{{ money(row.deductAmount) }}</template>
        </el-table-column>
        <el-table-column label="个税" width="95" align="right">
          <template #default="{ row }">{{ money(row.tax) }}</template>
        </el-table-column>
        <el-table-column label="实发" width="115" align="right">
          <template #default="{ row }">
            <strong>{{ money(row.netSalary) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              v-permission="'sal:archive:confirm'"
              link
              type="success"
              @click="onConfirm(row)"
            >
              确认
            </el-button>
            <el-button
              v-if="row.status === 0"
              v-permission="'sal:archive:edit'"
              link
              type="primary"
              @click="openEdit(row)"
            >
              改扣款
            </el-button>
            <el-button
              v-if="row.status === 0"
              v-permission="'sal:archive:remove'"
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

    <!-- 批量计算弹窗 -->
    <el-dialog
      v-model="calcDialog.visible"
      title="批量计算工资档案"
      width="860px"
      top="5vh"
      @closed="resetCalc"
    >
      <el-form :inline="true" :model="calcForm" label-width="86px">
        <el-form-item label="所属月份" required>
          <el-date-picker
            v-model="calcForm.month"
            type="month"
            value-format="YYYY-MM"
            placeholder="YYYY-MM"
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item label="计算范围">
          <el-select
            v-model="calcForm.employeeIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            clearable
            filterable
            placeholder="留空 = 全部未删除职工（含离职）"
            style="width: 420px"
          >
            <el-option v-for="e in employees" :key="e.id" :label="employeeLabel(e)" :value="e.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <div class="field-tip">
        不选职工即计算全部未删除职工（含离职，便于结清当月工资）；没设薪酬级别的职工会被跳过并在下方提示。
        重复计算只覆盖草稿，且不会冲掉已手改的「其他扣款 / 个税」。
      </div>

      <!-- 结果区（算完就地展示，不用另开弹窗） -->
      <div v-if="calcResult" class="calc-result">
        <el-divider content-position="left">计算结果 · {{ calcResult.month }}</el-divider>
        <div class="summary">
          <el-tag type="info">共 {{ calcResult.total }} 人</el-tag>
          <el-tag type="success">新建 {{ calcResult.created }}</el-tag>
          <el-tag type="warning">重算 {{ calcResult.updated }}</el-tag>
          <el-tag type="danger">跳过 {{ calcResult.skipped }}</el-tag>
        </div>
        <ul v-if="calcResult.notices.length" class="notices">
          <li v-for="(n, i) in calcResult.notices" :key="i">{{ n }}</li>
        </ul>
        <el-table :data="calcResult.items" border size="small" max-height="300" style="margin-top: 8px">
          <el-table-column label="职工" width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.empNo }} {{ row.employeeName }}</template>
          </el-table-column>
          <el-table-column label="级别" prop="levelName" width="120" show-overflow-tooltip />
          <el-table-column label="基本工资" width="105" align="right">
            <template #default="{ row }">{{ money(row.baseSalary) }}</template>
          </el-table-column>
          <el-table-column label="实发" width="110" align="right">
            <template #default="{ row }">{{ money(row.netSalary) }}</template>
          </el-table-column>
          <el-table-column label="动作" width="100">
            <template #default="{ row }">{{ actionLabel(row.action) }}</template>
          </el-table-column>
          <el-table-column label="提示" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">{{ (row.notices || []).join('；') }}</template>
          </el-table-column>
          <template #empty>没有可计算的职工</template>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="calcDialog.visible = false">关 闭</el-button>
        <el-button type="primary" :loading="calcDialog.submitting" @click="onCalc">
          {{ calcResult ? '再算一次' : '开始计算' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 手改扣款弹窗（只开放 其他扣款 / 个税） -->
    <el-dialog
      v-model="editDialog.visible"
      title="手改扣款项"
      width="520px"
      top="8vh"
      @closed="resetEdit"
    >
      <el-form ref="editFormRef" v-loading="editDialog.submitting" :model="editForm" label-width="96px">
        <el-form-item label="职工">
          <span>{{ editDialog.target ? `${editDialog.target.empNo} ${editDialog.target.employeeName}` : '—' }}</span>
        </el-form-item>
        <el-form-item label="其他扣款">
          <el-input-number
            v-model="editForm.deductAmount"
            :min="0"
            :max="9999999999.99"
            :precision="2"
            :value-on-clear="null"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="个税">
          <el-input-number
            v-model="editForm.tax"
            :min="0"
            :max="9999999999.99"
            :precision="2"
            :value-on-clear="null"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
        <div class="field-tip">
          只有这两项可以手改（其余各项都是服务端算出来的，契约上没开入口）；实发会按九项口径重算。
        </div>
      </el-form>

      <template #footer>
        <el-button @click="editDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="editDialog.submitting" @click="onEditSubmit">保 存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { MagicStick, Refresh, Search } from '@element-plus/icons-vue'
import { archivePage, batchCalcArchive, confirmArchive, deleteArchive, updateArchive } from '@/api/sal'
import { employeeLabel, employees, loadEmployees } from '@/utils/lookup'
import { money } from '@/utils/format'
import type {
  PageResult,
  SalArchiveBatchCalcDTO,
  SalArchiveCalcResultVO,
  SalArchivePageParams,
  SalArchiveUpdateDTO,
  SalArchiveVO
} from '@/types/api'

// ---------- 枚举 ----------
const STATUS_OPTIONS = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已确认' },
  { value: 2, label: '已发放' }
]

function statusLabel(status: number): string {
  return STATUS_OPTIONS.find((o) => o.value === status)?.label ?? String(status)
}

function statusTag(status: number): 'success' | 'warning' | 'info' {
  if (status === 2) return 'success'
  if (status === 1) return 'warning'
  return 'info'
}

/** 后端逐人动作：新建 / 重算 / 跳过 */
function actionLabel(action: string | null): string {
  if (action === 'CREATED') return '新建'
  if (action === 'UPDATED') return '重算'
  if (action === 'SKIPPED') return '跳过'
  return action || '—'
}

// ---------- 列表 ----------
const loading = ref(false)
const records = ref<SalArchiveVO[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })
const query = reactive<{ employeeId: number | null; month: string | null; status: number | null }>({
  employeeId: null,
  month: null,
  status: null
})

async function load() {
  loading.value = true
  try {
    const params: SalArchivePageParams = {
      current: page.current,
      size: page.size,
      employeeId: query.employeeId,
      month: query.month,
      status: query.status
    }
    const res = await archivePage(params)
    const data = (res.data || {}) as PageResult<SalArchiveVO>
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
  query.status = null
  onSearch()
}

// ---------- 批量计算 ----------
const calcDialog = reactive({ visible: false, submitting: false })
const calcForm = reactive<{ month: string | null; employeeIds: number[] }>({ month: null, employeeIds: [] })
const calcResult = ref<SalArchiveCalcResultVO | null>(null)

function openCalc() {
  calcForm.month = query.month
  calcForm.employeeIds = []
  calcResult.value = null
  calcDialog.visible = true
}

function resetCalc() {
  calcForm.month = null
  calcForm.employeeIds = []
  calcResult.value = null
}

async function onCalc() {
  if (!calcForm.month) {
    ElMessage.warning('请选择所属月份')
    return
  }
  calcDialog.submitting = true
  try {
    const payload: SalArchiveBatchCalcDTO = {
      month: calcForm.month,
      // 空数组 = 不传 = 全部未删除职工（后端语义）
      employeeIds: calcForm.employeeIds.length ? calcForm.employeeIds : null
    }
    const res = await batchCalcArchive(payload)
    calcResult.value = (res.data || null) as SalArchiveCalcResultVO | null
    ElMessage.success('计算完成')
    load()
  } catch {
    // 失败消息由 request 拦截器统一弹出
  } finally {
    calcDialog.submitting = false
  }
}

// ---------- 确认 ----------
async function onConfirm(row: SalArchiveVO) {
  try {
    await ElMessageBox.confirm(
      `确认职工【${row.employeeName}】${row.month} 的工资档案？确认后不可再改、不可重算，下一步才能生成发放单。`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await confirmArchive(row.id)
  ElMessage.success('已确认')
  load()
}

// ---------- 删除 ----------
async function onDelete(row: SalArchiveVO) {
  try {
    await ElMessageBox.confirm(
      `确认删除职工【${row.employeeName}】${row.month} 的工资档案（实发 ${money(row.netSalary)}）？`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await deleteArchive(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && page.current > 1) {
    page.current -= 1
  }
  load()
}

// ---------- 手改其他扣款 / 个税 ----------
const editDialog = reactive({
  visible: false,
  submitting: false,
  target: null as SalArchiveVO | null
})
const editFormRef = ref<FormInstance>()
const editForm = reactive<SalArchiveUpdateDTO>({ id: null, deductAmount: null, tax: null, remark: null })

function openEdit(row: SalArchiveVO) {
  editDialog.target = row
  editForm.id = row.id
  editForm.deductAmount = row.deductAmount
  editForm.tax = row.tax
  editForm.remark = null
  editDialog.visible = true
}

function resetEdit() {
  editDialog.target = null
  editForm.id = null
  editForm.deductAmount = null
  editForm.tax = null
  editForm.remark = null
}

async function onEditSubmit() {
  editDialog.submitting = true
  try {
    await updateArchive(editForm)
    ElMessage.success('已保存，实发已重算')
    editDialog.visible = false
    load()
  } catch {
    // 失败消息由 request 拦截器统一弹出
  } finally {
    editDialog.submitting = false
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
  line-height: 1.5;
  margin-bottom: 4px;
}

.calc-result {
  margin-top: 4px;
}

.summary {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.notices {
  margin: 0 0 8px;
  padding-left: 18px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
