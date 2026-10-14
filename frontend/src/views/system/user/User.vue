<template>
  <div class="user-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" class="search-form" @submit.prevent>
        <el-form-item label="登录账号">
          <el-input
            v-model="query.username"
            placeholder="模糊匹配"
            clearable
            style="width: 150px"
            @keyup.enter="onSearch"
          />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input
            v-model="query.realName"
            placeholder="模糊匹配"
            clearable
            style="width: 150px"
            @keyup.enter="onSearch"
          />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="query.deptId" placeholder="全部" clearable style="width: 170px">
            <el-option v-for="d in deptOptions" :key="d.id" :label="d.name" :value="d.id" />
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

    <el-card shadow="never">
      <div class="toolbar">
        <el-button v-permission="'system:user:add'" type="primary" :icon="Plus" @click="openCreate()">
          新增用户
        </el-button>
      </div>

      <!--
        列宽口径：min-width 是「下限」，1440 宽下 EP 会把富余空间摊给这些列，所以下限收紧不会让宽屏变挤。
        ⚠️ 下限不能贴内容宽：EP 的 .el-table .cell 自身还有 ~25px 横向内边距，写太窄会折行
        （实测把姓名/操作按钮折成两行）。下面这些值是「内容 + 25」量出来的不折行下限。
      -->
      <el-table :data="records" v-loading="loading" border>
        <el-table-column label="登录账号" min-width="110">
          <template #default="{ row }">
            {{ row.username }}
            <el-tag v-if="isSelf(row)" size="small" type="warning">本人</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="姓名" prop="realName" min-width="100" />
        <!-- 后端只回 deptId，部门名靠前端用部门树翻译 -->
        <el-table-column label="部门" min-width="94" show-overflow-tooltip>
          <template #default="{ row }">{{ deptName(row.deptId) }}</template>
        </el-table-column>
        <el-table-column label="手机" prop="phone" min-width="112" />
        <el-table-column label="邮箱" prop="email" min-width="120" show-overflow-tooltip />
        <!--
          ⚠️ 这一列必须是「固定 width」且 ≥148：
          开关本体带 active-text/inactive-text 实测 116px（停用 28 + 10 + 开关 40 + 10 + 正常 28）；
          而 EP 的 .el-table .cell 自带 padding: 0 12px（左右 24px）+ 1px 边框 → 列宽 = 内容 + 25。
          之前给 90px 时可用宽只有 65px：两个文字标签被挤成竖排、开关溢出单元格；
          给 140px 时可用 115px，比 116px 少 1px → 单元格判溢出，末尾画出省略号「正常…」。
        -->
        <el-table-column label="状态" width="148">
          <template #default="{ row }">
            <!--
              before-change：先问后端再翻转，失败就保持原状。
              不能直接 v-model 绑 row.status —— 请求还没回来视图就变了，失败了对不上。
            -->
            <el-switch
              :model-value="row.status === 1"
              :disabled="isSelf(row)"
              :before-change="() => beforeStatusChange(row)"
              active-text="正常"
              inactive-text="停用"
            />
          </template>
        </el-table-column>
        <el-table-column label="最后登录" min-width="170">
          <template #default="{ row }">{{ formatTime(row.lastLoginTime) }}</template>
        </el-table-column>
        <!--
          操作列：图标化 + tooltip（阶段 3 目标「操作列图标化」）。
          文字按钮 4 个实测占 239px，图标化后只剩 104px（4×20 按钮 + 3×8 间隙）→ 列宽 136
          （104 + 单元格 25），省下 124px，右侧列因此不再被 fixed 列压住。
          ⚠️ `v-permission` 是**从 DOM 移除**元素，所以不能把带指令的按钮直接放进 `el-tooltip`
          （会留下一个空触发器）→ 用 `<span v-permission>` 把整个 tooltip 包住。
        -->
        <el-table-column label="操作" width="136" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <span v-permission="'system:user:edit'">
                <el-tooltip content="修改" placement="top" :show-after="150">
                  <el-button link type="primary" :icon="Edit" aria-label="修改" @click="openEdit(row)" />
                </el-tooltip>
              </span>
              <span v-permission="'system:user:assign'">
                <el-tooltip content="分配角色" placement="top" :show-after="150">
                  <el-button
                    link
                    type="primary"
                    :icon="UserFilled"
                    aria-label="分配角色"
                    @click="openAssign(row)"
                  />
                </el-tooltip>
              </span>
              <span v-permission="'system:user:resetPwd'">
                <el-tooltip content="重置密码" placement="top" :show-after="150">
                  <el-button
                    link
                    type="primary"
                    :icon="Key"
                    aria-label="重置密码"
                    @click="openResetPwd(row)"
                  />
                </el-tooltip>
              </span>
              <span v-permission="'system:user:remove'">
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
      :title="dialog.mode === 'create' ? '新增用户' : '修改用户'"
      width="600px"
      top="6vh"
      @closed="resetDialog"
    >
      <el-form
        ref="formRef"
        v-loading="dialog.submitting"
        :model="form"
        :rules="rules"
        label-width="88px"
      >
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="登录账号" prop="username">
              <el-input
                v-model="form.username"
                maxlength="50"
                :disabled="dialog.mode === 'edit'"
                placeholder="新增后不可修改"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="form.realName" maxlength="30" />
            </el-form-item>
          </el-col>
          <!-- 口令只有新增能设；改口令走「重置密码」，那条路才会踢下线 -->
          <el-col v-if="dialog.mode === 'create'" :span="12">
            <el-form-item label="初始口令" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                maxlength="100"
                show-password
                placeholder="6-100 位"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属部门">
              <el-select v-model="form.deptId" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="d in deptOptions" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
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
          <!-- 新增时才能顺手分角色；改角色走「分配角色」（带踢下线），后端会忽略这里的 roleIds -->
          <el-col v-if="dialog.mode === 'create'" :span="24">
            <el-form-item label="角色">
              <el-select
                v-model="form.roleIds"
                multiple
                placeholder="可多选，稍后也能在列表里改"
                style="width: 100%"
              >
                <el-option
                  v-for="r in roleOptions"
                  :key="r.id"
                  :label="`${r.roleName}（${r.roleCode}）`"
                  :value="r.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="dialog.mode === 'create'" :span="12">
            <el-form-item label="状态">
              <el-switch
                v-model="form.status"
                :active-value="1"
                :inactive-value="0"
                active-text="正常"
                inactive-text="停用"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="onSubmit">保 存</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色弹窗 -->
    <el-dialog v-model="roleDialog.visible" :title="`分配角色 · ${roleDialog.username}`" width="460px">
      <div v-loading="roleDialog.loading">
        <el-select
          v-model="roleDialog.checked"
          multiple
          placeholder="请选择角色"
          style="width: 100%"
        >
          <el-option
            v-for="r in roleOptions"
            :key="r.id"
            :label="`${r.roleName}（${r.roleCode}）`"
            :value="r.id"
          />
        </el-select>
        <div class="field-tip mt-3">保存后该用户会被强制下线，重新登录后新权限生效</div>
      </div>
      <template #footer>
        <el-button @click="roleDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="roleDialog.submitting" @click="onSubmitRoles">
          保 存
        </el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="pwdDialog.visible" :title="`重置密码 · ${pwdDialog.username}`" width="420px">
      <el-form ref="pwdFormRef" :model="pwdDialog" :rules="pwdRules" label-width="88px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="pwdDialog.newPassword"
            type="password"
            maxlength="100"
            show-password
            placeholder="6-100 位"
          />
        </el-form-item>
      </el-form>
      <div class="field-tip mt-3">保存后该用户会被强制下线，需用新密码重新登录</div>
      <template #footer>
        <el-button @click="pwdDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="pwdDialog.submitting" @click="onSubmitPwd">
          保 存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Key, Plus, Refresh, Search, UserFilled } from '@element-plus/icons-vue'
