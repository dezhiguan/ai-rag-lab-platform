<template>
  <div class="slow-query-page">
    <el-card shadow="never" class="panel">
      <template #header>
        <div class="card-header">
          <span>慢查询分析（V8）</span>
          <el-button type="primary" :loading="loading" @click="loadAnalysis">
            刷新
          </el-button>
        </div>
      </template>

      <el-alert
        v-if="loadError"
        type="error"
        :closable="false"
        show-icon
        :title="loadError"
        class="analysis-alert"
      />

      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="analysis-alert"
      >
        <template #title>
          慢查询判定：总耗时 ≥ 3000ms，或检索耗时 ≥ 1000ms，或生成耗时 ≥ 2000ms
        </template>
      </el-alert>

      <div v-loading="loading" class="summary-row">
        <el-tag type="warning" size="large">慢查询总数：{{ analysis?.totalCount ?? 0 }}</el-tag>
        <el-tag type="info" size="large">当前展示：{{ records.length }} 条</el-tag>
      </div>

      <div class="rag-table-scroll">
      <el-table
        :data="records"
        stripe
        style="width: 100%"
        empty-text="暂无慢查询记录"
      >
        <el-table-column prop="question" label="问题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="searchMode" label="检索模式" width="90" />
        <el-table-column label="重排" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enableRerank ? 'success' : 'info'" size="small">
              {{ row.enableRerank ? '已启用' : '未启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="检索耗时" width="95">
          <template #default="{ row }">
            <span :class="latencyClass(row.retrievalTimeMs, 1000)">{{ row.retrievalTimeMs }} ms</span>
          </template>
        </el-table-column>
        <el-table-column label="生成耗时" width="95">
          <template #default="{ row }">
            <span :class="latencyClass(row.generationTimeMs, 2000)">{{ row.generationTimeMs }} ms</span>
          </template>
        </el-table-column>
        <el-table-column label="总耗时" width="95">
          <template #default="{ row }">
            <span :class="latencyClass(row.totalTimeMs, 3000)">{{ row.totalTimeMs }} ms</span>
          </template>
        </el-table-column>
        <el-table-column label="慢查询原因" min-width="160">
          <template #default="{ row }">
            <el-space wrap>
              <el-tag
                v-for="reason in row.slowReasons"
                :key="reason"
                type="warning"
                size="small"
              >
                {{ reason }}
              </el-tag>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column label="优化建议" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.suggestions?.join('；') || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="goToDetail(row.queryLogId)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getSlowQueryAnalysis } from '@/api/slowQueryAnalysis'
import type { SlowQueryAnalysis } from '@/types/slowQueryAnalysis'

const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const analysis = ref<SlowQueryAnalysis | null>(null)

const records = computed(() => analysis.value?.records ?? [])

function latencyClass(value: number | undefined, threshold: number): string {
  return value != null && value >= threshold ? 'slow-latency' : ''
}

async function loadAnalysis() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getSlowQueryAnalysis(50)
    if (res.code === 200 && res.data) {
      analysis.value = res.data
    } else {
      analysis.value = null
      loadError.value = res.message || '获取慢查询分析失败'
    }
  } catch (err: unknown) {
    analysis.value = null
    const message = err instanceof Error ? err.message : '无法连接后端服务'
    loadError.value = `请求失败：${message}`
  } finally {
    loading.value = false
  }
}

function goToDetail(queryLogId: number) {
  router.push(`/debug/${queryLogId}`)
}

onMounted(() => {
  loadAnalysis()
})
</script>

<style scoped>
.slow-query-page {
  max-width: 1400px;
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

.analysis-alert {
  margin-bottom: 16px;
}

.summary-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.slow-latency {
  color: #f56c6c;
  font-weight: 600;
}

.suggestion-list {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}
</style>
