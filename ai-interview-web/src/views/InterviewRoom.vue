<template>
  <div class="room">
    <!-- 顶部状态栏 -->
    <header class="room-header">
      <div class="room-header__inner">
        <div class="room-header__left">
          <el-button text @click="exitRoom">
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          <div>
            <div class="room-title">
              模拟面试
              <span class="room-no">{{ session?.sessionNo || `#${sessionId}` }}</span>
              <StatusBadge v-if="session" :status="session.status" />
            </div>
            <div class="room-sub">
              {{ directionText }} · {{ difficultyLabel(session?.difficulty) }} · 共
              {{ session?.totalQuestion || 0 }} 题
              <span v-if="session?.jdText" class="jd-flag">
                <el-icon><Document /></el-icon>已绑定 JD
              </span>
            </div>
          </div>
        </div>

        <div class="room-header__right">
          <el-button v-if="!isPaused" :disabled="busy" @click="pauseSession">
            <el-icon><VideoPause /></el-icon>暂停
          </el-button>
          <el-button v-else :disabled="busy" type="primary" plain @click="resumeSession">
            <el-icon><VideoPlay /></el-icon>恢复
          </el-button>
          <el-button type="danger" plain :disabled="busy" @click="finishSession">
            <el-icon><CircleCheck /></el-icon>结束并出报告
          </el-button>
        </div>
      </div>

      <!-- 进度条：整体 + 三阶段 -->
      <div class="room-progress">
        <el-progress
          :percentage="overallPercent"
          :stroke-width="6"
          :show-text="false"
          color="#4f46e5"
        />
        <div class="phase-bar">
          <div
            v-for="p in phaseList"
            :key="p.phase"
            class="phase-chip"
            :class="{ active: p.phase === currentPhase.phase, done: p.done }"
          >
            <span class="phase-chip__label">{{ p.label }}</span>
            <span class="phase-chip__count">{{ p.index }}/{{ p.count }}</span>
          </div>
        </div>
      </div>
    </header>

    <div class="room-body">
      <!-- 主区：题目 + 作答 + 评分 -->
      <main class="room-main">
        <!-- 降级提示 -->
        <DegradedTip v-model="degraded" class="mb-16" />

        <!-- 题目卡 -->
        <section class="card question-card">
          <div class="question-card__head">
            <div class="question-tags">
              <el-tag type="primary" size="small" effect="dark">{{ currentPhase.phaseLabel }}</el-tag>
              <el-tag size="small" effect="plain">
                第 {{ questionNo }} 题 / 共 {{ session?.totalQuestion || 0 }} 题
              </el-tag>
              <el-tag size="small" effect="plain">
                本阶段 {{ currentPhase.indexInPhase }}/{{ currentPhase.countInPhase }}
              </el-tag>
              <el-tag v-if="currentQuestion?.source" size="small" effect="light" type="info">
                {{ sourceLabel(currentQuestion.source) }}
              </el-tag>
              <el-tag v-if="currentQuestion?.difficulty" size="small" :type="difficultyTagType(currentQuestion.difficulty)">
                {{ difficultyLabel(currentQuestion.difficulty) }}
              </el-tag>
            </div>
            <div class="question-actions">
              <el-button v-if="referencePoints.length" text size="small" @click="showPoints = !showPoints">
                {{ showPoints ? '隐藏' : '查看' }}考察点
              </el-button>
            </div>
          </div>

          <h2 v-if="currentQuestion" class="question-title">{{ currentQuestion.title }}</h2>
          <PageLoading v-else :min-height="120" :text="loadingQuestion ? 'AI 正在出题…' : '加载中…'" />

          <div v-if="showPoints && referencePoints.length" class="reference-points">
            <div class="reference-points__title">考察要点</div>
            <ul>
              <li v-for="(p, i) in referencePoints" :key="i">{{ p }}</li>
            </ul>
          </div>

          <!-- 追问链 -->
          <div v-if="followUpChain.length" class="follow-chain">
            <div class="follow-chain__title">
              追问链（{{ followUpChain.length }}/{{ maxFollowUp }}）
            </div>
            <div v-for="(f, i) in followUpChain" :key="i" class="follow-item">
              <div class="follow-item__q">
                <span class="follow-index">追问 {{ i + 1 }}</span>{{ f.title }}
              </div>
              <div v-if="f.content" class="follow-item__a">
                <div class="follow-item__label">我的回答</div>
                <div class="follow-item__text">{{ f.content }}</div>
              </div>
              <div v-if="f.score !== undefined && f.score !== null" class="follow-item__score">
                {{ f.score }} 分
              </div>
            </div>
          </div>
        </section>

        <!-- 追问区（待作答） -->
        <section v-if="pendingFollowUp" class="card follow-up-card">
          <div class="card-header">
            <div class="card-title">
              追问 {{ pendingFollowUp.followUpCount }}/{{ pendingFollowUp.maxFollowUp }}
            </div>
            <el-tag size="small" type="warning" effect="light">答完后进入下一题</el-tag>
          </div>
          <p class="follow-up-question">{{ pendingFollowUp.title }}</p>
          <el-input
            v-model="followUpAnswer"
            type="textarea"
            :rows="5"
            :maxlength="answerMaxLength"
            placeholder="针对追问继续作答（10 ~ 5000 字）"
            resize="vertical"
            :disabled="submitting"
          />
          <div class="answer-foot">
            <span class="char-count" :class="{ danger: followUpLength < answerMinLength }">
              {{ followUpLength }} / {{ answerMaxLength }}
            </span>
            <el-button type="primary" :loading="submitting" @click="submitFollowUpAnswer">
              提交追问答案
            </el-button>
          </div>
        </section>

        <!-- 作答区 -->
        <section v-if="currentQuestion && !pendingFollowUp" class="card answer-card">
          <div class="card-header">
            <div class="card-title">你的回答</div>
            <span class="draft-tip">{{ draftTip }}</span>
          </div>

          <el-input
            v-model="answer"
            type="textarea"
            :rows="9"
            :maxlength="answerMaxLength"
            placeholder="请尽量结构化作答：先给结论，再讲原理，最后结合项目举例（10 ~ 5000 字）"
            resize="vertical"
            :disabled="submitting || isPaused"
            @input="onAnswerInput"
          />

          <div class="answer-foot">
            <span class="char-count" :class="{ danger: answerLength < answerMinLength }">
              {{ answerLength }} / {{ answerMaxLength }}
              <span v-if="answerLength < answerMinLength" class="char-count__hint">
                （至少 {{ answerMinLength }} 字）
              </span>
            </span>
            <div class="answer-foot__right">
              <el-button :disabled="submitting || isPaused" @click="skipQuestion">跳过本题</el-button>
              <el-button v-if="streaming" type="danger" plain @click="stopStream">
                <el-icon><VideoPause /></el-icon>停止生成
              </el-button>
              <el-button
                type="primary"
                :loading="submitting"
                :disabled="isPaused"
                @click="submitAnswer"
              >
                {{ submitting ? 'AI 评分中…' : '提交答案' }}
              </el-button>
            </div>
          </div>
        </section>

        <!-- 流式评分结果 -->
        <section v-if="streaming || commentText || scoreInfo" class="card eval-card">
          <div class="card-header">
            <div class="card-title">AI 点评</div>
            <div v-if="scoreInfo" class="eval-score">
              <span class="eval-score__value">{{ scoreInfo.score }}</span>
              <span class="eval-score__unit">分</span>
              <el-tag size="small" :type="scoreInfo.evaluatedBy === 'AI' ? 'success' : 'warning'">
                {{ scoreInfo.evaluatedBy === 'AI' ? 'AI 评分' : '规则评分' }}
              </el-tag>
            </div>
          </div>

          <!-- 流式正文：打字机 + Markdown -->
          <div class="eval-content">
            <MarkdownRender v-if="commentText" :source="commentText" />
            <StreamText
              v-if="streaming && !commentText"
              :text="progressText || 'AI 正在阅读你的回答…'"
              :cursor="true"
            />
          </div>

          <!-- 亮点 / 不足 -->
          <div v-if="scoreInfo && (highlightList.length || gapList.length)" class="eval-tags">
            <div v-if="highlightList.length" class="eval-tags__block">
              <div class="eval-tags__title success">亮点</div>
              <el-tag v-for="(h, i) in highlightList" :key="`h${i}`" type="success" size="small">
                {{ h }}
              </el-tag>
            </div>
            <div v-if="gapList.length" class="eval-tags__block">
              <div class="eval-tags__title danger">不足</div>
              <el-tag v-for="(g, i) in gapList" :key="`g${i}`" type="danger" size="small">
                {{ g }}
              </el-tag>
            </div>
          </div>

          <!-- 下一步操作 -->
          <div v-if="pendingAction && !streaming" class="eval-actions">
            <el-button v-if="pendingAction === 'NEXT_QUESTION'" type="primary" @click="goNextQuestion">
              进入下一题
              <el-icon><ArrowRight /></el-icon>
            </el-button>
            <el-button v-else-if="pendingAction === 'COMPLETED'" type="success" @click="goReport">
              查看面试报告
              <el-icon><ArrowRight /></el-icon>
            </el-button>
            <span v-else class="muted">请继续回答上方追问</span>
          </div>
        </section>
      </main>

      <!-- 侧栏：会话信息 -->
      <aside class="room-side">
        <section class="card">
          <div class="card-header"><div class="card-title">本场概览</div></div>
          <ul class="side-list">
            <li>
              <span>方向</span>
              <b>{{ directionText }}</b>
            </li>
            <li>
              <span>难度</span>
              <b>{{ difficultyLabel(session?.difficulty) }}</b>
            </li>
            <li>
              <span>题量</span>
              <b>{{ session?.totalQuestion || 0 }} 题</b>
            </li>
            <li>
              <span>已答</span>
              <b>{{ answeredCount }} 题</b>
            </li>
            <li v-if="session?.jdText">
              <span>已绑定 JD</span>
              <b>{{ jdCharCount }} 字</b>
            </li>
          </ul>
          <el-collapse v-if="session?.jdText" class="mt-8">
            <el-collapse-item title="查看 JD 全文">
              <pre class="jd-text">{{ session.jdText }}</pre>
            </el-collapse-item>
          </el-collapse>
        </section>

        <section class="card">
          <div class="card-header"><div class="card-title">作答小贴士</div></div>
          <ul class="tip-list">
            <li>先结论后展开：一句话给答案，再解释原理。</li>
            <li>结合项目：用「背景-方案-结果」三段式举例。</li>
            <li>遇到不会的题：说明思路与已知部分，不要空白。</li>
            <li>AI 追问往往针对薄弱点，是加分机会。</li>
          </ul>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { interviewApi, buildSubmitAnswerSse, buildNextQuestionSse, newClientToken } from '@/api/interview'
