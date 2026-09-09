<template>
  <div class="page-container report-list">
    <section class="card">
      <div class="card-header">
        <div class="card-title">面试报告历史</div>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <PageLoading v-if="loading" :min-height="200" text="加载中…" />
      <EmptyState
        v-else-if="!list.length"
        description="还没有生成过报告，完成一场面试后会自动生成"
        min-height="180"
      >
        <template #action>
          <el-button type="primary" size="small" @click="goInterview">去面试</el-button>
        </template>
      </EmptyState>

      <el-table v-else :data="list" border stripe @row-click="onRowClick">
        <el-table-column prop="sessionNo" label="场次编号" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="session-no">{{ row.sessionNo || `#${row.sessionId}` }}</span>
          </template>
        </el-table-column>
        <el-table-column label="总分" width="120" align="center">
          <template #default="{ row }">
            <span :class="scoreClass(row.totalScore)">{{ formatScore(row.totalScore) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="方向" min-width="170">
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
            >{{ directionLabel(d) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="difficultyTagType(row.difficulty)">{{ difficultyLabel(row.difficulty) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="生成方式" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.generatedBy === 'AI' ? 'success' : 'warning'">
              {{ row.generatedBy === 'AI' ? 'AI 生成' : row.generatedBy === 'RULE' ? '规则生成' : '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="生成时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click.stop="goDetail(row.sessionId)">查看</el-button>
            <el-button text type="success" size="small" @click.stop="goExportPdf(row.sessionId)">导出 PDF</el-button>
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
import { Refresh } from '@element-plus/icons-vue'
import { reportApi } from '@/api/report'
import type { Direction, ReportBrief } from '@/types'
import { directionLabel, difficultyLabel, difficultyTagType } from '@/utils/dict'
import { formatScore, formatTime } from '@/utils/format'
import PageLoading from '@/components/PageLoading/index.vue'
import EmptyState from '@/components/EmptyState/index.vue'

const router = useRouter()

const loading = ref<boolean>(false)
const list = ref<ReportBrief[]>([])
const pageNum = ref<number>(1)
const pageSize = 10
const total = ref<number>(0)

function scoreClass(score?: number | null): string {
  const n = Number(score || 0)
  if (n >= 80) return 'score-good'
  if (n >= 60) return 'score-mid'
  return 'score-bad'
}

async function loadList(): Promise<void> {
  loading.value = true
  try {
    const page = await reportApi.page({ pageNum: pageNum.value, pageSize })
    list.value = page?.list || []
    total.value = page?.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function goDetail(sessionId: number): void {
  router.push(`/report/${sessionId}`)
}

/** 跳转详情页并自动触发打印（?autoprint=1） */
function goExportPdf(sessionId: number): void {
  router.push(`/report/${sessionId}?autoprint=1`)
}

function onRowClick(row: ReportBrief): void {
  goDetail(row.sessionId)
}

function goInterview(): void {
  router.push('/interview/setup')
}

onMounted(() => {
  void loadList()
})
</script>

<style scoped lang="scss">
.report-list {
  padding-bottom: 40px;
}

.session-no {
  font-weight: 600;
  color: var(--text-primary);
}

.dir-tag {
  margin-right: 4px;
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

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
