<template>
  <div class="social-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" class="search-form" @submit.prevent>
        <el-form-item label="险种名称">
          <el-input
            v-model="query.rateName"
            placeholder="模糊匹配"
            clearable
            style="width: 170px"
            @keyup.enter="onSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <el-button
          v-permission="'system:social-rate:add'"
          type="primary"
          :icon="Plus"
          @click="openCreate()"
        >
          新增险种
        </el-button>
        <span class="tip">比例以小数存储：0.16 = 16%（工资计算直接取这两个值）</span>
      </div>

      <el-table :data="records" v-loading="loading" border>
        <el-table-column label="险种名称" prop="rateName" min-width="130" />
        <el-table-column label="编码" prop="rateCode" width="90" />
        <el-table-column label="单位比例" width="132" align="right">
          <template #default="{ row }">{{ rate(row.companyRate) }}</template>
        </el-table-column>
        <el-table-column label="个人比例" width="132" align="right">
          <template #default="{ row }">{{ rate(row.personalRate) }}</template>
        </el-table-column>
        <el-table-column label="基数下限" width="120" align="right">
          <template #default="{ row }">{{ money(row.baseMin) }}</template>
        </el-table-column>
        <el-table-column label="基数上限" width="120" align="right">
          <template #default="{ row }">{{ money(row.baseMax) }}</template>
        </el-table-column>
        <el-table-column label="生效月份" prop="effectiveMonth" width="100" />
        <el-table-column label="排序" prop="sort" width="70" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'system:social-rate:edit'"
              link
              type="primary"
              @click="openEdit(row)"
            >
              修改
            </el-button>
            <el-button
              v-permission="'system:social-rate:remove'"
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

    <!-- 新增 / 修改弹窗 -->
    <el-dialog
      v-model="dialog.visible"
      :title="dialog.mode === 'create' ? '新增社保比例' : '修改社保比例'"
      width="560px"
      top="6vh"
      @closed="resetDialog"
    >
      <el-form
        ref="formRef"
        v-loading="dialog.submitting"
        :model="form"
        :rules="rules"
        label-width="96px"
      >
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="险种名称" prop="rateName">
              <el-input v-model="form.rateName" maxlength="30" placeholder="如：养老保险" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="险种编码" prop="rateCode">
              <el-input v-model="form.rateCode" maxlength="30" placeholder="如：YL" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位比例" prop="companyRate">
              <el-input-number
                v-model="form.companyRate"
                :min="0"
                :max="1"
                :step="0.01"
                :precision="4"
                controls-position="right"
                style="width: 100%"
              />
              <div class="field-tip">小数：16% 请填 0.16</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="个人比例" prop="personalRate">
              <el-input-number
                v-model="form.personalRate"
                :min="0"
                :max="1"
                :step="0.01"
                :precision="4"
                controls-position="right"
                style="width: 100%"
              />
              <div class="field-tip">小数：8% 请填 0.08；工伤等仅单位缴纳的填 0</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="基数下限">
              <el-input-number
                v-model="form.baseMin"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
              <div class="field-tip">0 = 未配置</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="基数上限">
              <el-input-number
                v-model="form.baseMax"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
              <div class="field-tip">0 = 不限制</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生效月份">
              <el-date-picker
                v-model="form.effectiveMonth"
                type="month"
                value-format="YYYY-MM"
                placeholder="YYYY-MM"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch
                v-model="form.status"
                :active-value="1"
                :inactive-value="0"
                active-text="启用"
                inactive-text="停用"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createSocialRate, deleteSocialRate, socialRatePage, updateSocialRate } from '@/api/system'
import type { PageResult, SocialRateDTO, SocialRatePageParams, SocialRateVO } from '@/types/api'

// ---------- 列表 ----------
const loading = ref(false)
const records = ref<SocialRateVO[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })
const query = reactive<{ rateName: string; status: number | null }>({ rateName: '', status: null })

async function load() {
  loading.value = true
  try {
    const params: SocialRatePageParams = {
      current: page.current,
      size: page.size,
      rateName: query.rateName.trim() || undefined,
      status: query.status
    }
    const res = await socialRatePage(params)
    const data = (res.data || {}) as PageResult<SocialRateVO>
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
  query.rateName = ''
  query.status = null
  onSearch()
}

/** 比例是 0~1 小数，原样显示；顺手补个百分比便于阅读（只用于展示，不参与提交） */
function rate(value: number | null): string {
  if (value === null || value === undefined) return '—'
  return `${value}（${(value * 100).toFixed(2)}%）`
}

function money(value: number | null): string {
  if (value === null || value === undefined) return '—'
  return Number(value).toFixed(2)
}

// ---------- 删除 ----------
async function onDelete(row: SocialRateVO) {
  try {
    await ElMessageBox.confirm(`确认删除险种【${row.rateName}】？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteSocialRate(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && page.current > 1) {
    page.current -= 1
  }
  load()
}

// ---------- 新增 / 修改 ----------
const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', submitting: false })
const formRef = ref<FormInstance>()

const emptyForm = (): SocialRateDTO => ({
  id: null,
  rateName: '',
  rateCode: '',
  companyRate: 0,
  personalRate: 0,
  baseMin: 0,
  baseMax: 0,
  effectiveMonth: null,
  sort: 0,
  status: 1,
  remark: null
})

const form = reactive<SocialRateDTO>(emptyForm())

const rules: FormRules = {
  rateName: [{ required: true, message: '险种名称不能为空', trigger: 'blur' }],
  rateCode: [{ required: true, message: '险种编码不能为空', trigger: 'blur' }],
  companyRate: [
    { required: true, message: '单位比例不能为空', trigger: 'blur' },
    { type: 'number', min: 0, max: 1, message: '应为 0~1 的小数（16% 填 0.16）', trigger: 'blur' }
  ],
  personalRate: [
    { required: true, message: '个人比例不能为空', trigger: 'blur' },
    { type: 'number', min: 0, max: 1, message: '应为 0~1 的小数（8% 填 0.08）', trigger: 'blur' }
  ]
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialog.mode = 'create'
  dialog.visible = true
}

function openEdit(row: SocialRateVO) {
  Object.assign(form, emptyForm(), {
    id: row.id,
    rateName: row.rateName,
    rateCode: row.rateCode,
    companyRate: row.companyRate ?? 0,
    personalRate: row.personalRate ?? 0,
    baseMin: row.baseMin ?? 0,
    baseMax: row.baseMax ?? 0,
    effectiveMonth: row.effectiveMonth,
    sort: row.sort ?? 0,
    status: row.status ?? 1,
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
  const min = form.baseMin ?? 0
  const max = form.baseMax ?? 0
  // 与后端 assertBaseRange 同一口径：只有两端都大于 0 才算区间（0 是"未配置/不限制"的哨兵）
  if (min > 0 && max > 0 && min > max) {
    ElMessage.warning('缴费基数下限不能大于上限（上限填 0 表示不限制）')
    return
  }
  dialog.submitting = true
  try {
    if (dialog.mode === 'create') {
      await createSocialRate(form)
      ElMessage.success('新增成功')
    } else {
      await updateSocialRate(form)
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

onMounted(load)
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

.tip {
  font-size: 12px;
  color: #909399;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.field-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}
</style>
