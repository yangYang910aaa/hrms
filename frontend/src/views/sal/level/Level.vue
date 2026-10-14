<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" class="search-form" @submit.prevent>
        <el-form-item label="级别名称">
          <el-input
            v-model="query.levelName"
            placeholder="模糊匹配"
            clearable
            style="width: 170px"
            @keyup.enter="onSearch"
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
        <el-button v-permission="'sal:level:add'" type="primary" :icon="Plus" @click="openCreate">
          新增级别
        </el-button>
        <span class="tip">
          基本工资是加班时薪的计算基数（时薪 = 基本工资 ÷ 21.75 ÷ 8）；被加班标准或在册职工引用的级别不能删除
        </span>
      </div>

      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column label="级别编号" prop="levelNo" width="120" />
        <el-table-column label="级别名称" prop="levelName" min-width="160" show-overflow-tooltip />
        <el-table-column label="基本工资" width="130" align="right">
          <template #default="{ row }">{{ money(row.baseSalary) }}</template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="76" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <span v-permission="'sal:level:edit'">
                <el-tooltip content="修改" placement="top" :show-after="150">
                  <el-button link type="primary" :icon="Edit" aria-label="修改" @click="openEdit(row)" />
                </el-tooltip>
              </span>
              <span v-permission="'sal:level:remove'">
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
      :title="dialog.mode === 'create' ? '新增薪酬级别' : '修改薪酬级别'"
      width="560px"
      top="6vh"
      @closed="resetDialog"
    >
      <el-form ref="formRef" v-loading="dialog.submitting" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="级别名称" prop="levelName">
              <el-input v-model="form.levelName" maxlength="50" placeholder="如：一级（初级）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="级别编号">
              <el-input v-model="form.levelNo" maxlength="30" placeholder="唯一，如 L1" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="基本工资">
              <el-input-number
                v-model="form.baseSalary"
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
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createLevel, deleteLevel, levelPage, updateLevel } from '@/api/sal'
import { money } from '@/utils/format'
import type { PageResult, SalLevel, SalLevelDTO, SalLevelPageParams } from '@/types/api'
import { confirmDelete } from '@/utils/confirm'

// ---------- 列表 ----------
const loading = ref(false)
const records = ref<SalLevel[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })
const query = reactive<{ levelName: string }>({ levelName: '' })

async function load() {
  loading.value = true
  try {
    const params: SalLevelPageParams = {
      current: page.current,
      size: page.size,
      levelName: query.levelName.trim() || undefined
    }
    const res = await levelPage(params)
    const data = (res.data || {}) as PageResult<SalLevel>
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
  query.levelName = ''
  onSearch()
}

// ---------- 删除 ----------
async function onDelete(row: SalLevel) {
  try {
    await confirmDelete(`确认删除级别【${row.levelName}】？被加班标准或在册职工引用时后端会拒绝。`)
  } catch {
    return
  }
  await deleteLevel(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && page.current > 1) {
    page.current -= 1
  }
  load()
}

// ---------- 新增 / 修改 ----------
const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', submitting: false })
const formRef = ref<FormInstance>()

const emptyForm = (): SalLevelDTO => ({
  id: null,
  levelName: '',
  levelNo: null,
  baseSalary: null,
  remark: null
})

const form = reactive<SalLevelDTO>(emptyForm())

const rules: FormRules = {
  levelName: [{ required: true, message: '级别名称不能为空', trigger: 'blur' }]
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialog.mode = 'create'
  dialog.visible = true
}

function openEdit(row: SalLevel) {
  Object.assign(form, emptyForm(), {
    id: row.id,
    levelName: row.levelName,
    levelNo: row.levelNo,
    baseSalary: row.baseSalary,
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
      await createLevel(form)
      ElMessage.success('新增成功')
    } else {
      await updateLevel(form)
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
/* 骨架类（.page-container/.search-card/.search-form/.toolbar/.tip/.field-tip/.pagination/.op-btns/...）
   已在 styles/index.css 全局定义，这里不再各写一份副本（阶段 3 收敛）。 */
</style>
