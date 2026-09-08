import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { ClientConfig, DirectionOption, DifficultyOption } from '@/types'
import { configApi } from '@/api/admin'
import { questionApi } from '@/api/question'
import { getJson, setJson, StorageKey } from '@/utils/storage'
import { DIFFICULTY_OPTIONS, DIRECTION_OPTIONS } from '@/utils/dict'

/**
 * 全局配置 store：
 * - 方向 / 难度字典（优先取后端 /api/questions/directions，失败回落前端内置枚举，保证后端未就绪时也能渲染）
 * - /api/config/client 下发的题量范围、追问上限、mock 标识
 */
export const useConfigStore = defineStore('config', () => {
  /** 方向字典 */
  const directions = ref<DirectionOption[]>(
    getJson<DirectionOption[]>(`${StorageKey.Config}_directions`, null) ||
      DIRECTION_OPTIONS.map((i) => ({ code: i.value, label: i.label })),
  )
  /** 难度字典 */
  const difficulties = ref<DifficultyOption[]>(
    getJson<DifficultyOption[]>(`${StorageKey.Config}_difficulties`, null) ||
      DIFFICULTY_OPTIONS.map((i) => ({ code: i.value, label: i.label })),
  )

  /** 后端下发的客户端配置（带默认值，后端未就绪时仍可用） */
  const clientConfig = ref<ClientConfig>({
    mockMode: false,
    minQuestion: 3,
    maxQuestion: 15,
    defaultQuestion: 8,
    maxFollowUp: 2,
    answerMinLength: 10,
    answerMaxLength: 5000,
    appName: 'AI 模拟面试平台',
    ...(getJson<ClientConfig>(StorageKey.Config, null) || {}),
  })

  /** 是否已加载过 */
  const loaded = ref<boolean>(false)

  const minQuestion = computed<number>(() => clientConfig.value.minQuestion ?? 3)
  const maxQuestion = computed<number>(() => clientConfig.value.maxQuestion ?? 15)
  const defaultQuestion = computed<number>(() => clientConfig.value.defaultQuestion ?? 8)
  const maxFollowUp = computed<number>(() => clientConfig.value.maxFollowUp ?? 2)
  const answerMinLength = computed<number>(() => clientConfig.value.answerMinLength ?? 10)
  const answerMaxLength = computed<number>(() => clientConfig.value.answerMaxLength ?? 5000)
  const isMockMode = computed<boolean>(() => !!clientConfig.value.mockMode)

  /** 拉取方向 / 难度字典（失败时静默回落到内置枚举） */
  async function fetchDirections(): Promise<void> {
    try {
      const resp = await questionApi.directions()
      if (resp?.directions?.length) {
        directions.value = resp.directions
        setJson(`${StorageKey.Config}_directions`, resp.directions)
      }
      if (resp?.difficulties?.length) {
        difficulties.value = resp.difficulties
        setJson(`${StorageKey.Config}_difficulties`, resp.difficulties)
      }
    } catch (e) {
      // 后端未就绪：保留内置枚举，不影响页面渲染
    }
  }

  /** 拉取客户端配置 */
  async function fetchClientConfig(): Promise<void> {
    try {
      const resp = await configApi.client()
      if (resp) {
        clientConfig.value = { ...clientConfig.value, ...resp }
        setJson(StorageKey.Config, clientConfig.value)
      }
    } catch (e) {
      // 忽略：使用默认值
    }
  }

  /** 统一初始化：字典 + 配置，只保留一次并发 */
  let initializing: Promise<void> | null = null
  async function ensureLoaded(): Promise<void> {
    if (loaded.value) return
    if (initializing) return initializing
    initializing = (async () => {
      await Promise.all([fetchDirections(), fetchClientConfig()])
      loaded.value = true
      initializing = null
    })()
    return initializing
  }

  return {
    directions,
    difficulties,
    clientConfig,
    loaded,
    minQuestion,
    maxQuestion,
    defaultQuestion,
    maxFollowUp,
    answerMinLength,
    answerMaxLength,
    isMockMode,
    fetchDirections,
    fetchClientConfig,
    ensureLoaded,
  }
})

export default useConfigStore
