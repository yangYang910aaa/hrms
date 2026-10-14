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
            style="width: 210px"
          >
            <el-option v-for="e in employees" :key="e.id" :label="employeeLabel(e)" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 120px">
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
        <el-button v-permission="'hr:reward:add'" type="primary" :icon="Plus" @click="openCreate">
          新增奖罚
        </el-button>
        <span class="tip">奖罚按「发生日期」归入当月的工资档（奖励记 +、处罚记 −）</span>
      </div>

      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column label="职工" min-width="120">
          <template #default="{ row }">{{ empName(row.employeeId) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'success' : 'danger'" size="small">
              {{ label('reward_type', row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="事由" prop="reason" min-width="200" show-overflow-tooltip />
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">{{ money(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="发生日期" prop="happenDate" width="120" />
        <el-table-column label="经办人" prop="operator" width="110" />
        <el-table-column label="操作" width="76" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <span v-permission="'hr:reward:edit'">
                <el-tooltip content="修改" placement="top" :show-after="150">
                  <el-button link type="primary" :icon="Edit" aria-label="修改" @click="openEdit(row)" />
                </el-tooltip>
              </span>
              <span v-permission="'hr:reward:remove'">
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
      :title="dialog.mode === 'create' ? '新增奖罚' : '修改奖罚'"
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
            <el-form-item label="类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="事由" prop="reason">
              <el-input v-model="form.reason" type="textarea" :rows="2" maxlength="255" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="金额">
              <el-input-number
                v-model="form.amount"
                :min="0"
                :precision="2"
                :value-on-clear="null"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发生日期">
              <el-date-picker
                v-model="form.happenDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="计入该月工资"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经办人">
              <el-input v-model="form.operator" maxlength="30" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createReward, deleteReward, rewardPage, updateReward } from '@/api/hr'
import { empName, employeeLabel, employees, loadEmployees } from '@/utils/lookup'
import { useDict } from '@/utils/dict'
import { money } from '@/utils/format'
import type { PageResult, RewardPunish, RewardPunishDTO, RewardPunishPageParams } from '@/types/api'
import { confirmDelete } from '@/utils/confirm'

// ---------- 字典 ----------
// 字典值返回的是字符串，而 DTO 的 type 是整数 → 统一 Number() 转一下
const { dictMap, load: loadDict, label } = useDict(['reward_type'])
const typeOptions = computed(() =>
  (dictMap.value.reward_type || []).map((o) => ({ label: o.label, value: Number(o.value) }))
)

// ---------- 列表 ----------
const loading = ref(false)
const records = ref<RewardPunish[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })
const query = reactive<{ employeeId: number | null; type: number | null }>({
  employeeId: null,
  type: null
})

async function load() {
  loading.value = true
  try {
    const params: RewardPunishPageParams = {
      current: page.current,
      size: page.size,
      employeeId: query.employeeId,
      type: query.type
    }
    const res = await rewardPage(params)
    const data = (res.data || {}) as PageResult<RewardPunish>
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
  query.type = null
  onSearch()
}

// ---------- 删除 ----------
async function onDelete(row: RewardPunish) {
  try {
    await confirmDelete(`确认删除职工【${empName(row.employeeId)}】的这条${label('reward_type', row.type)}？`)
  } catch {
    return
  }
  await deleteReward(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && page.current > 1) {
    page.current -= 1
  }
  load()
}

// ---------- 新增 / 修改 ----------
const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', submitting: false })
const formRef = ref<FormInstance>()

const emptyForm = (): RewardPunishDTO => ({
  id: null,
  employeeId: null,
  type: 1,
  reason: '',
  amount: null,
  happenDate: null,
  operator: null
})

const form = reactive<RewardPunishDTO>(emptyForm())

const rules: FormRules = {
  employeeId: [{ required: true, message: '请选择职工', trigger: 'change' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  reason: [{ required: true, message: '事由不能为空', trigger: 'blur' }]
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialog.mode = 'create'
  dialog.visible = true
}

function openEdit(row: RewardPunish) {
  Object.assign(form, emptyForm(), {
    id: row.id,
    employeeId: row.employeeId,
    type: row.type,
    reason: row.reason,
    amount: row.amount,
    happenDate: row.happenDate,
    operator: row.operator
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
      await createReward(form)
      ElMessage.success('新增成功')
    } else {
      await updateReward(form)
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
  loadDict()
  loadEmployees()
  load()
})
</script>

<style scoped>
/* 骨架类（.page-container/.search-card/.search-form/.toolbar/.tip/.field-tip/.pagination/.op-btns/...）
   已在 styles/index.css 全局定义，这里不再各写一份副本（阶段 3 收敛）。 */
</style>
