<template>
  <div v-if="visible" class="degraded-tip">
    <el-icon class="degraded-tip__icon"><WarningFilled /></el-icon>
    <div class="degraded-tip__body">
      <div class="degraded-tip__title">{{ title }}</div>
      <div v-if="message" class="degraded-tip__desc">{{ message }}</div>
    </div>
    <el-button v-if="closable" text size="small" @click="onClose">
      <el-icon><Close /></el-icon>
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

/**
 * 降级提示条（BR-13）
 * 当 SSE 信封携带 degraded=true（AI 不可用走了规则引擎）时展示。
 */
const props = withDefaults(
  defineProps<{
    /** 是否展示 */
    modelValue?: boolean
    /** 标题文案 */
    title?: string
    /** 补充说明 */
    message?: string
    /** 是否可关闭 */
    closable?: boolean
  }>(),
  {
    modelValue: false,
    title: 'AI 服务繁忙，已切换为规则引擎结果',
    message: '本次结果由内置规则生成，评分与点评仅供参考，稍后可重试获取 AI 深度点评。',
    closable: true,
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const visible = ref<boolean>(props.modelValue)

watch(
  () => props.modelValue,
  (v) => {
    visible.value = v
  },
)

function onClose(): void {
  visible.value = false
  emit('update:modelValue', false)
}
</script>

<style scoped lang="scss">
.degraded-tip {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  background: #fffbeb;
  border: 1px solid #fde68a;
  color: #92400e;
}

.degraded-tip__icon {
  font-size: 18px;
  margin-top: 2px;
  color: var(--color-warning);
}

.degraded-tip__body {
  flex: 1;
  min-width: 0;
}

.degraded-tip__title {
  font-size: 13px;
  font-weight: 600;
}

.degraded-tip__desc {
  margin-top: 4px;
  font-size: 12px;
  color: #a16207;
  line-height: 1.6;
}
</style>
