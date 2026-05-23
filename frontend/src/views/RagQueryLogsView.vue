<template>
  <div class="rag-query-logs-page">
    <el-card shadow="never" class="panel">
      <template #header>
        <div class="card-header">
          <span>查询日志中心（V8）</span>
          <el-space wrap>
            <el-button @click="router.push('/slow-query-analysis')">慢查询分析</el-button>
            <el-button type="primary" :loading="loading" @click="loadLogs">
              刷新
            </el-button>
          </el-space>
        </div>
      </template>

      <el-alert
        v-if="loadError"
        type="error"
        :closable="false"
        show-icon
        :title="loadError"
        class="logs-alert"
      />

      <el-form :inline="true" class="filter-form" @submit.prevent="handleSearch">
        <el-form-item label="问题关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索问题内容"
            clearable
            style="width: 220px"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="检索模式">
          <el-select
            v-model="filters.searchMode"
            placeholder="全部"
            clearable
            style="width: 140px"
            @change="handleSearch"
          >
            <el-option label="VECTOR" value="VECTOR" />
            <el-option label="BM25" value="BM25" />
            <el-option label="HYBRID" value="HYBRID" />
          </el-select>
        </el-form-item>
        <el-form-item label="重排">
          <el-select
            v-model="filters.enableRerank"
            placeholder="全部"
            clearable
            style="width: 120px"
            @change="handleSearch"
          >
            <el-option label="已启用" :value="true" />
            <el-option label="未启用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="慢查询">
          <el-checkbox v-model="filters.slowOnly" @change="handleSearch">
            总耗时 &gt; 3000ms
          </el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="records"
        stripe
        style="width: 100%"
        empty-text="暂无查询日志"
      >
        <el-table-column prop="queryLogId" label="查询 ID" width="90" />
        <el-table-column prop="question" label="问题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="searchMode" label="检索模式" width="100" />
        <el-table-column label="重排" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enableRerank ? 'success' : 'info'" size="small">
              {{ row.enableRerank ? '已启用' : '未启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="总耗时" width="100">
          <template #default="{ row }">
            <span :class="{ 'slow-time': isSlow(row.totalTimeMs) }">{{ row.totalTimeMs }} ms</span>
          </template>
        </el-table-column>
        <el-table-column label="检索耗时" width="100">
          <template #default="{ row }">{{ row.retrievalTimeMs }} ms</template>
        </el-table-column>
        <el-table-column label="生成耗时" width="100">
          <template #default="{ row }">{{ row.generationTimeMs }} ms</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="查询时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="goToDetail(row.queryLogId)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10]"
          layout="total, prev, pager, next"
          background
          @current-change="loadLogs"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listRagQueryLogs } from '@/api/ragQueryLogs'
import type { RagQueryLogItem } from '@/types/ragQueryLogs'

const SLOW_QUERY_THRESHOLD_MS = 3000

const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const records = ref<RagQueryLogItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const filters = reactive({
  keyword: '',
  searchMode: '' as string | undefined,
  enableRerank: undefined as boolean | undefined,
  slowOnly: false,
})

function isSlow(totalTimeMs: number | undefined): boolean {
  return totalTimeMs != null && totalTimeMs > SLOW_QUERY_THRESHOLD_MS
}

async function loadLogs() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await listRagQueryLogs({
      page: page.value,
      pageSize: pageSize.value,
      keyword: filters.keyword.trim() || undefined,
      searchMode: filters.searchMode || undefined,
      enableRerank: filters.enableRerank,
      slowOnly: filters.slowOnly || undefined,
    })
    if (res.code === 200 && res.data) {
      records.value = res.data.records
      total.value = res.data.total
      page.value = res.data.page
      pageSize.value = res.data.pageSize
    } else {
      records.value = []
      total.value = 0
      loadError.value = res.message || '获取查询日志失败'
    }
  } catch (err: unknown) {
    records.value = []
    total.value = 0
    const message = err instanceof Error ? err.message : '无法连接后端服务'
    loadError.value = `请求失败：${message}`
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadLogs()
}

function handleReset() {
  filters.keyword = ''
  filters.searchMode = undefined
  filters.enableRerank = undefined
  filters.slowOnly = false
  page.value = 1
  loadLogs()
}

function goToDetail(queryLogId: number) {
  router.push(`/debug/${queryLogId}`)
}

onMounted(() => {
  loadLogs()
})
</script>

<style scoped>
.rag-query-logs-page {
  max-width: 1280px;
}

.panel {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.logs-alert {
  margin-bottom: 16px;
}

.filter-form {
  margin-bottom: 16px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.slow-time {
  color: #f56c6c;
  font-weight: 600;
}
</style>
