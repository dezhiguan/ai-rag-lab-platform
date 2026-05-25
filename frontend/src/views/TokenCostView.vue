<template>
  <div class="rag-page token-cost-page">
    <div class="rag-page-header">
      <h1>Token 成本</h1>
      <p class="rag-muted">Embedding（qwen/text-embedding-v4）+ Chat 分项统计，总成本 = Embedding + Chat</p>
    </div>

    <el-row v-loading="loading" :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="rag-section-card">
          <template #header><span>能力接入</span></template>
          <el-tag v-if="overview?.tokenTrackingEnabled" type="success" size="small">已接入</el-tag>
          <ul class="cap-list">
            <li v-for="item in overview?.capabilities ?? []" :key="item">{{ item }}</li>
          </ul>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="rag-section-card">
          <template #header><span>估算规则</span></template>
          <p class="rag-muted rule-line">Embedding Token ≈ 用户问题文本估算；费用 = tokens / 1000 × ¥0.0007</p>
          <p class="rag-muted rule-line">Chat：输入 + 输出按 deepseek/mock 单价；Total = Embedding + Chat</p>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="rag-section-card">
          <template #header>
            <span>qwen / text-embedding-v4 · 最近 7 天</span>
          </template>
          <div v-if="overview?.embeddingRecent7Days" class="rag-grid rag-grid-2">
            <div class="rag-kpi-card">
              <div class="label">查询次数</div>
              <div class="value">{{ overview.embeddingRecent7Days.queryCount }}</div>
            </div>
            <div class="rag-kpi-card">
              <div class="label">Embedding Token</div>
              <div class="value">{{ overview.embeddingRecent7Days.embeddingTokens }}</div>
            </div>
            <div class="rag-kpi-card">
              <div class="label">Embedding 费用</div>
              <div class="value">¥{{ formatMoney(overview.embeddingRecent7Days.embeddingCost) }}</div>
            </div>
            <div class="rag-kpi-card">
              <div class="label">平均 Emb Token</div>
              <div class="value">{{ overview.embeddingRecent7Days.avgEmbeddingTokens }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="rag-section-card">
          <template #header>
            <span>qwen / text-embedding-v4 · 全部</span>
          </template>
          <div v-if="overview?.embeddingAllTime" class="rag-grid rag-grid-2">
            <div class="rag-kpi-card">
              <div class="label">查询次数</div>
              <div class="value">{{ overview.embeddingAllTime.queryCount }}</div>
            </div>
            <div class="rag-kpi-card">
              <div class="label">Embedding Token</div>
              <div class="value">{{ overview.embeddingAllTime.embeddingTokens }}</div>
            </div>
            <div class="rag-kpi-card">
              <div class="label">Embedding 费用</div>
              <div class="value">¥{{ formatMoney(overview.embeddingAllTime.embeddingCost) }}</div>
            </div>
            <div class="rag-kpi-card">
              <div class="label">平均 Emb Token</div>
              <div class="value">{{ overview.embeddingAllTime.avgEmbeddingTokens }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="rag-section-card">
          <template #header><span>全链路 · 最近 7 天</span></template>
          <div v-if="overview?.recent7Days" class="rag-grid rag-grid-2">
            <div class="rag-kpi-card">
              <div class="label">查询次数</div>
              <div class="value">{{ overview.recent7Days.queryCount }}</div>
            </div>
            <div class="rag-kpi-card">
              <div class="label">总 Token</div>
              <div class="value">{{ overview.recent7Days.totalTokens }}</div>
            </div>
            <div class="rag-kpi-card">
              <div class="label">总费用</div>
              <div class="value">¥{{ formatMoney(overview.recent7Days.totalCost) }}</div>
            </div>
            <div class="rag-kpi-card">
              <div class="label">平均费用 / 次</div>
              <div class="value">¥{{ formatMoney(overview.recent7Days.avgCost) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="rag-section-card">
          <template #header><span>全链路 · 全部记录</span></template>
          <div v-if="overview?.allTime" class="rag-grid rag-grid-2">
            <div class="rag-kpi-card">
              <div class="label">查询次数</div>
              <div class="value">{{ overview.allTime.queryCount }}</div>
            </div>
            <div class="rag-kpi-card">
              <div class="label">总 Token</div>
              <div class="value">{{ overview.allTime.totalTokens }}</div>
            </div>
            <div class="rag-kpi-card">
              <div class="label">总费用</div>
              <div class="value">¥{{ formatMoney(overview.allTime.totalCost) }}</div>
            </div>
            <div class="rag-kpi-card">
              <div class="label">平均费用 / 次</div>
              <div class="value">¥{{ formatMoney(overview.allTime.avgCost) }}</div>
            </div>
          </div>
          <p
            v-if="overview && overview.allTime.queryCount === 0"
            class="rag-muted empty-hint"
          >
            暂无带 Token 字段的查询日志，请在 Debug 或参数实验台执行一次查询。
          </p>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="rag-section-card">
      <template #header><span>模型单价参考（静态配置）</span></template>
      <div class="rag-table-scroll">
        <el-table :data="overview?.modelPrices ?? []" stripe size="small">
          <el-table-column prop="model" label="模型" width="200" />
          <el-table-column prop="inputPriceNote" label="输入 / Embedding" min-width="200" />
          <el-table-column prop="outputPriceNote" label="输出" min-width="160" />
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getTokenCostOverview } from '@/api/tokenCost'
import type { TokenCostOverview } from '@/types/token'

const loading = ref(false)
const overview = ref<TokenCostOverview | null>(null)

function formatMoney(value: number): string {
  return Number(value ?? 0).toFixed(6)
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getTokenCostOverview()
    if (res.code === 200 && res.data) {
      overview.value = res.data
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.cap-list {
  margin: 12px 0 0;
  padding-left: 18px;
  color: var(--rag-text-secondary);
  font-size: 14px;
  line-height: 1.8;
}

.rule-line {
  margin: 0 0 8px;
}

.empty-hint {
  margin-top: 12px;
}
</style>
