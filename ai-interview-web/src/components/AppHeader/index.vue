<template>
  <header class="app-header">
    <div class="app-header__inner">
      <!-- Logo -->
      <div class="app-header__logo" @click="goHome">
        <span class="logo-mark">AI</span>
        <span class="logo-text">模拟面试平台</span>
      </div>

      <!-- 桌面端导航 -->
      <el-menu
        class="app-header__menu"
        mode="horizontal"
        :default-active="activeMenu"
        :ellipsis="false"
        @select="onSelect"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>

      <div class="app-header__right">
        <!-- 主题切换（日 / 夜） -->
        <el-button
          class="theme-toggle"
          text
          :title="themeStore.isDark ? '切换到浅色' : '切换到深色'"
          @click="themeStore.toggle()"
        >
          <el-icon><component :is="themeStore.isDark ? 'Sunny' : 'Moon'" /></el-icon>
        </el-button>

        <!-- 移动端菜单按钮 -->
        <el-button class="menu-toggle" text @click="drawerVisible = true">
          <el-icon><Menu /></el-icon>
        </el-button>

        <el-dropdown trigger="click" @command="onCommand">
          <div class="user-entry">
            <el-avatar :size="28" :src="avatar">
              {{ displayName.charAt(0) }}
            </el-avatar>
            <span class="user-name">{{ displayName }}</span>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>个人中心
              </el-dropdown-item>
              <el-dropdown-item command="resume">
                <el-icon><Document /></el-icon>我的简历
              </el-dropdown-item>
              <el-dropdown-item command="sessions">
                <el-icon><Tickets /></el-icon>面试记录
              </el-dropdown-item>
              <el-dropdown-item v-if="isAdmin" command="admin" divided>
                <el-icon><Setting /></el-icon>管理后台
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 移动端抽屉菜单 -->
    <el-drawer v-model="drawerVisible" title="导航" direction="rtl" size="72%">
      <div class="drawer-menu">
        <div
          v-for="item in allMenus"
          :key="item.path"
          class="drawer-menu__item"
          :class="{ active: activeMenu === item.path }"
          @click="goPath(item.path)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </div>
      </div>
    </el-drawer>
  </header>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

interface MenuItem {
  path: string
  title: string
  icon: string
  admin?: boolean
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const themeStore = useThemeStore()

const drawerVisible = ref<boolean>(false)

/** 主导航（桌面端横向） */
const menus = computed<MenuItem[]>(() => [
  { path: '/', title: '首页', icon: 'HomeFilled' },
  { path: '/interview/setup', title: '开始面试', icon: 'VideoPlay' },
  { path: '/interview', title: '面试记录', icon: 'Tickets' },
  { path: '/report', title: '报告历史', icon: 'DataAnalysis' },
  { path: '/resume', title: '简历管理', icon: 'Document' },
  { path: '/questions', title: '题库', icon: 'Collection' },
])

/** 抽屉中包含管理端入口 */
const allMenus = computed<MenuItem[]>(() => {
  const list: MenuItem[] = [...menus.value, { path: '/profile', title: '个人中心', icon: 'User' }]
  if (userStore.isAdmin) {
    list.push(
      { path: '/admin/dashboard', title: '数据看板', icon: 'DataLine', admin: true },
      { path: '/admin/questions', title: '题目管理', icon: 'EditPen', admin: true },
      { path: '/admin/users', title: '用户管理', icon: 'UserFilled', admin: true },
    )
  }
  return list
})

const activeMenu = computed<string>(() => {
  const p = route.path
  if (p.startsWith('/admin')) return p
  if (p.startsWith('/interview')) {
    return p === '/interview' || p === '/interview/setup' ? p : '/interview'
  }
  if (p.startsWith('/report')) return '/report'
  return p
})

const displayName = computed<string>(() => userStore.displayName)
const avatar = computed<string>(() => userStore.avatar)
const isAdmin = computed<boolean>(() => userStore.isAdmin)

function goHome(): void {
  router.push('/')
}

function goPath(path: string): void {
  drawerVisible.value = false
  router.push(path)
}

function onSelect(index: string): void {
  router.push(index)
}

async function onCommand(cmd: string): Promise<void> {
  switch (cmd) {
    case 'profile':
      router.push('/profile')
      break
    case 'resume':
      router.push('/resume')
      break
    case 'sessions':
      router.push('/interview')
      break
    case 'admin':
      router.push('/admin/dashboard')
      break
    case 'logout':
      await handleLogout()
      break
    default:
      break
  }
}

/** 退出登录：二次确认后清理状态并跳登录页 */
async function handleLogout(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消',
    })
  } catch (e) {
    return
  }
  await userStore.logout()
  router.push('/login')
}
</script>

<style scoped lang="scss">
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
}

.app-header__inner {
  max-width: var(--content-max-width);
  margin: 0 auto;
  height: var(--header-height);
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 20px;
}

.app-header__logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;
}

.logo-mark {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
}

.app-header__menu {
  flex: 1;
  min-width: 0;
  border-bottom: none !important;
}

.app-header__right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  outline: none;
}

.user-entry:hover {
  background: var(--bg-hover);
}

.user-name {
  font-size: 13px;
  color: var(--text-regular);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-toggle {
  display: none;
}

.drawer-menu__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--text-regular);
  cursor: pointer;
}

.drawer-menu__item:hover {
  background: var(--bg-hover);
}

.drawer-menu__item.active {
  background: var(--color-primary-bg);
  color: var(--color-primary);
  font-weight: 600;
}

@media (max-width: 900px) {
  .app-header__menu {
    display: none;
  }

  .menu-toggle {
    display: inline-flex;
  }

  .logo-text {
    display: none;
  }
}
</style>
