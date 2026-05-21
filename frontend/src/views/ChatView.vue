<template>
  <div class="chat-page">
    <el-card shadow="never" class="panel">
      <template #header>
        <span>知识库问答（V2 Naive RAG）</span>
      </template>

      <el-form label-width="100px">
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
            <el-button
              type="primary"
              :loading="rebuilding"
              @click="handleRebuild"
            >
              重建向量
            </el-button>
          </el-space>
        </el-form-item>

        <el-form-item label="TopK">
          <el-input-number v-model="topK" :min="1" :max="20" />
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
          <el-button
            type="primary"
            :disabled="!selectedKbId || !question.trim()"
            :loading="sending"
            @click="handleSend"
          >
            发送问题
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="lastAnswer" shadow="never" class="panel answer-card">
      <template #header>
        <span>回答</span>
      </template>
      <div class="answer-text">{{ lastAnswer }}</div>
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
import type { EmbeddingStatus } from '@/types/embedding'
import type { ChatSource } from '@/types/chat'

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

onMounted(async () => {
  const res = await listKnowledgeBases()
  if (res.code === 200) {
    knowledgeBases.value = res.data ?? []
  }
})

async function loadEmbeddingStatus() {
  if (!selectedKbId.value) return
  const res = await getEmbeddingStatus(selectedKbId.value)
  if (res.code === 200) {
    embeddingStatus.value = res.data
  }
}

async function onKbChange() {
  sessionId.value = undefined
  lastAnswer.value = ''
  lastSources.value = []
  await loadEmbeddingStatus()
}

async function handleRebuild() {
  if (!selectedKbId.value) return
  rebuilding.value = true
  try {
    const res = await rebuildEmbedding(selectedKbId.value)
    if (res.code === 200) {
      ElMessage.success(`向量重建完成，已向量化 ${res.data?.embeddedChunks ?? 0} 个 Chunk`)
      await loadEmbeddingStatus()
    } else {
      ElMessage.error(res.message || '重建失败')
    }
  } catch {
    ElMessage.error('重建向量请求失败')
  } finally {
    rebuilding.value = false
  }
}

async function handleSend() {
  if (!selectedKbId.value || !question.value.trim()) return
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
      ElMessage.success('回答已生成')
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
