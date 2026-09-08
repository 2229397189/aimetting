import type {
  Direction,
  Difficulty,
  EnumOption,
  InterviewPhase,
  QuestionSource,
  SessionStatus,
} from '@/types'

/**
 * 枚举字典与派生计算：与后端枚举一一对应（ARCHITECTURE.md 4.4）
 */

/** 8 个面试方向 */
export const DIRECTION_OPTIONS: EnumOption<Direction>[] = [
  { value: 'JAVA_BACKEND', label: 'Java后端' },
  { value: 'FRONTEND', label: '前端' },
  { value: 'DATABASE', label: '数据库' },
  { value: 'OS', label: '操作系统' },
  { value: 'NETWORK', label: '计算机网络' },
  { value: 'ALGORITHM', label: '算法' },
  { value: 'SYSTEM_DESIGN', label: '系统设计' },
  { value: 'BEHAVIORAL', label: '行为面试' },
]

/** 3 个难度 */
export const DIFFICULTY_OPTIONS: EnumOption<Difficulty>[] = [
  { value: 'EASY', label: '简单', type: 'success' },
  { value: 'MEDIUM', label: '中等', type: 'warning' },
  { value: 'HARD', label: '困难', type: 'danger' },
]

/** Δ1 增量：三阶段 */
export const PHASE_OPTIONS: EnumOption<InterviewPhase>[] = [
  { value: 'TECHNICAL', label: '技术面', type: 'info' },
  { value: 'PROJECT', label: '项目面', type: 'warning' },
  { value: 'BEHAVIORAL', label: '行为面', type: 'success' },
]

/** 会话状态 */
export const SESSION_STATUS_OPTIONS: EnumOption<SessionStatus>[] = [
  { value: 'INIT', label: '待开始', type: 'info' },
  { value: 'ASKING', label: '答题中', type: '' },
  { value: 'EVALUATING', label: '评分中', type: 'warning' },
  { value: 'FOLLOW_UP', label: '追问中', type: 'warning' },
  { value: 'PAUSED', label: '已暂停', type: 'info' },
  { value: 'COMPLETED', label: '已完成', type: 'success' },
  { value: 'ABORTED', label: '已放弃', type: 'danger' },
]

/** 题目来源 */
export const SOURCE_LABEL: Record<QuestionSource, string> = {
  AI: 'AI 生成',
  BANK: '精选题库',
  SEED: '内置题库',
  ADMIN: '管理员录入',
}

/** 五维雷达维度 */
export const DIMENSION_LABELS: Record<string, string> = {
  PROFESSIONAL: '专业技能',
  EXPRESSION: '表达沟通',
  LOGIC: '逻辑思维',
  PROJECT_DEPTH: '项目深度',
  POTENTIAL: '潜力',
}

/** 方向 label 映射 */
export function directionLabel(v?: Direction | string): string {
  if (!v) return '-'
  const hit = DIRECTION_OPTIONS.find((i) => i.value === v)
  return hit ? hit.label : String(v)
}

/** 难度 label 映射 */
export function difficultyLabel(v?: Difficulty | string): string {
  if (!v) return '-'
  const hit = DIFFICULTY_OPTIONS.find((i) => i.value === v)
  return hit ? hit.label : String(v)
}

/** 难度 tag 类型 */
export function difficultyTagType(v?: Difficulty | string): '' | 'success' | 'warning' | 'danger' {
  const hit = DIFFICULTY_OPTIONS.find((i) => i.value === v)
  return (hit?.type as any) || 'info'
}

/** 会话状态 label */
export function statusLabel(v?: SessionStatus | string): string {
  if (!v) return '-'
  const hit = SESSION_STATUS_OPTIONS.find((i) => i.value === v)
  return hit ? hit.label : String(v)
}

/** 会话状态 tag 类型 */
export function statusTagType(v?: SessionStatus | string): '' | 'success' | 'warning' | 'danger' | 'info' {
  const hit = SESSION_STATUS_OPTIONS.find((i) => i.value === v)
  return (hit?.type as any) || 'info'
}

