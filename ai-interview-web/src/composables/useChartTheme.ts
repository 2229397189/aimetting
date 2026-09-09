/**
 * 图表主题：根据全局暗色状态返回 ECharts 配色
 * 供 RadarChart / Profile 趋势图 / AdminDashboard 图表统一复用
 */
import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'

export interface ChartPalette {
  /** 文字主色（轴名 / 图例 / 标签） */
  text: string
  /** 轴线颜色 */
  axisLine: string
  /** 分割线颜色 */
  splitLine: string
  /** 雷达图 splitArea 背景渐变（外 → 内） */
  areaBg: string[]
}

/** 浅色与暗色两套配色，与 Element Plus / 全局 CSS 变量对齐 */
const LIGHT: ChartPalette = {
  text: '#4b5563',
  axisLine: '#e5e7eb',
  splitLine: '#e5e7eb',
  areaBg: ['#ffffff', '#fafaff'],
}

const DARK: ChartPalette = {
  text: '#CFD3DC',
  axisLine: '#3a3f4b',
  splitLine: '#2a2f3a',
  areaBg: ['#1a1d24', '#14161c'],
}

export function useChartTheme() {
  const themeStore = useThemeStore()
  const isDark = computed<boolean>(() => themeStore.isDark)
  const palette = computed<ChartPalette>(() => (isDark.value ? DARK : LIGHT))
  return { isDark, palette }
}
