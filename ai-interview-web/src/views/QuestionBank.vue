<template>
  <div class="page-container question-bank">
    <BackBar to="/" label="返回首页" />
    <section class="card">
      <div class="card-header">
        <div class="card-title grad-text">题库浏览</div>
        <span class="tip-text">共 {{ total }} 道题 · 来源含 AI 生成 / 精选题库 / 内置 / 管理员录入</span>
      </div>

      <!-- 筛选 -->
      <div class="filters">
        <el-select v-model="filterDirection" placeholder="全部方向" clearable class="filter-item" @change="onFilterChange">
          <el-option label="全部方向" value="" />
          <el-option v-for="opt in directionOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-select v-model="filterDifficulty" placeholder="全部难度" clearable class="filter-item" @change="onFilterChange">
          <el-option label="全部难度" value="" />
          <el-option v-for="opt in difficultyOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-input
          v-model="keyword"
          placeholder="搜索题目关键词"
          clearable
          class="filter-item filter-keyword"
          @keyup.enter="onFilterChange"
          @clear="onFilterChange"
        />
        <el-button type="primary" :icon="Search" @click="onFilterChange">查询</el-button>
        <el-button :icon="Refresh" @click="resetFilter">重置</el-button>
      </div>

      <PageLoading v-if="loading" :min-height="220" text="加载中…" />
      <EmptyState v-else-if="!list.length" description="没有符合条件的题目" min-height="180" />

      <el-table v-else :data="list" border stripe @row-click="openDetail">
        <el-table-column prop="title" label="题目" min-width="280" show-overflow-tooltip />
        <el-table-column label="方向" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">{{ directionLabel(row.direction) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="difficultyTagType(row.difficulty)">{{ difficultyLabel(row.difficulty) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="sourceTagType(row.source)">{{ sourceLabel(row.source) }}</el-tag>
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

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="detailData?.title || '题目详情'" size="92%" direction="rtl">
      <template v-if="detailData">
        <div class="detail-tags">
          <el-tag size="small" type="info" effect="plain">{{ directionLabel(detailData.direction) }}</el-tag>
          <el-tag size="small" :type="difficultyTagType(detailData.difficulty)">{{ difficultyLabel(detailData.difficulty) }}</el-tag>
          <el-tag size="small" :type="sourceTagType(detailData.source)">{{ sourceLabel(detailData.source) }}</el-tag>
        </div>

        <el-divider content-position="left">考察要点</el-divider>
        <ul v-if="referencePoints.length" class="point-list">
          <li v-for="(p, i) in referencePoints" :key="i">{{ p }}</li>
        </ul>
        <p v-else class="muted">暂无考察要点</p>

        <el-divider content-position="left">参考标签</el-divider>
        <div v-if="tagList.length" class="tag-wrap">
          <el-tag v-for="(t, i) in tagList" :key="i" size="small" effect="plain">{{ t }}</el-tag>
        </div>
        <p v-else class="muted">暂无标签</p>

        <el-divider content-position="left">解析</el-divider>
        <pre class="analysis-text">{{ detailData.analysis || '暂无解析' }}</pre>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { questionApi } from '@/api/question'
import type { Direction, Difficulty, QuestionDetailResp, QuestionResp, QuestionSource } from '@/types'
import {
  DIFFICULTY_OPTIONS,
  DIRECTION_OPTIONS,
  directionLabel,
  difficultyLabel,
  difficultyTagType,
  sourceLabel,
} from '@/utils/dict'
import { toArray } from '@/utils/format'
import PageLoading from '@/components/PageLoading/index.vue'
import EmptyState from '@/components/EmptyState/index.vue'
import BackBar from '@/components/BackBar/index.vue'

const loading = ref<boolean>(false)
const list = ref<QuestionResp[]>([])
const pageNum = ref<number>(1)
const pageSize = 10
const total = ref<number>(0)

const filterDirection = ref<Direction | ''>('')
const filterDifficulty = ref<Difficulty | ''>('')
const keyword = ref<string>('')

const drawerVisible = ref<boolean>(false)
const detailData = ref<QuestionDetailResp | null>(null)
const detailLoading = ref<boolean>(false)

const directionOptions = DIRECTION_OPTIONS
const difficultyOptions = DIFFICULTY_OPTIONS

const referencePoints = computed<string[]>(() => toArray(detailData.value?.referencePoints as any))
const tagList = computed<string[]>(() => toArray(detailData.value?.tags as any))

function sourceTagType(source?: QuestionSource | string): '' | 'success' | 'warning' | 'info' {
  switch (source) {
    case 'AI':
      return 'success'
    case 'BANK':
      return 'warning'
    case 'SEED':
      return 'info'
    default:
      return ''
  }
}

function onFilterChange(): void {
  pageNum.value = 1
  void loadList()
}

function resetFilter(): void {
  filterDirection.value = ''
  filterDifficulty.value = ''
  keyword.value = ''
  pageNum.value = 1
  void loadList()
}

async function loadList(): Promise<void> {
  loading.value = true
  try {
    const page = await questionApi.page({
      pageNum: pageNum.value,
      pageSize,
      direction: filterDirection.value || undefined,
      difficulty: filterDifficulty.value || undefined,
      keyword: keyword.value.trim() || undefined,
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

async function openDetail(row: QuestionResp): Promise<void> {
  drawerVisible.value = true
  detailData.value = null
  detailLoading.value = true
  try {
    const data = await questionApi.detail(row.id)
    detailData.value = data
  } catch (e) {
    ElMessage.error('题目详情加载失败')
    detailData.value = null
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  void loadList()
})
</script>

<style scoped lang="scss">
.question-bank {
  padding-bottom: 40px;
}

.tip-text {
  font-size: 12px;
  color: var(--text-secondary);
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

.filter-keyword {
  width: 220px;
}

.detail-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.point-list {
  margin: 0;
  padding-left: 20px;
}

.point-list li {
  font-size: 13px;
  color: var(--text-regular);
  line-height: 1.9;
}

.tag-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.analysis-text {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-regular);
  background: var(--bg-hover);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  padding: 12px;
  margin: 0;
  max-height: 360px;
  overflow: auto;
}

.muted {
  color: var(--text-secondary);
  font-size: 13px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-table__row) {
  cursor: pointer;
}

/* 移动端：筛选项占满整行，表格横向滚动 */
@media (max-width: 767px) {
  .filter-item,
  .filter-keyword {
    width: 100%;
  }

  .filters {
    gap: 8px;
  }

  .filters > .el-button {
    flex: 1;
  }
}
</style>
