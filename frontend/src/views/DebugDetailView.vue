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
        <template #header><span>Provider 信息</span></template>
        <el-descriptions :column="2" border size="small">
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

      <el-card shadow="never" class="panel">
        <template #header>
          <span>召回 Chunk（{{ detail.retrievedChunks.length }}）</span>
        </template>
        <div
          v-for="chunk in detail.retrievedChunks"
          :key="chunk.chunkId"
          class="chunk-item"
        >
          <div class="chunk-meta">
            <el-tag size="small">#{{ chunk.rankPosition }}</el-tag>
            <span class="doc-name">{{ chunk.documentName }}</span>
            <span>Chunk #{{ chunk.chunkIndex }}</span>
            <el-tag size="small" type="success">相似度 {{ formatScore(chunk.score) }}</el-tag>
          </div>
          <div class="chunk-content">{{ chunk.content }}</div>
        </div>
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header>
          <div class="section-header">
            <span>Context</span>
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
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDebugQueryLog } from '@/api/debug'
import type { DebugQueryResult } from '@/types/debug'

const route = useRoute()
const router = useRouter()
const queryLogId = Number(route.params.queryLogId)
const loading = ref(true)
const detail = ref<DebugQueryResult | null>(null)

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

.chunk-item {
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.chunk-item:last-child {
  border-bottom: none;
}

.chunk-meta {
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
