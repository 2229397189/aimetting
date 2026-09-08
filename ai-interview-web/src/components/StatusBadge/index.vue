<template>
  <el-tag :type="tagType" size="small" effect="light">{{ label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { SessionStatus } from '@/types'
import { statusLabel, statusTagType } from '@/utils/dict'

/** 会话状态标签：INIT / ASKING / EVALUATING / FOLLOW_UP / PAUSED / COMPLETED / ABORTED */
const props = withDefaults(
  defineProps<{
    status?: SessionStatus | string
  }>(),
  {
    status: 'INIT',
  },
)

const label = computed<string>(() => statusLabel(props.status))
const tagType = computed(() => statusTagType(props.status))
</script>
