import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 路由表（14 个页面）+ 鉴权守卫
 * - 未登录访问受保护页面 → /login?redirect=xxx
 * - /admin/** 需要 role === 'ADMIN'，否则跳首页并提示
 *
 * 注意：/interview/setup 必须排在 /interview/:sessionId 之前，
 * 否则 "setup" 会被当作 sessionId 匹配。
 */

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录 / 注册', public: true, hideHeader: true },
  },
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { title: '首页', icon: 'HomeFilled' },
  },
  {
    path: '/resume',
    name: 'Resume',
    component: () => import('@/views/Resume.vue'),
    meta: { title: '简历管理', icon: 'Document' },
  },
  {
    path: '/interview/setup',
    name: 'InterviewSetup',
    component: () => import('@/views/InterviewSetup.vue'),
    meta: { title: '开始一场模拟面试', icon: 'VideoPlay' },
  },
  {
    path: '/interview/:sessionId',
    name: 'InterviewRoom',
    component: () => import('@/views/InterviewRoom.vue'),
    meta: { title: '面试进行中', hideHeader: true },
  },
  {
    path: '/interview',
    name: 'SessionList',
    component: () => import('@/views/SessionList.vue'),
    meta: { title: '面试记录', icon: 'Tickets' },
  },
  {
    path: '/report/:sessionId',
    name: 'ReportDetail',
    component: () => import('@/views/ReportDetail.vue'),
    meta: { title: '面试报告' },
  },
  {
    path: '/report',
    name: 'ReportList',
    component: () => import('@/views/ReportList.vue'),
    meta: { title: '报告历史', icon: 'DataAnalysis' },
  },
  {
    path: '/questions',
    name: 'QuestionBank',
    component: () => import('@/views/QuestionBank.vue'),
    meta: { title: '题库浏览', icon: 'Collection' },
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人中心', icon: 'User' },
  },
  {
    path: '/admin/dashboard',
    name: 'AdminDashboard',
    component: () => import('@/views/AdminDashboard.vue'),
    meta: { title: '数据看板', admin: true },
  },
  {
    path: '/admin/questions',
    name: 'AdminQuestions',
    component: () => import('@/views/AdminQuestions.vue'),
    meta: { title: '题目管理', admin: true },
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('@/views/AdminUsers.vue'),
    meta: { title: '用户管理', admin: true },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在', public: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

/** 全局前置守卫：鉴权 + 标题 */
router.beforeEach((to, _from, next) => {
  const title = (to.meta?.title as string) || ''
  document.title = title ? `${title} · AI 模拟面试平台` : 'AI 模拟面试平台'

  if (to.meta?.public) {
    // 已登录用户访问登录页，直接回首页
    if (to.name === 'Login') {
      const userStore = useUserStore()
      if (userStore.isLogin) {
        next({ path: '/', replace: true })
        return
      }
    }
    next()
    return
  }

  const userStore = useUserStore()
  if (!userStore.isLogin) {
    next({ path: '/login', query: { redirect: to.fullPath }, replace: true })
    return
  }

  // 管理端路由：需要 ADMIN 角色
  if (to.meta?.admin && !userStore.isAdmin) {
    next({ path: '/', replace: true })
    return
  }

  next()
})

export default router
