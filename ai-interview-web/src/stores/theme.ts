/**
 * 主题状态：管理暗色 / 浅色切换与持久化
 * - 首次进入无选择时跟随系统 prefers-color-scheme
 * - 用户切换后用 localStorage 持久化，刷新保持
 * - 切换时给 document.documentElement 加 / 去 .dark（Element Plus 暗色方案）
 */
import { defineStore } from 'pinia'

const STORAGE_KEY = 'ai-interview-theme'

function systemPrefersDark(): boolean {
  return (
    typeof window !== 'undefined' &&
    !!window.matchMedia &&
    window.matchMedia('(prefers-color-scheme: dark)').matches
  )
}

function applyDark(dark: boolean): void {
  const el = document.documentElement
  // 跟随切换时同步 body 背景，避免暗色下底部白条
  el.classList.toggle('dark', dark)
}

export const useThemeStore = defineStore('theme', {
  state: () => ({
    isDark: false,
  }),
  actions: {
    /** 应用启动时调用：读取持久化值或跟随系统 */
    init(): void {
      // 默认深色科技风：无持久化选择时直接进深色（不再跟随系统浅色），
      // 用户随时可点顶栏日/夜按钮切回浅色并持久化。
      const saved = localStorage.getItem(STORAGE_KEY)
      const dark = saved ? saved === 'dark' : true
      this.isDark = dark
      applyDark(dark)
    },
    /** 切换主题并持久化 */
    toggle(): void {
      this.setDark(!this.isDark)
    },
    /** 显式设置主题并持久化 */
    setDark(dark: boolean): void {
      this.isDark = dark
      applyDark(dark)
      localStorage.setItem(STORAGE_KEY, dark ? 'dark' : 'light')
    },
  },
})