import { useSse, type SseHandle } from '@/api/sse'
import { useConfigStore } from '@/stores/config'
import type {
  Direction,
  DonePayload,
  FollowUpPayload,
  InterviewPhase,
  ScorePayload,
  SessionDetail,
} from '@/types'
import { calcPhase, difficultyLabel, difficultyTagType, directionLabel, sourceLabel, splitPhase } from '@/utils/dict'
import { getRaw, remove, setRaw } from '@/utils/storage'
import StatusBadge from '@/components/StatusBadge/index.vue'
import StreamText from '@/components/StreamText/index.vue'
import MarkdownRender from '@/components/MarkdownRender/index.vue'
import DegradedTip from '@/components/DegradedTip/index.vue'
import PageLoading from '@/components/PageLoading/index.vue'

const route = useRoute()
const router = useRouter()
const configStore = useConfigStore()

const sessionId = computed<string>(() => String(route.params.sessionId || ''))

/* ------------------------------ 会话状态 ------------------------------ */

const session = ref<SessionDetail | null>(null)
const loadingQuestion = ref<boolean>(false)
const currentQuestion = ref<{
  questionNo: number
  sessionQuestionId: number
  title: string
  referencePoints?: string[]
  difficulty?: string
  source?: string
} | null>(null)

const answer = ref<string>('')
const followUpAnswer = ref<string>('')
const submitting = ref<boolean>(false)
const streaming = ref<boolean>(false)
const commentText = ref<string>('')
const progressText = ref<string>('')
const scoreInfo = ref<ScorePayload | null>(null)
const pendingFollowUp = ref<FollowUpPayload | null>(null)
const followUpChain = ref<Array<{ title: string; content?: string; score?: number }>>([])
const degraded = ref<boolean>(false)
const pendingAction = ref<'NEXT_QUESTION' | 'FOLLOW_UP' | 'COMPLETED' | 'WAIT' | ''>('')
const showPoints = ref<boolean>(false)
const draftTip = ref<string>('草稿自动保存')

