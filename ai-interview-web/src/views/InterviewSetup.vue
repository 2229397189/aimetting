<template>
  <div class="page-container setup-page">
    <section class="card">
      <div class="card-header">
        <div class="card-title">配置一场模拟面试</div>
        <el-button text @click="resetForm">
          <el-icon><RefreshLeft /></el-icon>重置
        </el-button>
      </div>

      <el-form :model="form" label-position="top" size="large">
        <!-- 方向（多选） -->
        <el-form-item label="面试方向（可多选，题目将按权重混合）" required>
          <div class="direction-grid">
            <div
              v-for="opt in directionOptions"
              :key="opt.value"
              class="direction-item"
              :class="{ active: form.directions.includes(opt.value) }"
              @click="toggleDirection(opt.value)"
            >
              <span class="direction-item__label">{{ opt.label }}</span>
              <el-icon v-if="form.directions.includes(opt.value)" class="direction-item__check">
                <Check />
              </el-icon>
            </div>
          </div>
          <div v-if="!form.directions.length" class="form-error">请至少选择一个方向</div>
        </el-form-item>

        <!-- 难度 -->
        <el-form-item label="难度">
          <el-radio-group v-model="form.difficulty">
            <el-radio-button
              v-for="opt in difficultyOptions"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- 题量 -->
        <el-form-item label="题目数量">
          <div class="question-count">
            <el-slider
              v-model="form.totalQuestion"
              :min="minQuestion"
              :max="maxQuestion"
              :step="1"
              show-stops
              class="count-slider"
            />
            <el-input-number
              v-model="form.totalQuestion"
              :min="minQuestion"
              :max="maxQuestion"
              :step="1"
              size="default"
            />
          </div>
          <div class="phase-preview">
            <el-tag
              v-for="p in phasePreview"
              :key="p.phase"
              :type="p.tagType"
              size="small"
              effect="light"
            >
              {{ p.label }} {{ p.count }} 题
            </el-tag>
            <span class="phase-preview__tip">
              流程按 技术面 → 项目面 → 行为面 依次推进（默认 50% / 30% / 20%）
            </span>
          </div>
        </el-form-item>

        <!-- 绑定简历 -->
        <el-form-item label="绑定简历（选填）">
          <el-select
            v-model="form.resumeId"
            placeholder="不绑定则按方向通用出题"
            clearable
            filterable
            class="full-width"
          >
            <el-option
              v-for="r in resumeOptions"
              :key="r.id"
              :label="r.title + (r.score ? `（${r.score} 分）` : '')"
              :value="r.id"
            />
          </el-select>
          <div class="form-hint">
            绑定后 AI 会结合你的技能与项目经历出题，命中率更高；
            <el-button text type="primary" size="small" @click="goResume">去上传简历</el-button>
          </div>
        </el-form-item>

        <!-- Δ1：目标岗位 JD（可折叠，选填，0~3000 字） -->
        <el-form-item>
          <template #label>
            <div class="jd-label" @click="jdExpanded = !jdExpanded">
              <span>目标岗位 JD（选填）</span>
              <el-tag size="small" type="info" effect="plain">推荐填写</el-tag>
              <el-icon class="jd-label__arrow" :class="{ expanded: jdExpanded }">
                <ArrowDown />
              </el-icon>
            </div>
          </template>
          <el-collapse-transition>
            <div v-show="jdExpanded">
              <el-input
                v-model="form.jdText"
                type="textarea"
                :rows="7"
                maxlength="3000"
                show-word-limit
                resize="vertical"
                placeholder="粘贴目标岗位的 JD（岗位职责 / 任职要求），AI 会据此调整出题侧重点与追问深度。例如：&#10;1. 负责服务端接口设计与开发；&#10;2. 熟悉 Java、Spring Boot、MySQL、Redis；&#10;3. 有高并发系统经验者优先。"
              />
              <div class="form-hint">
                已输入 <b>{{ jdLength }}</b> / 3000 字；JD 越具体，题目与真实面试越接近
              </div>
            </div>
          </el-collapse-transition>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" :loading="creating" @click="submit">
            <el-icon><VideoPlay /></el-icon>创建并开始面试
          </el-button>
          <span class="submit-tip">创建后将立即进入答题页面，首题由 AI 生成</span>
        </el-form-item>
      </el-form>
    </section>

    <!-- 说明卡片 -->
    <section class="card mt-16">
      <div class="card-header"><div class="card-title">面试流程说明</div></div>
      <el-steps :active="3" align-center finish-status="success" class="flow-steps">
        <el-step title="技术面" description="基础知识与编码能力" />
        <el-step title="项目面" description="项目深度与难点复盘" />
        <el-step title="行为面" description="协作、抗压与成长性" />
        <el-step title="报告" description="五维雷达 + 逐题点评" />
      </el-steps>
      <ul class="rule-list">
        <li>每题作答 10 ~ 5000 字，AI 会实时评分并在薄弱点继续追问（每题最多 2 次）。</li>
        <li>有追问时该题得分 = 原题 70% + 追问均分 30%。</li>
        <li>中途可暂停/恢复，主动结束会基于已答题直接生成报告。</li>
      </ul>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { interviewApi } from '@/api/interview'
