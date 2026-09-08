<template>
  <div class="page-container session-list">
    <section class="card">
      <div class="card-header">
        <div class="card-title">面试记录</div>
        <el-button type="primary" @click="goSetup">
          <el-icon><VideoPlay /></el-icon>开始新面试
        </el-button>
      </div>

      <!-- 筛选 -->
      <div class="filters">
        <el-select v-model="filterStatus" placeholder="全部状态" clearable class="filter-item" @change="onFilterChange">
          <el-option label="全部状态" value="" />
          <el-option
            v-for="opt in statusOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-select v-model="filterDirection" placeholder="全部方向" clearable class="filter-item" @change="onFilterChange">
          <el-option label="全部方向" value="" />
          <el-option
            v-for="opt in directionOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <PageLoading v-if="loading" :min-height="200" text="加载中…" />
      <EmptyState
        v-else-if="!list.length"
        description="还没有面试记录，先来一场模拟面试吧"
        min-height="180"
      >
        <template #action>
          <el-button type="primary" size="small" @click="goSetup">立即开始</el-button>
        </template>
      </EmptyState>

      <el-table v-else :data="list" border stripe>
        <el-table-column prop="sessionNo" label="场次编号" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="session-no">{{ row.sessionNo || `#${row.id}` }}</span>
          </template>
        </el-table-column>
        <el-table-column label="方向" min-width="160">
          <template #default="{ row }">
            <span v-if="!row.directions || !row.directions.length" class="muted">综合</span>
            <el-tag
              v-for="d in row.directions"
              v-else
              :key="d"
              size="small"
              type="info"
              effect="plain"
              class="dir-tag"
            >
              {{ directionLabel(d) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="difficultyTagType(row.difficulty)">{{ difficultyLabel(row.difficulty) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="130" align="center">
          <template #default="{ row }">
            <span class="progress-text">{{ row.currentIndex }} / {{ row.totalQuestion }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <StatusBadge :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="得分" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.score !== null && row.score !== undefined" :class="scoreClass(row.score)">
              {{ formatScore(row.score) }}
            </span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="canContinue(row.status)"
              text
              type="primary"
              size="small"
              @click="goContinue(row.id)"
            >继续面试</el-button>
            <el-button
              v-if="row.status === 'COMPLETED'"
              text
              type="primary"
              size="small"
              @click="goReport(row.id)"
            >查看报告</el-button>
            <el-button text type="danger" size="small" @click="removeSession(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > pageSize" class="pager">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="loadList"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { interviewApi } from '@/api/interview'
import type { Direction, SessionDetail, SessionStatus } from '@/types'
import {
  DIRECTION_OPTIONS,
  directionLabel,
  difficultyLabel,
  difficultyTagType,
} from '@/utils/dict'
import { formatScore, formatTime } from '@/utils/format'
import StatusBadge from '@/components/StatusBadge/index.vue'
import PageLoading from '@/components/PageLoading/index.vue'
import EmptyState from '@/components/EmptyState/index.vue'

const router = useRouter()

const loading = ref<boolean>(false)
const list = ref<SessionDetail[]>([])
const pageNum = ref<number>(1)
const pageSize = 10
const total = ref<number>(0)

/** 筛选条件 */
const filterStatus = ref<SessionStatus | ''>('')
const filterDirection = ref<Direction | ''>('')

/** 可筛选的状态（排除瞬时态 EVALUATING / FOLLOW_UP） */
const statusOptions = [
  { value: 'INIT' as SessionStatus, label: '待开始' },
  { value: 'ASKING' as SessionStatus, label: '答题中' },
  { value: 'PAUSED' as SessionStatus, label: '已暂停' },
  { value: 'COMPLETED' as SessionStatus, label: '已完成' },
  { value: 'ABORTED' as SessionStatus, label: '已放弃' },
]
const directionOptions = DIRECTION_OPTIONS

function canContinue(status: SessionStatus): boolean {
  return ['INIT', 'ASKING', 'PAUSED', 'EVALUATING', 'FOLLOW_UP'].includes(status)
}

function scoreClass(score?: number | null): string {
  const n = Number(score || 0)
  if (n >= 80) return 'score-good'
  if (n >= 60) return 'score-mid'
  return 'score-bad'
}

function onFilterChange(): void {
  pageNum.value = 1
  void loadList()
}

async function loadList(): Promise<void> {
  loading.value = true
  try {
    const page = await interviewApi.page({
      pageNum: pageNum.value,
      pageSize,
      status: filterStatus.value || undefined,
      direction: filterDirection.value || undefined,
    })
    list.value = page?.list || []
    total.value = page?.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function removeSession(row: SessionDetail): void {
  ElMessageBox.confirm(`确定删除面试记录「${row.sessionNo || '#' + row.id}」吗？删除后不可恢复。`, '提示', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
    .then(async () => {
      await interviewApi.remove(row.id)
      ElMessage.success('已删除')
      await loadList()
    })
    .catch(() => {
      /* 取消 */
    })
}

function goContinue(id: number): void {
  router.push(`/interview/${id}`)
}
function goReport(id: number): void {
  router.push(`/report/${id}`)
}
function goSetup(): void {
  router.push('/interview/setup')
}

onMounted(() => {
  void loadList()
})
</script>

<style scoped lang="scss">
.session-list {
  padding-bottom: 40px;
}

.filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.filter-item {
  width: 160px;
}

.session-no {
  font-weight: 600;
  color: var(--text-primary);
}

.dir-tag {
  margin-right: 4px;
}

.progress-text {
  font-variant-numeric: tabular-nums;
  color: var(--text-regular);
}

.score-good {
  color: var(--color-success);
  font-weight: 600;
}
.score-mid {
  color: var(--color-warning);
  font-weight: 600;
}
.score-bad {
  color: var(--color-danger);
  font-weight: 600;
}

.muted {
  color: var(--text-secondary);
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
