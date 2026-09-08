/// <reference types="vite/client" />

/**
 * 单文件组件模块声明。
 * 说明：vue-tsc 能直接解析 .vue，此处兜底声明用于非 vue-tsc 场景（如纯 tsc / IDE 跳转）。
 */
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}

/** 环境变量类型声明 */
interface ImportMetaEnv {
  readonly VITE_API_BASE: string
  readonly VITE_APP_TITLE: string
  readonly VITE_MOCK: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

/** markdown-it 无 @types 时的兜底（已装 @types/markdown-it，此处仅防重复报错） */
declare module 'markdown-it' {
  const MarkdownIt: any
  export default MarkdownIt
}
