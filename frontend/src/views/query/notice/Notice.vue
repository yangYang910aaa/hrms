<template>
  <div class="query-notice-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" class="search-form" @submit.prevent>
        <el-form-item label="标题">
          <el-input
            v-model="query.title"
            placeholder="模糊匹配"
            clearable
            style="width: 200px"
            @keyup.enter="onSearch"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.noticeType" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="o in TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
    <el-card shadow="never">
      <div class="toolbar">
        <el-button v-permission="'query:notice:add'" type="primary" :icon="Plus" @click="openCreate">
          新增通知
        </el-button>
        <span class="tip">状态流转：草稿 → 发布 → 已发布 → 下线 → 已下线；已发布不可直接改/删</span>
      </div>

      <el-table :data="records" v-loading="loading" border>
        <el-table-column label="标题" prop="title" min-width="220" show-overflow-tooltip />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.noticeType === 2 ? 'warning' : 'primary'" size="small" effect="plain">
              {{ typeLabel(row.noticeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布人" prop="publisher" width="110" />
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.publishTime) }}</template>
        </el-table-column>
        <el-table-column label="打印次数" width="90" align="right">
          <template #default="{ row }">{{ row.printCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="操作" width="270" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            <el-button
              v-if="row.status !== 1"
              v-permission="'query:notice:publish'"
              link
              type="success"
              @click="onPublish(row)"
            >
              发布
            </el-button>
            <el-button
              v-else
              v-permission="'query:notice:publish'"
              link
              type="warning"
              @click="onOffline(row)"
            >
              下线
            </el-button>
            <el-button
              v-permission="'query:notice:edit'"
              link
              type="primary"
              :disabled="row.status === 1"
              @click="openEdit(row)"
            >
              修改
            </el-button>
            <el-button
              v-permission="'query:notice:remove'"
              link
              type="danger"
              :disabled="row.status === 1"
              @click="onDelete(row)"
            >
              删除
            </el-button>
            <el-button link @click="onPrint(row)">打印</el-button>
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
      :title="dialog.mode === 'create' ? '新增通知' : '修改通知'"
      width="720px"
      top="6vh"
      @closed="resetDialog"
    >
      <el-form ref="formRef" v-loading="dialog.submitting" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="通知标题" />
        </el-form-item>
        <el-form-item label="类型" prop="noticeType">
          <el-select v-model="form.noticeType" placeholder="请选择" style="width: 180px">
            <el-option v-for="o in TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="通知内容" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="onSubmit">保 存</el-button>
      </template>
    </el-dialog>

    <!-- 查看弹窗 -->
    <el-dialog v-model="detail.visible" title="通知详情" width="720px" top="6vh">
      <template v-if="detail.row">
        <h3 class="notice-title">{{ detail.row.title }}</h3>
        <div class="notice-meta">
          <el-tag :type="detail.row.noticeType === 2 ? 'warning' : 'primary'" size="small" effect="plain">
            {{ typeLabel(detail.row.noticeType) }}
          </el-tag>
          <el-tag :type="statusTag(detail.row.status)" size="small">{{ statusLabel(detail.row.status) }}</el-tag>
          <span>发布人：{{ detail.row.publisher || '—' }}</span>
          <span>发布时间：{{ fmtTime(detail.row.publishTime) }}</span>
          <span>打印次数：{{ detail.row.printCount ?? 0 }}</span>
        </div>
        <div class="notice-content">{{ detail.row.content || '（无正文）' }}</div>
        <div v-if="detail.row.remark" class="notice-remark">备注：{{ detail.row.remark }}</div>
      </template>
      <template #footer>
        <el-button @click="detail.visible = false">关 闭</el-button>
        <el-button v-if="detail.row" type="primary" @click="onPrint(detail.row)">打 印</el-button>
      </template>
    </el-dialog>

    <!-- 打印用隐藏区（@media print 里才可见） -->
    <div v-if="printRow" class="print-area">
      <h1 class="print-title">{{ printRow.title }}</h1>
      <div class="print-meta">
        <span>类型：{{ typeLabel(printRow.noticeType) }}</span>
        <span>发布人：{{ printRow.publisher || '—' }}</span>
        <span>发布时间：{{ fmtTime(printRow.publishTime) }}</span>
      </div>
      <div class="print-body">{{ printRow.content || '（无正文）' }}</div>
      <div class="print-foot">打印时间：{{ printStamp }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createNotice,
  deleteNotice,
  noticePage,
  offlineNotice,
  printNotice,
  publishNotice,
  updateNotice
} from '@/api/query'
import type { NoticeDTO, NoticePageParams, PageResult, SysNotice } from '@/types/api'

/**
 * 通知管理（公共查询 → 通知管理）。
 *
 * 状态机在后端：0 草稿 →发布→ 1 已发布 →下线→ 2 已下线 →发布→ 1。
 * 页面只负责按状态显示对应按钮；已发布行的"修改/删除"置灰（后端也会 400 兜底）。
 * 打印：把内容填进隐藏的 `.print-area`，用 `window.print()` + 全局 @media print 样式只印这一块，
 * 打印后调 `/{id}/print` 自增次数（课程要求的形式妥协，不做服务端打印）。
 */

const NOTICE_TYPE = { NOTICE: 1, BULLETIN: 2 }
const TYPE_OPTIONS = [
  { value: NOTICE_TYPE.NOTICE, label: '通知' },
  { value: NOTICE_TYPE.BULLETIN, label: '公告' }
]
const STATUS_OPTIONS = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已发布' },
  { value: 2, label: '已下线' }
]