import {
  assignUserRoles,
  changeUserStatus,
  createUser,
  deleteUser,
  resetUserPwd,
  roleList,
  updateUser,
  userPage,
  userRoleIds
} from '@/api/system'
import { deptTree as publicDeptTree } from '@/api/common'
import { useUserStore } from '@/store/user'
import { flattenTree } from '@/utils/tree'
import type { DeptNode, PageResult, RoleVO, UserDTO, UserPageParams, UserVO } from '@/types/api'

// ---------- 下拉数据 ----------
const deptTreeData = ref<DeptNode[]>([])
const roleOptions = ref<RoleVO[]>([])

const deptOptions = computed(() => flattenTree(deptTreeData.value, (n) => n.deptName))

/** 后端只回 deptId，列表要显示部门名只能自己翻译 */
const deptNameMap = computed(() => {
  const m = new Map<number, string>()
  for (const o of deptOptions.value) {
    m.set(o.id, o.node.deptName)
  }
  return m
})

function deptName(deptId: number | null): string {
  if (deptId === null || deptId === undefined) return '—'
  return deptNameMap.value.get(deptId) || '—'
}

async function loadOptions() {
  const [dept, roles] = await Promise.all([publicDeptTree(), roleList()])
  deptTreeData.value = dept.data || []
  roleOptions.value = roles.data || []
}

// ---------- 列表 ----------
const loading = ref(false)
const records = ref<UserVO[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })
const query = reactive<{
  username: string
  realName: string
  deptId: number | null
  status: number | null
}>({ username: '', realName: '', deptId: null, status: null })

