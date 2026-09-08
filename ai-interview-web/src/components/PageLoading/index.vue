<template>
  <div class="page-loading" :style="{ minHeight: minHeight + 'px' }">
    <el-icon class="page-loading__spin"><Loading /></el-icon>
    <p v-if="text" class="page-loading__text">{{ text }}</p>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    /** 提示文案 */
    text?: string
    /** 最小高度 */
    minHeight?: number
  }>(),
  {
    text: '加载中…',
    minHeight: 200,
  },
)
</script>

<style scoped lang="scss">
.page-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  color: var(--text-secondary);
}

.page-loading__spin {
  font-size: 26px;
  color: var(--color-primary);
  animation: rotate 1s linear infinite;
}

.page-loading__text {
  font-size: 13px;
  margin: 0;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