/** 阶段 label */
export function phaseLabel(v?: InterviewPhase | string): string {
  const hit = PHASE_OPTIONS.find((i) => i.value === v)
  return hit ? hit.label : '技术面'
}

/** 题目来源 label */
export function sourceLabel(v?: QuestionSource | string): string {
  if (!v) return '-'
  return (SOURCE_LABEL as Record<string, string>)[String(v)] || String(v)
}

/**
 * Δ1 增量：三阶段题量分配（默认 50% / 30% / 20%）
 * 保证每阶段至少 1 题（题量 ≥ 3），总数守恒。
 */
export function splitPhase(totalQuestion: number): Record<InterviewPhase, number> {
  const total = Math.max(1, Math.floor(totalQuestion || 1))
  let technical = Math.round(total * 0.5)
  let project = Math.round(total * 0.3)
  let behavioral = total - technical - project
  // 保证每阶段至少 1 题
  if (technical < 1) technical = 1
  if (project < 1) project = 1
  behavioral = total - technical - project
  if (behavioral < 1) {
    behavioral = 1
    // 从占比最大的阶段里扣，保证总和守恒
    if (technical >= project) {
      technical = total - project - behavioral
    } else {
      project = total - technical - behavioral
    }
  }
  // 二次兜底：总和必须与 total 一致
  const sum = technical + project + behavioral
  if (sum !== total) technical += total - sum
  return {
    TECHNICAL: Math.max(1, technical),
    PROJECT: Math.max(1, project),
    BEHAVIORAL: Math.max(1, behavioral),
  }
}

/**
 * Δ1 增量：根据题号计算所属阶段与阶段内进度
 * @param questionNo 题号（从 1 开始）
 * @param totalQuestion 总题量
 */
export function calcPhase(
  questionNo: number,
  totalQuestion: number,
): { phase: InterviewPhase; phaseLabel: string; indexInPhase: number; countInPhase: number } {
  const plan = splitPhase(totalQuestion)
  const no = Math.max(1, Math.floor(questionNo || 1))
  if (no <= plan.TECHNICAL) {
    return {
      phase: 'TECHNICAL',
      phaseLabel: PHASE_OPTIONS[0].label,
      indexInPhase: no,
      countInPhase: plan.TECHNICAL,
    }
  }
  if (no <= plan.TECHNICAL + plan.PROJECT) {
    return {
      phase: 'PROJECT',
      phaseLabel: PHASE_OPTIONS[1].label,
      indexInPhase: no - plan.TECHNICAL,
      countInPhase: plan.PROJECT,
    }
  }
  return {
    phase: 'BEHAVIORAL',
    phaseLabel: PHASE_OPTIONS[2].label,
    indexInPhase: Math.min(no - plan.TECHNICAL - plan.PROJECT, plan.BEHAVIORAL),
    countInPhase: plan.BEHAVIORAL,
  }
}

/** 五维分数归一化：缺失维度按总分兜底，全部 clamp 到 0-100 */
export function normalizeDimensions(
  raw?: Record<string, number> | null,
  fallback = 60,
): Record<string, number> {
  const result: Record<string, number> = {}
  Object.keys(DIMENSION_LABELS).forEach((key) => {
    const v = raw ? Number(raw[key]) : NaN
    result[key] = Number.isFinite(v) ? Math.min(100, Math.max(0, v)) : Math.min(100, Math.max(0, fallback))
  })
  return result
}

export default {
  DIRECTION_OPTIONS,
  DIFFICULTY_OPTIONS,
  PHASE_OPTIONS,
  SESSION_STATUS_OPTIONS,
  DIMENSION_LABELS,
  directionLabel,
  difficultyLabel,
  difficultyTagType,
  statusLabel,
  statusTagType,
  phaseLabel,
  sourceLabel,
  splitPhase,
  calcPhase,
  normalizeDimensions,
}
