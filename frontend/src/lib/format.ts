/** 跨页面共享的时间格式化：后端 ISO 时间 → "YYYY-MM-DD HH:mm"。 */

/** ISO 时间转 "YYYY-MM-DD HH:mm"，空值返回"未知"。 */
export function formatTime(value?: string | null): string {
  if (!value) return '未知'
  return value.replace('T', ' ').slice(0, 16)
}

/** ISO 时间转 "YYYY-MM-DD"。 */
export function formatDate(value?: string | null): string {
  if (!value) return '未知'
  return value.slice(0, 10)
}
