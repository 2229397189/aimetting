<template>
  <div class="page-container profile-page">
    <!-- 我的统计 -->
    <section class="card">
      <div class="card-header">
        <div class="card-title">我的统计</div>
        <el-button text type="primary" :icon="Refresh" @click="loadStats">刷新</el-button>
      </div>

      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-card__value">{{ stats.totalSessions }}</div>
          <div class="stat-card__label">总场次</div>
        </div>
        <div class="stat-card">
          <div class="stat-card__value">{{ stats.completedSessions }}</div>
          <div class="stat-card__label">完成场次</div>
        </div>
        <div class="stat-card">
          <div class="stat-card__value">{{ formatScore(stats.averageScore) }}</div>
          <div class="stat-card__label">平均分</div>
        </div>
        <div class="stat-card">
          <div class="stat-card__value">{{ stats.totalQuestions }}</div>
          <div class="stat-card__label">总题数</div>
        </div>
        <div class="stat-card">
          <div class="stat-card__value">{{ stats.resumeCount }}</div>
          <div class="stat-card__label">简历数</div>
        </div>
      </div>

      <el-divider content-position="left">练习趋势</el-divider>
      <PageLoading v-if="statsLoading" :min-height="220" text="加载中…" />
      <EmptyState v-else-if="!trendDates.length" description="暂无趋势数据" min-height="160" />
      <div v-else ref="trendChartRef" class="trend-chart"></div>
    </section>

    <!-- 资料表单 -->
    <section class="card mt-16">
      <div class="card-header">
        <div class="card-title">个人资料</div>
        <div class="flex gap-8">
          <el-button :icon="Key" @click="openPasswordDialog">修改密码</el-button>
          <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</el-button>
        </div>
      </div>

      <PageLoading v-if="profileLoading" :min-height="200" text="加载中…" />
      <el-form v-else :model="profileForm" label-position="top" class="profile-form">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="昵称">
              <el-input v-model="profileForm.nickname" placeholder="展示名称" maxlength="30" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="目标岗位">
              <el-input v-model="profileForm.targetPosition" placeholder="如：Java 后端开发" maxlength="60" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="工作年限">
              <el-input-number v-model="profileForm.workYears" :min="0" :max="50" :step="1" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" placeholder="选填" maxlength="20" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="个人简介">
              <el-input
                v-model="profileForm.intro"
                type="textarea"
                :rows="4"
                placeholder="一句话介绍你的背景、擅长方向等"
                maxlength="300"
                show-word-limit
                resize="vertical"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </section>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="420px" @closed="resetPwdForm">
      <el-form :model="pwdForm" label-position="top">
        <el-form-item label="原密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="8~32 位，建议含字母与数字" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPwd" @click="savePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Key, Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { userApi } from '@/api/user'
import { useUserStore } from '@/stores/user'
import type { UpdateProfileReq, UserProfile, UserStats } from '@/types'
import { formatScore } from '@/utils/format'

const userStore = useUserStore()

const profileLoading = ref<boolean>(false)
const savingProfile = ref<boolean>(false)
const statsLoading = ref<boolean>(false)

const profileForm = reactive<UpdateProfileReq & { nickname: string }>({
  nickname: '',
  targetPosition: '',
  workYears: 0,
  intro: '',
  phone: '',
})

const stats = ref<UserStats>({
  totalSessions: 0,
  completedSessions: 0,
  averageScore: 0,
  totalQuestions: 0,
  resumeCount: 0,
  trend: [],
})

/* ---------- 趋势图 ---------- */
const trendChartRef = ref<HTMLDivElement | null>(null)
let trendChart: echarts.ECharts | null = null
let resizeHandler: (() => void) | null = null

const trendDates = computed<string[]>(() => (stats.value.trend || []).map((t) => t.date))

