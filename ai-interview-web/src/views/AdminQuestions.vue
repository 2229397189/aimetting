<template>
  <div class="page-container admin-questions">
    <BackBar to="/admin/dashboard" label="返回管理后台" />
    <section class="card">
      <div class="card-header">
        <div class="card-title grad-text">题目管理</div>
        <div class="flex gap-8">
          <el-button type="success" :icon="Upload" @click="openImportDialog">批量导入</el-button>
          <el-button type="primary" :icon="Plus" @click="openCreate">新增题目</el-button>
        </div>
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
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <PageLoading v-if="loading" :min-height="220" text="加载中…" />
      <EmptyState v-else-if="!list.length" description="暂无题目，点击右上角新增" min-height="180" />

      <el-table v-else :data="list" border stripe>
        <el-table-column prop="title" label="题目" min-width="260" show-overflow-tooltip />
        <el-table-column label="方向" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">{{ directionLabel(row.direction) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="difficultyTagType(row.difficulty)">{{ difficultyLabel(row.difficulty) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="sourceTagType(row.source)">{{ sourceLabel(row.source) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="removeQuestion(row)">删除</el-button>
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

    <!-- 新增 / 编辑 对话框 -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑题目' : '新增题目'" width="560px" @closed="resetForm">
      <el-form :model="form" label-position="top">
        <el-form-item label="方向" required>
          <el-select v-model="form.direction" placeholder="选择方向" class="full-width">
            <el-option v-for="opt in directionOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度" required>
          <el-select v-model="form.difficulty" placeholder="选择难度" class="full-width">
            <el-option v-for="opt in difficultyOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目" required>
          <el-input v-model="form.title" type="textarea" :rows="3" placeholder="题面内容" resize="vertical" />
        </el-form-item>
        <el-form-item label="考察要点（每行一条）">
          <el-input v-model="referenceText" type="textarea" :rows="3" placeholder="逐行填写考察点" resize="vertical" />
        </el-form-item>
        <el-form-item label="标签（逗号分隔）">
          <el-input v-model="tagText" placeholder="如：JVM, 垃圾回收" clearable />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" :rows="4" placeholder="参考答案 / 解析" resize="vertical" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="statusOn" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog v-model="importVisible" title="批量导入题目" width="600px" @closed="resetImport">
      <el-alert
        class="mb-16"
        type="info"
        :closable="false"
        title="粘贴 JSON 数组，每条包含 direction / difficulty / title，可选 referencePoints(数组) / analysis"
        show-icon
      />
      <el-input
        v-model="importText"
        type="textarea"
        :rows="10"
        placeholder='[{"direction":"JAVA_BACKEND","difficulty":"MEDIUM","title":"简述 JVM 内存结构","referencePoints":["程序计数器","堆"],"analysis":"..."}]'
        resize="vertical"
      />
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="submitImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Upload } from '@element-plus/icons-vue'
import { questionApi } from '@/api/question'
import BackBar from '@/components/BackBar/index.vue'
import type { Difficulty, Direction, QuestionResp, QuestionSaveReq, QuestionSource } from '@/types'
import {
  DIFFICULTY_OPTIONS,
  DIRECTION_OPTIONS,
  directionLabel,
  difficultyLabel,
  difficultyTagType,
  sourceLabel,
} from '@/utils/dict'
import { formatTime } from '@/utils/format'

const loading = ref<boolean>(false)
const list = ref<QuestionResp[]>([])
const pageNum = ref<number>(1)
const pageSize = 10
const total = ref<number>(0)

const filterDirection = ref<Direction | ''>('')
const filterDifficulty = ref<Difficulty | ''>('')
const directionOptions = DIRECTION_OPTIONS
const difficultyOptions = DIFFICULTY_OPTIONS

/* ---------- 表单 ---------- */
const formVisible = ref<boolean>(false)
const isEdit = ref<boolean>(false)
const saving = ref<boolean>(false)
const editId = ref<number | null>(null)

const form = reactive<QuestionSaveReq>({
  direction: 'JAVA_BACKEND',
  difficulty: 'MEDIUM',
  title: '',
  referencePoints: [],
  tags: [],
  analysis: '',
  status: 1,
})
const referenceText = ref<string>('')
const tagText = ref<string>('')
const statusOn = ref<boolean>(true)

