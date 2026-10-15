<template>
  <div class="page-container">
    <el-row :gutter="12">
      <!-- 左：字典类型 -->
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>字典类型</span>
              <el-button
                v-permission="'system:dict:add'"
                link
                type="primary"
                :icon="Plus"
                @click="openTypeCreate"
              >
                新增
              </el-button>
            </div>
          </template>

          <el-input
            v-model="typeKeyword"
            placeholder="名称 / 标识"
            clearable
            size="small"
            style="margin-bottom: 10px"
          />

          <el-table
            ref="typeTableRef"
            :data="typeRows"
            v-loading="typeLoading"
            stripe
            size="small"
            highlight-current-row
            @current-change="onTypeChange"
          >
            <el-table-column label="名称" prop="dictName" min-width="84" show-overflow-tooltip />
            <el-table-column label="标识" prop="dictType" min-width="80" show-overflow-tooltip />
            <el-table-column label="状态" min-width="56">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '正常' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="76">
              <template #default="{ row }">
                <div class="op-btns">
                  <span v-permission="'system:dict:edit'">
                    <el-tooltip content="修改" placement="top" :show-after="150">
                      <el-button
                        link
                        type="primary"
                        :icon="Edit"
                        aria-label="修改"
                        @click.stop="openTypeEdit(row)"
                      />
                    </el-tooltip>
                  </span>
                  <span v-permission="'system:dict:remove'">
                    <el-tooltip content="删除" placement="top" :show-after="150">
                      <el-button
                        link
                        type="danger"
                        :icon="Delete"
                        aria-label="删除"
                        @click.stop="onTypeDelete(row)"
                      />
                    </el-tooltip>
                  </span>
                </div>
              </template>
            </el-table-column>
            <template #empty>暂无数据</template>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右：字典数据 -->
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>
                字典数据
                <span v-if="currentType" class="sub-title">· {{ currentType.dictName }}（{{ currentType.dictType }}）</span>
                <span v-else class="sub-title">· 请先在左侧选择类型</span>
              </span>
              <el-button
                v-permission="'system:dict:add'"
                link
                type="primary"
                :icon="Plus"
                :disabled="!currentType"
                @click="openDataCreate"
              >
                新增
              </el-button>
            </div>
          </template>

          <el-form :inline="true" :model="dataQuery" class="search-form" @submit.prevent>
            <el-form-item label="标签">
              <el-input
                v-model="dataQuery.dictLabel"
                placeholder="模糊匹配"
                clearable
                size="small"
                style="width: 140px"
                @keyup.enter="onDataSearch"
              />
            </el-form-item>
            <el-form-item label="状态">
              <el-select
                v-model="dataQuery.status"
                placeholder="全部"
                clearable
                size="small"
                style="width: 100px"
              >
                <el-option label="正常" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" size="small" @click="onDataSearch">
                查询
              </el-button>
              <el-button :icon="Refresh" size="small" @click="onDataReset">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="dataRecords" v-loading="dataLoading" stripe size="small">
            <el-table-column label="字典标签" prop="dictLabel" min-width="120" />
            <el-table-column label="字典键值" prop="dictValue" min-width="120" />
            <el-table-column label="排序" prop="sort" width="60" />
            <el-table-column label="默认" width="70">
              <template #default="{ row }">
                <el-tag v-if="row.isDefault === 1" type="warning" size="small">默认</el-tag>
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="70">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '正常' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
            <el-table-column label="操作" width="76" fixed="right">
              <template #default="{ row }">
                <div class="op-btns">
                  <span v-permission="'system:dict:edit'">
                    <el-tooltip content="修改" placement="top" :show-after="150">
                      <el-button link type="primary" :icon="Edit" aria-label="修改" @click="openDataEdit(row)" />
                    </el-tooltip>
                  </span>
                  <span v-permission="'system:dict:remove'">
                    <el-tooltip content="删除" placement="top" :show-after="150">
                      <el-button link type="danger" :icon="Delete" aria-label="删除" @click="onDataDelete(row)" />
                    </el-tooltip>
                  </span>
                </div>
              </template>
            </el-table-column>
            <template #empty>{{ currentType ? '暂无数据' : '请先选择左侧字典类型' }}</template>
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="dataPage.current"
              v-model:page-size="dataPage.size"
              :total="dataTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              background
              size="small"
              @size-change="loadData"
              @current-change="loadData"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 字典类型弹窗 -->
    <el-dialog
      v-model="typeDialog.visible"
      :title="typeDialog.mode === 'create' ? '新增字典类型' : '修改字典类型'"
      width="500px"
      @closed="resetTypeDialog"
    >
      <el-form
        ref="typeFormRef"
        v-loading="typeDialog.submitting"
        :model="typeForm"
        :rules="typeRules"
        label-width="88px"
      >
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeForm.dictName" maxlength="50" placeholder="如：性别" />
        </el-form-item>
        <el-form-item label="字典标识" prop="dictType">
          <el-input
            v-model="typeForm.dictType"
            maxlength="50"
            :disabled="typeDialog.mode === 'edit'"
            placeholder="如：sex"
          />
          <div class="field-tip">
            它是字典数据与前端代码的引用键，新增后不允许修改（改了会让已有数据项变孤儿）
          </div>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="typeForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="正常"
            inactive-text="停用"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="typeDialog.submitting" @click="onSubmitType">
          保 存
        </el-button>
      </template>
    </el-dialog>

    <!-- 字典数据弹窗 -->
    <el-dialog
      v-model="dataDialog.visible"
      :title="dataDialog.mode === 'create' ? '新增字典数据' : '修改字典数据'"
      width="500px"
      @closed="resetDataDialog"
    >
      <el-form
        ref="dataFormRef"
        v-loading="dataDialog.submitting"
        :model="dataForm"
        :rules="dataRules"
        label-width="88px"
      >
        <el-form-item label="所属类型">
          <el-input :model-value="dataForm.dictType" disabled />
        </el-form-item>
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="dataForm.dictLabel" maxlength="50" placeholder="显示文本，如：男" />
        </el-form-item>
        <el-form-item label="字典键值" prop="dictValue">
          <el-input v-model="dataForm.dictValue" maxlength="50" placeholder="存库值，如：1" />
          <div class="field-tip">同一类型下键值不允许重复</div>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dataForm.sort" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="是否默认">
          <el-switch
            v-model="dataForm.isDefault"
            :active-value="1"
            :inactive-value="0"
            active-text="是"
            inactive-text="否"
          />
          <div class="field-tip">设为默认会取消同类型下其他项的默认</div>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="dataForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="正常"
            inactive-text="停用"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dataForm.remark" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="dataDialog.submitting" @click="onSubmitData">
          保 存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import type { TableInstance } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createDictData,
  createDictType,
  deleteDictData,
  deleteDictType,
  dictDataPage,
  dictTypePage,
  updateDictData,
  updateDictType
} from '@/api/system'
import type {
  DictDataDTO,
  DictDataVO,
  DictTypeDTO,
  DictTypeVO,
  PageResult
} from '@/types/api'
import { confirmDelete } from '@/utils/confirm'

