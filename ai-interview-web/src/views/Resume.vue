<template>
  <div class="page-container resume-page">
    <el-tabs v-model="activeTab" class="resume-tabs">
      <!-- 粘贴文本解析 -->
      <el-tab-pane label="粘贴简历文本" name="paste">
        <div class="card">
          <div class="card-header">
            <div class="card-title">粘贴简历内容</div>
            <span class="tip-text">建议 200 ~ 8000 字，内容越完整解析越准</span>
          </div>
          <el-form :model="parseForm" label-position="top">
            <el-form-item label="简历标题">
              <el-input
                v-model="parseForm.title"
                placeholder="例如：Java 后端实习简历-2026 秋招版"
                maxlength="60"
                show-word-limit
                clearable
              />
            </el-form-item>
            <el-form-item label="简历正文">
              <el-input
                v-model="parseForm.content"
                type="textarea"
                :rows="12"
                placeholder="请粘贴你的简历全文（教育背景 / 技能栈 / 项目经历 / 实习经历等）"
                resize="vertical"
              />
              <div class="char-tip">
                已输入 <b>{{ charCount(parseForm.content) }}</b> 字
              </div>
            </el-form-item>
            <el-button type="primary" :loading="parsing" @click="submitParse">
              <el-icon><MagicStick /></el-icon>开始解析
            </el-button>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 文件上传 -->
      <el-tab-pane label="上传简历文件" name="upload">
        <div class="card">
          <div class="card-header">
            <div class="card-title">上传简历文件</div>
            <span class="tip-text">支持 TXT / MD / PDF，单个文件 ≤ 5MB</span>
          </div>
          <el-upload
            ref="uploadRef"
            class="resume-upload"
            drag
            :auto-upload="false"
            :limit="1"
            :accept="acceptTypes"
            :before-upload="beforeUpload"
            :on-change="onFileChange"
            :on-exceed="onExceed"
            :file-list="fileList"
          >
            <el-icon class="resume-upload__icon"><UploadFilled /></el-icon>
            <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">
                仅支持 .txt / .md / .pdf，且不超过 5MB；上传后会自动解析并评分
              </div>
            </template>
          </el-upload>
          <div class="mt-16">
            <el-button type="primary" :disabled="!fileList.length" :loading="uploading" @click="submitUpload">
              <el-icon><Upload /></el-icon>上传并解析
            </el-button>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 解析结果 -->
    <section v-if="detail" class="card mt-16">
      <div class="card-header">
        <div class="card-title">解析结果：{{ detail.title || '未命名简历' }}</div>
        <div class="flex gap-8">
          <el-tag v-if="detail.parsedBy" size="small" :type="detail.parsedBy === 'AI' ? 'success' : 'warning'">
            {{ detail.parsedBy === 'AI' ? 'AI 解析' : '规则解析（降级）' }}
          </el-tag>
          <el-tag v-if="detail.isDefault" type="primary" size="small">默认简历</el-tag>
          <el-button size="small" @click="setDefault">设为默认</el-button>
        </div>
      </div>

      <div class="result-head">
        <div class="score-ring">
          <el-progress
            type="dashboard"
            :percentage="Number(detail.score || 0)"
            :color="scoreColor"
            :width="120"
          />
          <div class="score-ring__label">简历评分</div>
        </div>
        <div class="result-summary">
          <div class="result-block">
            <div class="result-block__title">优势亮点</div>
            <p class="result-block__text">{{ detail.advantage || '暂无' }}</p>
          </div>
          <div class="result-block">
            <div class="result-block__title">修改建议</div>
            <ul v-if="suggestionList.length" class="suggest-list">
              <li v-for="(s, i) in suggestionList" :key="i">{{ s }}</li>
            </ul>
            <p v-else class="result-block__text">暂无</p>
          </div>
        </div>
      </div>

      <el-divider content-position="left">结构化信息</el-divider>
      <div class="parsed-grid">
        <div class="parsed-block">
          <div class="parsed-block__title">技能栈</div>
          <div v-if="parsedSkills.length" class="tag-wrap">
            <el-tag v-for="(s, i) in parsedSkills" :key="i" type="info" size="small">{{ s }}</el-tag>
          </div>
          <p v-else class="muted">未识别到技能关键词</p>
        </div>
        <div class="parsed-block">
          <div class="parsed-block__title">项目经历</div>
          <ul v-if="parsedProjects.length" class="project-list">
            <li v-for="(p, i) in parsedProjects" :key="i" class="hover-lift">
              <div class="project-name">{{ p.name || `项目 ${i + 1}` }}</div>
              <div v-if="p.description" class="project-desc">{{ p.description }}</div>
              <div v-if="p.techStack && p.techStack.length" class="tag-wrap">
                <el-tag v-for="(t, k) in p.techStack" :key="k" size="small" effect="plain">{{ t }}</el-tag>
              </div>
            </li>
          </ul>
          <p v-else class="muted">未识别到项目经历</p>
        </div>
        <div class="parsed-block">
          <div class="parsed-block__title">教育背景</div>
          <ul v-if="parsedEducations.length" class="edu-list">
            <li v-for="(e, i) in parsedEducations" :key="i">
              {{ [e.school, e.major, e.degree].filter(Boolean).join(' · ') || '未知' }}
              <span v-if="e.period" class="muted">{{ e.period }}</span>
            </li>
          </ul>
          <p v-else class="muted">未识别到教育背景</p>
        </div>
      </div>

      <el-divider content-position="left">简历原文</el-divider>
      <el-collapse>
        <el-collapse-item title="展开查看原文">
          <pre class="raw-text">{{ detail.rawText || '（无原文）' }}</pre>
        </el-collapse-item>
      </el-collapse>
    </section>

    <!-- 简历列表 -->
    <section class="card mt-16">
      <div class="card-header">
        <div class="card-title">我的简历</div>
        <el-button text type="primary" @click="loadList">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
      </div>

      <PageLoading v-if="listLoading" :min-height="120" text="加载中…" />
      <EmptyState v-else-if="!resumeList.length" description="还没有简历，先粘贴或上传一份吧" min-height="140" />
      <el-table v-else :data="resumeList" border stripe size="default">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="评分" width="100" align="center">
          <template #default="{ row }">
            <span :style="{ color: scoreColorOf(row.score), fontWeight: 600 }">
              {{ row.score ?? '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="解析方式" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.parsedBy === 'AI' ? 'success' : 'warning'">
              {{ row.parsedBy === 'AI' ? 'AI' : '规则' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="默认" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault" type="primary" size="small">默认</el-tag>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="viewDetail(row.id)">查看</el-button>
            <el-button text type="primary" size="small" @click="setDefaultById(row.id)">设为默认</el-button>
            <el-button text type="danger" size="small" @click="removeResume(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > pageSize" class="pager">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="loadList"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, UploadInstance, UploadUserFile } from 'element-plus'
import { resumeApi } from '@/api/resume'
import type { ResumeDetailResp, ResumeResp } from '@/types'
import { charCount, formatTime, toArray } from '@/utils/format'
import { clientToken } from '@/utils/crypto'
import PageLoading from '@/components/PageLoading/index.vue'
import EmptyState from '@/components/EmptyState/index.vue'

const activeTab = ref<'paste' | 'upload'>('paste')
const parsing = ref<boolean>(false)
const uploading = ref<boolean>(false)
const listLoading = ref<boolean>(false)

/** 粘贴解析表单 */
const parseForm = reactive({
  title: '',
  content: '',
})

/** 上传相关 */
const uploadRef = ref<UploadInstance | null>(null)
const fileList = ref<UploadUserFile[]>([])
const acceptTypes = '.txt,.md,.pdf,.TXT,.MD,.PDF'
const MAX_SIZE = 5 * 1024 * 1024

/** 列表与详情 */
const resumeList = ref<ResumeResp[]>([])
const detail = ref<ResumeDetailResp | null>(null)
const pageNum = ref<number>(1)
const pageSize = 10
const total = ref<number>(0)

/** 评分颜色：越高越绿 */
const scoreColor = computed<string>(() => {
  const s = Number(detail.value?.score || 0)
  if (s >= 85) return '#16a34a'
  if (s >= 70) return '#4f46e5'
  if (s >= 60) return '#d97706'
  return '#dc2626'
})

function scoreColorOf(score?: number | null): string {
  const s = Number(score || 0)
  if (s >= 85) return '#16a34a'
  if (s >= 70) return '#4f46e5'
  if (s >= 60) return '#d97706'
  return '#dc2626'
}

/** 建议列表（后端可能返回字符串或数组） */
const suggestionList = computed<string[]>(() => toArray(detail.value?.suggestions as any))

/** 技能列表 */
const parsedSkills = computed<string[]>(() => {
  const skills = detail.value?.parsedJson?.skills
  return Array.isArray(skills) ? skills : toArray(skills as any)
})

/** 项目列表 */
const parsedProjects = computed(() => detail.value?.parsedJson?.projects || [])

/** 教育背景 */
const parsedEducations = computed(() => detail.value?.parsedJson?.educations || [])

/** 文件大小与类型校验（BR-22 / R-02） */
function beforeUpload(file: File): boolean {
  const name = file.name.toLowerCase()
  const ok = name.endsWith('.txt') || name.endsWith('.md') || name.endsWith('.pdf')
  if (!ok) {
    ElMessage.error('仅支持 TXT / MD / PDF 格式的简历文件')
    return false
  }
  if (file.size > MAX_SIZE) {
    ElMessage.error('文件大小不能超过 5MB')
    return false
  }
  return true
}

function onFileChange(file: UploadUserFile): void {
  if (file && file.raw && !beforeUpload(file.raw)) {
    // 校验失败时移除该文件
    fileList.value = fileList.value.filter((f) => f.uid !== file.uid)
  }
}

function onExceed(): void {
  ElMessage.warning('一次只能上传一个文件，请先移除已选文件')
}

/** 提交文本解析 */
async function submitParse(): Promise<void> {
  const content = (parseForm.content || '').trim()
  if (content.length < 50) {
    ElMessage.warning('简历内容太短，请至少粘贴 50 个字符')
    return
  }
  parsing.value = true
  try {
    const id = await resumeApi.parse({
      title: parseForm.title || `简历-${new Date().toLocaleDateString('zh-CN')}`,
      content,
      clientToken: clientToken('resume'),
    })
    ElMessage.success('解析完成')
    await viewDetail(id)
    await loadList()
  } catch (e) {
    /* 已统一提示 */
  } finally {
    parsing.value = false
  }
}

/** 提交文件上传 */
async function submitUpload(): Promise<void> {
  const item = fileList.value[0]
  if (!item || !item.raw) {
    ElMessage.warning('请先选择文件')
    return
  }
  uploading.value = true
  try {
    const id = await resumeApi.upload(item.raw, parseForm.title || item.name)
    ElMessage.success('上传并解析完成')
    await viewDetail(id)
    await loadList()
    fileList.value = []
  } catch (e) {
    /* 已统一提示 */
  } finally {
    uploading.value = false
  }
}

/** 查看详情 */
async function viewDetail(id: number | string): Promise<void> {
  try {
    const data = await resumeApi.detail(id)
    detail.value = data
  } catch (e) {
    detail.value = null
  }
}

/** 加载列表 */
async function loadList(): Promise<void> {
  listLoading.value = true
  try {
    const page = await resumeApi.page({ pageNum: pageNum.value, pageSize })
    resumeList.value = page?.list || []
    total.value = page?.total || 0
  } catch (e) {
    resumeList.value = []
    total.value = 0
  } finally {
    listLoading.value = false
  }
}

/** 设为默认（当前详情） */
async function setDefault(): Promise<void> {
  if (!detail.value) return
  await setDefaultById(detail.value.id)
}

/** 设为默认（按 ID） */
async function setDefaultById(id: number | string): Promise<void> {
  try {
    await resumeApi.markDefault(id)
    ElMessage.success('已设为默认简历')
    await loadList()
  } catch (e) {
    /* 已统一提示 */
  }
}

/** 删除简历 */
async function removeResume(id: number | string): Promise<void> {
  try {
    await ElMessageBox.confirm('删除后不可恢复，确定删除这份简历吗？', '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch (e) {
    return
  }
  try {
    await resumeApi.remove(id)
    ElMessage.success('已删除')
    if (detail.value?.id === id) detail.value = null
    await loadList()
  } catch (e) {
    /* 已统一提示 */
  }
}

onMounted(() => {
  void loadList()
})
</script>

<style scoped lang="scss">
.resume-page {
  padding-bottom: 40px;
  background: var(--grad-page);
  border-radius: var(--radius-lg);
}

.tip-text {
  font-size: 12px;
  color: var(--text-secondary);
}

.char-tip {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  text-align: right;
}

.resume-upload {
  width: 100%;
}

.resume-upload__icon {
  font-size: 44px;
  color: var(--color-primary);
}

.result-head {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  padding: 16px;
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  background: var(--grad-brand-soft);
}

.score-ring {
  text-align: center;
  flex-shrink: 0;
}

.score-ring__label {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.result-summary {
  flex: 1;
  min-width: 0;
}

.result-block + .result-block {
  margin-top: 14px;
}

.result-block__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.result-block__text {
  margin: 0;
  font-size: 13px;
  color: var(--text-regular);
  line-height: 1.7;
}

.suggest-list {
  margin: 0;
  padding-left: 20px;
}

.suggest-list li {
  font-size: 13px;
  color: var(--text-regular);
  line-height: 1.8;
}

.parsed-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.parsed-block__title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.tag-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.project-list,
.edu-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.project-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.project-desc {
  font-size: 12px;
  color: var(--text-regular);
  line-height: 1.7;
  margin: 3px 0;
}

.muted {
  color: var(--text-secondary);
  font-size: 12px;
  margin: 0;
}

.raw-text {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.7;
  color: var(--text-regular);
  background: #fafafa;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  padding: 12px;
  margin: 0;
  max-height: 320px;
  overflow: auto;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1024px) {
  .parsed-grid {
    grid-template-columns: 1fr;
  }

  .result-head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
