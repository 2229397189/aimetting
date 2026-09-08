<template>
  <div class="page-container admin-users">
    <section class="card">
      <div class="card-header">
        <div class="card-title">用户管理</div>
        <div class="flex gap-8">
          <el-input
            v-model="keyword"
            placeholder="搜索用户名 / 昵称 / 邮箱"
            clearable
            class="search-item"
            @keyup.enter="onSearch"
            @clear="onSearch"
          />
          <el-button type="primary" :icon="Search" @click="onSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="loadList">刷新</el-button>
        </div>
      </div>

      <PageLoading v-if="loading" :min-height="220" text="加载中…" />
      <EmptyState v-else-if="!list.length" description="没有匹配的用户" min-height="180" />

      <el-table v-else :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" min-width="140" show-overflow-tooltip />
        <el-table-column prop="nickname" label="昵称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="角色" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.role === 'ADMIN' ? 'warning' : 'info'">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="180">
          <template #default="{ row }">{{ formatTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="注册时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.role !== 'ADMIN'"
              text
              :type="row.status === 1 ? 'danger' : 'success'"
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <span v-else class="muted">-</span>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { adminApi } from '@/api/admin'
import type { AdminUserResp } from '@/types'
import { formatTime } from '@/utils/format'

const loading = ref<boolean>(false)
const list = ref<AdminUserResp[]>([])
const pageNum = ref<number>(1)
const pageSize = 10
const total = ref<number>(0)
const keyword = ref<string>('')

async function loadList(): Promise<void> {
  loading.value = true
  try {
    const page = await adminApi.users({
      pageNum: pageNum.value,
      pageSize,
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

function onSearch(): void {
  pageNum.value = 1
  void loadList()
}

function toggleStatus(row: AdminUserResp): void {
  const target = row.status === 1 ? 0 : 1
  const actionText = target === 0 ? '禁用' : '启用'
  ElMessageBox.confirm(`确定${actionText}用户「${row.username}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: actionText,
    cancelButtonText: '取消',
  })
    .then(async () => {
      await adminApi.updateUserStatus(row.id, target)
      ElMessage.success(`已${actionText}`)
      row.status = target
    })
    .catch(() => {
      /* 取消 */
    })
}

onMounted(() => {
  void loadList()
})
</script>

<style scoped lang="scss">
.admin-users {
  padding-bottom: 40px;
}

.search-item {
  width: 260px;
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
