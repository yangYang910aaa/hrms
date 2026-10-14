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

/**
 * 日期时间：'2026-10-14 20:30:00' → '2026-10-14 20:30'（秒用不上，砍掉更清爽）；
 * 空值给「—」。只做切片不做解析 —— 后端一律返回 'yyyy-MM-dd HH:mm:ss'，切片比 toLocale 更可控。
 */
export function dateTime(value: string | null | undefined): string {
  if (!value) return '—'
  return value.length >= 16 ? value.slice(0, 16) : value
}

/** 月日：'1990-05-20' → '05-20'（生日列表用；不显示年份，避免暴露年龄细节） */
export function monthDay(value: string | null | undefined): string {
  if (!value || value.length < 10) return '—'
  return value.slice(5, 10)
}
