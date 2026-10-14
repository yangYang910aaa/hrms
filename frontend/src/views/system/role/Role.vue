<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" class="search-form" @submit.prevent>
        <el-form-item label="角色名称">
          <el-input
            v-model="query.roleName"
            placeholder="模糊匹配"
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
        <el-button v-permission="'system:role:add'" type="primary" :icon="Plus" @click="openCreate()">
          新增角色
        </el-button>
      </div>

      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column label="角色名称" prop="roleName" min-width="150" />
        <el-table-column label="角色标识" prop="roleCode" min-width="150" />
        <el-table-column label="排序" prop="sort" width="70" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
        <!-- 操作列：图标化 + tooltip（见 styles/index.css 的 .op-btns 与 交接文档 §9） -->
        <el-table-column label="操作" width="104" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <span v-permission="'system:role:edit'">
                <el-tooltip content="修改" placement="top" :show-after="150">
                  <el-button link type="primary" :icon="Edit" aria-label="修改" @click="openEdit(row)" />
                </el-tooltip>
              </span>
              <span v-permission="'system:role:assign'">
                <el-tooltip content="分配菜单" placement="top" :show-after="150">
                  <el-button link type="primary" :icon="UserFilled" aria-label="分配菜单" @click="openAssign(row)" />
                </el-tooltip>
              </span>
              <span v-permission="'system:role:remove'">
                <el-tooltip
                  :content="isBuiltin(row) ? '内置超级管理员不可删除' : '删除'"
                  placement="top"
                  :show-after="150"
                >
                  <el-button
                    link
                    type="danger"
                    :icon="Delete"
                    aria-label="删除"
                    :disabled="isBuiltin(row)"
                    @click="onDelete(row)"
                  />
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
      :title="dialog.mode === 'create' ? '新增角色' : '修改角色'"
      width="520px"
      top="10vh"
      @closed="resetDialog"
    >
      <el-form
        ref="formRef"
        v-loading="dialog.submitting"
        :model="form"
        :rules="rules"
        label-width="88px"
      >
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" maxlength="30" />
        </el-form-item>
        <el-form-item label="角色标识" prop="roleCode">
          <el-input
            v-model="form.roleCode"
            maxlength="50"
            placeholder="字母、数字、下划线"
            :disabled="form.roleCode === 'SUPER_ADMIN'"
          />
          <div class="field-tip">它是权限判断的依据，改动后相关用户需重新登录；内置超级管理员不可改</div>
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
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="onSubmit">保 存</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单弹窗 -->
    <el-dialog
      v-model="menuDialog.visible"
      :title="`分配菜单 · ${menuDialog.roleName}`"
      width="480px"
      top="6vh"
    >
      <div v-loading="menuDialog.loading" class="auth-panel">
        <div class="tree-toolbar">
          <el-button link type="primary" @click="checkAll">全选</el-button>
          <el-button link type="primary" @click="checkNone">清空</el-button>
          <span class="tip">含按钮权限点（F）</span>
        </div>
        <!--
          v-if + :key 让树每次打开都重新挂载：
          default-checked-keys / default-expanded-keys 只在挂载时生效，
          不重挂的话第二次打开回显的还是上一个角色的勾选。
        -->
        <el-tree
          v-if="menuDialog.ready"
          ref="treeRef"
          :key="menuDialog.openSeq"
          :data="menuTreeData"
          show-checkbox
          node-key="id"
          :props="treeProps"
          :default-expanded-keys="branchIds"
          :default-checked-keys="checkedIds"
          class="auth-tree"
        />
      </div>

      <template #footer>
        <el-button @click="menuDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="menuDialog.submitting" @click="onSubmitMenus">
          保 存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import type { TreeInstance } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search, UserFilled } from '@element-plus/icons-vue'
import {
  assignRoleMenus,
  createRole,
  deleteRole,
  menuTree,
  roleMenuIds,
  rolePage,
  updateRole
} from '@/api/system'
import { useUserStore } from '@/store/user'
import { collectBranchIds, flattenTree } from '@/utils/tree'
import type { MenuNode, PageResult, RoleDTO, RolePageParams, RoleVO } from '@/types/api'

// ---------- 列表（服务端分页） ----------
const loading = ref(false)
const records = ref<RoleVO[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })
const query = reactive<{ roleName: string; status: number | null }>({ roleName: '', status: null })

async function load() {
  loading.value = true
  try {
    const params: RolePageParams = {
      current: page.current,
      size: page.size,
      roleName: query.roleName.trim() || undefined,
      status: query.status
    }
    const res = await rolePage(params)
    const data = (res.data || {}) as PageResult<RoleVO>
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
  query.roleName = ''
  query.status = null
  onSearch()
}

/** 内置超级管理员：后端禁止停用与删除，前端直接不给入口，省得提交一次才看到报错 */
function isBuiltin(row: RoleVO): boolean {
  return row.roleCode === 'SUPER_ADMIN'
}

// ---------- 删除 ----------
async function onDelete(row: RoleVO) {
  try {
    await ElMessageBox.confirm(`确认删除角色【${row.roleName}】？`, '提示', { type: 'warning' })
  } catch {
    return // 用户点了取消
  }
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && page.current > 1) {
    page.current -= 1
  }
  load()
}

