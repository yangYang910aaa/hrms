<template>
  <div class="trn-course-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" class="search-form" @submit.prevent>
        <el-form-item label="课程名称">
          <el-input
            v-model="query.courseName"
            placeholder="模糊匹配"
            clearable
            style="width: 170px"
            @keyup.enter="onSearch"
          />
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="query.courseType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
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

    <!-- 列表 -->
    <el-card shadow="never">
      <div class="toolbar">
        <el-button v-permission="'trn:course:add'" type="primary" :icon="Plus" @click="openCreate">
          新增课程
        </el-button>
        <span class="tip">课程编号唯一；被培训期次引用的课程不能删除（先删期次）</span>
      </div>

      <el-table :data="records" v-loading="loading" border>
        <el-table-column label="课程编号" prop="courseNo" width="120" />
        <el-table-column label="课程名称" prop="courseName" min-width="160" show-overflow-tooltip />
        <el-table-column label="类别" width="110">
          <template #default="{ row }">{{ typeLabel(row.courseType) }}</template>
        </el-table-column>
        <el-table-column label="讲师" prop="teacher" width="110" />
        <el-table-column label="课时" width="80" align="right">
          <template #default="{ row }">{{ money(row.hours) }}</template>
        </el-table-column>
        <el-table-column label="学分" width="80" align="right">
          <template #default="{ row }">{{ money(row.credit) }}</template>
        </el-table-column>
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
            <el-button v-permission="'trn:course:edit'" link type="primary" @click="openEdit(row)">
              修改
            </el-button>
            <el-button v-permission="'trn:course:remove'" link type="danger" @click="onDelete(row)">
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
      :title="dialog.mode === 'create' ? '新增课程' : '修改课程'"
      width="680px"
      top="6vh"
      @closed="resetDialog"
    >
      <el-form ref="formRef" v-loading="dialog.submitting" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="课程编号" prop="courseNo">
              <el-input v-model="form.courseNo" maxlength="30" placeholder="唯一，如 C001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程名称" prop="courseName">
              <el-input v-model="form.courseName" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类别" prop="courseType">
              <el-select v-model="form.courseType" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="讲师">
              <el-input v-model="form.teacher" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课时">
              <el-input-number
                v-model="form.hours"
                :min="0"
                :precision="1"
                :value-on-clear="null"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学分">
              <el-input-number
                v-model="form.credit"
                :min="0"
                :precision="1"
                :value-on-clear="null"
                controls-position="right"
                style="width: 100%"
              />
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
            <el-form-item label="课程简介">
              <el-input v-model="form.content" type="textarea" :rows="3" />
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
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { coursePage, createCourse, deleteCourse, updateCourse } from '@/api/trn'
import { money } from '@/utils/format'
import type { PageResult, TrnCourse, TrnCourseDTO, TrnCoursePageParams } from '@/types/api'

// ---------- 类别枚举（不在 sys_dict 里，硬编码） ----------
const TYPE_OPTIONS = [
  { value: 1, label: '内部培训' },
  { value: 2, label: '一线工培训' },
  { value: 3, label: '其他' }
]

function typeLabel(type: number): string {
  return TYPE_OPTIONS.find((o) => o.value === type)?.label ?? String(type)
}

// ---------- 列表 ----------
const loading = ref(false)
const records = ref<TrnCourse[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })
const query = reactive<{ courseName: string; courseType: number | null; status: number | null }>({
  courseName: '',
  courseType: null,
  status: null
})

async function load() {
  loading.value = true
  try {
    const params: TrnCoursePageParams = {
      current: page.current,
      size: page.size,
      courseName: query.courseName.trim() || undefined,
      courseType: query.courseType,
      status: query.status
    }
    const res = await coursePage(params)
    const data = (res.data || {}) as PageResult<TrnCourse>
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
  query.courseName = ''
  query.courseType = null
  query.status = null
  onSearch()
}

// ---------- 删除 ----------
async function onDelete(row: TrnCourse) {
  try {
    await ElMessageBox.confirm(
      `确认删除课程【${row.courseName}】？若已被培训期次引用，后端会拒绝。`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await deleteCourse(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && page.current > 1) {
    page.current -= 1
  }
  load()
}

// ---------- 新增 / 修改 ----------
const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', submitting: false })
const formRef = ref<FormInstance>()

const emptyForm = (): TrnCourseDTO => ({
  id: null,
  courseNo: '',
  courseName: '',
  courseType: null,
  teacher: null,
  hours: null,
  credit: null,
  content: null,
  status: 1,
  remark: null
})

const form = reactive<TrnCourseDTO>(emptyForm())

const rules: FormRules = {
  courseNo: [{ required: true, message: '课程编号不能为空', trigger: 'blur' }],
  courseName: [{ required: true, message: '课程名称不能为空', trigger: 'blur' }],
  courseType: [{ required: true, message: '请选择课程类别', trigger: 'change' }]
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialog.mode = 'create'
  dialog.visible = true
}

function openEdit(row: TrnCourse) {
  Object.assign(form, emptyForm(), {
    id: row.id,
    courseNo: row.courseNo,
    courseName: row.courseName,
    courseType: row.courseType,
    teacher: row.teacher,
    hours: row.hours,
    credit: row.credit,
    content: row.content,
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
  dialog.submitting = true
  try {
    if (dialog.mode === 'create') {
      await createCourse(form)
      ElMessage.success('新增成功')
    } else {
      await updateCourse(form)
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
</style>
