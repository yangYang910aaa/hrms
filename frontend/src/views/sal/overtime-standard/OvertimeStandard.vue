<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" class="search-form" @submit.prevent>
        <el-form-item label="薪酬级别">
          <el-select v-model="query.levelId" placeholder="全部" clearable style="width: 190px">
            <el-option v-for="lv in salLevels" :key="lv.id" :label="salLevelLabel(lv)" :value="lv.id" />
          </el-select>
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
        <el-button
          v-permission="'sal:overtime-standard:add'"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          新增加班标准
        </el-button>
        <span class="tip">
          同一「级别 + 加班类型」只能配一条；倍率是乘数（工作日 1.5 倍就填 1.5），加班登记的金额按它算
        </span>
      </div>

      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column label="薪酬级别" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ salLevelName(row.levelId) }}</template>
        </el-table-column>
        <el-table-column label="加班类型" width="130">
          <template #default="{ row }">{{ label('overtime_type', row.overtimeType) }}</template>
        </el-table-column>
        <el-table-column label="倍率" width="100" align="right">
          <template #default="{ row }">{{ money(row.rate) }}</template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="76" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <span v-permission="'sal:overtime-standard:edit'">
                <el-tooltip content="修改" placement="top" :show-after="150">
                  <el-button link type="primary" :icon="Edit" aria-label="修改" @click="openEdit(row)" />
                </el-tooltip>
              </span>
              <span v-permission="'sal:overtime-standard:remove'">
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
      :title="dialog.mode === 'create' ? '新增加班标准' : '修改加班标准'"
      width="560px"
      top="6vh"
      @closed="resetDialog"
    >
      <el-form ref="formRef" v-loading="dialog.submitting" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="薪酬级别" prop="levelId">
              <el-select v-model="form.levelId" placeholder="请选择" style="width: 100%">
                <el-option v-for="lv in salLevels" :key="lv.id" :label="salLevelLabel(lv)" :value="lv.id" />
              </el-select>
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
            <el-form-item label="倍率">
              <el-input-number
                v-model="form.rate"
                :min="0"
                :max="999.99"
                :step="0.5"
                :precision="2"
                :value-on-clear="null"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
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
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createOvertimeStandard,
  deleteOvertimeStandard,
  overtimeStandardPage,
  updateOvertimeStandard
} from '@/api/sal'
import { loadSalLevels, salLevelLabel, salLevelName, salLevels } from '@/utils/lookup'
import { useDict } from '@/utils/dict'
import { money } from '@/utils/format'
import type {
  PageResult,
  SalOvertimeStandard,
  SalOvertimeStandardDTO,
  SalOvertimeStandardPageParams
} from '@/types/api'

// ---------- 字典 ----------
// 字典值是字符串、DTO 里是整数 → Number() 转换
const { dictMap, load: loadDict, label } = useDict(['overtime_type'])
const typeOptions = computed(() =>
  (dictMap.value.overtime_type || []).map((o) => ({ label: o.label, value: Number(o.value) }))
)

// ---------- 列表 ----------
const loading = ref(false)
const records = ref<SalOvertimeStandard[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })
const query = reactive<{ levelId: number | null; overtimeType: number | null }>({
  levelId: null,
  overtimeType: null
})

async function load() {
  loading.value = true
  try {
    const params: SalOvertimeStandardPageParams = {
      current: page.current,
      size: page.size,
      levelId: query.levelId,
      overtimeType: query.overtimeType
    }
    const res = await overtimeStandardPage(params)
    const data = (res.data || {}) as PageResult<SalOvertimeStandard>
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
  query.levelId = null
  query.overtimeType = null
  onSearch()
}

// ---------- 删除 ----------
async function onDelete(row: SalOvertimeStandard) {
  try {
    await ElMessageBox.confirm(
      `确认删除【${salLevelName(row.levelId)} · ${label('overtime_type', row.overtimeType)}】的加班标准？删除后可重建同组合。`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await deleteOvertimeStandard(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && page.current > 1) {
    page.current -= 1
  }
  load()
}

// ---------- 新增 / 修改 ----------
const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', submitting: false })
const formRef = ref<FormInstance>()

const emptyForm = (): SalOvertimeStandardDTO => ({
  id: null,
  levelId: null,
  overtimeType: null,
  rate: null,
  remark: null
})

const form = reactive<SalOvertimeStandardDTO>(emptyForm())

const rules: FormRules = {
  levelId: [{ required: true, message: '请选择薪酬级别', trigger: 'change' }],
  overtimeType: [{ required: true, message: '请选择加班类型', trigger: 'change' }]
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialog.mode = 'create'
  dialog.visible = true
}

function openEdit(row: SalOvertimeStandard) {
  Object.assign(form, emptyForm(), {
    id: row.id,
    levelId: row.levelId,
    overtimeType: row.overtimeType,
    rate: row.rate,
    remark: row.remark
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
      await createOvertimeStandard(form)
      ElMessage.success('新增成功')
    } else {
      await updateOvertimeStandard(form)
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
  loadSalLevels()
  load()
})
</script>

<style scoped>
/* 骨架类（.page-container/.search-card/.search-form/.toolbar/.tip/.field-tip/.pagination/.op-btns/...）
   已在 styles/index.css 全局定义，这里不再各写一份副本（阶段 3 收敛）。 */
</style>
