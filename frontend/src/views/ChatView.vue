<template>
  <div class="chat-page">
    <el-card shadow="never" class="panel">
      <template #header>
        <div class="card-header">
          <span>知识库问答（V2.5）</span>
        </div>
      </template>

      <el-descriptions v-if="modelProviders" :column="2" border size="small" class="model-status">
        <el-descriptions-item label="Embedding Provider">
          {{ modelProviders.embeddingProvider }}
        </el-descriptions-item>
        <el-descriptions-item label="Embedding Model">
          {{ modelProviders.embeddingModel }}
        </el-descriptions-item>
        <el-descriptions-item label="Embedding Dimension">
          {{ modelProviders.embeddingDimension }}
        </el-descriptions-item>
        <el-descriptions-item label="Embedding 实现">
          {{ modelProviders.embeddingDelegate }}
        </el-descriptions-item>
        <el-descriptions-item label="Chat Provider">
          {{ modelProviders.chatProvider }}
        </el-descriptions-item>
        <el-descriptions-item label="Chat Model">
          {{ modelProviders.chatModel }}
        </el-descriptions-item>
      </el-descriptions>

      <el-form label-width="100px" class="chat-form">
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
            <el-button type="primary" :loading="rebuilding" @click="handleRebuild">
              重建向量
            </el-button>
          </el-space>
          <div class="rebuild-hint">
            切换 Embedding Provider 或模型后，需要重新重建向量。
          </div>
        </el-form-item>

        <el-alert
          v-if="selectedKbId && (embeddingStatus?.notEmbeddedChunks ?? 0) > 0"
          type="warning"
          :closable="false"
          show-icon
          title="存在未向量化的 Chunk，请先点击「重建向量」再提问。"
          class="embed-alert"
        />

        <el-form-item label="TopK">
          <el-input-number v-model="topK" :min="1" :max="20" />
        </el-form-item>

        <el-form-item label="V2 验收">
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
            placeholder="请输入您的问题"
            @keydown.enter.exact.prevent="handleSend"
          />
        </el-form-item>

        <el-form-item>
          <el-space>
            <el-button
              type="primary"
              :disabled="!selectedKbId || !question.trim()"
              :loading="sending"
              @click="handleSend"
            >
              发送问题
            </el-button>
            <el-button :disabled="sending" @click="handleNewSession">新会话</el-button>
          </el-space>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="lastAnswer" shadow="never" class="panel answer-card">
      <template #header>
        <div class="answer-header">
          <span>回答</span>
          <el-tag v-if="sessionId" size="small" type="info">会话 #{{ sessionId }}</el-tag>
        </div>
      </template>
      <div class="answer-text">{{ lastAnswer }}</div>
      <el-alert
        v-if="lastAnswer && !lastSources.length"
        type="info"
        :closable="false"
        show-icon
        class="no-source-alert"
        title="本次回答未附带引用来源（可能为知识库中无相关依据）。"
      />
    </el-card>

    <el-card v-if="lastSources.length" shadow="never" class="panel">
      <template #header>
        <span>引用来源（{{ lastSources.length }}）</span>
      </template>
      <div v-for="(source, index) in lastSources" :key="source.chunkId" class="source-item">
        <div class="source-meta">
          <el-tag size="small">#{{ index + 1 }}</el-tag>
          <span class="doc-name">{{ source.documentName }}</span>
          <span>Chunk #{{ source.chunkIndex }}</span>
          <el-tag size="small" type="success">相似度 {{ formatScore(source.score) }}</el-tag>
        </div>
        <div class="source-content">{{ source.content }}</div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listKnowledgeBases, type KnowledgeBase } from '@/api/knowledgeBase'
import { getEmbeddingStatus, rebuildEmbedding } from '@/api/embedding'
import { sendChat } from '@/api/chat'
import { getModelProviders, type ModelProviders } from '@/api/model'
import type { EmbeddingStatus } from '@/types/embedding'
import type { ChatSource } from '@/types/chat'

interface QuickTest {
  label: string
  question: string
  expect?: string
}

const modelProviders = ref<ModelProviders | null>(null)
const knowledgeBases = ref<KnowledgeBase[]>([])
const selectedKbId = ref<number | undefined>()
const embeddingStatus = ref<EmbeddingStatus | null>(null)
const rebuilding = ref(false)
const sending = ref(false)
const question = ref('')
const topK = ref(5)
const sessionId = ref<number | undefined>()
const lastAnswer = ref('')
const lastSources = ref<ChatSource[]>([])

const quickTests: QuickTest[] = [
  { label: '验证码排查', question: '短信验证码发不出去怎么排查？', expect: '03-troubleshooting.md' },
  { label: 'SMS_429', question: 'SMS_429 是什么意思？', expect: '02-api-spec.md' },
  { label: 'send-code', question: 'send-code 接口路径是什么？', expect: '02-api-spec.md' },
  { label: 'PgVector', question: '这个项目为什么后续会使用 PgVector？', expect: '01-project-guideline.md' },
  { label: '无关问题', question: '公司年终奖发几个月？', expect: '无相关依据' },
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
  handleNewSession()
  await loadEmbeddingStatus()
}

function handleNewSession() {
  sessionId.value = undefined
  lastAnswer.value = ''
  lastSources.value = []
}

async function handleRebuild() {
  if (!selectedKbId.value) return
  rebuilding.value = true
  try {
    const res = await rebuildEmbedding(selectedKbId.value)
    if (res.code === 200) {
      ElMessage.success(`向量重建完成，已向量化 ${res.data?.embeddedChunks ?? 0} 个 Chunk`)
      await loadEmbeddingStatus()
      handleNewSession()
    } else {
      ElMessage.error(res.message || '重建失败')
    }
  } catch {
    ElMessage.error('重建向量请求失败')
  } finally {
    rebuilding.value = false
  }
}

function applyQuickTest(item: QuickTest) {
  question.value = item.question
  if (item.expect) {
    ElMessage.info(`预期：${item.expect}`)
  }
}

async function handleSend() {
  if (!selectedKbId.value || !question.value.trim()) return
  if ((embeddingStatus.value?.notEmbeddedChunks ?? 0) > 0) {
    ElMessage.warning('请先重建向量后再提问')
    return
  }
  sending.value = true
  try {
    const res = await sendChat({
      kbId: selectedKbId.value,
      sessionId: sessionId.value,
      question: question.value.trim(),
      topK: topK.value,
    })
    if (res.code === 200 && res.data) {
      sessionId.value = res.data.sessionId
      lastAnswer.value = res.data.answer
      lastSources.value = res.data.sources ?? []
      const topDoc = lastSources.value[0]?.documentName
      if (topDoc) {
        ElMessage.success(`回答已生成，Top 引用：${topDoc}`)
      } else {
        ElMessage.success('回答已生成')
      }
    } else {
      ElMessage.error(res.message || '问答失败')
    }
  } catch {
    ElMessage.error('问答请求失败')
  } finally {
    sending.value = false
  }
}

function formatScore(score: number) {
  return score.toFixed(4)
}
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel {
  width: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.model-status {
  margin-bottom: 16px;
}

.chat-form {
  margin-top: 8px;
}

.rebuild-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.embed-alert,
.no-source-alert {
  margin-top: 12px;
}

.answer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.answer-text {
  white-space: pre-wrap;
  line-height: 1.6;
}

.source-item {
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.source-item:last-child {
  border-bottom: none;
}

.source-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.doc-name {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.source-content {
  white-space: pre-wrap;
  font-size: 14px;
  line-height: 1.5;
  color: var(--el-text-color-regular);
}
</style>
