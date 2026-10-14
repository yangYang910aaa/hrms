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
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button v-permission="'sal:utility:add'" type="primary" :icon="Plus" @click="openCreate">
          新增水电费
        </el-button>
        <span class="tip">
          合计 = 水费 + 电费 + 住宿费（服务端算，前端不填）；同一职工同一月只能登记一条，工资计算时整笔从实发里扣
        </span>
      </div>

      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column label="职工" min-width="120">
          <template #default="{ row }">{{ empName(row.employeeId) }}</template>
        </el-table-column>
        <el-table-column label="所属月份" prop="month" width="100" />
        <el-table-column label="水费" width="110" align="right">
          <template #default="{ row }">{{ money(row.waterFee) }}</template>
        </el-table-column>
        <el-table-column label="电费" width="110" align="right">
          <template #default="{ row }">{{ money(row.electricFee) }}</template>
        </el-table-column>
        <el-table-column label="住宿费" width="110" align="right">
          <template #default="{ row }">{{ money(row.dormFee) }}</template>
        </el-table-column>
        <el-table-column label="合计" width="120" align="right">
          <template #default="{ row }">
            <strong>{{ money(row.totalFee) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="76" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <span v-permission="'sal:utility:edit'">
                <el-tooltip content="修改" placement="top" :show-after="150">
                  <el-button link type="primary" :icon="Edit" aria-label="修改" @click="openEdit(row)" />
                </el-tooltip>
              </span>
              <span v-permission="'sal:utility:remove'">
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
      :title="dialog.mode === 'create' ? '新增水电费' : '修改水电费'"
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
          <el-col :span="8">
            <el-form-item label="水费">
              <el-input-number
                v-model="form.waterFee"
                :min="0"
                :max="9999999999.99"
                :precision="2"
                :value-on-clear="null"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="电费">
              <el-input-number
                v-model="form.electricFee"
                :min="0"
                :max="9999999999.99"
                :precision="2"
                :value-on-clear="null"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="住宿费">
              <el-input-number
                v-model="form.dormFee"
                :min="0"
                :max="9999999999.99"
                :precision="2"
                :value-on-clear="null"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <div class="field-tip">
              三项都不填时按 0 计；合计由服务端按「水 + 电 + 住宿」重算（前端没有合计输入框）。
              修改时某一项留空 = 保持原值（要置 0 请显式填 0）。
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createUtility, deleteUtility, updateUtility, utilityPage } from '@/api/sal'
import { empName, employeeLabel, employees, loadEmployees } from '@/utils/lookup'
import { money } from '@/utils/format'
import type { PageResult, SalUtilityFee, SalUtilityFeeDTO, SalUtilityFeePageParams } from '@/types/api'

// ---------- 列表 ----------
const loading = ref(false)
const records = ref<SalUtilityFee[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })
const query = reactive<{ employeeId: number | null; month: string | null }>({
  employeeId: null,
  month: null
})

async function load() {
  loading.value = true
  try {
    const params: SalUtilityFeePageParams = {
      current: page.current,
      size: page.size,
      employeeId: query.employeeId,
      month: query.month
    }
    const res = await utilityPage(params)
    const data = (res.data || {}) as PageResult<SalUtilityFee>
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
  onSearch()
}

// ---------- 删除 ----------
async function onDelete(row: SalUtilityFee) {
  try {
    await ElMessageBox.confirm(
      `确认删除职工【${empName(row.employeeId)}】${row.month} 的水电费登记？删除后同月可重新登记。`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await deleteUtility(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && page.current > 1) {
    page.current -= 1
  }
  load()
}

// ---------- 新增 / 修改 ----------
const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', submitting: false })
const formRef = ref<FormInstance>()

const emptyForm = (): SalUtilityFeeDTO => ({
  id: null,
  employeeId: null,
  month: '',
  waterFee: null,
  electricFee: null,
  dormFee: null
})

const form = reactive<SalUtilityFeeDTO>(emptyForm())

const rules: FormRules = {
  employeeId: [{ required: true, message: '请选择职工', trigger: 'change' }],
  month: [{ required: true, message: '请选择所属月份', trigger: 'change' }]
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialog.mode = 'create'
  dialog.visible = true
}

function openEdit(row: SalUtilityFee) {
  Object.assign(form, emptyForm(), {
    id: row.id,
    employeeId: row.employeeId,
    month: row.month,
    waterFee: row.waterFee,
    electricFee: row.electricFee,
    dormFee: row.dormFee
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
      await createUtility(form)
      ElMessage.success('新增成功')
    } else {
      await updateUtility(form)
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
/* 骨架类（.page-container/.search-card/.search-form/.toolbar/.tip/.field-tip/.pagination/.op-btns/...）
   已在 styles/index.css 全局定义，这里不再各写一份副本（阶段 3 收敛）。 */
</style>
