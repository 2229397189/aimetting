<template>
  <!-- 内容已在 utils/markdown.ts 中经 markdown-it(html:false) + 链接白名单消毒后渲染 -->
  <div class="markdown-body markdown-render" v-html="html"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

/**
 * Markdown 渲染组件（BR-22 XSS 防护）
 * - markdown-it 关闭 html，禁止原始 HTML 透传；
 * - 链接仅放行 http/https，图片降级为文本；
 * - 因此此处 v-html 是安全的。
 */
const props = withDefaults(
  defineProps<{
    /** Markdown 源文本 */
    source?: string | null
  }>(),
  {
    source: '',
  },
)

const html = computed<string>(() => renderMarkdown(props.source))
</script>

<style scoped lang="scss">
.markdown-render {
  word-break: break-word;
}
</style>
