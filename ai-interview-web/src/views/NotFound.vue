<template>
  <div class="not-found">
    <div class="not-found__inner">
      <div class="not-found__code">404</div>
      <h1 class="not-found__title">页面走丢了</h1>
      <p class="not-found__desc">你访问的页面不存在或已被移动，请返回首页继续。</p>
      <div class="not-found__actions">
        <el-button type="primary" size="large" @click="goHome">
          <el-icon><HomeFilled /></el-icon>返回首页
        </el-button>
        <el-button size="large" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>上一页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome(): void {
  router.push('/')
}

function goBack(): void {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}
</script>

<style scoped lang="scss">
.not-found {
  min-height: calc(100vh - var(--header-height));
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--bg-page);
}

.not-found__inner {
  text-align: center;
  max-width: 420px;
}

.not-found__code {
  font-size: 96px;
  font-weight: 800;
  line-height: 1;
  color: var(--color-primary);
  letter-spacing: 4px;
  font-variant-numeric: tabular-nums;
}

.not-found__title {
  margin: 12px 0 8px;
  font-size: 22px;
  color: var(--text-primary);
}

.not-found__desc {
  margin: 0 0 24px;
  color: var(--text-secondary);
  font-size: 14px;
}

.not-found__actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}
</style>
