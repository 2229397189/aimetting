<template>
  <div class="page-container admin-dashboard">
    <BackBar to="/" label="返回首页" />
    <section class="card">
      <div class="card-header">
        <div class="card-title">数据看板</div>
        <el-button text type="primary" :icon="Refresh" @click="loadAll">刷新</el-button>
      </div>

      <PageLoading v-if="overviewLoading" :min-height="200" text="加载中…" />
      <div v-else class="overview-grid">
        <div class="ov-card hover-lift">
          <div class="ov-card__value">{{ overview.userCount }}</div>
          <div class="ov-card__label">用户数</div>
        </div>
        <div class="ov-card hover-lift">
          <div class="ov-card__value">{{ overview.sessionCount }}</div>
          <div class="ov-card__label">会话数</div>
        </div>
        <div class="ov-card hover-lift">
          <div class="ov-card__value">{{ overview.completedCount }}</div>
          <div class="ov-card__label">完成数</div>
        </div>
        <div class="ov-card hover-lift">
          <div class="ov-card__value">{{ (overview.completionRate * 100).toFixed(1) }}%</div>
          <div class="ov-card__label">完成率</div>
        </div>
        <div class="ov-card hover-lift">
          <div class="ov-card__value">{{ formatScore(overview.averageScore) }}</div>
          <div class="ov-card__label">平均分</div>
        </div>
        <div class="ov-card hover-lift">
          <div class="ov-card__value">{{ overview.questionCount }}</div>
          <div class="ov-card__label">题目数</div>
        </div>
      </div>
    </section>

    <div class="admin-cols mt-16">
      <!-- 会话趋势 -->
      <section class="card">
        <div class="card-header">
          <div class="card-title">会话趋势（近 {{ trend.days || 7 }} 天）</div>
        </div>
        <PageLoading v-if="trendLoading" :min-height="240" text="加载中…" />
        <EmptyState v-else-if="!trend.dates || !trend.dates.length" description="暂无趋势数据" min-height="200" />
        <div v-else ref="trendChartRef" class="chart-box"></div>
      </section>

      <!-- 方向分布 -->
      <section class="card">
        <div class="card-header">
          <div class="card-title">方向分布</div>
        </div>
        <PageLoading v-if="trendLoading" :min-height="240" text="加载中…" />
        <EmptyState v-else-if="!directionRows.length" description="暂无分布数据" min-height="200" />
        <div v-else ref="distChartRef" class="chart-box"></div>
      </section>
    </div>

    <!-- AI 调用健康 -->
    <section class="card mt-16">
      <div class="card-header">
        <div class="card-title">AI 服务健康</div>
        <el-tag :type="health.circuitBreakerOpen ? 'danger' : 'success'" size="small">
          {{ health.circuitBreakerOpen ? '熔断已开启' : '熔断关闭' }}
        </el-tag>
      </div>

      <PageLoading v-if="healthLoading" :min-height="120" text="加载中…" />
      <div v-else class="health-grid">
        <div class="health-item hover-lift">
          <div class="health-item__label">Provider</div>
          <div class="health-item__value">{{ health.provider || '-' }}</div>
        </div>
        <div class="health-item hover-lift">
          <div class="health-item__label">模型</div>
          <div class="health-item__value">{{ health.model || '-' }}</div>
        </div>
        <div class="health-item hover-lift">
          <div class="health-item__label">运行模式</div>
          <div class="health-item__value">
            <el-tag size="small" :type="health.mock ? 'warning' : 'success'">
              {{ health.mock ? 'Mock 模式' : '真实调用' }}
            </el-tag>
          </div>
        </div>
        <div class="health-item hover-lift">
          <div class="health-item__label">可用性</div>
          <div class="health-item__value">
            <el-tag size="small" :type="health.available === false ? 'danger' : 'success'">
              {{ health.available === false ? '不可用' : '正常' }}
            </el-tag>
          </div>
        </div>
        <div class="health-item hover-lift">
          <div class="health-item__label">舱壁占用</div>
          <div class="health-item__value">
            {{ health.bulkheadInUse ?? '-' }} / {{ health.bulkheadTotal ?? '-' }}
          </div>
        </div>
      </div>

      <el-alert
        v-if="health.message"
        class="mt-16"
        :title="health.message"
        type="info"
        :closable="false"
        show-icon
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { adminApi } from '@/api/admin'
import type { AiHealthResp, OverviewStats, SessionTrend } from '@/types'
import { directionLabel } from '@/utils/dict'
import { formatScore } from '@/utils/format'
import { useChartTheme } from '@/composables/useChartTheme'
import PageLoading from '@/components/PageLoading/index.vue'
import EmptyState from '@/components/EmptyState/index.vue'
import BackBar from '@/components/BackBar/index.vue'

const overviewLoading = ref<boolean>(false)
const trendLoading = ref<boolean>(false)
const healthLoading = ref<boolean>(false)