import { resumeApi } from '@/api/resume'
import { useConfigStore } from '@/stores/config'
import type { CreateSessionReq, Difficulty, Direction, ResumeResp } from '@/types'
import { DIFFICULTY_OPTIONS, PHASE_OPTIONS, splitPhase } from '@/utils/dict'
import { difficultyTagType } from '@/utils/dict'

const router = useRouter()
const configStore = useConfigStore()

const creating = ref<boolean>(false)
const jdExpanded = ref<boolean>(false)

/** 表单 */
const form = reactive<CreateSessionReq>({
  directions: ['JAVA_BACKEND'],
  difficulty: 'MEDIUM',
  totalQuestion: 8,
  resumeId: null,
  jdText: '',
})

/** 方向 / 难度选项：优先后端字典，回落前端内置 */
const directionOptions = computed(() =>
  configStore.directions.length
    ? configStore.directions.map((d) => ({ value: d.code, label: d.label }))
    : [],
)
const difficultyOptions = computed(() =>
  configStore.difficulties.length
    ? configStore.difficulties.map((d) => ({ value: d.code, label: d.label }))
    : DIFFICULTY_OPTIONS.map((d) => ({ value: d.value, label: d.label })),
)

const minQuestion = computed<number>(() => configStore.minQuestion)
const maxQuestion = computed<number>(() => configStore.maxQuestion)

/** 简历下拉选项 */
const resumeOptions = ref<ResumeResp[]>([])

/** JD 字数（中文按字符计） */
const jdLength = computed<number>(() => Array.from(form.jdText || '').length)

/** 三阶段题量预览（Δ1） */
const phasePreview = computed(() => {
  const plan = splitPhase(form.totalQuestion)
  return PHASE_OPTIONS.map((p) => ({
    phase: p.value,
    label: p.label,
    count: plan[p.value],
    tagType: (p.type || 'info') as any,
  }))
})

/** 选择 / 取消方向 */
function toggleDirection(value: Direction): void {
  const idx = form.directions.indexOf(value)
  if (idx >= 0) {
    if (form.directions.length === 1) {
      ElMessage.warning('至少保留一个方向')
      return
    }
    form.directions.splice(idx, 1)
  } else {
    form.directions.push(value)
  }
}

/** 重置表单 */
function resetForm(): void {
  form.directions = ['JAVA_BACKEND']
  form.difficulty = 'MEDIUM'
  form.totalQuestion = configStore.defaultQuestion
  form.resumeId = null
  form.jdText = ''
}

/** 加载简历列表（用于下拉绑定） */
async function loadResumes(): Promise<void> {
  try {
    const page = await resumeApi.page({ pageNum: 1, pageSize: 50 })
    resumeOptions.value = page?.list || []
  } catch (e) {
    resumeOptions.value = []
  }
}

/** 提交创建 */
async function submit(): Promise<void> {
  if (!form.directions.length) {
    ElMessage.warning('请至少选择一个面试方向')
    return
  }
  if (form.totalQuestion < minQuestion.value || form.totalQuestion > maxQuestion.value) {
    ElMessage.warning(`题量需在 ${minQuestion.value} ~ ${maxQuestion.value} 之间`)
    return
  }
  if (jdLength.value > 3000) {
    ElMessage.warning('JD 内容不能超过 3000 字')
    return
  }
  creating.value = true
  try {
    const sessionId = await interviewApi.create({
      directions: form.directions,
      difficulty: form.difficulty as Difficulty,
      totalQuestion: form.totalQuestion,
      resumeId: form.resumeId || null,
      jdText: (form.jdText || '').trim(),
      phasePlan: splitPhase(form.totalQuestion),
    })
    ElMessage.success('会话创建成功，正在进入面试…')
    router.push(`/interview/${sessionId}`)
  } catch (e) {
    /* 已统一提示 */
  } finally {
    creating.value = false
  }
}

function goResume(): void {
  router.push('/resume')
}

onMounted(() => {
  void configStore.ensureLoaded()
  void loadResumes()
  form.totalQuestion = configStore.defaultQuestion
})
</script>

<style scoped lang="scss">
.setup-page {
  padding-bottom: 40px;
}

.direction-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
}

.direction-item {
  position: relative;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  cursor: pointer;
  text-align: center;
  font-size: 13px;
  color: var(--text-regular);
  transition: all 0.15s ease;
  user-select: none;
}

.direction-item:hover {
  border-color: var(--color-primary-light);
}

.direction-item.active {
  border-color: var(--color-primary);
  background: var(--color-primary-bg);
  color: var(--color-primary);
  font-weight: 600;
}

.direction-item__check {
  position: absolute;
  top: 4px;
  right: 4px;
  font-size: 14px;
}

.form-error {
  margin-top: 6px;
  font-size: 12px;
  color: var(--color-danger);
}

.question-count {
  display: flex;
  align-items: center;
  gap: 20px;
  width: 100%;
}

.count-slider {
  flex: 1;
  min-width: 200px;
}

.phase-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.phase-preview__tip {
  font-size: 12px;
  color: var(--text-secondary);
}

.full-width {
  width: 100%;
}

.form-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.jd-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.jd-label__arrow {
  transition: transform 0.2s ease;
  color: var(--text-secondary);
}

.jd-label__arrow.expanded {
  transform: rotate(180deg);
}

.submit-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--text-secondary);
}

.flow-steps {
  margin-bottom: 18px;
}

.rule-list {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  color: var(--text-regular);
  line-height: 2;
}

@media (max-width: 900px) {
  .direction-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .question-count {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
