<template>
  <div class="page-container replay">
    <section class="card">
      <div class="replay-header">
        <el-button text :icon="ArrowLeft" @click="goBack">返回列表</el-button>
        <h2 class="replay-title">面试回放</h2>
      </div>

      <!-- 加载态 -->
      <el-skeleton v-if="loading" :rows="5" animated />

      <!-- 接口失败：错误提示 + 重试 -->
      <el-result
        v-else-if="error"
        icon="error"
        title="加载失败"
        :sub-title="error"
      >
        <template #extra>
          <el-button type="primary" @click="load">重试</el-button>
          <el-button @click="goBack">返回列表</el-button>
        </template>
      </el-result>

      <template v-else>
        <!-- 顶部会话概览 -->
        <div v-if="meta" class="meta-bar">
          <div class="meta-item">
            <span class="meta-label">会话方向</span>
            <span class="meta-value">
              <span v-if="!meta.directions || !meta.directions.length" class="muted">综合</span>
              <el-tag
                v-for="d in meta.directions"
                v-else
                :key="d"
                size="small"
                type="info"
                effect="plain"
                class="dir-tag"
              >{{ directionLabel(d) }}</el-tag>
            </span>
          </div>
          <div class="meta-item">
            <span class="meta-label">难度</span>
            <el-tag size="small" :type="difficultyTagType(meta.difficulty)">
              {{ difficultyLabel(meta.difficulty) }}
            </el-tag>
          </div>
          <div class="meta-item">
            <span class="meta-label">总分</span>
            <span
              v-if="meta.score !== null && meta.score !== undefined"
              :class="scoreClass(meta.score)"
            >{{ formatScore(meta.score) }}</span>
            <span v-else class="muted">-</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">完成时间</span>
            <span class="meta-value">{{ meta.finishedAt ? formatTime(meta.finishedAt) : '未完成' }}</span>
          </div>
        </div>

        <!-- 空数据 -->
        <EmptyState
          v-if="!questions.length"
          description="该会话暂无题目或作答记录"
          min-height="180"
        />

        <!-- 逐题时间线 -->
        <el-timeline v-else class="replay-timeline">
          <el-timeline-item
            v-for="q in questions"
            :key="q.questionNo"
            :timestamp="`第 ${q.questionNo} 题`"
            placement="top"
            type="primary"
          >
            <div class="question-card">
              <div class="q-head">
                <span class="q-no">第 {{ q.questionNo }} 题</span>
                <el-tag
                  v-if="q.score !== undefined && q.score !== null"
                  :type="scoreTagType(q.score)"
                  size="small"
                >得分 {{ formatScore(q.score) }}</el-tag>
              </div>

              <div class="q-block">
                <div class="q-label">题目</div>
                <div class="q-content">{{ q.question || '（无题目内容）' }}</div>
              </div>
              <div class="q-block">
                <div class="q-label">我的回答</div>
                <div class="q-content">{{ q.answer || '（未作答）' }}</div>
              </div>

              <!-- AI 点评：亮点 / 不足 / 改进建议 -->
              <template v-if="hasReview(q)">
                <el-divider content-position="left">AI 点评</el-divider>
                <div v-if="q.comment" class="q-block">
                  <div class="q-label">点评</div>
                  <div class="q-content">{{ q.comment }}</div>
                </div>
                <div v-if="q.highlights && q.highlights.length" class="q-block">
                  <div class="q-label">亮点</div>
                  <ul class="tag-list">
                    <li v-for="(h, i) in q.highlights" :key="i" class="hl">{{ h }}</li>
                  </ul>
                </div>
                <div v-if="q.gaps && q.gaps.length" class="q-block">
                  <div class="q-label">不足</div>
                  <ul class="tag-list">
                    <li v-for="(g, i) in q.gaps" :key="i" class="gap">{{ g }}</li>
                  </ul>
                </div>
                <div v-if="q.improvedAnswer" class="q-block">
                  <div class="q-label">改进建议</div>
                  <div class="q-content improve">{{ q.improvedAnswer }}</div>
                </div>
              </template>

              <!-- 追问与追问回答 -->
              <template v-if="q.followUps.length">
                <el-divider content-position="left">追问</el-divider>
                <div v-for="(f, i) in q.followUps" :key="i" class="followup">
                  <div class="q-label">追问 {{ i + 1 }}</div>
                  <div class="q-content">{{ f.question || '（无追问内容）' }}</div>
                  <div class="q-label q-answer-label">追问回答</div>
                  <div class="q-content">{{ f.answer || '（未作答）' }}</div>
                </div>
              </template>
            </div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { interviewApi } from '@/api/interview'
import type { Difficulty, Direction, SessionDetail } from '@/types'
import { directionLabel, difficultyLabel, difficultyTagType } from '@/utils/dict'
import { formatScore, formatTime } from '@/utils/format'
import EmptyState from '@/components/EmptyState/index.vue'

/**
 * messages 接口为扁平流水，后端可能额外返回点评细分字段。
 * 这里用可选字段兼容，渲染时按需展示。
 */