let sseHandle: SseHandle | null = null
let draftTimer: number | undefined

/* ------------------------------ 计算属性 ------------------------------ */

const answerMinLength = computed<number>(() => configStore.answerMinLength)
const answerMaxLength = computed<number>(() => configStore.answerMaxLength)
const maxFollowUp = computed<number>(() => configStore.maxFollowUp)

const answerLength = computed<number>(() => Array.from(answer.value.trim()).length)
const followUpLength = computed<number>(() => Array.from(followUpAnswer.value.trim()).length)

const isPaused = computed<boolean>(() => session.value?.status === 'PAUSED')
const busy = computed<boolean>(() => submitting.value || streaming.value)

const questionNo = computed<number>(() => currentQuestion.value?.questionNo || 1)

const directionText = computed<string>(() => {
  const list = session.value?.directions || []
  if (!list.length) return '综合方向'
  return list.map((d) => directionLabel(d as Direction)).join('、')
})

/** Δ1：当前阶段与阶段内进度 */
const currentPhase = computed(() =>
  calcPhase(questionNo.value, session.value?.totalQuestion || configStore.defaultQuestion),
)

/** Δ1：三阶段列表（用于顶部阶段条） */
const phaseList = computed(() => {
  const total = session.value?.totalQuestion || configStore.defaultQuestion
  const plan = splitPhase(total)
  const cur = calcPhase(questionNo.value, total)
  const order: Array<{ key: InterviewPhase; label: string }> = [
    { key: 'TECHNICAL', label: '技术面' },
    { key: 'PROJECT', label: '项目面' },
    { key: 'BEHAVIORAL', label: '行为面' },
  ]
  return order.map((o) => {
    const count = plan[o.key]
    const isCurrent = o.key === cur.phase
    return {
      phase: o.key,
      label: o.label,
      count,
      index: isCurrent ? cur.indexInPhase : o.key === cur.phase ? cur.indexInPhase : 0,
      done: !isCurrent && isPhaseDone(o.key, cur.phase),
    }
  })
})

