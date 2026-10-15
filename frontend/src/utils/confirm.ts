import { ElMessageBox } from 'element-plus'

/**
 * 二次确认的统一封装（UI 阶段 4）。
 *
 * 为什么要封：改造前全站 26 个页面、32 处确认框，每处都手写一遍
 *   `try { await ElMessageBox.confirm(文案, '提示', { type: 'warning' }) } catch { return }`
 * 于是「删除」和「发布」长得一模一样 —— 危险操作的确认按钮还是 EP 默认的**蓝色**，
 * 不够醒目；文案/标题/type 靠人工保持一致也容易走偏。
 *
 * 两个入口（都返回 Promise；**用户取消时 reject**，所以调用处照旧用 try/catch 或 .catch 处理）。
 * ⚠️ **两者都接收「完整文案」，函数不做任何拼串** —— 调用侧把整句原样传进来即可：
 *
 *   try {
 *     await confirmDelete(`确认删除职工【${row.name}】？删除后工号可重新录入。`)
 *   } catch {
 *     return   // 用户取消
 *   }
 *
 *   try {
 *     await confirmAction(`确认发布通知【${row.title}】？发布后将记录发布人与发布时间。`)
 *   } catch {
 *     return
 *   }
 */

/**
 * 危险操作（删除）确认：标题「删除确认」+ 红色「确认删除」按钮。
 * ⚠️ `message` 传的是**完整文案**（调用侧写全整句）。
 *    早期版本试过"传 target 由函数自动拼串"，结果与铺开时统一采用的"透传完整文案"调用方式混用，
 *    出现「确认删除确认删除部门【x】？？」这种重复 —— 故统一为"透传完整文案"。
 */
export function confirmDelete(message: string) {
  return ElMessageBox.confirm(message, '删除确认', {
    type: 'warning',
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    // 危险操作的确认按钮用 danger 变体（红色）——EP 里 .el-button--danger 定义在 --primary 之后，
    // 同时带上两个类时后者胜，实测确认按钮呈红色（见阶段 4 的截图证据）。
    confirmButtonClass: 'el-button--danger'
  })
}

/**
 * **不可逆操作**确认（确认工资档案 / 发放工资这类"做了就回不去"的动作）：
 * 与 `confirmDelete` 一样用**红色**确认按钮，但标题是「请确认」——
 * 这些动作本身不是"删除"，用「删除确认」会词不达意。
 * （发布 / 下线 / 启用停用这类可撤销的仍用 `confirmAction` 的蓝色「确定」。）
 */
export function confirmIrreversible(message: string) {
  return ElMessageBox.confirm(message, '请确认', {
    type: 'warning',
    confirmButtonText: '确认执行',
    cancelButtonText: '取消',
    confirmButtonClass: 'el-button--danger'
  })
}

/** 非删除类、且可撤销的操作确认（发布 / 下线 / 启用停用等）：「提示」+ 蓝色确定 */
export function confirmAction(message: string, title = '提示') {
  return ElMessageBox.confirm(message, title, {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
}
