<template>
  <div class="hr-social-page">
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
    <el-card shadow="never">
      <div class="toolbar">
        <el-button v-permission="'hr:social:add'" type="primary" :icon="Plus" @click="openCreate">
          新增社保
        </el-button>
        <span class="tip">同一职工同一月只能登记一条（唯一键）；比例填小数，缴纳额留空由系统按「基数 × 比例」计算</span>
      </div>

      <el-table :data="records" v-loading="loading" border>
        <el-table-column label="职工" min-width="120">
          <template #default="{ row }">{{ empName(row.employeeId) }}</template>
        </el-table-column>
        <el-table-column label="社保编号" prop="ssNo" min-width="120" show-overflow-tooltip />
        <el-table-column label="缴费基数" width="120" align="right">
          <template #default="{ row }">{{ money(row.baseAmount) }}</template>
        </el-table-column>
        <el-table-column label="单位比例" width="150" align="right">
          <template #default="{ row }">{{ percent(row.companyRate) }}</template>
        </el-table-column>
        <el-table-column label="个人比例" width="150" align="right">
          <template #default="{ row }">{{ percent(row.personalRate) }}</template>
        </el-table-column>
        <el-table-column label="单位缴纳额" width="120" align="right">
          <template #default="{ row }">{{ money(row.companyAmount) }}</template>
        </el-table-column>
        <el-table-column label="个人缴纳额" width="120" align="right">
          <template #default="{ row }">{{ money(row.personalAmount) }}</template>
        </el-table-column>
        <el-table-column label="所属月份" prop="month" width="100" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'hr:social:edit'" link type="primary" @click="openEdit(row)">
              修改
            </el-button>
            <el-button v-permission="'hr:social:remove'" link type="danger" @click="onDelete(row)">
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
      :title="dialog.mode === 'create' ? '新增社保' : '修改社保'"
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
                  :label="`${e.empNo} ${e.name}`"
                  :value="e.id"
                />
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
            <el-form-item label="社保编号">
              <el-input v-model="form.ssNo" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="缴费基数">
              <el-input-number
                v-model="form.baseAmount"
                :min="0"
                :precision="2"
                :value-on-clear="null"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位比例">
              <el-input-number
                v-model="form.companyRate"
                :min="0"
                :max="1"
                :step="0.01"
                :precision="4"
                :value-on-clear="null"
                controls-position="right"
                style="width: 100%"
              />
              <div class="field-tip">小数：16% 请填 0.16</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="个人比例">
              <el-input-number
                v-model="form.personalRate"
                :min="0"
                :max="1"
                :step="0.01"
                :precision="4"
                :value-on-clear="null"
                controls-position="right"
                style="width: 100%"
              />
              <div class="field-tip">小数：8% 请填 0.08</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位缴纳额">
              <el-input-number
                v-model="form.companyAmount"
                :min="0"
                :precision="2"
                :value-on-clear="null"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="个人缴纳额">
              <el-input-number
                v-model="form.personalAmount"
                :min="0"
                :precision="2"
                :value-on-clear="null"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <div class="field-tip">
              缴纳额留空：新增时由系统按「基数 × 比例」自动计算；修改时留空 = 保持原值
              （只有动了基数或比例才会重算）。
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
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createSocial, deleteSocial, socialPage, updateSocial } from '@/api/hr'
import { empName, employees, loadEmployees } from '@/utils/lookup'
import { money, percent } from '@/utils/format'
import type { PageResult, SocialSecurity, SocialSecurityDTO, SocialSecurityPageParams } from '@/types/api'

// ---------- 下拉数据 ----------
// 列表接口直出实体，只有 employeeId → 用共享的 employee 下拉翻译姓名

// ---------- 列表 ----------
const loading = ref(false)
const records = ref<SocialSecurity[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })
const query = reactive<{ employeeId: number | null; month: string | null }>({
  employeeId: null,
  month: null
})

async function load() {
  loading.value = true
  try {
    const params: SocialSecurityPageParams = {
      current: page.current,
      size: page.size,
      employeeId: query.employeeId,
      month: query.month
    }
    const res = await socialPage(params)
    const data = (res.data || {}) as PageResult<SocialSecurity>
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
async function onDelete(row: SocialSecurity) {
  try {
    await ElMessageBox.confirm(
      `确认删除职工【${empName(row.employeeId)}】${row.month} 的社保记录？删除后同月可重新登记。`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return // 用户取消：confirm 取消会抛异常，不接住就是未处理的 rejection
  }
  await deleteSocial(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && page.current > 1) {
    page.current -= 1
  }
  load()
}

// ---------- 新增 / 修改 ----------
const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', submitting: false })
const formRef = ref<FormInstance>()

const emptyForm = (): SocialSecurityDTO => ({
  id: null,
  employeeId: null,
  ssNo: null,
  baseAmount: null,
  companyRate: null,
  personalRate: null,
  companyAmount: null,
  personalAmount: null,
  month: ''
})

const form = reactive<SocialSecurityDTO>(emptyForm())

const rules: FormRules = {
  employeeId: [{ required: true, message: '请选择职工', trigger: 'change' }],
  month: [{ required: true, message: '请选择所属月份', trigger: 'change' }]
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialog.mode = 'create'
  dialog.visible = true
}

function openEdit(row: SocialSecurity) {
  Object.assign(form, emptyForm(), {
    id: row.id,
    employeeId: row.employeeId,
    ssNo: row.ssNo,
    baseAmount: row.baseAmount,
    companyRate: row.companyRate,
    personalRate: row.personalRate,
    companyAmount: row.companyAmount,
    personalAmount: row.personalAmount,
    month: row.month
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
  // 用 Promise 形式：validate(callback) 里的异步错误不在 await 链上，会漏成未处理的 rejection
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  dialog.submitting = true
  try {
    if (dialog.mode === 'create') {
      await createSocial(form)
      ElMessage.success('新增成功')
    } else {
      await updateSocial(form)
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