/** 判断某阶段是否已走完（按阶段顺序） */
function isPhaseDone(target: InterviewPhase, current: InterviewPhase): boolean {
  const order: InterviewPhase[] = ['TECHNICAL', 'PROJECT', 'BEHAVIORAL']
  return order.indexOf(target) < order.indexOf(current)
}

const overallPercent = computed<number>(() => {
  const total = session.value?.totalQuestion || 1
  return Math.min(100, Math.round((questionNo.value / total) * 100))
})

const referencePoints = computed<string[]>(() => currentQuestion.value?.referencePoints || [])
const highlightList = computed<string[]>(() => scoreInfo.value?.highlights || [])
const gapList = computed<string[]>(() => scoreInfo.value?.gaps || [])
const answeredCount = computed<number>(
  () => session.value?.questions?.filter((q) => q.answer && !q.answer.skipped).length || 0,
)
const jdCharCount = computed<number>(() => Array.from(session.value?.jdText || '').length)

/** 草稿本地存储 key */
const draftKey = computed<string>(() => `draft_${sessionId.value}_${currentQuestion.value?.sessionQuestionId || 0}`)

/* ------------------------------ SSE 处理 ------------------------------ */

/** 重置单题的评分状态 */
function resetEvalState(): void {
  commentText.value = ''
  progressText.value = ''
  scoreInfo.value = null
  degraded.value = false
  pendingAction.value = ''
  pendingFollowUp.value = null
}

