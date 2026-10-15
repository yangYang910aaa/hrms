<template>
  <div class="page-container">
    <!-- 搜索栏（完整树一次拉回，过滤在本地做） -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" class="search-form" @submit.prevent>
        <el-form-item label="菜单名称">
          <el-input
            v-model="query.keyword"
            placeholder="名称 / 路由 / 权限标识"
            clearable
            style="width: 200px"
            @keyup.enter="onSearch"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 110px">
            <el-option label="目录" value="M" />
            <el-option label="菜单" value="C" />
            <el-option label="按钮" value="F" />
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

    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button v-permission="'system:menu:add'" type="primary" :icon="Plus" @click="openCreate()">
          新增菜单
        </el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <span class="tip">改动菜单或授权后，相应用户会被强制下线重登才会生效</span>
      </div>

      <el-table
        :key="treeKey"
        v-loading="loading"
        :data="rows"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
        stripe
      >
        <el-table-column label="菜单名称" prop="name" min-width="150" />
        <el-table-column label="类型" min-width="64">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" prop="sort" min-width="56" />
        <el-table-column label="路由地址" prop="path" min-width="110" show-overflow-tooltip />
        <el-table-column label="组件路径" prop="component" min-width="140" show-overflow-tooltip />
        <el-table-column label="权限标识" prop="perms" min-width="140" show-overflow-tooltip />
        <el-table-column label="图标" prop="icon" min-width="76" />
        <el-table-column label="显示" min-width="64">
          <template #default="{ row }">
            <el-tag :type="row.visible === 1 ? 'success' : 'info'" size="small">
              {{ row.visible === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="64">
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
              <!-- 按钮权限点下不能再挂任何节点，后端会拒，这里直接不给入口 -->
              <span v-if="row.type !== 'F'" v-permission="'system:menu:add'">
                <el-tooltip content="新增下级" placement="top" :show-after="150">
                  <el-button link type="primary" :icon="Plus" aria-label="新增下级" @click="openCreate(row)" />
                </el-tooltip>
              </span>
              <span v-permission="'system:menu:edit'">
                <el-tooltip content="修改" placement="top" :show-after="150">
                  <el-button link type="primary" :icon="Edit" aria-label="修改" @click="openEdit(row)" />
                </el-tooltip>
              </span>
              <span v-permission="'system:menu:remove'">
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
      :title="dialog.mode === 'create' ? '新增菜单' : '修改菜单'"
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
        <el-form-item label="上级菜单" prop="parentId">
          <el-select v-model="form.parentId" placeholder="请选择" style="width: 100%">
            <el-option label="顶级菜单" :value="0" />
            <el-option v-for="p in parentOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="F">按钮</el-radio>
          </el-radio-group>
          <div class="field-tip">目录下可挂目录 / 菜单 / 按钮；菜单下只能挂按钮；按钮下不能再挂</div>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" maxlength="50" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'F'" label="路由地址">
          <el-input v-model="form.path" maxlength="200" placeholder="目录 /system，菜单 dept" />
        </el-form-item>
        <el-form-item v-if="form.menuType === 'C'" label="组件路径">
          <el-input v-model="form.component" maxlength="255" placeholder="如 system/dept/index" />
        </el-form-item>
        <el-form-item label="权限标识" prop="perms">
          <el-input
            v-model="form.perms"
            maxlength="100"
            :placeholder="form.menuType === 'F' ? '必填，如 hr:employee:add（全表唯一）' : '如 system:dept:list'"
          />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'F'" label="图标">
          <el-input v-model="form.icon" maxlength="100" placeholder="Element Plus 图标名，如 Setting" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'F'" label="显示状态">
          <el-switch
            v-model="form.visible"
            :active-value="1"
            :inactive-value="0"
            active-text="显示"
            inactive-text="隐藏"
          />
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
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createMenu, deleteMenu, menuTree, updateMenu } from '@/api/system'
import { filterTree, flattenTree } from '@/utils/tree'
import type { MenuDTO, MenuNode } from '@/types/api'
import { confirmDelete } from '@/utils/confirm'

// ---------- 树数据 ----------
const loading = ref(false)
const tree = ref<MenuNode[]>([])

async function load() {
  loading.value = true
  try {
    const res = await menuTree()
    tree.value = res.data || []
  } finally {
    loading.value = false
  }
}

/** id → 节点，切换类型时查上级用得上 */
const nodeMap = computed(() => {
  const m = new Map<number, MenuNode>()
  const walk = (list: MenuNode[]) => {
    for (const n of list) {
      m.set(n.id, n)
      walk(n.children || [])
    }
  }
  walk(tree.value)
  return m
})

// ---------- 过滤 / 展开 ----------
const query = reactive<{ keyword: string; type: string; status: number | null }>({
  keyword: '',
  type: '',
  status: null
})
const applied = reactive<{ keyword: string; type: string; status: number | null }>({
  keyword: '',
  type: '',
  status: null
})

function onSearch() {
  applied.keyword = query.keyword.trim()
  applied.type = query.type
  applied.status = query.status
}

function onReset() {
  query.keyword = ''
  query.type = ''
  query.status = null
  onSearch()
}

const rows = computed(() =>
  filterTree(tree.value, (n) => {
    const kw = applied.keyword.toLowerCase()
    const hitKeyword =
      !kw ||
      n.name.toLowerCase().includes(kw) ||
      (n.path || '').toLowerCase().includes(kw) ||
      (n.perms || '').toLowerCase().includes(kw)
    const hitType = !applied.type || n.type === applied.type
    const hitStatus = applied.status === null || n.status === applied.status
    return hitKeyword && hitType && hitStatus
  })
)

/**
 * 默认全部展开。
 * ⚠️ EP 树表的 `expand-row-keys` 只在**挂载**时可靠生效：与数据同帧更新会被内部重置吞掉
 *    （watch 路径实测无效："已展开数=0"，dept/menu 同踩；数据渲染完的下一拍再赋值同样无效）。
 *    → 用 `default-expand-all` + `:key`：数据/过滤变化后让表格重挂载一次，挂载时全部展开即生效。
 *      树很小（dept 5 行 / menu 116 行）重挂载无感；用户随后的折叠不受影响（rows 不变就不重挂载）。
 */
const treeKey = ref(0)
watch(rows, () => {
  nextTick(() => {
    treeKey.value++
  })
})

function typeTag(type: string): 'primary' | 'success' | 'warning' {
  if (type === 'M') return 'primary'
  if (type === 'C') return 'success'
  return 'warning'
}

function typeText(type: string): string {
  if (type === 'M') return '目录'
  if (type === 'C') return '菜单'
  return '按钮'
}

// ---------- 删除 ----------
async function onDelete(row: MenuNode) {
  try {
    await confirmDelete(`确认删除【${row.name}】？该节点下还有子节点时会删除失败。`)
  } catch {
    return // 用户点了取消
  }
  await deleteMenu(row.id)
  ElMessage.success('删除成功')
  load()
}

// ---------- 新增 / 修改 ----------
const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', submitting: false })
const formRef = ref<FormInstance>()

const emptyForm = (): MenuDTO => ({
  id: null,
  parentId: 0,
  menuName: '',
  menuType: 'M',
  path: null,
  component: null,
  perms: null,
  icon: null,
  sort: 0,
  visible: 1,
  status: 1
})

const form = reactive<MenuDTO>(emptyForm())

/** 权限标识只有按钮 F 是必填的，所以规则跟着类型走 */
const rules = computed<FormRules>(() => ({
  menuName: [{ required: true, message: '菜单名称不能为空', trigger: 'blur' }],
  perms:
    form.menuType === 'F'
      ? [{ required: true, message: '按钮权限点必须填权限标识', trigger: 'blur' }]
      : []
}))

/** 当前类型允许的父级类型：非按钮只能挂在目录下，按钮可以挂在目录或菜单下 */
const allowedParentTypes = computed(() => (form.menuType === 'F' ? ['M', 'C'] : ['M']))

const parentOptions = computed(() =>
  flattenTree(tree.value, (n) => n.name, form.id).filter((o) =>
    allowedParentTypes.value.includes(o.node.type)
  )
)

/**
 * 改类型后原来的上级可能就不合法了（比如把「菜单」改成「目录」，而它的上级是个「菜单」）。
 * 不处理的话下拉里会留下一个不在选项里的值，看着像个 bug，提交还会 400。
 */
watch(
  () => form.menuType,
  (type) => {
    const parent = form.parentId ? nodeMap.value.get(form.parentId) : null
    if (parent && !allowedParentTypes.value.includes(parent.type)) {
      form.parentId = 0
      ElMessage.info('该类型不能挂在原上级下，已改为「顶级菜单」')
    }
  }
)

/** 新增下级时按父级类型给个合法默认值：目录→菜单，菜单→按钮，顶级→目录 */
function openCreate(parent?: MenuNode) {
  const menuType: MenuDTO['menuType'] = !parent ? 'M' : parent.type === 'C' ? 'F' : 'C'
  Object.assign(form, emptyForm(), { parentId: parent ? parent.id : 0, menuType })
  dialog.mode = 'create'
  dialog.visible = true
}

function openEdit(row: MenuNode) {
  Object.assign(form, emptyForm(), {
    id: row.id,
    parentId: row.parentId ?? 0,
    menuName: row.name,
    menuType: row.type,
    path: row.path,
    component: row.component,
    perms: row.perms,
    icon: row.icon,
    sort: row.sort ?? 0,
    visible: row.visible ?? 1,
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
  try {
    await formRef.value.validate()
  } catch {
    return // 校验不通过，消息由表单自己提示
  }
  dialog.submitting = true
  try {
    if (dialog.mode === 'create') {
      await createMenu(form)
      ElMessage.success('新增成功')
    } else {
      await updateMenu(form)
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
/* 骨架类（.page-container/.search-card/.search-form/.toolbar/.tip/.field-tip/.op-btns/...）已在
   styles/index.css 全局定义，这里不再各写一份副本（阶段 3 收敛）。 */
</style>
