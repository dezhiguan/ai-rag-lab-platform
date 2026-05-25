<template>
  <div class="rag-page token-cost-page">
    <div class="rag-page-header">
      <h1>Token 成本</h1>
      <p class="rag-muted">基于工程估算的 Token 统计与静态模型单价预估（元）</p>
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
          <p class="rag-muted rule-line">中文约 1 字 ≈ 1 token；英文/数字/符号约 4 字符 ≈ 1 token</p>
          <p class="rag-muted rule-line">输入 = 问题 + 上下文 + 系统 Prompt；输出 = 回答</p>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="rag-section-card">
          <template #header><span>最近 7 天</span></template>
          <div v-if="overview" class="rag-grid rag-grid-2">
            <div class="rag-kpi-card">
              <div class="label">有 Token 记录的查询</div>
              <div class="value">{{ overview.recent7Days.queryCount }}</div>
            </div>
            <div class="rag-kpi-card">
              <div class="label">总 Token</div>
              <div class="value">{{ overview.recent7Days.totalTokens }}</div>
            </div>
            <div class="rag-kpi-card">
              <div class="label">预估总费用</div>
              <div class="value">¥{{ formatMoney(overview.recent7Days.totalCost) }}</div>
            </div>
            <div class="rag-kpi-card">
              <div class="label">平均 Token / 次</div>
              <div class="value">{{ overview.recent7Days.avgTokens }}</div>
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
          <template #header><span>全部记录</span></template>
          <div v-if="overview" class="rag-grid rag-grid-2">
            <div class="rag-kpi-card">
              <div class="label">有 Token 记录的查询</div>
              <div class="value">{{ overview.allTime.queryCount }}</div>
            </div>
            <div class="rag-kpi-card">
              <div class="label">总 Token</div>
              <div class="value">{{ overview.allTime.totalTokens }}</div>
            </div>
            <div class="rag-kpi-card">
              <div class="label">预估总费用</div>
              <div class="value">¥{{ formatMoney(overview.allTime.totalCost) }}</div>
            </div>
            <div class="rag-kpi-card">
              <div class="label">平均 Token / 次</div>
              <div class="value">{{ overview.allTime.avgTokens }}</div>
            </div>
            <div class="rag-kpi-card">
              <div class="label">平均费用 / 次</div>
              <div class="value">¥{{ formatMoney(overview.allTime.avgCost) }}</div>
            </div>
          </div>
          <p v-if="overview && overview.allTime.queryCount === 0" class="rag-muted empty-hint">
            暂无带 Token 字段的查询日志，请在 Debug 或参数实验台执行一次查询。
          </p>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="rag-section-card">
      <template #header><span>模型单价参考（静态配置）</span></template>
      <div class="rag-table-scroll">
        <el-table :data="overview?.modelPrices ?? []" stripe size="small">
          <el-table-column prop="model" label="模型" width="160" />
          <el-table-column prop="inputPriceNote" label="输入" min-width="200" />
          <el-table-column prop="outputPriceNote" label="输出" min-width="200" />
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
