<template>
  <div class="employee-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="姓名">
          <el-input v-model="query.name" placeholder="模糊匹配" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="query.deptId" placeholder="全部" clearable style="width: 180px">
            <el-option v-for="d in deptOptions" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="在职状态">
          <el-select v-model="query.employType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in employTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option label="正常" :value="1" />
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
        <el-button
          v-permission="'hr:employee:add'"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          新增职工
        </el-button>
      </div>

      <el-table :data="records" v-loading="loading" border row-key="id">
        <el-table-column label="工号" prop="empNo" width="100" />
        <el-table-column label="姓名" min-width="110">
          <template #default="{ row }">
            <el-avatar :size="22" :src="row.photo || undefined" style="vertical-align: middle; margin-right: 6px" />
            {{ row.name }}
          </template>
        </el-table-column>
        <el-table-column label="性别" width="70">
          <template #default="{ row }">{{ label('sex', row.gender) }}</template>
        </el-table-column>
        <el-table-column label="部门" prop="deptName" min-width="110" show-overflow-tooltip />
        <el-table-column label="学历" min-width="90">
          <template #default="{ row }">{{ label('edu_level', row.eduLevel) }}</template>
        </el-table-column>
        <el-table-column label="职位" prop="position" min-width="100" show-overflow-tooltip />
        <el-table-column label="入职日期" prop="hireDate" width="120" />
        <el-table-column label="在职状态" width="90">
          <template #default="{ row }">
            <el-tag :type="employTypeTag(row.employType)" size="small">
              {{ label('employ_type', row.employType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="70">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="年龄" width="70">
          <template #default="{ row }">{{ row.age ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'hr:employee:edit'"
              link
              type="primary"
              @click="openEdit(row)"
            >
              修改
            </el-button>
            <el-button
              v-permission="'hr:employee:remove'"
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
      :title="dialog.mode === 'create' ? '新增职工' : '修改职工'"
      width="680px"
      top="6vh"
      @closed="resetDialog"
    >
      <el-form
        ref="formRef"
        v-loading="dialog.submitting"
        :model="form"
        :rules="rules"
        label-width="84px"
      >
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="工号" prop="empNo">
              <el-input v-model="form.empNo" maxlength="30" placeholder="唯一，如 E1001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="form.gender" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in sexOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期">
              <el-date-picker
                v-model="form.birthDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="不能晚于今天"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证">
              <el-input v-model="form.idCard" maxlength="18" placeholder="18 位" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机">
              <el-input v-model="form.phone" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学历">
              <el-select v-model="form.eduLevel" placeholder="请选择" clearable style="width: 100%">
                <el-option
                  v-for="o in dictMap.edu_level || []"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门">
              <el-select v-model="form.deptId" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="d in deptOptions" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位">
              <el-input v-model="form.position" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="薪酬级别">
              <el-select v-model="form.salaryLevelId" placeholder="工资计算用" clearable style="width: 100%">
                <el-option
                  v-for="lv in levelOptions"
                  :key="lv.id"
                  :label="`${lv.levelName}（${lv.levelNo}）`"
                  :value="lv.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入职日期">
              <el-date-picker
                v-model="form.hireDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="在职状态">
              <el-select v-model="form.employType" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in employTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="离职日期">
              <el-date-picker
                v-model="form.leaveDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="仅离职状态可填"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="照片">
              <div class="photo-row">
                <el-avatar :size="64" :src="form.photo || undefined" shape="square" />
                <el-upload
                  :action="EMPLOYEE_PHOTO_URL"
                  :headers="uploadHeaders"
                  :show-file-list="false"
                  :before-upload="beforeUpload"
                  :on-success="onUploadSuccess"
                  accept=".jpg,.jpeg,.png,.gif,.webp"
                >
                  <el-button size="small">上传照片</el-button>
                </el-upload>
                <span class="photo-tip">不超过 5MB；上传成功后自动带入表单</span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用">
              <el-switch
                v-model="form.status"
                :active-value="1"
                :inactive-value="0"
                active-text="正常"
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadProps } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  EMPLOYEE_PHOTO_URL,
  createEmployee,
  deleteEmployee,
  employeePage,
  updateEmployee
} from '@/api/employee'
import { salaryLevelOptions } from '@/api/common'
import { useDict } from '@/utils/dict'
import { deptOptions, loadDepts } from '@/utils/lookup'
import { getToken } from '@/utils/auth'
import type {
  EmployeeDTO,
  EmployeePageParams,
  EmployeeVO,
  PageResult,
  SalaryLevelOption
} from '@/types/api'
import type { ApiResult } from '@/types/api'

// ---------- 字典与下拉数据 ----------
const { dictMap, load: loadDict, label } = useDict(['sex', 'edu_level', 'employ_type'])

/** 字典值是字符串，性别/在职状态在表单里是数字 → 转一下 */
const sexOptions = computed(() => (dictMap.value.sex || []).map((o) => ({ label: o.label, value: Number(o.value) })))
const employTypeOptions = computed(() =>
  (dictMap.value.employ_type || []).map((o) => ({ label: o.label, value: Number(o.value) }))
)

const levelOptions = ref<SalaryLevelOption[]>([])

async function loadOptions() {
  // 部门下拉走共享 lookup 层（与批次3 的 5 个页面同一份实现，别再各拍平一份）
  await Promise.all([loadDict(), loadDepts()])
  const level = await salaryLevelOptions()
  levelOptions.value = level.data || []
}

// ---------- 列表 ----------
const loading = ref(false)
const records = ref<EmployeeVO[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })
const query = reactive<{ name: string; deptId: number | null; employType: number | null; status: number | null }>({
  name: '',
  deptId: null,
  employType: null,
  status: null
})