/** 统一的 SSE 事件处理（评分流与出题流共用） */
function createEventHandlers() {
  return {
    onEvent: (env: any) => {
      switch (env.type) {
        case 'progress': {
          // 长等待提示（如"AI 正在出题…"）
          progressText.value = env.payload?.text || '处理中…'
          break
        }
        case 'question': {
          // 出题完成（取下一题 / 首题）
          const p = env.payload || {}
          currentQuestion.value = {
            questionNo: p.questionNo || 1,
            sessionQuestionId: p.sessionQuestionId,
            title: p.title || '',
            referencePoints: p.referencePoints || [],
            difficulty: p.difficulty,
            source: p.source,
          }
          answer.value = ''
          followUpAnswer.value = ''
          followUpChain.value = []
          resetEvalState()
          restoreDraft()
          if (env.degraded === true) {
            degraded.value = true
          }
          break
        }
        case 'comment': {
          // 评分正文增量：直接追加，StreamText/MarkdownRender 负责渲染
          const delta = env.payload?.delta || ''
          if (delta) commentText.value += delta
          break
        }
        case 'score': {
          scoreInfo.value = env.payload as ScorePayload
          break
        }
        case 'follow_up': {
          pendingFollowUp.value = env.payload as FollowUpPayload
          break
        }
        case 'done': {
          handleDone(env.payload as DonePayload)
          break
        }
        default:
          break
      }
    },
    onDegraded: () => {
      degraded.value = true
      ElMessage.info('AI 服务繁忙，本次结果由规则引擎生成，仅供参考')
    },
    onError: (err: Error & { code?: string }) => {
      streaming.value = false
      submitting.value = false
      loadingQuestion.value = false
      const msg = err?.message || '连接中断'
      ElMessage.error(`${msg}（可点击重试或刷新页面查看进度）`)
    },
    onDone: () => {
      streaming.value = false
      submitting.value = false
      loadingQuestion.value = false
      sseHandle = null
    },
  }
}

/** done 事件：按 nextAction 决定下一步 */
function handleDone(payload: DonePayload): void {
  if (!payload) return
  switch (payload.nextAction) {
    case 'FOLLOW_UP':
      pendingAction.value = 'FOLLOW_UP'
      break
    case 'NEXT_QUESTION':
      pendingAction.value = 'NEXT_QUESTION'
      break
    case 'COMPLETED':
      pendingAction.value = 'COMPLETED'
      if (payload.reportId) {
        ElMessage.success('面试已完成，报告已生成')
      }
      break
    default:
      pendingAction.value = 'WAIT'
      break
  }
}

/** 提交答案（SSE POST） */
async function submitAnswer(): Promise<void> {
  if (!currentQuestion.value) return
  const content = answer.value.trim()
  if (content.length < answerMinLength.value) {
    ElMessage.warning(`答案至少需要 ${answerMinLength.value} 个字符`)
    return
  }
  if (content.length > answerMaxLength.value) {
    ElMessage.warning(`答案不能超过 ${answerMaxLength.value} 个字符`)
    return
  }
  startSubmit(content, false, null)
}

/** 提交追问答案 */
async function submitFollowUpAnswer(): Promise<void> {
  const content = followUpAnswer.value.trim()
  if (content.length < answerMinLength.value) {
    ElMessage.warning(`答案至少需要 ${answerMinLength.value} 个字符`)
    return
  }
  const parentId = pendingFollowUp.value?.answerId || null
  startSubmit(content, true, parentId)
}

