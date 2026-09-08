<template>
  <div class="page-container report-detail">
    <PageLoading v-if="loading" :min-height="320" text="报告加载中…" />
    <EmptyState v-else-if="!report" description="未找到该场次的面试报告" min-height="240">
      <template #action>
        <el-button type="primary" size="small" @click="goBack">返回记录</el-button>
      </template>
    </EmptyState>

    <template v-else>
      <!-- 头部：总分 + 雷达 -->
      <section class="card head-card">
        <div class="head-left">
          <div class="head-meta">
            <span class="session-no">{{ report.sessionNo || `#${report.sessionId}` }}</span>
            <el-tag v-if="report.directions && report.directions.length" size="small" type="info" effect="plain">
              {{ directionsText }}
            </el-tag>
            <el-tag v-if="report.difficulty" size="small" :type="difficultyTagType(report.difficulty)">
              {{ difficultyLabel(report.difficulty) }}
            </el-tag>
            <el-tag size="small" :type="report.generatedBy === 'AI' ? 'success' : 'warning'">
              {{ report.generatedBy === 'AI' ? 'AI 生成' : '规则生成' }}
            </el-tag>
          </div>
          <div class="score-hero-wrap">
            <div class="score-hero">{{ formatScore(report.totalScore) }}</div>
            <div class="score-hero__label">总分 / 100</div>
            <div class="score-level" :class="levelClass">{{ scoreLevel(report.totalScore) }}</div>
          </div>
          <div class="head-actions">
            <el-button type="primary" :icon="Download" :loading="exporting" @click="exportMarkdown">导出 Markdown</el-button>
            <el-button :icon="Refresh" @click="loadReport">刷新</el-button>
          </div>
        </div>
        <div class="head-right">
          <RadarChart :dimensions="dimensionsData" :height="340" />
        </div>
      </section>

      <!-- 总体点评 -->
      <section v-if="report.overallComment" class="card mt-16">
        <div class="card-title">总体点评</div>
        <p class="overall">{{ report.overallComment }}</p>
      </section>

      <!-- 亮点 / 待改进 / 后续行动 -->
      <section class="grid-3 mt-16">
        <div class="card list-card">
          <div class="list-card__title list-card__title--good">
            <el-icon><CircleCheck /></el-icon>亮点
          </div>
          <ul v-if="report.highlights && report.highlights.length" class="item-list">
            <li v-for="(h, i) in report.highlights" :key="i">{{ h }}</li>
          </ul>
          <p v-else class="muted">暂无</p>
        </div>
        <div class="card list-card">
          <div class="list-card__title list-card__title--warn">
            <el-icon><WarningFilled /></el-icon>待改进
          </div>
          <ul v-if="report.improvements && report.improvements.length" class="item-list">
            <li v-for="(m, i) in report.improvements" :key="i">{{ m }}</li>
          </ul>
          <p v-else class="muted">暂无</p>
        </div>
        <div class="card list-card">
          <div class="list-card__title list-card__title--act">
            <el-icon><Promotion /></el-icon>后续行动
          </div>
          <ul v-if="report.actions && report.actions.length" class="item-list">
            <li v-for="(a, i) in report.actions" :key="i">{{ a }}</li>
          </ul>
          <p v-else class="muted">暂无</p>
        </div>
      </section>

      <!-- 逐题点评 -->
      <section class="card mt-16">
        <div class="card-header">
          <div class="card-title">逐题点评（{{ report.items ? report.items.length : 0 }} 题）</div>
        </div>

        <EmptyState v-if="!report.items || !report.items.length" description="该报告暂无逐题点评" min-height="160" />

        <div v-else class="q-card-list">
          <div v-for="item in report.items" :key="item.questionNo" class="q-card">
            <div class="q-card__head">
              <div class="q-card__no">Q{{ item.questionNo }}</div>
              <div class="q-card__title">{{ item.title }}</div>
              <div class="q-card__score" :class="scoreClass(item.score)">
                {{ formatScore(item.score) }}<span v-if="item.score != null" class="unit">分</span>
              </div>
            </div>

            <div v-if="item.phase || item.difficulty" class="q-card__tags">
              <el-tag v-if="item.phase" size="small" effect="plain" type="info">{{ phaseLabel(item.phase) }}</el-tag>
              <el-tag v-if="item.difficulty" size="small" :type="difficultyTagType(item.difficulty)">
                {{ difficultyLabel(item.difficulty) }}
              </el-tag>
              <el-tag v-if="item.skipped" size="small" type="info">已跳过</el-tag>
            </div>

            <div class="q-block">
              <div class="q-block__label">你的答案</div>
              <pre class="q-block__text">{{ item.answer || '（未作答）' }}</pre>
            </div>

            <div class="q-block">
              <div class="q-block__label">点评</div>
              <p class="q-block__text q-block__comment">{{ item.comment || '暂无点评' }}</p>
            </div>

            <div v-if="item.highlights && item.highlights.length" class="q-block">
              <div class="q-block__label q-block__label--good">亮点</div>
              <ul class="mini-list">
                <li v-for="(h, i) in item.highlights" :key="'h' + i">{{ h }}</li>
              </ul>
            </div>

            <div v-if="item.gaps && item.gaps.length" class="q-block">
              <div class="q-block__label q-block__label--warn">不足</div>
              <ul class="mini-list">
                <li v-for="(g, i) in item.gaps" :key="'g' + i">{{ g }}</li>
              </ul>
            </div>

            <div v-if="item.score != null && item.score < 80 && item.improvedAnswer" class="q-block">
              <div class="q-block__label q-block__label--act">改进后参考答案</div>
              <pre class="q-block__text q-block__improved">{{ item.improvedAnswer }}</pre>
            </div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheck, Download, Promotion, Refresh, WarningFilled } from '@element-plus/icons-vue'
