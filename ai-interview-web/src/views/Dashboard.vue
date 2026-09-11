<template>
  <div class="page-container dashboard">
    <!-- 欢迎卡片 -->
    <section class="welcome card">
      <div class="welcome__main">
        <h2 class="welcome__title grad-text">
          {{ greeting }}，{{ userStore.displayName }}
          <el-tag v-if="userStore.isAdmin" type="warning" size="small" effect="light">管理员</el-tag>
        </h2>
        <p class="welcome__desc">
          {{ welcomeTip }}
        </p>
        <div class="welcome__actions">
          <el-button type="primary" size="large" @click="goSetup">
            <el-icon><VideoPlay /></el-icon>开始一场模拟面试
          </el-button>
          <el-button size="large" @click="goResume">
            <el-icon><Upload /></el-icon>上传简历
          </el-button>
          <el-button size="large" @click="goReports">
            <el-icon><DataAnalysis /></el-icon>查看报告
          </el-button>
        </div>
      </div>
      <div class="welcome__stats">
        <div class="stat-mini">
          <div class="stat-mini__value">{{ stats.totalSessions }}</div>
          <div class="stat-mini__label">累计场次</div>
        </div>
        <div class="stat-mini">
          <div class="stat-mini__value">{{ stats.completedSessions }}</div>
          <div class="stat-mini__label">完成场次</div>
        </div>
        <div class="stat-mini">
          <div class="stat-mini__value">{{ formatScore(stats.averageScore) }}</div>
          <div class="stat-mini__label">平均分</div>
        </div>
        <div class="stat-mini">
          <div class="stat-mini__value">{{ stats.resumeCount }}</div>
          <div class="stat-mini__label">简历数</div>
        </div>
      </div>
    </section>

    <!-- 快捷入口（Bento 网格） -->
    <section class="bento mt-16">
      <div v-for="item in quickEntries" :key="item.path" class="quick-card hover-lift" @click="go(item.path)">
        <div class="quick-card__icon" :style="{ background: item.bg }">
          <el-icon :size="22" :color="item.color"><component :is="item.icon" /></el-icon>
        </div>
        <div class="quick-card__body">
          <div class="quick-card__title">{{ item.title }}</div>
          <div class="quick-card__desc">{{ item.desc }}</div>
        </div>
        <el-icon class="quick-card__arrow"><ArrowRight /></el-icon>
      </div>
    </section>

    <div class="dashboard-cols mt-16">
      <!-- 最近面试 -->
      <section class="card recent">
        <div class="card-header">
          <div class="card-title">最近面试</div>
          <el-button text type="primary" @click="go('/interview')">全部记录</el-button>
        </div>
        <PageLoading v-if="loading" :min-height="160" text="加载中…" />
        <EmptyState
          v-else-if="!recentSessions.length"
          description="还没有面试记录，先来一场吧"
          min-height="150"
        >
          <template #action>
            <el-button type="primary" size="small" @click="goSetup">立即开始</el-button>
          </template>
        </EmptyState>
        <ul v-else class="recent-list">
          <li v-for="s in recentSessions" :key="s.id" class="recent-item" @click="openSession(s)">
            <div class="recent-item__main">
              <div class="recent-item__title">
                <span class="session-no">{{ s.sessionNo || `#${s.id}` }}</span>
                <StatusBadge :status="s.status" />
              </div>
              <div class="recent-item__meta">
                <span>{{ directionText(s.directions) }}</span>
                <el-divider direction="vertical" />
                <span>{{ difficultyLabel(s.difficulty) }}</span>
                <el-divider direction="vertical" />
                <span>{{ s.totalQuestion }} 题</span>
              </div>
            </div>
            <div class="recent-item__right">
              <div v-if="s.score" class="recent-item__score">{{ formatScore(s.score) }}</div>
              <div class="recent-item__time">{{ fromNow(s.createdAt) }}</div>
            </div>
          </li>
        </ul>
      </section>

      <!-- 能力提升建议 -->
      <section class="card suggest">
        <div class="card-header">
          <div class="card-title">本周期练习建议</div>
        </div>
        <ul class="suggest-list">
          <li v-for="(tip, i) in suggestList" :key="i">
            <span class="suggest-index">{{ i + 1 }}</span>
            <span class="suggest-text">{{ tip }}</span>
          </li>
        </ul>
        <div class="suggest-more">
          <el-button text type="primary" @click="go('/questions')">去题库刷题</el-button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'