/**
 * 发起评分 SSE
 * @param content 答案内容
 * @param isFollowUp 是否为追问答案
 * @param parentAnswerId 追问对应的父答案 ID
 */
function startSubmit(content: string, isFollowUp: boolean, parentAnswerId: number | null): void {
  if (!currentQuestion.value) return
  const token = newClientToken()
  const { url, headers, body } = buildSubmitAnswerSse(
    sessionId.value,
    {
      sessionQuestionId: currentQuestion.value.sessionQuestionId,
      content,
      isFollowUp,
      parentAnswerId,
    },
    token,
  )

  submitting.value = true
  streaming.value = true
  resetEvalState()
  streaming.value = true
  submitting.value = true

  const handlers = createEventHandlers()
  sseHandle = useSse(url, {
    method: 'POST',
    headers,
    body,
    onEvent: (env) => {
      handlers.onEvent(env)
      // 追问提交成功后，把追问与其回答并入追问链
      if (env.type === 'done' && isFollowUp) {
        followUpChain.value.push({
          title: pendingFollowUp.value?.title || '追问',
          content,
          score: (env.payload as DonePayload)?.score,
        })
        pendingFollowUp.value = null
        followUpAnswer.value = ''
      }
    },
    onDegraded: handlers.onDegraded,
    onError: handlers.onError,
    onDone: handlers.onDone,
  })
}

/** 用户主动中断生成 */
function stopStream(): void {
  if (sseHandle) {
    sseHandle.stop()
    sseHandle = null
  }
  streaming.value = false
  submitting.value = false
  ElMessage.info('已停止生成，已收到的内容仍会保留')
}

/** 取下一题（SSE GET） */
function goNextQuestion(): void {
  const { url, headers } = buildNextQuestionSse(sessionId.value, 0)
  loadingQuestion.value = true
  streaming.value = true
  resetEvalState()
  const handlers = createEventHandlers()
  sseHandle = useSse(url, {
    method: 'GET',
    headers,
    onEvent: (env) => {
      handlers.onEvent(env)
      if (env.type === 'done') {
        const p = env.payload as DonePayload
        if (p?.nextAction === 'COMPLETED') {
          handleDone(p)
        }
      }
    },
    onDegraded: handlers.onDegraded,
    onError: handlers.onError,
    onDone: handlers.onDone,
  })
}

/* ------------------------------ 会话动作 ------------------------------ */

/** 加载会话详情，并确定当前题目 */
async function loadSession(): Promise<void> {
  loadingQuestion.value = true
  try {
    const data = await interviewApi.detail(sessionId.value)
    session.value = data
    if (!data) return

    if (data.status === 'COMPLETED' || data.status === 'ABORTED') {
      router.replace(`/report/${sessionId.value}`)
      return
    }
    if (data.status === 'INIT') {
      await startSession()
      return
    }

    // 已有题目：取最后一题；若已作答则继续取下一题
    const questions = data.questions || []
    if (questions.length) {
      const last = questions[questions.length - 1]
      if (last && !last.answer) {
        currentQuestion.value = {
          questionNo: last.questionNo,
          sessionQuestionId: last.sessionQuestionId,
          title: last.title,
          referencePoints: last.referencePoints || [],
          difficulty: last.difficulty,
          source: last.source,
        }
        restoreDraft()
        return
      }
    }
    goNextQuestion()
  } catch (e) {
    ElMessage.error('会话加载失败，请返回列表重试')
  } finally {
    loadingQuestion.value = false
  }
}

/** 开始面试（INIT → ASKING，产出首题） */
async function startSession(): Promise<void> {
  loadingQuestion.value = true
  try {
    const q = await interviewApi.start(sessionId.value)
    if (q) {
      currentQuestion.value = {
        questionNo: q.questionNo || 1,
        sessionQuestionId: q.sessionQuestionId,
        title: q.title || '',
        referencePoints: q.referencePoints || [],
        difficulty: q.difficulty,
        source: q.source,
      }
      if (session.value) session.value.status = 'ASKING'
      restoreDraft()
    } else {
      goNextQuestion()
    }
  } catch (e) {
    /* 已统一提示 */
  } finally {
    loadingQuestion.value = false
  }
}