async function load() {
  loading.value = true
  try {
    const params: EmployeePageParams = {
      current: page.current,
      size: page.size,
      name: query.name.trim() || undefined,
      deptId: query.deptId,
      employType: query.employType,
      status: query.status
    }
    const res = await employeePage(params)
    const data = (res.data || {}) as PageResult<EmployeeVO>
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
  query.name = ''
  query.deptId = null
  query.employType = null
  query.status = null
  onSearch()
}

function employTypeTag(employType: number | null): 'success' | 'warning' | 'info' {
  if (employType === 2) return 'warning'
  if (employType === 3) return 'info'
  return 'success'
}

// ---------- 删除 ----------
async function onDelete(row: EmployeeVO) {
  try {
    await ElMessageBox.confirm(`确认删除职工【${row.name}】？删除后工号可重新录入。`, '提示', {
      type: 'warning'
    })
  } catch {
    return // 用户点了取消：confirm 取消是抛异常，不接住就是一条未处理的 rejection
  }
  await deleteEmployee(row.id)
  ElMessage.success('删除成功')
  // 删完本页只剩一条且不在第一页时，退一页，避免看到空页
  if (records.value.length === 1 && page.current > 1) {
    page.current -= 1
  }
  load()
}

// ---------- 新增 / 修改 ----------
const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', submitting: false })
const formRef = ref<FormInstance>()

const emptyForm = (): EmployeeDTO => ({
  id: null,
  empNo: '',
  name: '',
  gender: null,
  birthDate: null,
  idCard: null,
  phone: null,
  email: null,
  address: null,
  eduLevel: null,
  deptId: null,
  position: null,
  hireDate: null,
  leaveDate: null,
  employType: 1,
  salaryLevelId: null,
  status: 1,
  photo: null,
  remark: null
})

const form = reactive<EmployeeDTO>(emptyForm())

const rules: FormRules = {
  empNo: [{ required: true, message: '工号不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '姓名不能为空', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialog.mode = 'create'
  dialog.visible = true
}

async function openEdit(row: EmployeeVO) {
  Object.assign(form, emptyForm(), {
    id: row.id,
    empNo: row.empNo,
    name: row.name,
    gender: row.gender,
    birthDate: row.birthDate,
    idCard: row.idCard,
    phone: row.phone,
    email: row.email,
    address: row.address,
    eduLevel: row.eduLevel,
    deptId: row.deptId,
    position: row.position,
    hireDate: row.hireDate,
    leaveDate: row.leaveDate,
    employType: row.employType,
    salaryLevelId: row.salaryLevelId,
    status: row.status ?? 1,
    photo: row.photo,
    remark: row.remark
  })
  dialog.mode = 'edit'
  dialog.visible = true
}

function resetDialog() {
  formRef.value?.clearValidate()
  Object.assign(form, emptyForm())
}

// ---------- 照片上传（el-upload 直传，带 satoken 头） ----------
const uploadHeaders = computed(() => ({ satoken: getToken() }))

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('照片不能超过 5MB')
    return false
  }
  return true
}

/** 响应体是信封 {code,msg,data}，data 才是可访问的 URL */
function onUploadSuccess(res: ApiResult<string>) {
  if (res && res.code === 200) {
    form.photo = res.data
    ElMessage.success('照片已上传')
  } else {
    ElMessage.error((res && res.msg) || '上传失败')
  }
}

// ---------- 保存 ----------
async function onSubmit() {
  if (!formRef.value) return
  // 用 Promise 形式：validate(callback) 里的异步错误不在 await 链上，会漏成未处理的 rejection
  try {
    await formRef.value.validate()
  } catch {
    return // 校验不通过，消息由表单自己提示
  }
  dialog.submitting = true
  try {
    if (dialog.mode === 'create') {
      await createEmployee(form)
      ElMessage.success('新增成功')
    } else {
      await updateEmployee(form)
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
  loadOptions()
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
  margin-bottom: 12px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.photo-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.photo-tip {
  font-size: 12px;
  color: #909399;
}
</style>
