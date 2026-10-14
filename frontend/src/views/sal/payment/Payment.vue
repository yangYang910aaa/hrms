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
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="未发放" :value="0" />
            <el-option label="已发放" :value="1" />
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
        <el-button
          v-permission="'sal:payment:add'"
          type="primary"
          :icon="Plus"
          @click="openGenerate"
        >
          生成发放单
        </el-button>
        <span class="tip">
          只能按「已确认」的工资档案生成；发放日期与状态由服务端写（发放时同时把档案置为已发放，不可逆）
        </span>
      </div>

      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column label="职工" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.empNo }} {{ row.employeeName }}</template>
        </el-table-column>
        <el-table-column label="月份" prop="month" width="100" />
        <el-table-column label="应发金额" width="110" align="right">
          <template #default="{ row }">
            <strong>{{ money(row.netSalary) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="发放方式" prop="payWay" width="110" />
        <el-table-column label="银行账号" prop="bankAccount" min-width="150" show-overflow-tooltip />
        <el-table-column label="发放日期" prop="payDate" width="112" />
        <el-table-column label="状态" width="88">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已发放' : '未发放' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="100" show-overflow-tooltip />
        <el-table-column label="操作" width="104" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <span v-if="row.status === 0" v-permission="'sal:payment:pay'">
                <el-tooltip content="发放" placement="top" :show-after="150">
                  <el-button link type="success" :icon="Money" aria-label="发放" @click="onPay(row)" />
                </el-tooltip>
              </span>
              <span v-if="row.status === 0" v-permission="'sal:payment:edit'">
                <el-tooltip content="修改" placement="top" :show-after="150">
                  <el-button link type="primary" :icon="Edit" aria-label="修改" @click="openEdit(row)" />
                </el-tooltip>
              </span>
              <span v-if="row.status === 0" v-permission="'sal:payment:remove'">
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

    <!-- 生成发放单弹窗 -->
    <el-dialog
      v-model="genDialog.visible"
      title="生成工资发放单"
      width="720px"
      top="6vh"
      @closed="resetGen"
    >
      <el-form :inline="true" :model="genForm" label-width="86px">
        <el-form-item label="所属月份" required>
          <el-date-picker
            v-model="genForm.month"
            type="month"
            value-format="YYYY-MM"
            placeholder="YYYY-MM"
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item label="生成范围">
          <el-select
            v-model="genForm.employeeIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            clearable
            filterable
            placeholder="留空 = 该月全部已确认档案"
            style="width: 380px"
          >
            <el-option v-for="e in employees" :key="e.id" :label="employeeLabel(e)" :value="e.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <div class="field-tip">
        只认「已确认」的工资档案；某职工该月已有发放单会自动跳过。若该月还有草稿档案，下方会提示先去确认。
      </div>

      <div v-if="genResult" class="gen-result">
        <el-divider content-position="left">生成结果 · {{ genResult.month }}</el-divider>
        <div class="summary">
          <el-tag type="info">涉及 {{ genResult.total }} 人</el-tag>
          <el-tag type="success">新建 {{ genResult.created }}</el-tag>
          <el-tag type="warning">已存在跳过 {{ genResult.skippedExists }}</el-tag>
          <el-tag type="danger">草稿档案 {{ genResult.draftCount }}</el-tag>
          <el-tag type="success">已发放档案 {{ genResult.paidCount }}</el-tag>
        </div>
        <ul v-if="genResult.notices.length" class="notices">
          <li v-for="(n, i) in genResult.notices" :key="i">{{ n }}</li>
        </ul>
      </div>

      <template #footer>
        <el-button @click="genDialog.visible = false">关 闭</el-button>
        <el-button type="primary" :loading="genDialog.submitting" @click="onGenerate">
          {{ genResult ? '再生成一次' : '生成' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 修改发放单弹窗（不含 发放日期 / 状态） -->
    <el-dialog
      v-model="editDialog.visible"
      title="修改发放单"
      width="520px"
      top="8vh"
      @closed="resetEdit"
    >
      <el-form ref="editFormRef" v-loading="editDialog.submitting" :model="editForm" label-width="96px">
        <el-form-item label="职工">
          <span>{{ editDialog.target ? `${editDialog.target.empNo} ${editDialog.target.employeeName}` : '—' }}</span>
        </el-form-item>
        <el-form-item label="应发金额">
          <span>{{ editDialog.target ? money(editDialog.target.netSalary) : '—' }}</span>
        </el-form-item>
        <el-form-item label="发放方式">
          <el-input v-model="editForm.payWay" maxlength="20" placeholder="如：银行代发 / 现金" />
        </el-form-item>
        <el-form-item label="银行账号">
          <el-input v-model="editForm.bankAccount" maxlength="30" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
        <div class="field-tip">应发金额来自工资档案快照，这里不可改；发放日期与状态只能由「发放」动作写入。</div>
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
import { Delete, Edit, Money, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { deletePayment, generatePayment, payPayment, paymentPage, updatePayment } from '@/api/sal'
import { employeeLabel, employees, loadEmployees } from '@/utils/lookup'
import { money } from '@/utils/format'
import type {
  PageResult,
  SalPaymentGenDTO,
  SalPaymentGenResultVO,
  SalPaymentPageParams,
  SalPaymentUpdateDTO,
  SalPaymentVO
} from '@/types/api'

// ---------- 列表 ----------
const loading = ref(false)
const records = ref<SalPaymentVO[]>([])
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
    const params: SalPaymentPageParams = {
      current: page.current,
      size: page.size,
      employeeId: query.employeeId,
      month: query.month,
      status: query.status
    }
    const res = await paymentPage(params)
    const data = (res.data || {}) as PageResult<SalPaymentVO>
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

// ---------- 生成发放单 ----------
const genDialog = reactive({ visible: false, submitting: false })
const genForm = reactive<{ month: string | null; employeeIds: number[] }>({ month: null, employeeIds: [] })
const genResult = ref<SalPaymentGenResultVO | null>(null)

function openGenerate() {
  genForm.month = query.month
  genForm.employeeIds = []
  genResult.value = null
  genDialog.visible = true
}

function resetGen() {
  genForm.month = null
  genForm.employeeIds = []
  genResult.value = null
}

async function onGenerate() {
  if (!genForm.month) {
    ElMessage.warning('请选择所属月份')
    return
  }
  genDialog.submitting = true
  try {
    const payload: SalPaymentGenDTO = {
      month: genForm.month,
      // 空数组 = 不传 = 该月全部已确认档案（后端语义）
      employeeIds: genForm.employeeIds.length ? genForm.employeeIds : null
    }
    const res = await generatePayment(payload)
    genResult.value = (res.data || null) as SalPaymentGenResultVO | null
    ElMessage.success('生成完成')
    load()
  } catch {
    // 失败消息由 request 拦截器统一弹出
  } finally {
    genDialog.submitting = false
  }
}

// ---------- 发放 ----------
async function onPay(row: SalPaymentVO) {
  try {
    await ElMessageBox.confirm(
      `确认给【${row.employeeName}】发放 ${row.month} 的工资 ${money(row.netSalary)}？发放后不可撤销，对应工资档案也会置为已发放。`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await payPayment(row.id)
  ElMessage.success('已发放')
  load()
}

// ---------- 删除 ----------
async function onDelete(row: SalPaymentVO) {
  try {
    await ElMessageBox.confirm(`确认删除【${row.employeeName}】${row.month} 的发放单？`, '提示', {
      type: 'warning'
    })
  } catch {
    return
  }
  await deletePayment(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && page.current > 1) {
    page.current -= 1
  }
  load()
}

// ---------- 修改发放单 ----------
const editDialog = reactive({
  visible: false,
  submitting: false,
  target: null as SalPaymentVO | null
})
const editFormRef = ref<FormInstance>()
const editForm = reactive<SalPaymentUpdateDTO>({ id: null, payWay: null, bankAccount: null, remark: null })

function openEdit(row: SalPaymentVO) {
  editDialog.target = row
  editForm.id = row.id
  editForm.payWay = row.payWay
  editForm.bankAccount = row.bankAccount
  editForm.remark = row.remark
  editDialog.visible = true
}

function resetEdit() {
  editDialog.target = null
  editForm.id = null
  editForm.payWay = null
  editForm.bankAccount = null
  editForm.remark = null
}

async function onEditSubmit() {
  editDialog.submitting = true
  try {
    await updatePayment(editForm)
    ElMessage.success('修改成功')
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
/* 骨架类（.page-container/.search-card/.search-form/.toolbar/.tip/.field-tip/.pagination/.op-btns/...）
   已在 styles/index.css 全局定义，这里不再各写一份副本（阶段 3 收敛）。只留本页特有的。 */
.field-tip {
  margin-bottom: 4px;
}

.gen-result {
  margin-top: var(--hr-space-1);
}

.summary {
  display: flex;
  flex-wrap: wrap;
  gap: var(--hr-space-2);
  margin-bottom: var(--hr-space-2);
}

.notices {
  margin: 0 0 var(--hr-space-2);
  padding-left: 18px;
  font-size: var(--hr-font-xs);
  color: var(--hr-text-placeholder);
  line-height: 1.6;
}
</style>