function renderTrend(): void {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  const trend = stats.value.trend || []
  const dates = trend.map((t) => t.date)
  const counts = trend.map((t) => t.count)
  const scores = trend.map((t) => (t.score != null ? t.score : null))
  trendChart.setOption(
    {
      tooltip: { trigger: 'axis' },
      legend: { data: ['场次', '平均分'], bottom: 0 },
      grid: { left: 40, right: 40, top: 24, bottom: 48 },
      xAxis: { type: 'category', data: dates, boundaryGap: false },
      yAxis: [
        { type: 'value', name: '场次', minInterval: 1 },
        { type: 'value', name: '分数', min: 0, max: 100 },
      ],
      series: [
        {
          name: '场次',
          type: 'line',
          smooth: true,
          data: counts,
          itemStyle: { color: '#4f46e5' },
          areaStyle: { color: 'rgba(79,70,229,0.12)' },
        },
        {
          name: '平均分',
          type: 'line',
          smooth: true,
          yAxisIndex: 1,
          data: scores,
          connectNulls: true,
          itemStyle: { color: '#16a34a' },
        },
      ],
    },
    true,
  )
}

/* ---------- 加载数据 ---------- */
async function loadProfile(): Promise<void> {
  profileLoading.value = true
  try {
    const data = await userApi.profile()
    profileForm.nickname = data.nickname || ''
    profileForm.targetPosition = data.targetPosition || ''
    profileForm.workYears = data.workYears ?? 0
    profileForm.intro = data.intro || ''
    profileForm.phone = data.phone || ''
  } catch (e) {
    /* 已统一提示 */
  } finally {
    profileLoading.value = false
  }
}

async function loadStats(): Promise<void> {
  statsLoading.value = true
  try {
    const data = await userApi.stats()
    if (data) stats.value = data
    await nextTick()
    renderTrend()
  } catch (e) {
    /* 后端未就绪时使用默认值 */
  } finally {
    statsLoading.value = false
  }
}

async function saveProfile(): Promise<void> {
  savingProfile.value = true
  try {
    const payload: UpdateProfileReq = {
      nickname: profileForm.nickname,
      targetPosition: profileForm.targetPosition || undefined,
      workYears: profileForm.workYears,
      intro: profileForm.intro || undefined,
      phone: profileForm.phone || undefined,
    }
    await userApi.updateProfile(payload)
    ElMessage.success('资料已保存')
    // 同步本地展示名
    const cur = userStore.userInfo as UserProfile | null
    if (cur) {
      userStore.setUserInfo({ ...cur, nickname: payload.nickname || cur.nickname })
    }
  } catch (e) {
    /* 已统一提示 */
  } finally {
    savingProfile.value = false
  }
}

/* ---------- 修改密码 ---------- */
const pwdDialogVisible = ref<boolean>(false)
const savingPwd = ref<boolean>(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '' })

function openPasswordDialog(): void {
  pwdDialogVisible.value = true
}
function resetPwdForm(): void {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
}

async function savePassword(): Promise<void> {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写原密码与新密码')
    return
  }
  if (pwdForm.newPassword.length < 8 || pwdForm.newPassword.length > 32) {
    ElMessage.warning('新密码长度需为 8~32 位')
    return
  }
  savingPwd.value = true
  try {
    await userApi.changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    pwdDialogVisible.value = false
    await userStore.logout()
  } catch (e) {
    /* 已统一提示 */
  } finally {
    savingPwd.value = false
  }
}

onMounted(() => {
  void loadProfile()
  void loadStats()
  resizeHandler = () => {
    if (trendChart) trendChart.resize()
  }
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
})
</script>

<style scoped lang="scss">
.profile-page {
  padding-bottom: 40px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.stat-card {
  background: linear-gradient(120deg, #ffffff 0%, #f8f9ff 100%);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 16px 12px;
  text-align: center;
}

.stat-card__value {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-primary);
  font-variant-numeric: tabular-nums;
}

.stat-card__label {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

.trend-chart {
  width: 100%;
  height: 260px;
}

.profile-form {
  max-width: 880px;
}

@media (max-width: 1024px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