// ------------------------------------------------------------------ 左：字典类型
const typeLoading = ref(false)
const typeList = ref<DictTypeVO[]>([])
const typeKeyword = ref('')
const currentType = ref<DictTypeVO | null>(null)
const typeTableRef = ref<TableInstance>()

const typeRows = computed(() => {
  const kw = typeKeyword.value.trim().toLowerCase()
  if (!kw) return typeList.value
  return typeList.value.filter(
    (t) => t.dictName.toLowerCase().includes(kw) || t.dictType.toLowerCase().includes(kw)
  )
})

async function loadTypes() {
  typeLoading.value = true
  try {
    // 字典类型数量有限，一次拉全（size 给大值），左侧不再做分页器
    const res = await dictTypePage({ current: 1, size: 100 })
    typeList.value = ((res.data || {}) as PageResult<DictTypeVO>).records || []
    await nextTick()
    // 默认选中第一行，省得进来右侧是空的。
    // setCurrentRow 会触发 current-change → onTypeChange 里顺带把右侧数据加载了，不用再手动赋值一次。
    if (!currentType.value && typeList.value.length) {
      typeTableRef.value?.setCurrentRow(typeList.value[0])
    }
  } finally {
    typeLoading.value = false
  }
}

function onTypeChange(row: DictTypeVO | null) {
  currentType.value = row
  dataPage.current = 1
  loadData()
}

// ------------------------------------------------------------------ 右：字典数据
const dataLoading = ref(false)
const dataRecords = ref<DictDataVO[]>([])
const dataTotal = ref(0)
const dataPage = reactive({ current: 1, size: 10 })
const dataQuery = reactive<{ dictLabel: string; status: number | null }>({
  dictLabel: '',
  status: null
})

async function loadData() {
  if (!currentType.value) {
    dataRecords.value = []
    dataTotal.value = 0
    return
  }
  dataLoading.value = true
  try {
    const res = await dictDataPage({
      current: dataPage.current,
      size: dataPage.size,
      dictType: currentType.value.dictType,
      dictLabel: dataQuery.dictLabel.trim() || undefined,
      status: dataQuery.status
    })
    const data = (res.data || {}) as PageResult<DictDataVO>
    dataRecords.value = data.records || []
    dataTotal.value = data.total || 0
  } finally {
    dataLoading.value = false
  }
}

function onDataSearch() {
  dataPage.current = 1
  loadData()
}

function onDataReset() {
  dataQuery.dictLabel = ''
  dataQuery.status = null
  onDataSearch()
}

// ------------------------------------------------------------------ 类型 CRUD
const typeDialog = reactive({
  visible: false,
  mode: 'create' as 'create' | 'edit',
  submitting: false
})
const typeFormRef = ref<FormInstance>()

const emptyTypeForm = (): DictTypeDTO => ({
  id: null,
  dictName: '',
  dictType: '',
  status: 1,
  remark: null
})

