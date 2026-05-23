<template>
  <div class="debug-page">
    <el-card shadow="never" class="panel">
      <template #header>
        <div class="card-header">
          <span>RAG Debug（V6）</span>
          <el-tag type="info" size="small">Vector / BM25 / Hybrid</el-tag>
        </div>
      </template>

      <el-descriptions v-if="modelProviders" :column="2" border size="small" class="provider-block">
        <el-descriptions-item label="Embedding Provider">
          {{ modelProviders.embeddingProvider }}
        </el-descriptions-item>
        <el-descriptions-item label="Embedding Model">
          {{ modelProviders.embeddingModel }}
        </el-descriptions-item>
        <el-descriptions-item label="Chat Provider">
          {{ modelProviders.chatProvider }}
        </el-descriptions-item>
        <el-descriptions-item label="Chat Model">
          {{ modelProviders.chatModel }}
        </el-descriptions-item>
      </el-descriptions>

      <el-form label-width="100px" class="debug-form">
        <el-form-item label="知识库">
          <el-select
            v-model="selectedKbId"
            placeholder="请选择知识库"
            style="width: 100%"
            @change="onKbChange"
          >
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="selectedKbId" label="向量化状态">
          <el-space wrap>
            <el-tag type="info">总 Chunk：{{ embeddingStatus?.totalChunks ?? 0 }}</el-tag>
            <el-tag type="success">已向量化：{{ embeddingStatus?.embeddedChunks ?? 0 }}</el-tag>
            <el-tag type="warning">未向量化：{{ embeddingStatus?.notEmbeddedChunks ?? 0 }}</el-tag>
          </el-space>
        </el-form-item>

        <el-alert
          v-if="needsVectorEmbedding && selectedKbId && (embeddingStatus?.notEmbeddedChunks ?? 0) > 0"
          type="warning"
          :closable="false"
          show-icon
          title="Vector / Hybrid 模式：存在未向量化的 Chunk，请先前往问答页重建向量。"
          class="embed-alert"
        />

        <el-form-item label="检索模式">
          <el-radio-group v-model="searchMode">
            <el-radio-button value="VECTOR">Vector</el-radio-button>
            <el-radio-button value="BM25">BM25</el-radio-button>
            <el-radio-button value="HYBRID">Hybrid</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="searchMode === 'BM25' || searchMode === 'HYBRID'" label="ES 索引">
          <el-space wrap>
            <el-button size="small" :loading="rebuildingIndex" @click="handleRebuildEsIndex">
              重建 ES 索引
            </el-button>
            <span v-if="esIndexInfo" class="es-hint">
              已同步 {{ esIndexInfo.syncedCount }} 条 → {{ esIndexInfo.indexName }}
            </span>
          </el-space>
        </el-form-item>

        <el-alert
          v-if="searchMode === 'BM25'"
          type="info"
          :closable="false"
          show-icon
          title="BM25 模式不依赖向量；请先重建 ES 索引后再查询。"
          class="embed-alert"
        />

        <el-alert
          v-if="searchMode === 'HYBRID'"
          type="info"
          :closable="false"
          show-icon
          title="Hybrid 模式同时使用向量与 BM25，请确保已完成向量重建并重建 ES 索引。"
          class="embed-alert"
        />

        <el-form-item label="TopK">
          <el-input-number v-model="topK" :min="1" :max="20" />
        </el-form-item>

        <el-form-item label="重排">
          <el-switch v-model="enableRerank" active-text="启用重排" inactive-text="关闭" />
        </el-form-item>

        <el-form-item label="V4 验收">
          <el-space wrap>
            <el-button
              v-for="item in quickTests"
              :key="item.label"
              size="small"
              @click="applyQuickTest(item)"
            >
              {{ item.label }}
            </el-button>
          </el-space>
        </el-form-item>

        <el-form-item label="问题">
          <el-input
            v-model="question"
            type="textarea"
            :rows="3"
            placeholder="请输入 Debug 问题"
          />
        </el-form-item>

        <el-form-item>
          <el-space wrap>
            <el-button
              type="primary"
              :disabled="!selectedKbId || !question.trim()"
              :loading="querying"
              @click="handleDebugQuery"
            >
              执行 Debug 查询
            </el-button>
            <el-button :disabled="!selectedKbId" @click="openHistoryDrawer">
              查询历史
            </el-button>
          </el-space>
        </el-form-item>
      </el-form>
    </el-card>

    <template v-if="result">
      <el-card shadow="never" class="panel">
        <template #header>
          <span>原始问题</span>
        </template>
        <div class="text-block">{{ result.question }}</div>
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header>
          <span>检索与耗时</span>
        </template>
        <el-descriptions :column="2" border size="small" class="mode-block">
          <el-descriptions-item label="检索模式">
            <el-tag :type="searchModeTagType(result.searchMode)" size="small">
              {{ result.searchMode ?? 'VECTOR' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Reranker">
            <el-tag :type="result.enableRerank ? 'success' : 'info'" size="small">
              {{ result.enableRerank ? '已启用' : '未启用' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <el-descriptions :column="3" border size="small" class="latency-block">
          <el-descriptions-item label="检索耗时">
            {{ result.latency.retrievalTimeMs }} ms
          </el-descriptions-item>
          <el-descriptions-item label="生成耗时">
            {{ result.latency.generationTimeMs }} ms
          </el-descriptions-item>
          <el-descriptions-item label="总耗时">
            {{ result.latency.totalTimeMs }} ms
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header>
          <span>召回 Chunk（{{ result.retrievedChunks.length }}，进入 Prompt {{ result.contextChunks.length }}）</span>
        </template>
        <el-alert
          v-if="showRerankColumns"
          type="success"
          :closable="false"
          show-icon
          class="rerank-summary"
          :title="`重排摘要：${rerankSummaryText}`"
          description="表格按重排后排名（rerankRank）展示；Context / Prompt 已使用重排后顺序。"
        />
        <el-table :data="displayRetrievedChunks" stripe style="width: 100%" empty-text="无召回结果">
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
          <el-table-column
            v-if="showRerankColumns"
            label="重排分"
            width="90"
          >
            <template #default="{ row }">{{ formatOptionalScore(row.rerankScore) }}</template>
          </el-table-column>
          <el-table-column prop="documentName" label="文档" min-width="160" show-overflow-tooltip />
          <el-table-column prop="chunkIndex" label="Chunk" width="70" />
          <el-table-column
            v-if="result.searchMode === 'HYBRID'"
            label="来源"
            width="120"
          >
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ hybridSourceLabel(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="result.searchMode === 'HYBRID'"
            label="Vector 分"
            width="90"
          >
            <template #default="{ row }">{{ formatOptionalScore(row.vectorScore) }}</template>
          </el-table-column>
          <el-table-column
            v-if="result.searchMode === 'HYBRID'"
            label="BM25 分"
            width="90"
          >
            <template #default="{ row }">{{ formatOptionalScore(row.bm25Score) }}</template>
          </el-table-column>
          <el-table-column
            v-if="result.searchMode === 'HYBRID'"
            label="Hybrid 分"
            width="95"
          >
            <template #default="{ row }">{{ formatOptionalScore(row.hybridScore ?? row.score) }}</template>
          </el-table-column>
          <el-table-column
            v-else
            :label="scoreColumnLabel(result.searchMode)"
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
            <span>注入 Prompt 的 Context（{{ result.contextChunks.length }} 个片段）</span>
            <el-button size="small" @click="copyText(result.context)">复制</el-button>
          </div>
        </template>
        <p v-if="showRerankColumns" class="context-order-hint">
          以下片段顺序与重排后进入 Context 的顺序一致。
        </p>
        <el-table
          v-if="showRerankColumns && result.contextChunks.length"
          :data="result.contextChunks"
          stripe
          size="small"
          class="context-chunk-table"
        >
          <el-table-column prop="rankPosition" label="重排#" width="70" />
          <el-table-column prop="documentName" label="文档" min-width="140" show-overflow-tooltip />
          <el-table-column prop="chunkIndex" label="Chunk" width="70" />
          <el-table-column label="重排分" width="90">
            <template #default="{ row }">
              {{ formatOptionalScore(findRetrievedChunk(row.chunkId)?.rerankScore) }}
            </template>
          </el-table-column>
        </el-table>
        <el-input
          v-model="result.context"
          type="textarea"
          :rows="8"
          readonly
          class="mono-area"
        />
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header>
          <div class="section-header">
            <span>完整 Prompt</span>
            <el-button size="small" @click="copyText(result.prompt)">复制</el-button>
          </div>
        </template>
        <el-input
          v-model="result.prompt"
          type="textarea"
          :rows="12"
          readonly
          class="mono-area"
        />
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header>
          <span>模型回答</span>
        </template>
        <div class="text-block answer-text">{{ result.answer }}</div>
      </el-card>
    </template>

    <el-drawer
      v-model="historyDrawerVisible"
      title="Debug 查询历史"
      direction="rtl"
      size="520px"
      :destroy-on-close="false"
    >
      <div class="history-drawer">
        <div class="history-toolbar">
          <span class="history-total">共 {{ history.length }} 条</span>
          <el-button size="small" :loading="loadingHistory" @click="loadHistory">刷新</el-button>
        </div>
        <el-table
          :data="paginatedHistory"
          stripe
          style="width: 100%"
          v-loading="loadingHistory"
          empty-text="暂无记录"
        >
          <el-table-column prop="queryLogId" label="ID" width="70" />
          <el-table-column prop="question" label="问题" min-width="160" show-overflow-tooltip />
          <el-table-column prop="totalTimeMs" label="总耗时(ms)" width="110" />
          <el-table-column prop="createdAt" label="时间" width="170" />
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="goDetail(row.queryLogId)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-if="history.length > 0"
          v-model:current-page="historyPage"
          v-model:page-size="historyPageSize"
          class="history-pagination"
          :total="history.length"
          :page-sizes="[5, 10, 20]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="onHistoryPageSizeChange"
        />
        <el-empty v-else-if="!loadingHistory" description="暂无查询历史" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listKnowledgeBases, type KnowledgeBase } from '@/api/knowledgeBase'
import { getEmbeddingStatus } from '@/api/embedding'
import { getModelProviders, type ModelProviders } from '@/api/model'
import { executeDebugQuery, listDebugQueryLogs } from '@/api/debug'
import { rebuildSearchIndex, type EsIndexRebuildResult } from '@/api/search'
import type { EmbeddingStatus } from '@/types/embedding'
import type {
  DebugQueryLogSummary,
  DebugQueryResult,
  DebugRetrievedChunk,
  DebugSearchMode,
} from '@/types/debug'
import { filterReasonLabel } from '@/utils/contextFilter'
import { formatOptionalScore, hybridSourceLabel } from '@/utils/hybridDebug'
import {
  hasRerankObservability,
  rerankChangeKind,
  rerankChangeLabel,
  rerankChangeTagType,
  sortChunksForDisplay,
  summarizeRerankChanges,
} from '@/utils/rerankDebug'

interface QuickTest {
  label: string
  question: string
}

const router = useRouter()
const modelProviders = ref<ModelProviders | null>(null)
const knowledgeBases = ref<KnowledgeBase[]>([])
const selectedKbId = ref<number | undefined>()
const embeddingStatus = ref<EmbeddingStatus | null>(null)
const question = ref('')
const topK = ref(5)
const searchMode = ref<DebugSearchMode>('VECTOR')
const enableRerank = ref(false)
const rebuildingIndex = ref(false)
const esIndexInfo = ref<EsIndexRebuildResult | null>(null)
const querying = ref(false)
const result = ref<DebugQueryResult | null>(null)
const history = ref<DebugQueryLogSummary[]>([])
const loadingHistory = ref(false)
const historyDrawerVisible = ref(false)
const historyPage = ref(1)
const historyPageSize = ref(10)

const paginatedHistory = computed(() => {
  const start = (historyPage.value - 1) * historyPageSize.value
  return history.value.slice(start, start + historyPageSize.value)
})

const showRerankColumns = computed(() =>
  result.value ? hasRerankObservability(result.value.enableRerank, result.value.retrievedChunks) : false
)

const displayRetrievedChunks = computed(() => {
  if (!result.value) return []
  return sortChunksForDisplay(result.value.retrievedChunks, result.value.enableRerank)
})

const rerankSummaryText = computed(() => {
  if (!result.value || !showRerankColumns.value) return ''
  return summarizeRerankChanges(result.value.retrievedChunks)
})

const needsVectorEmbedding = computed(
  () => searchMode.value === 'VECTOR' || searchMode.value === 'HYBRID'
)

const quickTests: QuickTest[] = [
  { label: 'SMS_429', question: 'SMS_429 是什么意思？' },
  { label: 'send-code', question: 'send-code 接口路径是什么？' },
  { label: 'send-code路径', question: '/api/sms/send-code 是什么接口？' },
  { label: '验证码排查', question: '短信验证码发不出去怎么排查？' },
  { label: 'PgVector', question: '这个项目为什么后续会使用 PgVector？' },
]

onMounted(async () => {
  await loadModelProviders()
  const res = await listKnowledgeBases()
  if (res.code === 200 && res.data?.length) {
    knowledgeBases.value = res.data
    selectedKbId.value = res.data[0].id
    await loadEmbeddingStatus()
  }
})

watch(selectedKbId, () => {
  history.value = []
  historyPage.value = 1
})

async function loadModelProviders() {
  try {
    const res = await getModelProviders()
    if (res.code === 200) {
      modelProviders.value = res.data
    }
  } catch {
    // 非阻塞
  }
}

async function loadEmbeddingStatus() {
  if (!selectedKbId.value) return
  const res = await getEmbeddingStatus(selectedKbId.value)
  if (res.code === 200) {
    embeddingStatus.value = res.data
  }
}

async function onKbChange() {
  result.value = null
  history.value = []
  historyPage.value = 1
  await loadEmbeddingStatus()
}

async function loadHistory() {
  loadingHistory.value = true
  try {
    const res = await listDebugQueryLogs(selectedKbId.value)
    if (res.code === 200) {
      history.value = res.data ?? []
      const maxPage = Math.max(1, Math.ceil(history.value.length / historyPageSize.value))
      if (historyPage.value > maxPage) {
        historyPage.value = maxPage
      }
    }
  } catch {
    ElMessage.error('加载历史失败')
  } finally {
    loadingHistory.value = false
  }
}

function applyQuickTest(item: QuickTest) {
  question.value = item.question
}

async function handleRebuildEsIndex() {
  rebuildingIndex.value = true
  try {
    const res = await rebuildSearchIndex()
    if (res.code === 200 && res.data) {
      esIndexInfo.value = res.data
      ElMessage.success(`ES 索引重建完成，同步 ${res.data.syncedCount} 条`)
    } else {
      ElMessage.error(res.message || 'ES 索引重建失败')
    }
  } catch {
    ElMessage.error('ES 索引重建请求失败，请确认 Elasticsearch 已启动')
  } finally {
    rebuildingIndex.value = false
  }
}

async function handleDebugQuery() {
  if (!selectedKbId.value || !question.value.trim()) return
  if (
    needsVectorEmbedding.value &&
    (embeddingStatus.value?.notEmbeddedChunks ?? 0) > 0
  ) {
    ElMessage.warning('请先完成向量重建后再执行 Debug 查询')
    return
  }
  querying.value = true
  try {
    const res = await executeDebugQuery({
      kbId: selectedKbId.value,
      question: question.value.trim(),
      topK: topK.value,
      searchMode: searchMode.value,
      enableRerank: enableRerank.value,
    })
    if (res.code === 200 && res.data) {
      result.value = res.data
      ElMessage.success('Debug 查询完成')
      await loadHistory()
    } else {
      ElMessage.error(res.message || 'Debug 查询失败')
    }
  } catch {
    ElMessage.error('Debug 查询请求失败')
  } finally {
    querying.value = false
  }
}

async function openHistoryDrawer() {
  historyDrawerVisible.value = true
  historyPage.value = 1
  await loadHistory()
}

function onHistoryPageSizeChange() {
  historyPage.value = 1
}

function goDetail(queryLogId: number) {
  historyDrawerVisible.value = false
  router.push(`/debug/${queryLogId}`)
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

function findRetrievedChunk(chunkId: number): DebugRetrievedChunk | undefined {
  return result.value?.retrievedChunks.find((c) => c.chunkId === chunkId)
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
.debug-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel {
  width: 100%;
}

.card-header,
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.provider-block {
  margin-bottom: 16px;
}

.debug-form {
  margin-top: 8px;
}

.embed-alert {
  margin-bottom: 12px;
}

.text-block {
  white-space: pre-wrap;
  line-height: 1.6;
}

.answer-text {
  font-size: 15px;
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

.es-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.mode-block {
  margin-bottom: 12px;
}

.latency-block {
  margin-top: 0;
}

.history-drawer {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 200px;
}

.history-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.history-total {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.history-pagination {
  margin-top: 8px;
  justify-content: flex-end;
}

.rerank-summary {
  margin-bottom: 12px;
}

.context-order-hint {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.context-chunk-table {
  margin-bottom: 12px;
}
</style>
