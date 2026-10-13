/**
 * 树工具：系统设置里的树形页面（部门 / 菜单 / 角色授权树）共用。
 *
 * 三件事各自都只有一处实现，页面里不要再抄第二遍：
 * 1. filterTree —— 树形表格的本地过滤（后端 /tree 接口不接受查询参数，只能前端筛）；
 * 2. collectBranchIds —— 默认全部展开用的受控展开键；
 * 3. flattenTree —— 树拍平成下拉选项（上级部门 / 上级菜单）。
 */

/**
 * 过滤树：
 * - 节点自身命中 → 保留它**和它的整棵子树**（不再逐层筛）；
 * - 自身没命中但子孙命中 → 保留它当路径，只带命中的子孙；
 * - 自身和子孙都没命中 → 丢掉。
 *
 * 直接按条件丢不匹配节点是错的：父被丢掉会把命中的子孙一起连坐掉，树就散了。
 */
export function filterTree<T extends { children?: T[] }>(
  nodes: T[] | undefined,
  predicate: (node: T) => boolean
): T[] {
  const out: T[] = []
  for (const n of nodes || []) {
    const children = filterTree(n.children, predicate)
    const selfHit = predicate(n)
    if (selfHit || children.length) {
      out.push({ ...n, children: selfHit ? n.children || [] : children } as T)
    }
  }
  return out
}

/**
 * 收集所有「有子节点」的节点 id，给 el-table 的 expand-row-keys 用。
 * 不用 default-expand-all：它只在首次渲染生效，而树数据是异步拉回来的，赶不上。
 */
export function collectBranchIds<T extends { id: number; children?: T[] }>(nodes: T[]): number[] {
  const ids: number[] = []
  const walk = (list: T[]) => {
    for (const n of list) {
      if (n.children && n.children.length) {
        ids.push(n.id)
        walk(n.children)
      }
    }
  }
  walk(nodes)
  return ids
}

/** 拍平后的下拉选项；带上原节点，调用方可以按类型再筛一层（菜单页要用） */
export interface TreeOption<T> {
  id: number
  name: string
  node: T
}

/**
 * 树拍平成下拉选项。
 *
 * excludeId（一般是正在编辑的那条）及其**整棵子树**都会被跳过 ——
 * 把自己或自己的子孙选成上级 = 自己挂自己下面，后端同样有环检测会拦，
 * 但前端先禁掉，别让用户提交一次才看到报错。
 *
 * 缩进用全角空格：普通连续空格会被 HTML 折叠掉，看着就没有层级了。
 */
export function flattenTree<T extends { id: number; children?: T[] }>(
  nodes: T[],
  labelOf: (node: T) => string,
  excludeId?: number | null
): TreeOption<T>[] {
  const out: TreeOption<T>[] = []
  const walk = (list: T[], depth: number, banned: boolean) => {
    for (const n of list) {
      const skip = banned || (excludeId != null && n.id === excludeId)
      if (!skip) {
        out.push({ id: n.id, name: '　'.repeat(depth) + (depth ? '└ ' : '') + labelOf(n), node: n })
      }
      walk(n.children || [], depth + 1, skip)
    }
  }
  walk(nodes, 0, false)
  return out
}