const typeForm = reactive<DictTypeDTO>(emptyTypeForm())

const typeRules: FormRules = {
  dictName: [{ required: true, message: '字典名称不能为空', trigger: 'blur' }],
  dictType: [
    { required: true, message: '字典标识不能为空', trigger: 'blur' },
    { pattern: /^[A-Za-z][A-Za-z0-9_]*$/, message: '建议字母开头，可含数字与下划线', trigger: 'blur' }
  ]
}

function openTypeCreate() {
  Object.assign(typeForm, emptyTypeForm())
  typeDialog.mode = 'create'
  typeDialog.visible = true
}

function openTypeEdit(row: DictTypeVO) {
  Object.assign(typeForm, emptyTypeForm(), {
    id: row.id,
    dictName: row.dictName,
    dictType: row.dictType,
    status: row.status ?? 1,
    remark: row.remark
  })
  typeDialog.mode = 'edit'
  typeDialog.visible = true
}

function resetTypeDialog() {
  typeFormRef.value?.clearValidate()
  Object.assign(typeForm, emptyTypeForm())
}

async function onTypeDelete(row: DictTypeVO) {
  try {
    await confirmDelete(`确认删除字典类型【${row.dictName}】？该类型下还有数据项时会删除失败。`)
  } catch {
    return
  }
  await deleteDictType(row.id)
  ElMessage.success('删除成功')
  if (currentType.value?.id === row.id) {
    currentType.value = null
    dataRecords.value = []
    dataTotal.value = 0
  }
  loadTypes()
}

async function onSubmitType() {
  if (!typeFormRef.value) return
  try {
    await typeFormRef.value.validate()
  } catch {
    return
  }
  typeDialog.submitting = true
  try {
    if (typeDialog.mode === 'create') {
      await createDictType(typeForm)
      ElMessage.success('新增成功')
    } else {
      await updateDictType(typeForm)
      ElMessage.success('修改成功')
    }
    typeDialog.visible = false
    loadTypes()
  } catch {
    // 失败消息由 request 拦截器统一弹出
  } finally {
    typeDialog.submitting = false
  }
}

// ------------------------------------------------------------------ 数据 CRUD
const dataDialog = reactive({
  visible: false,
  mode: 'create' as 'create' | 'edit',
  submitting: false
})
const dataFormRef = ref<FormInstance>()

const emptyDataForm = (): DictDataDTO => ({
  id: null,
  dictType: '',
  dictLabel: '',
  dictValue: '',
  sort: 0,
  isDefault: 0,
  status: 1,
  remark: null
})

const dataForm = reactive<DictDataDTO>(emptyDataForm())

const dataRules: FormRules = {
  dictLabel: [{ required: true, message: '字典标签不能为空', trigger: 'blur' }],
  dictValue: [{ required: true, message: '字典键值不能为空', trigger: 'blur' }]
}

function openDataCreate() {
  if (!currentType.value) return
  Object.assign(dataForm, emptyDataForm(), { dictType: currentType.value.dictType })
  dataDialog.mode = 'create'
  dataDialog.visible = true
}

function openDataEdit(row: DictDataVO) {
  Object.assign(dataForm, emptyDataForm(), {
    id: row.id,
    dictType: row.dictType,
    dictLabel: row.dictLabel,
    dictValue: row.dictValue,
    sort: row.sort ?? 0,
    isDefault: row.isDefault ?? 0,
    status: row.status ?? 1,
    remark: row.remark
  })
  dataDialog.mode = 'edit'
  dataDialog.visible = true
}

function resetDataDialog() {
  dataFormRef.value?.clearValidate()
  Object.assign(dataForm, emptyDataForm())
}

async function onDataDelete(row: DictDataVO) {
  try {
    await confirmDelete(`确认删除字典数据【${row.dictLabel}】？`)
  } catch {
    return
  }
  await deleteDictData(row.id)
  ElMessage.success('删除成功')
  if (dataRecords.value.length === 1 && dataPage.current > 1) {
    dataPage.current -= 1
  }
  loadData()
}

async function onSubmitData() {
  if (!dataFormRef.value) return
  try {
    await dataFormRef.value.validate()
  } catch {
    return
  }
  dataDialog.submitting = true
  try {
    if (dataDialog.mode === 'create') {
      await createDictData(dataForm)
      ElMessage.success('新增成功')
    } else {
      await updateDictData(dataForm)
      ElMessage.success('修改成功')
    }
    dataDialog.visible = false
    loadData()
  } catch {
    // 失败消息由 request 拦截器统一弹出
  } finally {
    dataDialog.submitting = false
  }
}

onMounted(loadTypes)
</script>

<style scoped>
/* 骨架类（.page-container/.search-form/.field-tip/.pagination/.op-btns/...）已在
   styles/index.css 全局定义，这里不再各写一份副本（阶段 3 收敛）。只留本页特有的。 */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sub-title {
  font-size: var(--hr-font-xs);
  color: var(--hr-text-placeholder);
}
</style>
