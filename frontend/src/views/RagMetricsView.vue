<template>
  <div class="rag-metrics-page">
    <el-card shadow="never" class="panel">
      <template #header>
        <div class="card-header">
          <span>RAG 运行指标（V8）</span>
          <el-button type="primary" :loading="loading" @click="loadMetrics">
            刷新指标
          </el-button>
        </div>
      </template>

      <el-alert
        v-if="loadError"
        type="error"
        :closable="false"
        show-icon
        :title="loadError"
        class="metrics-alert"
      />

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="指标数据来自 Debug 查询日志（rag_query_log），在 Debug 页执行查询后会自动累计。"
        class="metrics-alert"
      />

      <div v-loading="loading">
        <el-row :gutter="16" class="stat-row">
          <el-col :xs="12" :sm="8" :lg="4">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-value">{{ metrics?.totalQueryCount ?? '-' }}</div>
              <div class="stat-label">总查询次数</div>
            </el-card>
          </el-col>
          <el-col :xs="12" :sm="8" :lg="4">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-value">{{ metrics?.todayQueryCount ?? '-' }}</div>
              <div class="stat-label">今日查询次数</div>
            </el-card>
          </el-col>
          <el-col :xs="12" :sm="8" :lg="4">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-value">{{ formatMs(metrics?.avgTotalTimeMs) }}</div>
              <div class="stat-label">平均总耗时</div>
            </el-card>
          </el-col>
          <el-col :xs="12" :sm="8" :lg="4">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-value">{{ formatMs(metrics?.avgRetrievalTimeMs) }}</div>
              <div class="stat-label">平均检索耗时</div>
            </el-card>
          </el-col>
          <el-col :xs="12" :sm="8" :lg="4">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-value">{{ formatMs(metrics?.avgGenerationTimeMs) }}</div>
              <div class="stat-label">平均生成耗时</div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :xs="24" :lg="12">
            <el-card shadow="hover" class="section-card">
              <template #header>检索模式分布</template>
              <el-descriptions v-if="metrics" :column="1" border>
                <el-descriptions-item label="VECTOR">
                  {{ metrics.searchModeStats.vectorCount }}
                </el-descriptions-item>
                <el-descriptions-item label="BM25">
                  {{ metrics.searchModeStats.bm25Count }}
                </el-descriptions-item>
                <el-descriptions-item label="HYBRID">
                  {{ metrics.searchModeStats.hybridCount }}
                </el-descriptions-item>
              </el-descriptions>
              <p v-else class="placeholder">暂无数据</p>
            </el-card>
          </el-col>

          <el-col :xs="24" :lg="12">
            <el-card shadow="hover" class="section-card">
              <template #header>Reranker 使用情况</template>
              <el-descriptions v-if="metrics" :column="1" border>
                <el-descriptions-item label="启用重排">
                  {{ metrics.rerankStats.enabledCount }}
                </el-descriptions-item>
                <el-descriptions-item label="未启用重排">
                  {{ metrics.rerankStats.disabledCount }}
                </el-descriptions-item>
              </el-descriptions>
              <p v-else class="placeholder">暂无数据</p>
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="hover" class="section-card">
          <template #header>最近慢查询 Top 10</template>
          <el-table
            v-if="metrics"
            :data="metrics.slowQueries"
            stripe
            style="width: 100%"
            empty-text="暂无慢查询记录"
          >
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
              <template #default="{ row }">{{ row.totalTimeMs }} ms</template>
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
          <p v-else class="placeholder">暂无数据</p>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getRagMetrics } from '@/api/ragMetrics'
import type { RagMetrics } from '@/types/ragMetrics'

const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const metrics = ref<RagMetrics | null>(null)

function formatMs(value: number | undefined): string {
  if (value === undefined || value === null) {
    return '-'
  }
  return `${value} ms`
}

async function loadMetrics() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getRagMetrics()
    if (res.code === 200 && res.data) {
      metrics.value = res.data
    } else {
      metrics.value = null
      loadError.value = res.message || '获取 RAG 指标失败'
    }
  } catch (err: unknown) {
    metrics.value = null
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
  loadMetrics()
})
</script>

<style scoped>
.rag-metrics-page {
  max-width: 1200px;
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

.metrics-alert {
  margin-bottom: 16px;
}

.stat-row {
  margin-bottom: 8px;
}

.stat-card {
  margin-bottom: 16px;
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}

.stat-label {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}

.section-card {
  margin-bottom: 16px;
}

.placeholder {
  color: #909399;
  font-size: 14px;
  margin: 0;
}
</style>
