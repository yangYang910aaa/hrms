<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" class="search-form" @submit.prevent>
        <el-form-item label="课程">
          <el-select v-model="query.courseId" placeholder="全部" clearable filterable style="width: 200px">
            <el-option v-for="c in courses" :key="c.id" :label="courseLabel(c)" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="参训部门">
          <el-select v-model="query.deptId" placeholder="全部" clearable style="width: 180px">
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
        <el-button
          v-permission="'trn:frontline:add'"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          新增期次
        </el-button>
        <span class="tip">面向生产一线工人；参训部门必填，合格人数不得大于参训人数</span>
      </div>

      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column label="课程" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ courseName(row.courseId) }}</template>
        </el-table-column>
        <el-table-column label="期次编号" prop="batchNo" width="120" />
        <el-table-column label="参训部门" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ deptName(row.deptId) }}</template>
        </el-table-column>
        <el-table-column label="讲师" prop="trainer" width="100" />
        <el-table-column label="开始日期" prop="startDate" width="115" />
        <el-table-column label="结束日期" prop="endDate" width="115" />
        <el-table-column label="参训人数" prop="workerCount" width="90" align="right" />
        <el-table-column label="合格人数" prop="qualifiedCount" width="90" align="right" />
        <el-table-column label="状态" width="92">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="76" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <span v-permission="'trn:frontline:edit'">
                <el-tooltip content="修改" placement="top" :show-after="150">
                  <el-button link type="primary" :icon="Edit" aria-label="修改" @click="openEdit(row)" />
                </el-tooltip>
              </span>
              <span v-permission="'trn:frontline:remove'">
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
      :title="dialog.mode === 'create' ? '新增一线工培训期次' : '修改一线工培训期次'"
      width="660px"
      top="6vh"
      @closed="resetDialog"
    >
      <el-form ref="formRef" v-loading="dialog.submitting" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="培训课程" prop="courseId">
              <el-select v-model="form.courseId" placeholder="请选择" filterable style="width: 100%">
                <el-option
                  v-for="c in courses"
                  :key="c.id"
                  :label="courseLabel(c)"
                  :value="c.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="期次编号">
              <el-input v-model="form.batchNo" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="参训部门" prop="deptId">
              <el-select v-model="form.deptId" placeholder="请选择" style="width: 100%">
                <el-option v-for="d in deptOptions" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="讲师">
              <el-input v-model="form.trainer" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
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
          <el-col :span="12">
            <el-form-item label="参训人数">
              <el-input-number
                v-model="form.workerCount"
                :min="0"
                :precision="0"
                :value-on-clear="null"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合格人数">
              <el-input-number
                v-model="form.qualifiedCount"
                :min="0"
                :precision="0"
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createFrontline, deleteFrontline, frontlinePage, updateFrontline } from '@/api/trn'
import { courseLabel, courseName, courses, deptName, deptOptions, loadCourses, loadDepts } from '@/utils/lookup'
import type { PageResult, TrnFrontline, TrnFrontlineDTO, TrnFrontlinePageParams } from '@/types/api'

// ---------- 枚举（不在 sys_dict 里，硬编码） ----------
const STATUS_OPTIONS = [
  { value: 0, label: '计划' },
  { value: 1, label: '进行中' },
  { value: 2, label: '已完成' }
]

function statusLabel(status: number): string {
  return STATUS_OPTIONS.find((o) => o.value === status)?.label ?? String(status)
}

function statusTag(status: number): 'success' | 'warning' | 'info' {
  if (status === 2) return 'success'
  if (status === 1) return 'warning'
  return 'info'
}

// ---------- 列表 ----------
const loading = ref(false)
const records = ref<TrnFrontline[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })
const query = reactive<{ courseId: number | null; deptId: number | null; status: number | null }>({
  courseId: null,
  deptId: null,
  status: null
})

async function load() {
  loading.value = true
  try {
    const params: TrnFrontlinePageParams = {
      current: page.current,
      size: page.size,
      courseId: query.courseId,
      deptId: query.deptId,
      status: query.status
    }
    const res = await frontlinePage(params)
    const data = (res.data || {}) as PageResult<TrnFrontline>
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
  query.courseId = null
  query.deptId = null
  query.status = null
  onSearch()
}

// ---------- 删除 ----------
async function onDelete(row: TrnFrontline) {
  try {
    await ElMessageBox.confirm(
      `确认删除课程【${courseName(row.courseId)}】的这条一线工培训期次？`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await deleteFrontline(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && page.current > 1) {
    page.current -= 1
  }
  load()
}

// ---------- 新增 / 修改 ----------
const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', submitting: false })
const formRef = ref<FormInstance>()

const emptyForm = (): TrnFrontlineDTO => ({
  id: null,
  courseId: null,
  batchNo: null,
  deptId: null,
  trainer: null,
  startDate: null,
  endDate: null,
  workerCount: null,
  qualifiedCount: null,
  status: 0,
  remark: null
})

const form = reactive<TrnFrontlineDTO>(emptyForm())

const rules: FormRules = {
  courseId: [{ required: true, message: '请选择培训课程', trigger: 'change' }],
  deptId: [{ required: true, message: '请选择参训部门', trigger: 'change' }]
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialog.mode = 'create'
  dialog.visible = true
}

function openEdit(row: TrnFrontline) {
  Object.assign(form, emptyForm(), {
    id: row.id,
    courseId: row.courseId,
    batchNo: row.batchNo,
    deptId: row.deptId,
    trainer: row.trainer,
    startDate: row.startDate,
    endDate: row.endDate,
    workerCount: row.workerCount,
    qualifiedCount: row.qualifiedCount,
    status: row.status,
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
  if (form.startDate && form.endDate && form.endDate < form.startDate) {
    ElMessage.warning('结束日期不能早于开始日期')
    return
  }
  // 与后端同口径：按「最终值」比较（空值当 0）
  if ((form.qualifiedCount ?? 0) > (form.workerCount ?? 0)) {
    ElMessage.warning('合格人数不能大于参训人数')
    return
  }
  dialog.submitting = true
  try {
    if (dialog.mode === 'create') {
      await createFrontline(form)
      ElMessage.success('新增成功')
    } else {
      await updateFrontline(form)
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
  loadCourses()
  loadDepts()
  load()
})
</script>

<style scoped>
/* 骨架类（.page-container/.search-card/.search-form/.toolbar/.tip/.field-tip/.pagination/.op-btns/...）
   已在 styles/index.css 全局定义，这里不再各写一份副本（阶段 3 收敛）。 */
</style>
