/**
 * 展示用的数值格式化（**只影响显示，不参与提交**）。
 * 放在这里避免每个页面各抄一份 —— 金额与百分比在人事/薪酬多页都要用。
 */

/** 金额 / 数量：保留 2 位小数；空值给「—」 */
export function money(value: number | null | undefined): string {
  if (value === null || value === undefined) return '—'
  return Number(value).toFixed(2)
}

/**
 * 比例：存的是 0~1 小数，显示时顺手补百分比便于阅读。
 * 例：0.16 → 「0.16（16.00%）」。**提交时永远传原值**，不要拿它回写表单。
 */
export function percent(value: number | null | undefined): string {
  if (value === null || value === undefined) return '—'
  return `${value}（${(Number(value) * 100).toFixed(2)}%）`
}
