<template>
  <div class="back-bar">
    <el-button text :icon="ArrowLeft" @click="handleBack">{{ label }}</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'

const props = withDefaults(
  defineProps<{
    /** 点击后跳转的路由，缺省回上一页 */
    to?: string
    label?: string
  }>(),
  {
    to: '',
    label: '返回',
  },
)

const router = useRouter()

function handleBack(): void {
  if (props.to) {
    router.push(props.to)
  } else {
    router.back()
  }
}
</script>

<style scoped lang="scss">
.back-bar {
  margin-bottom: 12px;
}
</style>
