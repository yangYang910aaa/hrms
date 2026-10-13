import { ref } from 'vue'
import { dictOptions } from '@/api/common'
import type { DictOption } from '@/types/api'

/**
 * 字典 composable：按 dictType 拉启用中的字典项，带进程内缓存（多页面共用不重复请求）。
 *
 * 用法：
 *   const { dictMap, load, label } = useDict(['sex', 'edu_level', 'employ_type'])
 *   onMounted(load)
 *   模板里 label('sex', row.gender) → '男'
 */
const cache = new Map<string, DictOption[]>()

async function loadDict(type: string): Promise<DictOption[]> {
  if (!cache.has(type)) {
    const res = await dictOptions(type)
    cache.set(type, res.data || [])
  }
  return cache.get(type) as DictOption[]
}

export function useDict(types: string[] = []) {
  const dictMap = ref<Record<string, DictOption[]>>({})

  const load = async () => {
    const entries = await Promise.all(
      types.map(async (t) => [t, await loadDict(t)] as const)
    )
    dictMap.value = Object.fromEntries(entries)
  }

  /** 字典值 → 显示文本；查不到原样返回（前端拿到的就是后端原值） */
  const label = (type: string, value: string | number | null | undefined): string => {
    if (value === null || value === undefined || value === '') return '—'
    const hit = (dictMap.value[type] || []).find((i) => String(i.value) === String(value))
    return hit ? hit.label : String(value)
  }

  return { dictMap, load, label }
}