interface ReplayMessage {
  id: number
  role: 'INTERVIEWER' | 'CANDIDATE'
  content: string
  questionNo?: number
  score?: number
  comment?: string
  highlights?: string[]
  gaps?: string[]
  improvedAnswer?: string
  isFollowUp?: boolean
  createdAt?: string
}

interface ReplayFollowUp {
  question: string
  answer: string
}

interface ReplayQuestion {
  questionNo: number
  question: string
  answer: string
  score?: number
  comment?: string
  highlights?: string[]
  gaps?: string[]
  improvedAnswer?: string
  followUps: ReplayFollowUp[]
}

/** 按 questionNo 将扁平流水重组成逐题结构 */
function groupMessages(messages: ReplayMessage[]): ReplayQuestion[] {
  const map = new Map<number, ReplayQuestion>()
  const order: number[] = []

  for (const msg of messages) {
    const qNo = msg.questionNo ?? 0
    if (!map.has(qNo)) {
      map.set(qNo, { questionNo: qNo, question: '', answer: '', followUps: [] })
      order.push(qNo)
    }
    const item = map.get(qNo) as ReplayQuestion

    if (msg.role === 'INTERVIEWER') {
      if (!item.question) {
        // 首条 INTERVIEWER 消息为主问题
        item.question = msg.content
      } else {
        // 其余为追问，等待对应回答填入
        item.followUps.push({ question: msg.content, answer: '' })
      }
    } else {
      // CANDIDATE 作答：先填主回答，再填未完成的追问
      if (!item.answer) {
        item.answer = msg.content
        if (msg.score !== undefined) item.score = msg.score
        item.comment = msg.comment
        item.highlights = msg.highlights
        item.gaps = msg.gaps
        item.improvedAnswer = msg.improvedAnswer
      } else {
        const pending = [...item.followUps].reverse().find((f) => !f.answer)
        if (pending) pending.answer = msg.content
      }
    }
  }

  return order.map((n) => map.get(n) as ReplayQuestion)
}

function hasReview(q: ReplayQuestion): boolean {
  return Boolean(
    q.comment ||
      (q.highlights && q.highlights.length) ||
      (q.gaps && q.gaps.length) ||
      q.improvedAnswer,
  )
}

/** 评分着色：≥80 绿、60-79 橙、<60 红 */
function scoreTagType(score?: number | null): 'success' | 'warning' | 'danger' {
  const n = Number(score ?? 0)
  if (n >= 80) return 'success'
  if (n >= 60) return 'warning'
  return 'danger'
}

function scoreClass(score?: number | null): string {
  const n = Number(score ?? 0)
  if (n >= 80) return 'score-good'
  if (n >= 60) return 'score-mid'
  return 'score-bad'
}

const route = useRoute()
const router = useRouter()

const sessionId = computed<number>(() => Number(route.params.id))
const loading = ref<boolean>(true)
const error = ref<string>('')
const questions = ref<ReplayQuestion[]>([])
const meta = ref<SessionDetail | null>(null)

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    const [detailResp, msgResp] = await Promise.all([
      interviewApi.detail(sessionId.value),
      interviewApi.messages(sessionId.value),
    ])
    meta.value = detailResp
    questions.value = groupMessages(msgResp as ReplayMessage[])
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败，请稍后重试'
    questions.value = []
  } finally {
    loading.value = false
  }
}

function goBack(): void {
  router.push('/interview')
}

onMounted(() => {
  void load()
})
</script>

<style scoped lang="scss">
.replay-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.replay-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
}

.meta-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  padding: 16px;
  margin-bottom: 20px;
  background: var(--bg-secondary, #f7f8fa);
  border-radius: 8px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.meta-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.meta-value {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.dir-tag {
  margin-right: 4px;
}

.replay-timeline {
  padding-top: 8px;
}

.question-card {
  padding: 4px 0;
}

.q-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.q-no {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
}

.q-block {
  margin-bottom: 12px;
}

.q-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-regular);
  margin-bottom: 4px;
}

.q-answer-label {
  margin-top: 8px;
}

.q-content {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.q-content.improve {
  background: rgba(64, 158, 255, 0.06);
  border-left: 3px solid var(--el-color-primary);
  padding: 8px 12px;
  border-radius: 4px;
}

.tag-list {
  margin: 0;
  padding-left: 18px;
}

.tag-list li {
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-primary);
}

.tag-list li.hl {
  color: var(--color-success, #67c23a);
}

.tag-list li.gap {
  color: var(--color-danger, #f56c6c);
}

.followup {
  margin-bottom: 12px;
  padding: 12px;
  background: var(--bg-secondary, #f7f8fa);
  border-radius: 6px;
}

.score-good {
  color: var(--color-success, #67c23a);
  font-weight: 600;
}
.score-mid {
  color: var(--color-warning, #e6a23c);
  font-weight: 600;
}
.score-bad {
  color: var(--color-danger, #f56c6c);
  font-weight: 600;
}

.muted {
  color: var(--text-secondary);
}

@media (max-width: 767px) {
  .meta-bar {
    gap: 16px;
  }
}
</style>
