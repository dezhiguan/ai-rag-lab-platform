<template>
  <div class="debug-detail-page">
    <el-page-header @back="goBack">
      <template #content>
        <span>Debug 查询详情 #{{ queryLogId }}</span>
      </template>
    </el-page-header>

    <el-skeleton v-if="loading" :rows="8" animated class="skeleton" />

    <template v-else-if="detail">
      <el-card shadow="never" class="panel">
        <template #header><span>检索与 Provider</span></template>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="检索模式">
            <el-tag :type="searchModeTagType(detail.searchMode)" size="small">
              {{ detail.searchMode ?? 'VECTOR' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Reranker">
            <el-tag :type="showRerankColumns ? 'success' : 'info'" size="small">
              {{ showRerankColumns ? '已启用' : '未启用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Embedding Provider">
            {{ detail.embeddingProvider }}
          </el-descriptions-item>
          <el-descriptions-item label="Embedding Model">
            {{ detail.embeddingModel }}
          </el-descriptions-item>
          <el-descriptions-item label="Chat Provider">
            {{ detail.chatProvider }}
          </el-descriptions-item>
          <el-descriptions-item label="Chat Model">
            {{ detail.chatModel }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header><span>原始问题</span></template>
        <div class="text-block">{{ detail.question }}</div>
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header><span>耗时统计</span></template>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="检索耗时">
            {{ detail.latency.retrievalTimeMs }} ms
          </el-descriptions-item>
          <el-descriptions-item label="生成耗时">
            {{ detail.latency.generationTimeMs }} ms
          </el-descriptions-item>
          <el-descriptions-item label="总耗时">
            {{ detail.latency.totalTimeMs }} ms
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <TokenUsagePanel :usage="detail.tokenUsage" />

      <el-card shadow="never" class="panel">
        <template #header>
          <span>召回 Chunk（{{ detail.retrievedChunks.length }}，进入 Prompt {{ detail.contextChunks?.length ?? 0 }}）</span>
        </template>
        <el-alert
          v-if="showRerankColumns"
          type="success"
          :closable="false"
          show-icon
          class="rerank-summary"
          :title="`重排摘要：${rerankSummaryText}`"
          description="按重排后排名展示；V6-02 之前的历史记录可能无重排明细字段。"
        />
        <div class="rag-table-scroll">
        <el-table :data="displayRetrievedChunks" stripe style="width: 100%">
          <el-table-column
            prop="rankPosition"
            :label="showRerankColumns ? '重排#' : '#'"
            width="60"
          />
          <el-table-column
            v-if="showRerankColumns"
            prop="originalRank"
            label="原排名"
            width="70"
          />
          <el-table-column
            v-if="showRerankColumns"
            prop="rerankRank"
            label="重排排名"
            width="80"
          />
          <el-table-column v-if="showRerankColumns" label="排名变化" width="100">
            <template #default="{ row }">
              <el-tag :type="rerankChangeTagType(rerankChangeKind(row))" size="small">
                {{ rerankChangeLabel(row) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="showRerankColumns" label="重排分" width="90">
            <template #default="{ row }">{{ formatOptionalScore(row.rerankScore) }}</template>
          </el-table-column>
          <el-table-column prop="documentName" label="文档" min-width="160" show-overflow-tooltip />
          <el-table-column prop="chunkIndex" label="Chunk" width="70" />
          <el-table-column
            v-if="detail.searchMode === 'HYBRID' && hasHybridDetailFields"
            label="来源"
            width="120"
          >
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ hybridSourceLabel(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="detail.searchMode === 'HYBRID' && hasHybridDetailFields"
            label="Vector 分"
            width="90"
          >
            <template #default="{ row }">{{ formatOptionalScore(row.vectorScore) }}</template>
          </el-table-column>
          <el-table-column
            v-if="detail.searchMode === 'HYBRID' && hasHybridDetailFields"
            label="BM25 分"
            width="90"
          >
            <template #default="{ row }">{{ formatOptionalScore(row.bm25Score) }}</template>
          </el-table-column>
          <el-table-column
            v-if="detail.searchMode === 'HYBRID' && hasHybridDetailFields"
            label="Hybrid 分"
            width="95"
          >
            <template #default="{ row }">{{ formatOptionalScore(row.hybridScore ?? row.score) }}</template>
          </el-table-column>
          <el-table-column
            v-else
            :label="scoreColumnLabel(detail.searchMode)"
            width="100"
          >
            <template #default="{ row }">{{ formatScore(row.score) }}</template>
          </el-table-column>
          <el-table-column label="进入 Prompt" width="130">
            <template #default="{ row }">
              <el-tag v-if="row.usedInPrompt" type="success" size="small">已进入</el-tag>
              <el-tag v-else type="info" size="small">未进入</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="过滤原因" min-width="140">
            <template #default="{ row }">
              <span v-if="row.usedInPrompt">—</span>
              <span v-else class="filter-reason">{{ filterReasonLabel(row.filterReason) }}</span>
            </template>
          </el-table-column>
        </el-table>
        </div>
        <el-collapse class="chunk-collapse">
          <el-collapse-item
            v-for="chunk in displayRetrievedChunks"
            :key="chunk.chunkId"
            :title="chunkCollapseTitle(chunk)"
          >
            <div class="chunk-content">{{ chunk.content }}</div>
          </el-collapse-item>
        </el-collapse>
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header>
          <div class="section-header">
            <span>注入 Prompt 的 Context（{{ detail.contextChunks?.length ?? 0 }} 个片段）</span>
            <el-button size="small" @click="copyText(detail.context)">复制</el-button>
          </div>
        </template>
        <el-input v-model="detail.context" type="textarea" :rows="8" readonly class="mono-area" />
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header>
          <div class="section-header">
            <span>完整 Prompt</span>
            <el-button size="small" @click="copyText(detail.prompt)">复制</el-button>
          </div>
        </template>
        <el-input v-model="detail.prompt" type="textarea" :rows="12" readonly class="mono-area" />
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header><span>模型回答</span></template>
        <div class="text-block">{{ detail.answer }}</div>
      </el-card>
    </template>

    <el-empty v-else description="记录不存在或加载失败" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDebugQueryLog } from '@/api/debug'
import TokenUsagePanel from '@/components/TokenUsagePanel.vue'
import type { DebugQueryResult, DebugRetrievedChunk } from '@/types/debug'
import { filterReasonLabel } from '@/utils/contextFilter'
import { formatOptionalScore, hasHybridObservability, hybridSourceLabel } from '@/utils/hybridDebug'
import {
  hasRerankObservability,
  rerankChangeKind,
  rerankChangeLabel,
  rerankChangeTagType,
  sortChunksForDisplay,
  summarizeRerankChanges,
} from '@/utils/rerankDebug'

const route = useRoute()
const router = useRouter()
const queryLogId = Number(route.params.queryLogId)
const loading = ref(true)
const detail = ref<DebugQueryResult | null>(null)

const hasHybridDetailFields = computed(() => {
  if (detail.value?.searchMode !== 'HYBRID') return false
  return (detail.value.retrievedChunks ?? []).some((c) =>
    hasHybridObservability('HYBRID', c)
  )
})

const showRerankColumns = computed(() =>
  detail.value
    ? hasRerankObservability(detail.value.enableRerank, detail.value.retrievedChunks)
    : false
)

const displayRetrievedChunks = computed(() => {
  if (!detail.value) return []
  return sortChunksForDisplay(detail.value.retrievedChunks, detail.value.enableRerank)
})

const rerankSummaryText = computed(() => {
  if (!detail.value || !showRerankColumns.value) return ''
  return summarizeRerankChanges(detail.value.retrievedChunks)
})

onMounted(async () => {
  if (!queryLogId || Number.isNaN(queryLogId)) {
    loading.value = false
    return
  }
  try {
    const res = await getDebugQueryLog(queryLogId)
    if (res.code === 200) {
      detail.value = res.data
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch {
    ElMessage.error('加载详情失败')
  } finally {
    loading.value = false
  }
})

function goBack() {
  router.push('/debug')
}

function formatScore(score: number) {
  return score.toFixed(4)
}

function searchModeTagType(mode?: string) {
  if (mode === 'BM25') return 'warning'
  if (mode === 'HYBRID') return 'success'
  return 'primary'
}

function scoreColumnLabel(mode?: string) {
  if (mode === 'BM25') return 'BM25 分'
  if (mode === 'HYBRID') return 'Hybrid 分'
  return '相似度'
}

function chunkCollapseTitle(chunk: DebugRetrievedChunk): string {
  const rank = chunk.rerankRank ?? chunk.rankPosition
  if (showRerankColumns.value && chunk.originalRank != null) {
    return `#${rank}（原 #${chunk.originalRank}，${rerankChangeLabel(chunk)}） ${chunk.documentName} · Chunk #${chunk.chunkIndex}`
  }
  return `#${rank} ${chunk.documentName} · Chunk #${chunk.chunkIndex}`
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text ?? '')
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}
</script>

<style scoped>
.debug-detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton {
  margin-top: 16px;
}

.panel {
  width: 100%;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.text-block {
  white-space: pre-wrap;
  line-height: 1.6;
}

.chunk-collapse {
  margin-top: 12px;
}

.filter-reason {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.chunk-content {
  white-space: pre-wrap;
  font-size: 14px;
  line-height: 1.5;
}

.mono-area :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
}

.rerank-summary {
  margin-bottom: 12px;
}
</style>