import { reportApi } from '@/api/report'
import type { DimensionKey, ReportDetail, ReportItem } from '@/types'
import {
  DIMENSION_LABELS,
  difficultyLabel,
  difficultyTagType,
  directionLabel,
  phaseLabel,
} from '@/utils/dict'
import { formatScore, scoreLevel } from '@/utils/format'
import RadarChart from '@/components/RadarChart/index.vue'
import PageLoading from '@/components/PageLoading/index.vue'
import EmptyState from '@/components/EmptyState/index.vue'

const route = useRoute()
const router = useRouter()

const loading = ref<boolean>(false)
const exporting = ref<boolean>(false)
const report = ref<ReportDetail | null>(null)

const sessionId = computed<string>(() => String(route.params.sessionId))

const directionsText = computed<string>(() => {
  const dirs = report.value?.directions || []
  return dirs.map((d) => directionLabel(d)).join('、') || '综合'
})

/** 维度数据：优先 dimensions，回退 dimensionJson */
const dimensionsData = computed<Record<string, number> | null>(() => {
  const r = report.value
  if (!r) return null
  return (r.dimensions || r.dimensionJson || null) as Record<string, number> | null
})

function scoreClass(score?: number | null): string {
  const n = Number(score ?? 0)
  if (n >= 80) return 'score-good'
  if (n >= 60) return 'score-mid'
  return 'score-bad'
}

const levelClass = computed<string>(() => {
  const lvl = scoreLevel(report.value?.totalScore)
  if (lvl === '优秀' || lvl === '良好') return 'level-good'
  if (lvl === '中等' || lvl === '及格') return 'level-mid'
  return 'level-bad'
})

async function loadReport(): Promise<void> {
  loading.value = true
  try {
    const data = await reportApi.getBySession(sessionId.value)
    report.value = data
  } catch (e) {
    report.value = null
  } finally {
    loading.value = false
  }
}

async function exportMarkdown(): Promise<void> {
  exporting.value = true
  try {
    const md = await reportApi.exportMarkdown(sessionId.value)
    if (!md) {
      ElMessage.warning('当前报告暂不支持导出')
      return
    }
    const blob = new Blob([md], { type: 'text/markdown;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const no = report.value?.sessionNo || `report-${sessionId.value}`
    a.download = `${no}-面试报告.md`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('已导出 Markdown')
  } catch (e) {
    /* 已统一提示 */
  } finally {
    exporting.value = false
  }
}

function goBack(): void {
  router.push('/interview')
}

onMounted(() => {
  void loadReport()
})
</script>

<style scoped lang="scss">
.report-detail {
  padding-bottom: 40px;
}

.head-card {
  display: flex;
  gap: 24px;
  align-items: stretch;
}

.head-left {
  flex: 0 0 320px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.head-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.session-no {
  font-weight: 700;
  font-size: 15px;
  color: var(--text-primary);
}

.score-hero-wrap {
  text-align: center;
  padding: 8px 0;
}

.score-hero__label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.score-level {
  display: inline-block;
  margin-top: 8px;
  padding: 2px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.level-good {
  background: #dcfce7;
  color: #16a34a;
}
.level-mid {
  background: #fef3c7;
  color: #d97706;
}
.level-bad {
  background: #fee2e2;
  color: #dc2626;
}

.head-actions {
  margin-top: auto;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.head-right {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.overall {
  margin: 8px 0 0;
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-regular);
}

.grid-3 {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--gap-md);
}

.list-card__title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}

.list-card__title--good {
  color: var(--color-success);
}
.list-card__title--warn {
  color: var(--color-warning);
}
.list-card__title--act {
  color: var(--color-primary);
}

.item-list {
  margin: 0;
  padding-left: 20px;
}

.item-list li {
  font-size: 13px;
  color: var(--text-regular);
  line-height: 1.9;
}

.q-card-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.q-card {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 16px;
  background: #fcfcff;
}

.q-card__head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.q-card__no {
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: var(--color-primary-bg);
  color: var(--color-primary);
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.q-card__title {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.6;
}

.q-card__score {
  flex-shrink: 0;
  font-size: 22px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.q-card__score .unit {
  font-size: 12px;
  font-weight: 400;
  margin-left: 2px;
}

.score-good {
  color: var(--color-success);
}
.score-mid {
  color: var(--color-warning);
}
.score-bad {
  color: var(--color-danger);
}

.q-card__tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin: 10px 0;
}

.q-block {
  margin-top: 12px;
}

.q-block__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.q-block__label--good {
  color: var(--color-success);
}
.q-block__label--warn {
  color: var(--color-warning);
}
.q-block__label--act {
  color: var(--color-primary);
}

.q-block__text {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-regular);
  white-space: pre-wrap;
  word-break: break-word;
}

.q-block__comment {
  white-space: pre-wrap;
}

.q-block__improved {
  background: var(--color-primary-bg);
  border-radius: var(--radius-sm);
  padding: 12px;
}

.mini-list {
  margin: 0;
  padding-left: 20px;
}

.mini-list li {
  font-size: 13px;
  color: var(--text-regular);
  line-height: 1.8;
}

.muted {
  color: var(--text-secondary);
  font-size: 13px;
}

@media (max-width: 1024px) {
  .head-card {
    flex-direction: column;
  }

  .head-left {
    flex: 1;
  }

  .grid-3 {
    grid-template-columns: 1fr;
  }
}
</style>
