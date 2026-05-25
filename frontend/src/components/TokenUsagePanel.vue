<template>
  <el-card v-if="usage" shadow="never" class="rag-section-card token-usage-panel">
    <template #header>
      <el-space wrap>
        <span>Token 用量与预估费用</span>
        <el-tag v-if="usage.embeddingModel" type="warning" size="small">
          {{ usage.embeddingProvider }}/{{ usage.embeddingModel }}
        </el-tag>
        <el-tag v-if="usage.chatModel" type="info" size="small">{{ usage.chatModel }}</el-tag>
      </el-space>
    </template>

    <div class="token-section">
      <div class="section-title">Embedding</div>
      <div class="rag-grid rag-grid-3 token-kpi-grid">
        <div class="rag-kpi-card">
          <div class="label">Provider / Model</div>
          <div class="value value-sm">{{ embeddingLabel }}</div>
        </div>
        <div class="rag-kpi-card">
          <div class="label">Embedding Token</div>
          <div class="value">{{ embeddingTokens }}</div>
        </div>
        <div class="rag-kpi-card">
          <div class="label">Embedding 费用</div>
          <div class="value cost-value">¥{{ formatMoney(embeddingCost) }}</div>
        </div>
      </div>
    </div>

    <div class="token-section">
      <div class="section-title">Chat</div>
      <div class="rag-grid rag-grid-3 token-kpi-grid">
        <div class="rag-kpi-card">
          <div class="label">输入 Token</div>
          <div class="value">{{ chatInputTokens }}</div>
        </div>
        <div class="rag-kpi-card">
          <div class="label">输出 Token</div>
          <div class="value">{{ chatOutputTokens }}</div>
        </div>
        <div class="rag-kpi-card">
          <div class="label">Chat 费用</div>
          <div class="value cost-value">¥{{ formatMoney(chatCost) }}</div>
        </div>
      </div>
      <details class="chat-detail rag-muted">
        <summary>Chat 明细（问题 / 上下文 / 系统 / 回答）</summary>
        <span>问题 {{ usage.questionTokens }} · 上下文 {{ usage.contextTokens }} · 系统 {{ usage.systemPromptTokens }} · 回答 {{ usage.answerTokens }}</span>
      </details>
    </div>

    <div class="token-section total-section">
      <div class="section-title">Total</div>
      <div class="rag-grid rag-grid-2 token-kpi-grid">
        <div class="rag-kpi-card highlight">
          <div class="label">总 Token</div>
          <div class="value">{{ usage.totalTokens ?? 0 }}</div>
        </div>
        <div class="rag-kpi-card highlight">
          <div class="label">总费用</div>
          <div class="value cost-value">{{ formatTotalCost(usage) }}</div>
          <div v-if="usage.costNote" class="rag-muted cost-note">{{ usage.costNote }}</div>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TokenUsage } from '@/types/token'

const props = defineProps<{
  usage: TokenUsage | null | undefined
}>()

const embeddingTokens = computed(() => props.usage?.embeddingTokens ?? 0)
const embeddingCost = computed(() => props.usage?.embeddingCost ?? 0)
const chatInputTokens = computed(() => props.usage?.chatInputTokens ?? props.usage?.inputTokens ?? 0)
const chatOutputTokens = computed(() => props.usage?.chatOutputTokens ?? props.usage?.outputTokens ?? 0)
const chatCost = computed(() => props.usage?.chatCost ?? props.usage?.estimatedCost ?? 0)

const embeddingLabel = computed(() => {
  const u = props.usage
  if (!u?.embeddingProvider && !u?.embeddingModel) return '—'
  return `${u.embeddingProvider ?? '—'} / ${u.embeddingModel ?? '—'}`
})

function formatMoney(value: number): string {
  return Number(value ?? 0).toFixed(6)
}

function formatTotalCost(usage: TokenUsage): string {
  if (usage.priceConfigured === false && !usage.totalCost && !usage.estimatedCost) {
    return '未配置价格'
  }
  const cost = Number(usage.totalCost ?? usage.estimatedCost ?? 0)
  return `¥${cost.toFixed(6)}`
}
</script>

<style scoped>
.token-usage-panel {
  margin-bottom: 16px;
}

.token-section {
  margin-bottom: 16px;
}

.token-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--rag-text-main);
  margin-bottom: 10px;
}

.total-section {
  padding-top: 4px;
  border-top: 1px dashed var(--rag-border);
}

.token-kpi-grid {
  margin-top: 0;
}

.token-kpi-card.highlight {
  border-color: var(--rag-primary);
  background: var(--rag-primary-soft);
}

.cost-value {
  font-size: 18px;
}

.value-sm {
  font-size: 14px;
  line-height: 1.4;
  word-break: break-all;
}

.cost-note {
  margin-top: 4px;
  font-size: 12px;
}

.chat-detail {
  margin-top: 8px;
  font-size: 12px;
}

.chat-detail summary {
  cursor: pointer;
  margin-bottom: 4px;
}
</style>