/** 暂停 */
async function pauseSession(): Promise<void> {
  try {
    await interviewApi.pause(sessionId.value)
    if (session.value) session.value.status = 'PAUSED'
    ElMessage.success('已暂停，可随时恢复')
  } catch (e) {
    /* 已统一提示 */
  }
}

/** 恢复 */
async function resumeSession(): Promise<void> {
  try {
    await interviewApi.resume(sessionId.value)
    if (session.value) session.value.status = 'ASKING'
    ElMessage.success('已恢复')
  } catch (e) {
    /* 已统一提示 */
  }
}

/** 结束并生成报告 */
async function finishSession(): Promise<void> {
  try {
    await ElMessageBox.confirm('结束后将基于已答题生成报告，确定结束本场面试吗？', '提示', {
      type: 'warning',
      confirmButtonText: '结束并出报告',
      cancelButtonText: '继续答题',
    })
  } catch (e) {
    return
  }
  try {
    await interviewApi.finish(sessionId.value)
    ElMessage.success('已结束，报告生成中…')
    router.replace(`/report/${sessionId.value}`)
  } catch (e) {
    /* 已统一提示 */
  }
}

/** 跳过本题 */
async function skipQuestion(): Promise<void> {
  if (!currentQuestion.value) return
  try {
    await ElMessageBox.confirm('跳过本题将记 0 分，确定跳过吗？', '提示', {
      type: 'warning',
      confirmButtonText: '跳过',
      cancelButtonText: '继续作答',
    })
  } catch (e) {
    return
  }
  try {
    await interviewApi.skip(sessionId.value, currentQuestion.value.sessionQuestionId)
    ElMessage.info('已跳过本题')
    goNextQuestion()
  } catch (e) {
    /* 已统一提示 */
  }
}

function goReport(): void {
  router.push(`/report/${sessionId.value}`)
}

/** 离开房间：未完成的会话返回列表 */
function exitRoom(): void {
  router.push('/interview')
}

/* ------------------------------ 草稿（W-06） ------------------------------ */

/** 输入时 5s 防抖保存草稿 */
function onAnswerInput(): void {
  if (draftTimer) window.clearTimeout(draftTimer)
  draftTip.value = '输入中…'
  draftTimer = window.setTimeout(() => {
    if (answer.value) {
      setRaw(draftKey.value, answer.value)
      draftTip.value = '草稿已保存'
    } else {
      draftTip.value = '草稿自动保存'
    }
  }, 5000)
}

/** 恢复草稿 */
function restoreDraft(): void {
  const saved = getRaw(draftKey.value)
  if (saved) {
    answer.value = saved
    draftTip.value = '已恢复本地草稿'
  } else {
    draftTip.value = '草稿自动保存'
  }
}

onMounted(() => {
  void configStore.ensureLoaded()
  void loadSession()
})

onBeforeUnmount(() => {
  if (sseHandle) {
    sseHandle.stop()
    sseHandle = null
  }
  if (draftTimer) {
    window.clearTimeout(draftTimer)
    draftTimer = undefined
  }
  // 提交成功后清理草稿，避免下次进入误用
  if (scoreInfo.value) remove(draftKey.value)
})
</script>

<style scoped lang="scss">
.room {
  min-height: 100vh;
  background: var(--bg-page);
  display: flex;
  flex-direction: column;
}

.room-header {
  position: sticky;
  top: 0;
  z-index: 50;
  background: #fff;
  border-bottom: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
}

.room-header__inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.room-header__left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.room-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.room-no {
  font-family: ui-monospace, Menlo, Consolas, monospace;
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 400;
}

.room-sub {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}

.jd-flag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: var(--color-primary);
  background: var(--color-primary-bg);
  border-radius: 4px;
  padding: 0 5px;
}