import { interviewApi } from '@/api/interview'
import type { Direction, SessionDetail, UserStats } from '@/types'
import { difficultyLabel, directionLabel } from '@/utils/dict'
import { formatScore, fromNow } from '@/utils/format'
import StatusBadge from '@/components/StatusBadge/index.vue'
import PageLoading from '@/components/PageLoading/index.vue'
import EmptyState from '@/components/EmptyState/index.vue'

const router = useRouter()
const userStore = useUserStore()

const loading = ref<boolean>(false)
const stats = ref<UserStats>({
  totalSessions: 0,
  completedSessions: 0,
  averageScore: 0,
  totalQuestions: 0,
  resumeCount: 0,
  trend: [],
})
const recentSessions = ref<SessionDetail[]>([])

/** 问候语：按当前时间给出不同称呼 */
const greeting = computed<string>(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

/** 欢迎语：根据完成场次给不同引导 */
const welcomeTip = computed<string>(() => {
  if (!stats.value.totalSessions) {
    return '你还没有开始过模拟面试。选择方向、难度和题量，AI 会为你出题、实时评分并追问薄弱环节。'
  }
  const avg = Number(stats.value.averageScore || 0)
  if (avg >= 85) return '你的表现很稳定，建议挑战更高难度或系统设计方向，进一步拉开差距。'
  if (avg >= 70) return '基础扎实，重点补齐高频追问点，把项目经历讲深讲透。'
  return '建议先从中等难度入手，答完后认真阅读逐题点评与改进参考答案。'
})

/** 快捷入口 */
const quickEntries = computed(() => [
  {
    path: '/interview/setup',
    title: '开始面试',
    desc: '选方向 · 定难度 · AI 出题追问',
    icon: 'VideoPlay',
    color: '#4f46e5',
    bg: '#eef2ff',
  },
  {
    path: '/resume',
    title: '简历管理',
    desc: '粘贴或上传，AI 解析评分',
    icon: 'Document',
    color: '#0284c7',
    bg: '#e0f2fe',
  },
  {
    path: '/report',
    title: '面试报告',
    desc: '五维雷达图 · 逐题点评',
    icon: 'DataAnalysis',
    color: '#16a34a',
    bg: '#dcfce7',
  },
  {
    path: '/questions',
    title: '题库浏览',
    desc: '8 大方向 · 3 档难度',
    icon: 'Collection',
    color: '#d97706',
    bg: '#fef3c7',
  },
  {
    path: '/profile',
    title: '个人中心',
    desc: '资料 · 目标岗位 · 改密码',
    icon: 'User',
    color: '#7c3aed',
    bg: '#f3e8ff',
  },
  {
    path: '/admin/dashboard',
    title: '管理后台',
    desc: '数据统计 · 题目与用户管理',
    icon: 'Setting',
    color: '#dc2626',
    bg: '#fee2e2',
  },
])

/** 练习建议：根据平均分动态生成 */
const suggestList = computed<string[]>(() => {
  const avg = Number(stats.value.averageScore || 0)
  if (!stats.value.totalSessions) {
    return [
      '先完成一场 8 题的中等难度模拟面试，建立基线分数。',
      '上传简历并绑定会话，让 AI 围绕你的项目经历出题。',
      '答完后重点看「不足」与「改进后的参考答案」，逐条对照。',
    ]
  }
  const base: string[] = [
    '每天固定 15 分钟口述一道高频题，训练表达的结构化。',
    '针对报告中得分最低的维度，挑 3 道同方向题目专项突破。',
    '把项目经历按「背景-方案-难点-结果-复盘」五段式整理成话术。',
  ]
  if (avg < 70) base.unshift('先夯实基础：把错题的参考答案要点背熟再复述一遍。')
  if (avg >= 85) base.unshift('尝试困难难度与系统设计方向，拉开区分度。')
  return base.slice(0, 4)
})

/** 方向数组转文案 */
function directionText(list?: Direction[] | string[]): string {
  if (!list || !list.length) return '综合方向'
  return list.map((d) => directionLabel(d as Direction)).join('、')
}

/** 加载统计数据 */
async function loadStats(): Promise<void> {
  try {
    const data = await userApi.stats()
    if (data) stats.value = data
  } catch (e) {
    // 后端未就绪时保持默认值 0，页面仍可渲染
  }
}

/** 加载最近面试 */
async function loadRecent(): Promise<void> {
  loading.value = true
  try {
    const page = await interviewApi.page({ pageNum: 1, pageSize: 5 })
    recentSessions.value = page?.list || []
  } catch (e) {
    recentSessions.value = []
  } finally {
    loading.value = false
  }
}

/** 打开会话：已完成看报告，进行中回到房间 */
function openSession(session: SessionDetail): void {
  if (session.status === 'COMPLETED') {
    router.push(`/report/${session.id}`)
  } else {
    router.push(`/interview/${session.id}`)
  }
}

function go(path: string): void {
  if (path.startsWith('/admin') && !userStore.isAdmin) {
    return
  }
  router.push(path)
}
function goSetup(): void {
  router.push('/interview/setup')
}
function goResume(): void {
  router.push('/resume')
}
function goReports(): void {
  router.push('/report')
}

onMounted(() => {
  void loadStats()
  void loadRecent()
})
</script>

<style scoped lang="scss">
.dashboard {
  padding-top: 8px;
}

.welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  background: var(--grad-brand-soft);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-md), var(--shadow-inset);
}

