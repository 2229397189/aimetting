<template>
  <div class="empty-state" :style="{ minHeight: minHeight + 'px' }">
    <el-icon class="empty-state__icon">
      <component :is="iconComponent" />
    </el-icon>
    <p class="empty-state__desc">{{ description }}</p>
    <slot name="action" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

/** 空状态占位：列表无数据 / 搜索无结果时使用 */
const props = withDefaults(
  defineProps<{
    description?: string
    icon?: string
    minHeight?: number
  }>(),
  {
    description: '暂无数据',
    icon: 'DocumentDelete',
    minHeight: 180,
  },
)

/** 图标按名称渲染（Element Plus 图标已全局注册） */
const iconComponent = computed<string>(() => props.icon || 'DocumentDelete')
</script>

<style scoped lang="scss">
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  padding: 24px 0;
}

.empty-state__icon {
  font-size: 46px;
  color: #cbd5e1;
}

.empty-state__desc {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
}
</style>