/* ---------- 导入 ---------- */
const importVisible = ref<boolean>(false)
const importing = ref<boolean>(false)
const importText = ref<string>('')

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

async function loadList(): Promise<void> {
  loading.value = true
  try {
    const page = await questionApi.page({
      pageNum: pageNum.value,
      pageSize,
      direction: filterDirection.value || undefined,
      difficulty: filterDifficulty.value || undefined,
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

function openCreate(): void {
  isEdit.value = false
  editId.value = null
  resetForm()
  formVisible.value = true
}

function openEdit(row: QuestionResp): void {
  isEdit.value = true
  editId.value = row.id
  form.direction = row.direction
  form.difficulty = row.difficulty
  form.title = row.title
  form.referencePoints = Array.isArray(row.referencePoints)
    ? [...row.referencePoints]
    : (row.referencePoints ? String(row.referencePoints).split(/[,，;；\n]/).map((s) => s.trim()).filter(Boolean) : [])
  form.tags = Array.isArray(row.tags)
    ? [...row.tags]
    : (row.tags ? String(row.tags).split(/[,，;；\n]/).map((s) => s.trim()).filter(Boolean) : [])
  form.analysis = row.analysis || ''
  form.status = row.status ?? 1
  referenceText.value = (form.referencePoints || []).join('\n')
  tagText.value = (form.tags || []).join(',')
  statusOn.value = form.status === 1
  formVisible.value = true
}

function resetForm(): void {
  form.direction = 'JAVA_BACKEND'
  form.difficulty = 'MEDIUM'
  form.title = ''
  form.referencePoints = []
  form.tags = []
  form.analysis = ''
  form.status = 1
  referenceText.value = ''
  tagText.value = ''
  statusOn.value = true
}

function submitForm(): void {
  if (!form.title.trim()) {
    ElMessage.warning('请填写题目')
    return
  }
  const referencePoints = referenceText.value
    .split('\n')
    .map((s) => s.trim())
    .filter(Boolean)
  const tags = tagText.value
    .split(/[,，;；]/)
    .map((s) => s.trim())
    .filter(Boolean)
  const payload: QuestionSaveReq = {
    direction: form.direction,
    difficulty: form.difficulty,
    title: form.title.trim(),
    referencePoints,
    tags,
    analysis: form.analysis || undefined,
    status: statusOn.value ? 1 : 0,
  }
  saving.value = true
  const task = isEdit.value && editId.value != null
    ? questionApi.update(editId.value, payload)
    : questionApi.save(payload)
  task
    .then(() => {
      ElMessage.success(isEdit.value ? '已更新' : '已新增')
      formVisible.value = false
      void loadList()
    })
    .catch(() => {
      /* 已统一提示 */
    })
    .finally(() => {
      saving.value = false
    })
}

function removeQuestion(row: QuestionResp): void {
  ElMessageBox.confirm(`确定删除题目「${row.title}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
    .then(async () => {
      await questionApi.remove(row.id)
      ElMessage.success('已删除')
      void loadList()
    })
    .catch(() => {
      /* 取消 */
    })
}

/* ---------- 批量导入 ---------- */
function openImportDialog(): void {
  importVisible.value = true
}
function resetImport(): void {
  importText.value = ''
}
function submitImport(): void {
  if (!importText.value.trim()) {
    ElMessage.warning('请粘贴待导入的 JSON 数组')
    return
  }
  let parsed: QuestionSaveReq[]
  try {
    parsed = JSON.parse(importText.value)
  } catch (e) {
    ElMessage.error('JSON 解析失败，请检查格式')
    return
  }
  if (!Array.isArray(parsed) || !parsed.length) {
    ElMessage.warning('JSON 必须是非空数组')
    return
  }
  importing.value = true
  questionApi
    .importQuestions(parsed)
    .then((res) => {
      ElMessage.success(`导入完成：成功 ${res.successCount}，失败 ${res.failCount}`)
      importVisible.value = false
      void loadList()
    })
    .catch(() => {
      /* 已统一提示 */
    })
    .finally(() => {
      importing.value = false
    })
}

onMounted(() => {
  void loadList()
})
</script>

<style scoped lang="scss">
.admin-questions {
  padding-bottom: 40px;
  background: var(--grad-page);
  border-radius: var(--radius-lg);
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

.full-width {
  width: 100%;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
