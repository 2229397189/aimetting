<template>
  <div class="app-root">
    <!-- 登录页与面试房间页使用自定义布局，不显示全局导航 -->
    <AppHeader v-if="!hideHeader" />
    <main class="app-main" :class="{ 'no-header': hideHeader }">
      <router-view v-slot="{ Component, route }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" :key="route.fullPath" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader/index.vue'

const route = useRoute()

/** 登录页 / 面试房间页隐藏顶部导航 */
const hideHeader = computed<boolean>(() => !!route.meta?.hideHeader)
</script>

<style scoped lang="scss">
.app-root {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-page);
}

.app-main {
  flex: 1;
  width: 100%;
  box-sizing: border-box;
}

.app-main:not(.no-header) {
  padding: 20px 0 40px;
}

/* 路由切换动画：轻量淡入上移，避免卡顿感 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
