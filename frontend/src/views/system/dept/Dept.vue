<template>
  <div class="page-container">
    <!-- 搜索栏（树是全量拉回来的，过滤在本地做） -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" class="search-form" @submit.prevent>
        <el-form-item label="部门">
          <el-input
            v-model="query.keyword"
            placeholder="名称或编码"
            clearable
            style="width: 180px"
            @keyup.enter="onSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
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

    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button v-permission="'system:dept:add'" type="primary" :icon="Plus" @click="openCreate()">
          新增部门
        </el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        :tree-props="{ children: 'children' }"
        :expand-row-keys="expandKeys"
        stripe
      >
        <el-table-column label="部门名称" prop="deptName" min-width="180" />
        <el-table-column label="编码" prop="deptCode" width="120" />
        <el-table-column label="负责人" prop="leader" width="110" />
        <el-table-column label="电话" prop="phone" width="130" />
        <el-table-column label="排序" prop="sort" width="70" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 操作列：图标化 + tooltip（见 styles/index.css 的 .op-btns 与 交接文档 §9） -->
        <el-table-column label="操作" width="104" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <span v-permission="'system:dept:add'">
                <el-tooltip content="新增下级" placement="top" :show-after="150">
                  <el-button link type="primary" :icon="Plus" aria-label="新增下级" @click="openCreate(row)" />
                </el-tooltip>
              </span>
              <span v-permission="'system:dept:edit'">
                <el-tooltip content="修改" placement="top" :show-after="150">
                  <el-button link type="primary" :icon="Edit" aria-label="修改" @click="openEdit(row)" />
                </el-tooltip>
              </span>
              <span v-permission="'system:dept:remove'">
                <el-tooltip content="删除" placement="top" :show-after="150">
                  <el-button link type="danger" :icon="Delete" aria-label="删除" @click="onDelete(row)" />
                </el-tooltip>
              </span>
            </div>
          </template>
        </el-table-column>
        <template #empty>暂无数据</template>
      </el-table>
    </el-card>

    <!-- 新增 / 修改弹窗 -->
    <el-dialog
      v-model="dialog.visible"
      :title="dialog.mode === 'create' ? '新增部门' : '修改部门'"
      width="560px"
      top="8vh"
      @closed="resetDialog"
    >
      <el-form
        ref="formRef"
        v-loading="dialog.submitting"
        :model="form"
        :rules="rules"
        label-width="88px"
      >
        <el-form-item label="上级部门" prop="parentId">
          <el-select v-model="form.parentId" placeholder="请选择" style="width: 100%">
            <el-option label="顶级部门" :value="0" />
            <el-option v-for="p in parentOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="form.deptName" maxlength="50" />
        </el-form-item>
        <el-form-item label="部门编码">
          <el-input v-model="form.deptCode" maxlength="50" placeholder="唯一，删除后可重新录入" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.leader" maxlength="30" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="正常"
            inactive-text="停用"
          />
        </el-form-item>
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
import { createDept, deleteDept, deptTree, updateDept } from '@/api/system'
import { collectBranchIds, filterTree, flattenTree } from '@/utils/tree'
import type { DeptDTO, DeptNode } from '@/types/api'

// ---------- 树数据 ----------
const loading = ref(false)
const tree = ref<DeptNode[]>([])

async function load() {
  loading.value = true
  try {
    const res = await deptTree()
    tree.value = res.data || []
  } finally {
    loading.value = false
  }
}

// ---------- 过滤（本地；/tree 不接受查询参数） ----------
const query = reactive<{ keyword: string; status: number | null }>({ keyword: '', status: null })
/** 查询时才把 query 落到这里，避免边输边筛把树抖来抖去 */
const applied = reactive<{ keyword: string; status: number | null }>({ keyword: '', status: null })

function onSearch() {
  applied.keyword = query.keyword.trim()
  applied.status = query.status
}

function onReset() {
  query.keyword = ''
  query.status = null
  onSearch()
}

const rows = computed(() =>
  filterTree(tree.value, (n) => {
    const hitKeyword =
      !applied.keyword ||
      n.deptName.includes(applied.keyword) ||
      (n.deptCode || '').toUpperCase().includes(applied.keyword.toUpperCase())
    const hitStatus = applied.status === null || n.status === applied.status
    return hitKeyword && hitStatus
  })
)

/** 默认全部展开（受控展开键，见 utils/tree 的说明） */
const expandKeys = computed(() => collectBranchIds(rows.value))

// ---------- 删除 ----------
async function onDelete(row: DeptNode) {
  try {
    await ElMessageBox.confirm(`确认删除部门【${row.deptName}】？`, '提示', { type: 'warning' })
  } catch {
    return // 用户点了取消：confirm 取消是抛异常，不接住就是一条未处理的 rejection
  }
  await deleteDept(row.id)
  ElMessage.success('删除成功')
  load()
}

// ---------- 新增 / 修改 ----------
const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', submitting: false })
const formRef = ref<FormInstance>()

const emptyForm = (): DeptDTO => ({
  id: null,
  parentId: 0,
  deptName: '',
  deptCode: null,
  leader: null,
  phone: null,
  sort: 0,
  status: 1
})

const form = reactive<DeptDTO>(emptyForm())

const rules: FormRules = {
  deptName: [{ required: true, message: '部门名称不能为空', trigger: 'blur' }]
}

/** 上级部门下拉：拍平树并排除「自己 + 自己的整棵子树」（见 utils/tree 的 flattenTree） */
const parentOptions = computed(() => flattenTree(tree.value, (n) => n.deptName, form.id))

/** parentId 是必须的（后端 @NotNull），清空下拉会变 null，兜回 0（顶级） */
function openCreate(parent?: DeptNode) {
  Object.assign(form, emptyForm(), { parentId: parent ? parent.id : 0 })
  dialog.mode = 'create'
  dialog.visible = true
}

function openEdit(row: DeptNode) {
  Object.assign(form, emptyForm(), {
    id: row.id,
    parentId: row.parentId ?? 0,
    deptName: row.deptName,
    deptCode: row.deptCode,
    leader: row.leader,
    phone: row.phone,
    sort: row.sort ?? 0,
    status: row.status ?? 1
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
  // Promise 形式：validate(callback) 里的异步错误不在 await 链上，会漏成未处理的 rejection
  try {
    await formRef.value.validate()
  } catch {
    return // 校验不通过，消息由表单自己提示
  }
  dialog.submitting = true
  try {
    if (dialog.mode === 'create') {
      await createDept(form)
      ElMessage.success('新增成功')
    } else {
      await updateDept(form)
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
/* 骨架类（.page-container/.search-card/.search-form/.toolbar/.op-btns/...）已在
   styles/index.css 全局定义，这里不再各写一份副本（阶段 3 收敛）。 */
</style>