.welcome__main {
  flex: 1;
  min-width: 0;
}

.welcome__title {
  margin: 0 0 8px;
  font-size: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.welcome__title.grad-text {
  font-weight: 700;
}

.welcome__desc {
  margin: 0 0 18px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
  max-width: 620px;
}

.welcome__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.welcome__stats {
  display: grid;
  grid-template-columns: repeat(2, 96px);
  gap: 14px;
  flex-shrink: 0;
}

.stat-mini {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 12px 8px;
  text-align: center;
}

.stat-mini__value {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-primary);
  font-variant-numeric: tabular-nums;
}

.stat-mini__label {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-secondary);
}

/* 快捷入口卡片（悬停微交互由全局 .hover-lift token 提供） */
.quick-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  cursor: pointer;
}

.quick-card__icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.quick-card__body {
  flex: 1;
  min-width: 0;
}

.quick-card__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.quick-card__desc {
  margin-top: 3px;
  font-size: 12px;
  color: var(--text-secondary);
}

.quick-card__arrow {
  color: #cbd5e1;
}

/* 两栏：最近面试 + 建议 */
.dashboard-cols {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: var(--gap-md);
  align-items: start;
}

.recent-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.recent-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 10px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.15s ease;
}

.recent-item:hover {
  background: var(--bg-hover);
}

.recent-item + .recent-item {
  border-top: 1px solid var(--border-light);
}

.recent-item__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.session-no {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.recent-item__meta {
  font-size: 12px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
}

.recent-item__right {
  text-align: right;
  flex-shrink: 0;
}

.recent-item__score {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
}

.recent-item__time {
  font-size: 12px;
  color: var(--text-secondary);
}

.suggest-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.suggest-list li {
  display: flex;
  gap: 10px;
  font-size: 13px;
  color: var(--text-regular);
  line-height: 1.7;
}

.suggest-index {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--color-primary-bg);
  color: var(--color-primary);
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.suggest-more {
  margin-top: 14px;
  text-align: right;
}

@media (max-width: 1024px) {
  .dashboard-cols {
    grid-template-columns: 1fr;
  }

  .welcome {
    flex-direction: column;
    align-items: flex-start;
  }

  .welcome__stats {
    width: 100%;
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 767px) {
  .welcome__stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