async function load() {
  loading.value = true
  try {
    const params: UserPageParams = {
      current: page.current,
      size: page.size,
      username: query.username.trim() || undefined,
      realName: query.realName.trim() || undefined,
      deptId: query.deptId,
      status: query.status
    }
    const res = await userPage(params)
    const data = (res.data || {}) as PageResult<UserVO>
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
  query.username = ''
  query.realName = ''
  query.deptId = null
  query.status = null
  onSearch()
}

/** 当前登录账号自己：停用/改角色都会被后端的防自锁拦成 403，这里直接不给开关 */
function isSelf(row: UserVO): boolean {
  return useUserStore().info?.id === row.id
}

function formatTime(value: string | null): string {
  return value ? value.replace('T', ' ') : '—'
}

// ---------- 启停 ----------
async function beforeStatusChange(row: UserVO): Promise<boolean> {
  const next = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      `确认${next === 1 ? '启用' : '停用'}账号【${row.username}】？停用后该用户会被强制下线。`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return false // 取消：开关保持原状
  }
  try {
    await changeUserStatus(row.id, next)
    ElMessage.success(next === 1 ? '已启用' : '已停用，该用户已被强制下线')
    load()
    return true
  } catch {
    return false // 失败（含防自锁 403）：开关保持原状，错误消息已由拦截器弹出
  }
}

// ---------- 删除 ----------
async function onDelete(row: UserVO) {
  try {
    await ElMessageBox.confirm(`确认删除用户【${row.username}】？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && page.current > 1) {
    page.current -= 1
  }
  load()
}

// ---------- 新增 / 修改 ----------
const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', submitting: false })
const formRef = ref<FormInstance>()

const emptyForm = (): UserDTO => ({
  id: null,
  username: '',
  password: null,
  realName: '',
  deptId: null,
  phone: null,
  email: null,
  avatar: null,
  status: 1,
  roleIds: []
})

const form = reactive<UserDTO>(emptyForm())

const rules: FormRules = {
  username: [{ required: true, message: '登录账号不能为空', trigger: 'blur' }],
  realName: [{ required: true, message: '姓名不能为空', trigger: 'blur' }],
  password: [
    { required: true, message: '新增用户必须设置初始口令', trigger: 'blur' },
    { min: 6, max: 100, message: '长度需在 6-100 之间', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialog.mode = 'create'
  dialog.visible = true
}

function openEdit(row: UserVO) {
  // 刻意不带 password / status / roleIds：后端修改时会忽略这三项，
  // 带上只会让人以为改成功了（改它们有各自的专用接口）。
  Object.assign(form, emptyForm(), {
    id: row.id,
    username: row.username,
    realName: row.realName,
    deptId: row.deptId,
    phone: row.phone,
    email: row.email,
    // 后端无条件 set avatar，不带原值就等于把头像清掉
    avatar: row.avatar
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
      await createUser(form)
      ElMessage.success('新增成功')
    } else {
      await updateUser(form)
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

// ---------- 分配角色 ----------
const roleDialog = reactive({
  visible: false,
  loading: false,
  submitting: false,
  userId: 0,
  username: '',
  checked: [] as number[]
})

async function openAssign(row: UserVO) {
  roleDialog.userId = row.id
  roleDialog.username = row.username
  roleDialog.submitting = false
  roleDialog.checked = []
  roleDialog.visible = true
  roleDialog.loading = true
  try {
    const res = await userRoleIds(row.id)
    roleDialog.checked = res.data || []
  } finally {
    roleDialog.loading = false
  }
}

async function onSubmitRoles() {
  roleDialog.submitting = true
  try {
    await assignUserRoles(roleDialog.userId, roleDialog.checked)
    ElMessage.success('角色已更新，该用户需重新登录')
    roleDialog.visible = false
  } catch {
    // 失败消息（含防自锁 403）由 request 拦截器统一弹出
  } finally {
    roleDialog.submitting = false
  }
}

// ---------- 重置密码 ----------
const pwdDialog = reactive({
  visible: false,
  submitting: false,
  userId: 0,
  username: '',
  newPassword: ''
})
const pwdFormRef = ref<FormInstance>()
const pwdRules: FormRules = {
  newPassword: [
    { required: true, message: '新密码不能为空', trigger: 'blur' },
    { min: 6, max: 100, message: '长度需在 6-100 之间', trigger: 'blur' }
  ]
}

function openResetPwd(row: UserVO) {
  pwdDialog.userId = row.id
  pwdDialog.username = row.username
  pwdDialog.newPassword = ''
  pwdDialog.submitting = false
  pwdDialog.visible = true
}

async function onSubmitPwd() {
  if (!pwdFormRef.value) return
  try {
    await pwdFormRef.value.validate()
  } catch {
    return
  }
  pwdDialog.submitting = true
  try {
    await resetUserPwd(pwdDialog.userId, pwdDialog.newPassword)
    ElMessage.success('口令已重置，该用户需重新登录')
    pwdDialog.visible = false
  } catch {
    // 失败消息由 request 拦截器统一弹出
  } finally {
    pwdDialog.submitting = false
  }
}

onMounted(async () => {
  await loadOptions()
  load()
})
</script>

<style scoped>
/* 骨架类（.page-container/.search-card/.toolbar/.pagination/.op-btns/...）已在
   styles/index.css 全局定义，这里不再各写一份副本（阶段 3 收敛）。 */
</style>
