<template>
  <div class="rag-page evaluation-page">
    <el-card shadow="never" class="panel">
      <template #header>
        <div class="card-header">
          <span>评测中心（V7）</span>
          <el-space wrap>
            <el-tag type="info" size="small">Top1 文档命中验收</el-tag>
            <el-tag v-if="lastEnableRerank" type="success" size="small">Reranker 已启用</el-tag>
            <el-tag v-else-if="hasAnyResult" type="info" size="small">Reranker 未启用</el-tag>
          </el-space>
        </div>
      </template>

      <el-form label-width="120px" class="eval-form">
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
          title="Vector / Hybrid 评测需先完成向量重建；多模式对比包含 Vector 与 Hybrid。"
          class="eval-alert"
        />

        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="BM25 / Hybrid 请先确保 ES 索引已重建（可在 Debug 页操作）。"
          class="eval-alert"
        />

        <el-form-item label="检索模式">
          <el-radio-group v-model="searchMode" :disabled="comparing">
            <el-radio-button value="VECTOR">Vector</el-radio-button>
            <el-radio-button value="BM25">BM25</el-radio-button>
            <el-radio-button value="HYBRID">Hybrid</el-radio-button>
          </el-radio-group>
          <span class="mode-hint">单模式评测时使用</span>
        </el-form-item>

        <el-form-item label="重排">
          <el-switch v-model="enableRerank" active-text="启用重排" inactive-text="关闭" />
          <span class="mode-hint">开启后评测取 Rerank 后 Top1，便于对比 Hybrid / Reranker 收益</span>
        </el-form-item>

        <el-form-item label="测试用例">
          <el-table :data="testCases" stripe size="small" style="width: 100%" empty-text="加载中…">
            <el-table-column prop="question" label="问题" min-width="240" show-overflow-tooltip />
            <el-table-column prop="expectedDocument" label="期望 Top1 文档" width="180" />
          </el-table>
        </el-form-item>

        <el-form-item label="操作">
          <el-space wrap>
            <el-button
              type="primary"
              :disabled="!selectedKbId || !testCases.length"
              :loading="running"
              @click="handleRunEvaluation"
            >
              执行单模式评测
            </el-button>
            <el-button
              type="success"
              :disabled="!selectedKbId || !testCases.length"
              :loading="comparing"
              @click="handleCompareEvaluation"
            >
              执行多模式对比
            </el-button>
          </el-space>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 多模式对比统计 -->
    <el-card v-if="compareResult" shadow="never" class="panel">
      <template #header>
        <span>多模式对比统计</span>
      </template>
      <div class="rag-table-scroll">
      <el-table :data="compareResult.modeSummaries" stripe style="width: 100%">
        <el-table-column prop="searchMode" label="检索模式" width="100" />
        <el-table-column prop="totalCount" label="总用例" width="80" />
        <el-table-column prop="passedCount" label="通过" width="70">
          <template #default="{ row }">
            <span class="stat-pass">{{ row.passedCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="failedCount" label="失败" width="70">
          <template #default="{ row }">
            <span class="stat-fail">{{ row.failedCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="通过率" width="100">
          <template #default="{ row }">{{ formatPassRate(row.passRate) }}</template>
        </el-table-column>
        <el-table-column label="平均耗时(ms)" width="120">
          <template #default="{ row }">{{ row.avgLatencyMs }}</template>
        </el-table-column>
        <el-table-column label="总耗时(ms)" width="110">
          <template #default="{ row }">{{ row.totalLatencyMs }}</template>
        </el-table-column>
      </el-table>
      </div>
      <p class="stats-meta">
        Reranker：{{ compareResult.enableRerank ? '已启用' : '未启用' }} · 对比 VECTOR / BM25 / HYBRID
      </p>
    </el-card>

    <!-- 多模式对比明细 -->
    <el-card v-if="compareResult" shadow="never" class="panel">
      <template #header>
        <span>多模式对比明细</span>
      </template>
      <div class="rag-table-scroll">
      <el-table :data="compareResult.cases" stripe style="width: 100%">
        <el-table-column prop="question" label="问题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="expectedDocument" label="期望文档" width="150" />
        <el-table-column label="VECTOR Top1" min-width="140">
          <template #default="{ row }">{{ hitDoc(row.vector) }}</template>
        </el-table-column>
        <el-table-column label="VECTOR" width="80">
          <template #default="{ row }">
            <el-tag :type="passTagType(row.vector)" size="small">{{ passLabel(row.vector) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="BM25 Top1" min-width="140">
          <template #default="{ row }">{{ hitDoc(row.bm25) }}</template>
        </el-table-column>
        <el-table-column label="BM25" width="80">
          <template #default="{ row }">
            <el-tag :type="passTagType(row.bm25)" size="small">{{ passLabel(row.bm25) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="HYBRID Top1" min-width="140">
          <template #default="{ row }">{{ hitDoc(row.hybrid) }}</template>
        </el-table-column>
        <el-table-column label="HYBRID" width="80">
          <template #default="{ row }">
            <el-tag :type="passTagType(row.hybrid)" size="small">{{ passLabel(row.hybrid) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      </div>
    </el-card>

    <!-- 单模式统计 -->
    <el-card v-if="runResult && !compareResult" shadow="never" class="panel">
      <template #header>
        <span>单模式统计 · {{ runResult.searchMode }}</span>
      </template>
      <div class="rag-grid rag-grid-3 stats-kpi-row">
        <div class="rag-kpi-card">
          <div class="label">总用例数</div>
          <div class="value">{{ runResult.totalCount }}</div>
        </div>
        <div class="rag-kpi-card">
          <div class="label">通过</div>
          <div class="value stat-pass">{{ runResult.passedCount }}</div>
        </div>
        <div class="rag-kpi-card">
          <div class="label">失败</div>
          <div class="value stat-fail">{{ runResult.failedCount }}</div>
        </div>
        <div class="rag-kpi-card">
          <div class="label">通过率</div>
          <div class="value">{{ formatPassRate(runResult.passRate) }}</div>
        </div>
      </div>
      <p class="stats-meta">
        检索模式：{{ runResult.searchMode }} · Reranker：{{ runResult.enableRerank ? '已启用' : '未启用' }}
        · 平均耗时 {{ runResult.avgLatencyMs ?? '—' }} ms · 总耗时 {{ runResult.totalLatencyMs }} ms
      </p>
    </el-card>

    <el-card v-if="runResult && !compareResult" shadow="never" class="panel">
      <template #header>
        <span>单模式评测明细</span>
      </template>
      <el-table :data="runResult.results" stripe style="width: 100%">
        <el-table-column prop="question" label="问题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="expectedDocument" label="期望文档" width="160" />
        <el-table-column prop="actualTop1Document" label="实际 Top1" min-width="160" show-overflow-tooltip />
        <el-table-column label="是否通过" width="100">
          <template #default="{ row }">
            <el-tag :type="row.passed ? 'success' : 'danger'" size="small">
              {{ row.passed ? '通过' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="searchMode" label="检索模式" width="90" />
        <el-table-column label="耗时(ms)" width="100">
          <template #default="{ row }">{{ row.latencyMs }}</template>
        </el-table-column>
        <el-table-column prop="message" label="说明" min-width="160" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listKnowledgeBases, type KnowledgeBase } from '@/api/knowledgeBase'
import { getEmbeddingStatus } from '@/api/embedding'
import { compareEvaluation, listEvaluationCases, runEvaluation } from '@/api/evaluation'
import type { EmbeddingStatus } from '@/types/embedding'
import type {
  EvaluationCompareResult,
  EvaluationModeHit,
  EvaluationRunResult,
  EvaluationSearchMode,
  EvaluationTestCase,
} from '@/types/evaluation'

const knowledgeBases = ref<KnowledgeBase[]>([])
const selectedKbId = ref<number | undefined>()
const embeddingStatus = ref<EmbeddingStatus | null>(null)
const searchMode = ref<EvaluationSearchMode>('VECTOR')
const enableRerank = ref(false)
const testCases = ref<EvaluationTestCase[]>([])
const running = ref(false)
const comparing = ref(false)
const runResult = ref<EvaluationRunResult | null>(null)
const compareResult = ref<EvaluationCompareResult | null>(null)
const lastEnableRerank = ref(false)

const needsVectorEmbedding = computed(
  () => searchMode.value === 'VECTOR' || searchMode.value === 'HYBRID'
)

const hasAnyResult = computed(() => runResult.value != null || compareResult.value != null)

onMounted(async () => {
  await Promise.all([loadKnowledgeBases(), loadTestCases()])
})

async function loadKnowledgeBases() {
  const res = await listKnowledgeBases()
  if (res.code === 200 && res.data?.length) {
    knowledgeBases.value = res.data
    selectedKbId.value = res.data[0].id
    await loadEmbeddingStatus()
  }
}

async function loadTestCases() {
  try {
    const res = await listEvaluationCases()
    if (res.code === 200) {
      testCases.value = res.data ?? []
    }
  } catch {
    ElMessage.error('加载测试用例失败')
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
  runResult.value = null
  compareResult.value = null
  await loadEmbeddingStatus()
}

function validateBeforeRun(requireVector: boolean): boolean {
  if (!selectedKbId.value) return false
  if (requireVector && (embeddingStatus.value?.notEmbeddedChunks ?? 0) > 0) {
    ElMessage.warning('请先完成向量重建后再执行评测')
    return false
  }
  return true
}

async function handleRunEvaluation() {
  if (!validateBeforeRun(needsVectorEmbedding.value)) return
  running.value = true
  runResult.value = null
  compareResult.value = null
  lastEnableRerank.value = enableRerank.value
  try {
    const res = await runEvaluation({
      kbId: selectedKbId.value!,
      searchMode: searchMode.value,
      topK: 5,
      enableRerank: enableRerank.value,
    })
    if (res.code === 200 && res.data) {
      runResult.value = res.data
      const rate = Math.round((res.data.passRate ?? 0) * 100)
      ElMessage.success(
        `${res.data.searchMode} 评测完成：${res.data.passedCount}/${res.data.totalCount} 通过（${rate}%）`
      )
    } else {
      ElMessage.error(res.message || '评测失败')
    }
  } catch {
    ElMessage.error('评测请求失败')
  } finally {
    running.value = false
  }
}

async function handleCompareEvaluation() {
  if (!validateBeforeRun(true)) return
  comparing.value = true
  runResult.value = null
  compareResult.value = null
  lastEnableRerank.value = enableRerank.value
  try {
    const res = await compareEvaluation({
      kbId: selectedKbId.value!,
      topK: 5,
      enableRerank: enableRerank.value,
    })
    if (res.code === 200 && res.data) {
      compareResult.value = res.data
      const hybrid = res.data.modeSummaries.find((m) => m.searchMode === 'HYBRID')
      const vector = res.data.modeSummaries.find((m) => m.searchMode === 'VECTOR')
      const rateH = hybrid ? Math.round((hybrid.passRate ?? 0) * 100) : 0
      const rateV = vector ? Math.round((vector.passRate ?? 0) * 100) : 0
      ElMessage.success(`多模式对比完成：HYBRID ${rateH}% · VECTOR ${rateV}%`)
    } else {
      ElMessage.error(res.message || '对比评测失败')
    }
  } catch {
    ElMessage.error('对比评测请求失败')
  } finally {
    comparing.value = false
  }
}

function formatPassRate(rate: number) {
  return `${Math.round((rate ?? 0) * 1000) / 10}%`
}

function hitDoc(hit?: EvaluationModeHit) {
  return hit?.actualTop1Document?.trim() ? hit.actualTop1Document : '—'
}

function passLabel(hit?: EvaluationModeHit) {
  if (!hit) return '—'
  return hit.passed ? '通过' : '失败'
}

function passTagType(hit?: EvaluationModeHit) {
  if (!hit) return 'info'
  return hit.passed ? 'success' : 'danger'
}
</script>

<style scoped>
.evaluation-page {
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

.eval-form {
  margin-top: 8px;
}

.eval-alert {
  margin-bottom: 12px;
}

.mode-hint {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.stats-row {
  margin-bottom: 8px;
}

.stats-meta {
  margin: 12px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.stat-pass {
  color: var(--el-color-success);
  font-weight: 600;
}

.stat-fail {
  color: var(--el-color-danger);
  font-weight: 600;
}
</style>
