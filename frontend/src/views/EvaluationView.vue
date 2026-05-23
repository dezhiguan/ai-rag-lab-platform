<template>
  <div class="evaluation-page">
    <el-card shadow="never" class="panel">
      <template #header>
        <div class="card-header">
          <span>评测中心（V7）</span>
          <el-tag type="info" size="small">Top1 文档命中验收</el-tag>
        </div>
      </template>

      <el-form label-width="100px" class="eval-form">
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
          title="Vector / Hybrid 模式需先完成向量重建。"
          class="eval-alert"
        />

        <el-form-item label="检索模式">
          <el-radio-group v-model="searchMode">
            <el-radio-button value="VECTOR">Vector</el-radio-button>
            <el-radio-button value="BM25">BM25</el-radio-button>
            <el-radio-button value="HYBRID">Hybrid</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-alert
          v-if="searchMode === 'BM25' || searchMode === 'HYBRID'"
          type="info"
          :closable="false"
          show-icon
          title="BM25 / Hybrid 请先确保 ES 索引已重建（可在 Debug 页操作）。"
          class="eval-alert"
        />

        <el-form-item label="测试用例">
          <el-table :data="testCases" stripe size="small" style="width: 100%" empty-text="加载中…">
            <el-table-column prop="question" label="问题" min-width="240" show-overflow-tooltip />
            <el-table-column prop="expectedDocument" label="期望 Top1 文档" width="180" />
          </el-table>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :disabled="!selectedKbId || !testCases.length"
            :loading="running"
            @click="handleRunEvaluation"
          >
            执行评测
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="runResult" shadow="never" class="panel">
      <template #header>
        <span>整体统计</span>
      </template>
      <el-row :gutter="16" class="stats-row">
        <el-col :xs="12" :sm="6">
          <el-statistic title="总用例数" :value="runResult.totalCount" />
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-statistic title="通过数">
            <template #default>
              <span class="stat-pass">{{ runResult.passedCount }}</span>
            </template>
          </el-statistic>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-statistic title="失败数">
            <template #default>
              <span class="stat-fail">{{ runResult.failedCount }}</span>
            </template>
          </el-statistic>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-statistic title="通过率" :value="passRatePercent" suffix="%" />
        </el-col>
      </el-row>
      <p class="stats-meta">
        检索模式：{{ runResult.searchMode }} · 总耗时 {{ runResult.totalLatencyMs }} ms
      </p>
    </el-card>

    <el-card v-if="runResult" shadow="never" class="panel">
      <template #header>
        <span>评测结果</span>
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
import { listEvaluationCases, runEvaluation } from '@/api/evaluation'
import type { EmbeddingStatus } from '@/types/embedding'
import type {
  EvaluationRunResult,
  EvaluationSearchMode,
  EvaluationTestCase,
} from '@/types/evaluation'

const knowledgeBases = ref<KnowledgeBase[]>([])
const selectedKbId = ref<number | undefined>()
const embeddingStatus = ref<EmbeddingStatus | null>(null)
const searchMode = ref<EvaluationSearchMode>('VECTOR')
const testCases = ref<EvaluationTestCase[]>([])
const running = ref(false)
const runResult = ref<EvaluationRunResult | null>(null)

const needsVectorEmbedding = computed(
  () => searchMode.value === 'VECTOR' || searchMode.value === 'HYBRID'
)

const passRatePercent = computed(() => {
  if (!runResult.value) return 0
  return Math.round((runResult.value.passRate ?? 0) * 1000) / 10
})

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
  await loadEmbeddingStatus()
}

async function handleRunEvaluation() {
  if (!selectedKbId.value) return
  if (
    needsVectorEmbedding.value &&
    (embeddingStatus.value?.notEmbeddedChunks ?? 0) > 0
  ) {
    ElMessage.warning('请先完成向量重建后再执行评测')
    return
  }
  running.value = true
  runResult.value = null
  try {
    const res = await runEvaluation({
      kbId: selectedKbId.value,
      searchMode: searchMode.value,
      topK: 5,
    })
    if (res.code === 200 && res.data) {
      runResult.value = res.data
      const rate = Math.round((res.data.passRate ?? 0) * 100)
      ElMessage.success(`评测完成：${res.data.passedCount}/${res.data.totalCount} 通过（${rate}%）`)
    } else {
      ElMessage.error(res.message || '评测失败')
    }
  } catch {
    ElMessage.error('评测请求失败')
  } finally {
    running.value = false
  }
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
  gap: 12px;
  flex-wrap: wrap;
}

.eval-form {
  margin-top: 8px;
}

.eval-alert {
  margin-bottom: 12px;
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
  font-size: 20px;
  font-weight: 600;
}

.stat-fail {
  color: var(--el-color-danger);
  font-size: 20px;
  font-weight: 600;
}
</style>
