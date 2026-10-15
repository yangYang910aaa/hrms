<template>
  <div class="page-container">
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
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <span class="tip">公共查询为只读视图：职工资料的新增/修改请在「人事管理 → 职工管理」进行</span>
      </div>

      <el-table :data="records" v-loading="loading" stripe>
        <!--
          列宽口径「列宽 = 内容 + 25px」（EP 的 .el-table .cell 自带 padding: 0 12px + 1px 边框）。
          12 列原来合计 ≈1330px > 1440 下容器可用宽 → 横向溢出、fixed 操作列压住「状态」。
          姓名/部门/岗位/学历 用 min-width（下限，宽屏 EP 会把富余摊给它们，不会右侧留白）。
        -->
        <el-table-column label="工号" prop="empNo" min-width="84" />
        <el-table-column label="姓名" prop="name" min-width="96" show-overflow-tooltip />
        <el-table-column label="性别" min-width="60">
          <template #default="{ row }">{{ label('sex', row.gender) }}</template>
        </el-table-column>
        <el-table-column label="年龄" min-width="52" align="right">
          <template #default="{ row }">{{ row.age ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="部门" prop="deptName" min-width="96" show-overflow-tooltip />
        <el-table-column label="岗位" prop="position" min-width="88" show-overflow-tooltip />
        <el-table-column label="学历" min-width="84" show-overflow-tooltip>
          <template #default="{ row }">{{ label('edu_level', row.eduLevel) }}</template>
        </el-table-column>
        <el-table-column label="入职日期" prop="hireDate" min-width="104" />
        <el-table-column label="在职状态" min-width="80">
          <template #default="{ row }">
            <el-tag :type="row.employType === 3 ? 'info' : 'success'" size="small">
              {{ label('employ_type', row.employType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="手机" prop="phone" min-width="104" />
        <el-table-column label="状态" min-width="72">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 操作列图标化（阶段 3）：单动作「查看」，列宽 56（内容 20 + 单元格 25 + 余量） -->
        <el-table-column label="操作" width="56" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <el-tooltip content="查看" placement="top" :show-after="150">
                <span class="op-icon">
                  <el-button link type="primary" :icon="View" aria-label="查看" @click="openDetail(row)" />
                </span>
              </el-tooltip>
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
import { Refresh, Search, View } from '@element-plus/icons-vue'
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
/* 本页骨架类已全局（.page-container/.search-card/.toolbar/.tip/.pagination），无特有样式 */
</style>