// ---------- 新增 / 修改 ----------
const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', submitting: false })
const formRef = ref<FormInstance>()

const emptyForm = (): RoleDTO => ({
  id: null,
  roleName: '',
  roleCode: '',
  sort: 0,
  status: 1,
  remark: null
})

const form = reactive<RoleDTO>(emptyForm())

const rules: FormRules = {
  roleName: [{ required: true, message: '角色名称不能为空', trigger: 'blur' }],
  roleCode: [
    { required: true, message: '角色标识不能为空', trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9_]+$/,
      message: '只能包含字母、数字和下划线',
      trigger: 'blur'
    }
  ]
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialog.mode = 'create'
  dialog.visible = true
}

function openEdit(row: RoleVO) {
  Object.assign(form, emptyForm(), {
    id: row.id,
    roleName: row.roleName,
    roleCode: row.roleCode,
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
  dialog.submitting = true
  try {
    if (dialog.mode === 'create') {
      await createRole(form)
      ElMessage.success('新增成功')
    } else {
      await updateRole(form)
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

// ---------- 分配菜单 ----------
const menuDialog = reactive({
  visible: false,
  loading: false,
  submitting: false,
  ready: false,
  openSeq: 0,
  roleId: 0,
  roleName: '',
  roleCode: ''
})

const treeRef = ref<TreeInstance>()
const menuTreeData = ref<MenuNode[]>([])
const checkedIds = ref<number[]>([])
const treeProps = { label: 'name', children: 'children' }

const branchIds = computed(() => collectBranchIds(menuTreeData.value))
/** 全选要用到的全部节点 id（含目录、菜单、按钮） */
const allIds = computed(() => flattenTree(menuTreeData.value, (n) => n.name).map((o) => o.id))

async function openAssign(row: RoleVO) {
  menuDialog.roleId = row.id
  menuDialog.roleName = row.roleName
  menuDialog.roleCode = row.roleCode
  menuDialog.submitting = false
  menuDialog.ready = false
  menuDialog.visible = true
  menuDialog.loading = true
  try {
    const [tree, ids] = await Promise.all([menuTree(), roleMenuIds(row.id)])
    menuTreeData.value = tree.data || []
    checkedIds.value = ids.data || []
    // 数据齐了再挂载树，并换 key 强制重建（见模板里的说明）
    menuDialog.openSeq += 1
    menuDialog.ready = true
  } finally {
    menuDialog.loading = false
  }
}

function checkAll() {
  treeRef.value?.setCheckedKeys(allIds.value, false)
}

function checkNone() {
  treeRef.value?.setCheckedKeys([], false)
}

async function onSubmitMenus() {
  if (!treeRef.value) return
  // getCheckedKeys 只给「全选」的节点，半选（部分子节点被勾）的父节点在 getHalfCheckedKeys 里。
  // 后端不会自动补父节点，少了父节点的话菜单会被上提成根，侧边栏层级就乱了 —— 所以两个都要提交。
  const checked = (treeRef.value.getCheckedKeys() || []) as number[]
  const half = (treeRef.value.getHalfCheckedKeys() || []) as number[]
  const ids = Array.from(new Set([...checked, ...half]))

  menuDialog.submitting = true
  try {
    await assignRoleMenus(menuDialog.roleId, ids)
    // 后端会踢掉该角色下其他已登录用户，但**排除操作者本人** ——
    // 所以改到自己所属角色时，自己的菜单树不会自动刷新，得提醒一句。
    const mine = useUserStore().roles.includes(menuDialog.roleCode)
    ElMessage.success(
      mine
        ? `已保存 ${ids.length} 项授权；你本人属于该角色，刷新页面后菜单才会更新`
        : `已保存 ${ids.length} 项授权，该角色下其他已登录用户已被强制下线`
    )
    menuDialog.visible = false
  } catch {
    // 失败消息由 request 拦截器统一弹出
  } finally {
    menuDialog.submitting = false
  }
}

onMounted(load)
</script>

<style scoped>
/* 骨架类（.page-container/.search-card/.search-form/.toolbar/.tip/.field-tip/.pagination/.op-btns/...）
   已在 styles/index.css 全局定义，这里不再各写一份副本（阶段 3 收敛）。只留本页特有的。 */
.auth-panel {
  max-height: 52vh;
  overflow: auto;
}

.tree-toolbar {
  display: flex;
  align-items: center;
  gap: var(--hr-space-2);
  margin-bottom: var(--hr-space-2);
}

.auth-tree {
  margin-top: var(--hr-space-1);
}
</style>