.room-header__right {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.room-progress {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 20px 10px;
}

.phase-bar {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.phase-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  font-size: 12px;
  color: var(--text-secondary);
  background: #fff;
}

.phase-chip.active {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-bg);
  font-weight: 600;
}

.phase-chip.done {
  border-color: #bbf7d0;
  color: var(--color-success);
  background: #f0fdf4;
}

.phase-chip__count {
  font-variant-numeric: tabular-nums;
  opacity: 0.85;
}

.room-body {
  flex: 1;
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 20px;
  box-sizing: border-box;
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 16px;
  align-items: start;
}

.room-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.question-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.question-title {
  font-size: 17px;
  line-height: 1.7;
  color: var(--text-primary);
  margin: 0 0 12px;
  font-weight: 600;
}

.reference-points {
  background: #fafaff;
  border: 1px dashed #dfe3ff;
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  margin-bottom: 12px;
}

.reference-points__title {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary);
  margin-bottom: 6px;
}

.reference-points ul {
  margin: 0;
  padding-left: 20px;
}

.reference-points li {
  font-size: 13px;
  color: var(--text-regular);
  line-height: 1.8;
}

.follow-chain {
  border-top: 1px dashed var(--border-color);
  padding-top: 12px;
}

.follow-chain__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.follow-item {
  padding: 8px 10px;
  border-radius: var(--radius-sm);
  background: #fffbeb;
  border: 1px solid #fde68a;
  margin-bottom: 8px;
}

.follow-item__q {
  font-size: 13px;
  color: #92400e;
  line-height: 1.7;
}

.follow-index {
  display: inline-block;
  font-size: 11px;
  background: var(--color-warning);
  color: #fff;
  border-radius: 4px;
  padding: 0 5px;
  margin-right: 6px;
}

.follow-item__a {
  margin-top: 6px;
}

.follow-item__label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 2px;
}

.follow-item__text {
  font-size: 13px;
  color: var(--text-regular);
  line-height: 1.7;
  white-space: pre-wrap;
}

.follow-item__score {
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-primary);
  font-weight: 600;
}

.follow-up-question {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.7;
  margin: 0 0 12px;
}

.answer-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.answer-foot__right {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.char-count {
  font-size: 12px;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}

.char-count.danger {
  color: var(--color-danger);
}

.char-count__hint {
  margin-left: 4px;
}

.draft-tip {
  font-size: 12px;
  color: var(--text-secondary);
}

.eval-card {
  border-color: #e8eaff;
}

.eval-score {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.eval-score__value {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-primary);
  font-variant-numeric: tabular-nums;
}

.eval-score__unit {
  font-size: 12px;
  color: var(--text-secondary);
  margin-right: 8px;
}

.eval-content {
  min-height: 40px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-regular);
}

.eval-tags {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 14px;
}

.eval-tags__block {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.eval-tags__title {
  font-size: 12px;
  font-weight: 600;
  margin-right: 4px;
}

.eval-tags__title.success {
  color: var(--color-success);
}

.eval-tags__title.danger {
  color: var(--color-danger);
}

.eval-actions {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed var(--border-color);
}

.room-side {
  position: sticky;
  top: 128px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.side-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.side-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 0;
  font-size: 13px;
  color: var(--text-secondary);
  border-bottom: 1px dashed var(--border-light);
}

.side-list li b {
  color: var(--text-primary);
}

.jd-text {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.7;
  color: var(--text-regular);
  background: #fafafa;
  border-radius: var(--radius-sm);
  padding: 10px;
  margin: 0;
  max-height: 240px;
  overflow: auto;
}

.tip-list {
  margin: 0;
  padding-left: 18px;
}

.tip-list li {
  font-size: 12px;
  color: var(--text-regular);
  line-height: 1.9;
}

.muted {
  color: var(--text-secondary);
  font-size: 12px;
}

@media (max-width: 1024px) {
  .room-body {
    grid-template-columns: 1fr;
    padding: 14px;
  }

  .room-side {
    position: static;
  }

  .room-header__inner {
    padding: 10px 14px;
    flex-wrap: wrap;
  }
}
</style>
