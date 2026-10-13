<template>
  <div class="query-employee-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" class="search-form" @submit.prevent>
        <el-form-item label="姓名">
          <el-input
            v-model="query.name"
            placeholder="模糊匹配"
            clearable
            style="width: 160px"
            @keyup.enter="onSearch"
          />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="query.deptId" placeholder="全部" clearable filterable style="width: 170px">
            <el-option v-for="o in deptOptions" :key="o.id" :label="o.name" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="在职状态">
          <el-select v-model="query.employType" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in dictMap.employ_type || []" :key="o.value" :label="o.label" :value="Number(o.value)" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
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

    <!-- 结果列表（只读） -->
    <el-card shadow="never">
      <div class="toolbar">
        <span class="tip">公共查询为只读视图：职工资料的新增/修改请在「人事管理 → 职工管理」进行</span>
      </div>

      <el-table :data="records" v-loading="loading" border>
        <el-table-column label="工号" prop="empNo" width="110" />
        <el-table-column label="姓名" prop="name" width="110" show-overflow-tooltip />
        <el-table-column label="性别" width="70">
          <template #default="{ row }">{{ label('sex', row.gender) }}</template>
        </el-table-column>
        <el-table-column label="年龄" width="70" align="right">
          <template #default="{ row }">{{ row.age ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="部门" prop="deptName" width="120" show-overflow-tooltip />
        <el-table-column label="岗位" prop="position" width="120" show-overflow-tooltip />
        <el-table-column label="学历" width="110">
          <template #default="{ row }">{{ label('edu_level', row.eduLevel) }}</template>
        </el-table-column>
        <el-table-column label="入职日期" prop="hireDate" width="120" />
        <el-table-column label="在职状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.employType === 3 ? 'info' : 'success'" size="small">
              {{ label('employ_type', row.employType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="手机" prop="phone" width="130" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
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

    <!-- 详情弹窗 -->
    <el-dialog v-model="detail.visible" title="职工详情" width="680px" top="6vh">
      <el-descriptions v-if="detail.row" :column="2" border>
        <el-descriptions-item label="工号">{{ detail.row.empNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ detail.row.name }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ label('sex', detail.row.gender) }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ detail.row.age ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ detail.row.idCard || '—' }}</el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ detail.row.birthDate || '—' }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ detail.row.deptName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="岗位">{{ detail.row.position || '—' }}</el-descriptions-item>
        <el-descriptions-item label="学历">{{ label('edu_level', detail.row.eduLevel) }}</el-descriptions-item>
        <el-descriptions-item label="薪酬级别">{{ detail.row.salaryLevelName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="入职日期">{{ detail.row.hireDate || '—' }}</el-descriptions-item>
        <el-descriptions-item label="离职日期">{{ detail.row.leaveDate || '—' }}</el-descriptions-item>
        <el-descriptions-item label="在职状态">{{ label('employ_type', detail.row.employType) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.row.status === 1 ? '正常' : '停用' }}</el-descriptions-item>
        <el-descriptions-item label="手机">{{ detail.row.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.row.email || '—' }}</el-descriptions-item>
        <el-descriptions-item label="住址" :span="2">{{ detail.row.address || '—' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.row.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detail.visible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { queryEmployeeDetail, queryEmployeePage } from '@/api/query'
import { deptOptions, loadDepts } from '@/utils/lookup'
import { useDict } from '@/utils/dict'
import type { EmployeePageParams, EmployeeVO, PageResult } from '@/types/api'

/**
 * 职工查询（公共查询 → 职工查询）。
 * 只读：复用 hr 职工主档的查询口径（后端 /api/query/employee/page 直连 EmployeeService.page），
 * 差别只在权限点（query:employee:list 而非 hr:employee:list）。
 */

const loading = ref(false)
const records = ref<EmployeeVO[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })
const query = reactive<{
  name: string
  deptId: number | null
  employType: number | null
  status: number | null
}>({ name: '', deptId: null, employType: null, status: null })

const { dictMap, load: loadDicts, label } = useDict(['sex', 'edu_level', 'employ_type'])

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
    const res = await queryEmployeePage(params)
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

// ---------- 详情 ----------
const detail = reactive<{ visible: boolean; row: EmployeeVO | null }>({ visible: false, row: null })

async function openDetail(row: EmployeeVO) {
  // 用详情接口取一次最新数据（列表里已够用，但保持与管理面一致的"查看"语义）
  const res = await queryEmployeeDetail(row.id)
  detail.row = res.data || row
  detail.visible = true
}

onMounted(async () => {
  await Promise.all([loadDicts(), loadDepts()])
  await load()
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
