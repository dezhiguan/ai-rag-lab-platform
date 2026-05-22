<template>
  <div class="debug-page">
    <el-card shadow="never" class="panel">
      <template #header>
        <div class="card-header">
          <span>RAG Debug（V3）</span>
          <el-tag type="info" size="small">可观察版</el-tag>
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
          v-if="selectedKbId && (embeddingStatus?.notEmbeddedChunks ?? 0) > 0"
          type="warning"
          :closable="false"
          show-icon
          title="存在未向量化的 Chunk，请先前往问答页重建向量。"
          class="embed-alert"
        />

        <el-form-item label="TopK">
          <el-input-number v-model="topK" :min="1" :max="20" />
        </el-form-item>

        <el-form-item label="V3 验收">
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
          <el-button
            type="primary"
            :disabled="!selectedKbId || !question.trim()"
            :loading="querying"
            @click="handleDebugQuery"
          >
            执行 Debug 查询
          </el-button>
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
          <span>耗时统计</span>
        </template>
        <el-descriptions :column="3" border size="small">
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
        <el-table :data="result.retrievedChunks" stripe style="width: 100%" empty-text="无召回结果">
          <el-table-column prop="rankPosition" label="#" width="50" />
          <el-table-column prop="documentName" label="文档" min-width="160" show-overflow-tooltip />
          <el-table-column prop="chunkIndex" label="Chunk" width="70" />
          <el-table-column label="相似度" width="100">
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
            v-for="chunk in result.retrievedChunks"
            :key="chunk.chunkId"
            :title="`#${chunk.rankPosition} ${chunk.documentName} · Chunk #${chunk.chunkIndex}`"
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

    <el-card shadow="never" class="panel">
      <template #header>
        <div class="card-header">
          <span>Debug 查询历史</span>
          <el-button size="small" :loading="loadingHistory" @click="loadHistory">刷新</el-button>
        </div>
      </template>
      <el-table :data="history" stripe style="width: 100%" empty-text="暂无记录">
        <el-table-column prop="queryLogId" label="ID" width="70" />
        <el-table-column prop="question" label="问题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="totalTimeMs" label="总耗时(ms)" width="110" />
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="goDetail(row.queryLogId)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listKnowledgeBases, type KnowledgeBase } from '@/api/knowledgeBase'
import { getEmbeddingStatus } from '@/api/embedding'
import { getModelProviders, type ModelProviders } from '@/api/model'
import { executeDebugQuery, listDebugQueryLogs } from '@/api/debug'
import type { EmbeddingStatus } from '@/types/embedding'
import type { DebugQueryLogSummary, DebugQueryResult } from '@/types/debug'
import { filterReasonLabel } from '@/utils/contextFilter'

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
const querying = ref(false)
const result = ref<DebugQueryResult | null>(null)
const history = ref<DebugQueryLogSummary[]>([])
const loadingHistory = ref(false)

const quickTests: QuickTest[] = [
  { label: '验证码排查', question: '短信验证码发不出去怎么排查？' },
  { label: 'SMS_429', question: 'SMS_429 是什么意思？' },
  { label: 'send-code', question: 'send-code 接口路径是什么？' },
  { label: 'PgVector', question: '这个项目为什么后续会使用 PgVector？' },
  { label: '无关问题', question: '公司年终奖发几个月？' },
]

onMounted(async () => {
  await loadModelProviders()
  const res = await listKnowledgeBases()
  if (res.code === 200 && res.data?.length) {
    knowledgeBases.value = res.data
    selectedKbId.value = res.data[0].id
    await loadEmbeddingStatus()
    await loadHistory()
  }
})

watch(selectedKbId, async () => {
  await loadHistory()
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
  await loadEmbeddingStatus()
  await loadHistory()
}

async function loadHistory() {
  loadingHistory.value = true
  try {
    const res = await listDebugQueryLogs(selectedKbId.value)
    if (res.code === 200) {
      history.value = res.data ?? []
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

async function handleDebugQuery() {
  if (!selectedKbId.value || !question.value.trim()) return
  if ((embeddingStatus.value?.notEmbeddedChunks ?? 0) > 0) {
    ElMessage.warning('请先完成向量重建后再执行 Debug 查询')
    return
  }
  querying.value = true
  try {
    const res = await executeDebugQuery({
      kbId: selectedKbId.value,
      question: question.value.trim(),
      topK: topK.value,
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

function goDetail(queryLogId: number) {
  router.push(`/debug/${queryLogId}`)
}

function formatScore(score: number) {
  return score.toFixed(4)
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
</style>