const overview = reactive<OverviewStats>({
  userCount: 0,
  sessionCount: 0,
  completedCount: 0,
  completionRate: 0,
  averageScore: 0,
  questionCount: 0,
})

const trend = reactive<SessionTrend>({
  days: 7,
  dates: [],
  counts: [],
  directionDistribution: [],
})

const health = reactive<AiHealthResp>({
  provider: '',
  model: '',
  mock: false,
  available: true,
  circuitBreakerOpen: false,
  bulkheadInUse: 0,
  bulkheadTotal: 0,
  message: '',
})

/* ---------- 图表 ---------- */
const trendChartRef = ref<HTMLDivElement | null>(null)
const distChartRef = ref<HTMLDivElement | null>(null)
let trendChart: echarts.ECharts | null = null
let distChart: echarts.ECharts | null = null
let resizeHandler: (() => void) | null = null

const { isDark, palette } = useChartTheme()

const directionRows = computed(() =>
  (trend.directionDistribution || []).map((d) => ({
    name: d.label || directionLabel(d.direction),
    value: d.count,
  })),
)

function renderTrendChart(): void {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  const c = palette.value
  trendChart.setOption(
    {
      textStyle: { color: c.text },
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 24, top: 24, bottom: 36 },
      xAxis: {
        type: 'category',
        data: trend.dates,
        boundaryGap: false,
        axisLine: { lineStyle: { color: c.axisLine } },
        axisLabel: { color: c.text },
        splitLine: { lineStyle: { color: c.splitLine } },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLine: { lineStyle: { color: c.axisLine } },
        axisLabel: { color: c.text },
        splitLine: { lineStyle: { color: c.splitLine } },
      },
      series: [
        {
          name: '会话数',
          type: 'line',
          smooth: true,
          data: trend.counts,
          itemStyle: { color: '#4f46e5' },
          areaStyle: { color: 'rgba(79,70,229,0.12)' },
        },
      ],
    },
    true,
  )
}

function renderDistChart(): void {
  if (!distChartRef.value) return
  if (!distChart) distChart = echarts.init(distChartRef.value)
  const c = palette.value
  distChart.setOption(
    {
      textStyle: { color: c.text },
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 0, type: 'scroll', textStyle: { color: c.text } },
      series: [
        {
          name: '方向分布',
          type: 'pie',
          radius: ['42%', '66%'],
          center: ['50%', '46%'],
          data: directionRows.value,
          label: { formatter: '{b}\n{c}', color: c.text },
        },
      ],
    },
    true,
  )
}

/* ---------- 数据加载 ---------- */
async function loadOverview(): Promise<void> {
  overviewLoading.value = true
  try {
    const data = await adminApi.overview()
    if (data) Object.assign(overview, data)
  } catch (e) {
    /* 后端未就绪时保持 0 */
  } finally {
    overviewLoading.value = false
  }
}

async function loadTrend(): Promise<void> {
  trendLoading.value = true
  try {
    const data = await adminApi.sessionTrend(7)
    if (data) {
      trend.days = data.days || 7
      trend.dates = data.dates || []
      trend.counts = data.counts || []
      trend.directionDistribution = data.directionDistribution || []
    }
    await nextTick()
    renderTrendChart()
    renderDistChart()
  } catch (e) {
    trend.dates = []
    trend.counts = []
    trend.directionDistribution = []
  } finally {
    trendLoading.value = false
  }
}

async function loadHealth(): Promise<void> {
  healthLoading.value = true
  try {
    const data = await adminApi.aiHealth()
    if (data) Object.assign(health, data)
  } catch (e) {
    /* 保持默认 */
  } finally {
    healthLoading.value = false
  }
}

async function loadAll(): Promise<void> {
  await Promise.all([loadOverview(), loadTrend(), loadHealth()])
}

onMounted(() => {
  void loadAll()
  resizeHandler = () => {
    if (trendChart) trendChart.resize()
    if (distChart) distChart.resize()
  }
  window.addEventListener('resize', resizeHandler)
})

// 主题切换时重绘两张图表配色
watch(isDark, () => {
  renderTrendChart()
  renderDistChart()
})

onBeforeUnmount(() => {
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  trendChart?.dispose()
  distChart?.dispose()
  trendChart = null
  distChart = null
})
</script>

<style scoped lang="scss">
.admin-dashboard {
  padding-bottom: 40px;
  background: var(--grad-page);
  border-radius: var(--radius-lg);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.ov-card {
  background: var(--grad-brand-soft);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 16px 10px;
  text-align: center;
}

.ov-card__value {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-primary);
  font-variant-numeric: tabular-nums;
}

.ov-card__label {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

.admin-cols {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: var(--gap-md);
  align-items: start;
}

.chart-box {
  width: 100%;
  height: 260px;
}

.health-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.health-item {
  background: var(--bg-hover);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  padding: 14px;
  text-align: center;
}

.health-item__label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.health-item__value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

@media (max-width: 1279px) {
  .overview-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1024px) {
  .admin-cols {
    grid-template-columns: 1fr;
  }
  .health-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 767px) {
  .overview-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
