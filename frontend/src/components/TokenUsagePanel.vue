<template>
  <el-card v-if="usage" shadow="never" class="rag-section-card token-usage-panel">
    <template #header>
      <el-space>
        <span>Token 用量与预估费用</span>
        <el-tag v-if="usage.chatModel" type="info" size="small">{{ usage.chatModel }}</el-tag>
      </el-space>
    </template>
    <div class="rag-grid rag-grid-3 token-kpi-grid">
      <div class="rag-kpi-card">
        <div class="label">用户问题</div>
        <div class="value">{{ usage.questionTokens }}</div>
      </div>
      <div class="rag-kpi-card">
        <div class="label">上下文</div>
        <div class="value">{{ usage.contextTokens }}</div>
      </div>
      <div class="rag-kpi-card">
        <div class="label">系统 Prompt</div>
        <div class="value">{{ usage.systemPromptTokens }}</div>
      </div>
      <div class="rag-kpi-card">
        <div class="label">回答</div>
        <div class="value">{{ usage.answerTokens }}</div>
      </div>
      <div class="rag-kpi-card">
        <div class="label">输入 Token</div>
        <div class="value">{{ usage.inputTokens }}</div>
      </div>
      <div class="rag-kpi-card">
        <div class="label">输出 Token</div>
        <div class="value">{{ usage.outputTokens }}</div>
      </div>
      <div class="rag-kpi-card highlight">
        <div class="label">总 Token</div>
        <div class="value">{{ usage.totalTokens }}</div>
      </div>
      <div class="rag-kpi-card highlight">
        <div class="label">预估费用</div>
        <div class="value cost-value">{{ formatCost(usage) }}</div>
        <div v-if="usage.costNote" class="rag-muted cost-note">{{ usage.costNote }}</div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { TokenUsage } from '@/types/token'

defineProps<{
  usage: TokenUsage | null | undefined
}>()

function formatCost(usage: TokenUsage): string {
  if (!usage.priceConfigured) {
    return '未配置价格'
  }
  const cost = Number(usage.estimatedCost ?? 0)
  if (cost === 0) {
    return '¥0.000000'
  }
  return `¥${cost.toFixed(6)}`
}
</script>

<style scoped>
.token-usage-panel {
  margin-bottom: 16px;
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

.cost-note {
  margin-top: 4px;
  font-size: 12px;
}
</style>
