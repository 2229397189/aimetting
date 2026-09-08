<template>
  <span class="stream-text">
    <span class="stream-content">{{ shown }}</span>
    <span v-if="cursor" class="stream-cursor" aria-hidden="true"></span>
  </span>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'

/**
 * 打字机增量文本渲染
 *
 * 设计说明：
 * - 父组件通过 SSE 不断把新收到的 delta 追加进 `text`，本组件负责"追赶"渲染；
 * - 使用 requestAnimationFrame 逐帧补充字符，避免高频 watch 直接全量重绘；
 * - 若 `text` 不再是当前已渲染内容的前缀（例如切到下一题清空），立即重置再追赶；
 * - 不做二次节流（ARCHITECTURE 6.4 要求已收到内容即可渲染）。
 */
const props = withDefaults(
  defineProps<{
    /** 目标文本（增量追加） */
    text?: string
    /** 每帧补充的字符数，越大越快 */
    speed?: number
    /** 是否显示闪烁光标 */
    cursor?: boolean
  }>(),
  {
    text: '',
    speed: 8,
    cursor: true,
  },
)

/** 当前已渲染的文本 */
const shown = ref<string>('')
/** rAF 句柄 */
let rafId = 0

/** 逐帧追赶目标文本 */
function tick(): void {
  const target = props.text || ''
  if (shown.value.length >= target.length) {
    rafId = 0
    return
  }
  const nextLen = Math.min(target.length, shown.value.length + Math.max(1, props.speed))
  shown.value = target.slice(0, nextLen)
  rafId = requestAnimationFrame(tick)
}

/** 启动追赶（幂等：已在运行则不重复启动） */
function ensureRunning(): void {
  if (rafId) return
  rafId = requestAnimationFrame(tick)
}

watch(
  () => props.text,
  (next) => {
    const target = next || ''
    // 目标文本被替换（非增量追加）时，先重置再渲染，避免串台
    if (!target.startsWith(shown.value)) {
      shown.value = ''
    }
    if (target.length > shown.value.length) {
      ensureRunning()
    } else if (target.length < shown.value.length) {
      shown.value = target
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  if (rafId) {
    cancelAnimationFrame(rafId)
    rafId = 0
  }
})
</script>

<style scoped lang="scss">
.stream-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.stream-content {
  white-space: pre-wrap;
}
</style>