function typeLabel(type: number): string {
  return TYPE_OPTIONS.find((o) => o.value === type)?.label ?? String(type)
}

function statusLabel(status: number): string {
  return STATUS_OPTIONS.find((o) => o.value === status)?.label ?? String(status)
}

function statusTag(status: number): 'info' | 'success' | 'warning' {
  if (status === 1) return 'success'
  if (status === 2) return 'warning'
  return 'info'
}

/** `2026-10-10T14:30:00` → `2026-10-10 14:30` */
function fmtTime(value: string | null | undefined): string {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

// ---------- 列表 ----------
const loading = ref(false)
const records = ref<SysNotice[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })
const query = reactive<{ title: string; noticeType: number | null; status: number | null }>({
  title: '',
  noticeType: null,
  status: null
})

async function load() {
  loading.value = true
  try {
    const params: NoticePageParams = {
      current: page.current,
      size: page.size,
      title: query.title.trim() || undefined,
      noticeType: query.noticeType,
      status: query.status
    }
    const res = await noticePage(params)
    const data = (res.data || {}) as PageResult<SysNotice>
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
  query.title = ''
  query.noticeType = null
  query.status = null
  onSearch()
}

// ---------- 状态流转 ----------
async function onPublish(row: SysNotice) {
  try {
    await ElMessageBox.confirm(`确认发布通知【${row.title}】？发布后将记录发布人与发布时间。`, '提示', {
      type: 'warning'
    })
  } catch {
    return
  }
  await publishNotice(row.id)
  ElMessage.success('发布成功')
  load()
}

async function onOffline(row: SysNotice) {
  try {
    await ElMessageBox.confirm(`确认下线通知【${row.title}】？下线后可从列表中再次发布。`, '提示', {
      type: 'warning'
    })
  } catch {
    return
  }
  await offlineNotice(row.id)
  ElMessage.success('已下线')
  load()
}

async function onDelete(row: SysNotice) {
  try {
    await ElMessageBox.confirm(`确认删除通知【${row.title}】？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteNotice(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && page.current > 1) {
    page.current -= 1
  }
  load()
}

// ---------- 打印 ----------
const printRow = ref<SysNotice | null>(null)
// 打印时间戳用 ref 在每次打印时刷新（computed 无响应式依赖只会算一次并永久缓存，会印出挂载时刻）
const printStamp = ref('')

async function onPrint(row: SysNotice) {
  printRow.value = row
  printStamp.value = new Date().toLocaleString('zh-CN')
  await nextTick()
  window.print()
  // window.print() 在多数浏览器阻塞到关闭打印框；之后计数 +1 并就地更新
  const res = await printNotice(row.id)
  row.printCount = res.data ?? (row.printCount ?? 0) + 1
  ElMessage.success('打印次数已更新')
}

// ---------- 新增 / 修改 ----------
const dialog = reactive({ visible: false, mode: 'create' as 'create' | 'edit', submitting: false })
const formRef = ref<FormInstance>()

const emptyForm = (): NoticeDTO => ({
  id: null,
  title: '',
  noticeType: NOTICE_TYPE.NOTICE,
  content: null,
  remark: null
})

const form = reactive<NoticeDTO>(emptyForm())

const rules: FormRules = {
  title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
  noticeType: [{ required: true, message: '请选择通知类型', trigger: 'change' }]
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialog.mode = 'create'
  dialog.visible = true
}

function openEdit(row: SysNotice) {
  Object.assign(form, emptyForm(), {
    id: row.id,
    title: row.title,
    noticeType: row.noticeType,
    content: row.content,
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
      await createNotice(form)
      ElMessage.success('新增成功（草稿）')
    } else {
      await updateNotice(form)
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

// ---------- 详情 ----------
const detail = reactive<{ visible: boolean; row: SysNotice | null }>({ visible: false, row: null })

function openDetail(row: SysNotice) {
  detail.row = row
  detail.visible = true
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

/* 详情弹窗排版 */
.notice-title {
  margin: 0 0 12px;
  text-align: center;
  font-size: 18px;
}

.notice-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #909399;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.notice-content {
  margin-top: 12px;
  line-height: 1.9;
  white-space: pre-wrap;
  word-break: break-word;
  min-height: 80px;
}

.notice-remark {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}

/* 打印区平时不显示，打印时才铺开 */
.print-area {
  display: none;
}
</style>

<!-- 打印样式必须是全局的（scoped 会把 `body *` 改写成带 data 属性的选择器，拦不住侧边栏等外部元素） -->
<style>
@media print {
  body * {
    visibility: hidden !important;
  }

  .print-area,
  .print-area * {
    visibility: visible !important;
  }

  .print-area {
    display: block !important;
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    padding: 28px 36px;
    color: #000;
  }

  .print-area .print-title {
    text-align: center;
    font-size: 22px;
    margin: 0 0 16px;
  }

  .print-area .print-meta {
    display: flex;
    gap: 24px;
    justify-content: center;
    font-size: 13px;
    color: #333;
    padding-bottom: 12px;
    border-bottom: 1px solid #000;
  }

  .print-area .print-body {
    margin-top: 18px;
    line-height: 2;
    font-size: 15px;
    white-space: pre-wrap;
    word-break: break-word;
    min-height: 320px;
  }

  .print-area .print-foot {
    margin-top: 28px;
    text-align: right;
    font-size: 13px;
    color: #333;
  }
}
</style>
