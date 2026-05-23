<template>
  <div class="rag-experiment-page">
    <el-card shadow="never" class="panel">
      <template #header>
        <div class="card-header">
          <span>参数实验台（V8）</span>
          <el-tag type="info" size="small">Context 参数仅对本次实验生效</el-tag>
        </div>
      </template>

      <el-form label-width="120px" class="experiment-form">
        <el-form-item label="知识库">
          <el-select
            v-model="selectedKbId"
            placeholder="请选择知识库"
            style="width: 100%"
          >
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="检索模式">
          <el-radio-group v-model="searchMode">
            <el-radio-button value="VECTOR">Vector</el-radio-button>
            <el-radio-button value="BM25">BM25</el-radio-button>
            <el-radio-button value="HYBRID">Hybrid</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="重排">
          <el-switch v-model="enableRerank" active-text="启用" inactive-text="关闭" />
        </el-form-item>

        <el-form-item label="快捷问题">
          <el-space wrap>
            <el-button
              v-for="item in quickQuestions"
              :key="item.label"
              size="small"
              @click="applyQuickQuestion(item.question)"
            >
              {{ item.label }}
            </el-button>
          </el-space>
        </el-form-item>

        <el-form-item label="问题">
          <el-input
            v-model="question"
            type="textarea"
            :rows="2"
            placeholder="输入实验问题"
          />
        </el-form-item>

        <el-divider content-position="left">实验参数</el-divider>

        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :lg="6">
            <el-form-item label="topK">
              <el-input-number v-model="topK" :min="1" :max="20" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="6">
            <el-form-item label="maxChunks">
              <el-input-number v-model="maxChunks" :min="1" :max="10" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="6">
            <el-form-item label="minScore">
              <el-input-number
                v-model="minScore"
                :min="0"
                :max="1"
                :step="0.05"
                :precision="2"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="6">
            <el-form-item label="maxScoreGap">
              <el-input-number
                v-model="maxScoreGap"
                :min="0"
                :max="1"
                :step="0.05"
                :precision="2"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-button
            type="primary"
            :loading="running"
            :disabled="!selectedKbId || !question.trim()"
            @click="handleRunExperiment"
          >
            运行实验
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-alert
      v-if="runError"
      type="error"
      :closable="false"
      show-icon
      :title="runError"
      class="result-alert"
    />

    <template v-if="result">
      <el-card shadow="never" class="panel">
        <template #header>参数影响概览</template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="topK">{{ result.usedParams.topK }}</el-descriptions-item>
          <el-descriptions-item label="maxChunks">{{ result.usedParams.maxChunks }}</el-descriptions-item>
          <el-descriptions-item label="minScore">{{ result.usedParams.minScore }}</el-descriptions-item>
          <el-descriptions-item label="maxScoreGap">{{ result.usedParams.maxScoreGap }}</el-descriptions-item>
          <el-descriptions-item label="检索模式">{{ result.usedParams.searchMode }}</el-descriptions-item>
          <el-descriptions-item label="重排">
            {{ result.usedParams.enableRerank ? '已启用' : '未启用' }}
          </el-descriptions-item>
          <el-descriptions-item label="召回 Chunk 数">
            {{ result.impact.retrievedCount }}
          </el-descriptions-item>
          <el-descriptions-item label="进入 Prompt">
            {{ result.impact.contextCount }}
          </el-descriptions-item>
          <el-descriptions-item label="被过滤">
            {{ result.impact.filteredCount }}
          </el-descriptions-item>
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
          <div class="card-header">
            <span>Answer</span>
            <el-button
              v-if="result.queryLogId"
              type="primary"
              link
              @click="goToDetail(result.queryLogId)"
            >
              查看 Debug 详情
            </el-button>
          </div>
        </template>
        <p class="answer-text">{{ result.answer }}</p>
      </el-card>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-card shadow="never" class="panel">
            <template #header>召回 Chunks（{{ result.retrievedChunks.length }}）</template>
            <el-table :data="result.retrievedChunks" stripe size="small" max-height="360">
              <el-table-column prop="rankPosition" label="#" width="50" />
              <el-table-column prop="documentName" label="文档" min-width="120" show-overflow-tooltip />
              <el-table-column prop="score" label="分数" width="80">
                <template #default="{ row }">{{ formatScore(row.score) }}</template>
              </el-table-column>
              <el-table-column label="Prompt" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.usedInPrompt ? 'success' : 'info'" size="small">
                    {{ row.usedInPrompt ? '是' : '否' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never" class="panel">
            <template #header>Context Chunks（{{ result.contextChunks.length }}）</template>
            <el-table :data="result.contextChunks" stripe size="small" max-height="360">
              <el-table-column prop="rankPosition" label="#" width="50" />
              <el-table-column prop="documentName" label="文档" min-width="120" show-overflow-tooltip />
              <el-table-column prop="score" label="分数" width="80">
                <template #default="{ row }">{{ formatScore(row.score) }}</template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="panel">
        <template #header>Prompt</template>
        <pre class="code-block">{{ result.prompt }}</pre>
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listKnowledgeBases, type KnowledgeBase } from '@/api/knowledgeBase'
import { runRagExperiment } from '@/api/experiment'
import type { ExperimentRagQueryResult } from '@/types/experiment'

interface QuickQuestion {
  label: string
  question: string
}

const router = useRouter()
const knowledgeBases = ref<KnowledgeBase[]>([])
const selectedKbId = ref<number | undefined>()
const question = ref('')
const searchMode = ref<'VECTOR' | 'BM25' | 'HYBRID'>('VECTOR')
const enableRerank = ref(false)
const topK = ref(5)
const maxChunks = ref(2)
const minScore = ref(0.45)
const maxScoreGap = ref(0.35)
const running = ref(false)
const runError = ref('')
const result = ref<ExperimentRagQueryResult | null>(null)

const quickQuestions: QuickQuestion[] = [
  { label: 'SMS_429', question: 'SMS_429 是什么意思？' },
  { label: 'send-code', question: 'send-code 接口路径是什么？' },
  { label: '验证码排查', question: '短信验证码发不出去怎么排查？' },
  { label: 'PgVector', question: '这个项目为什么后续会使用 PgVector？' },
]

function applyQuickQuestion(text: string) {
  question.value = text
}

function formatScore(score: number | undefined): string {
  if (score == null) return '-'
  return score.toFixed(4)
}

async function handleRunExperiment() {
  if (!selectedKbId.value || !question.value.trim()) return
  running.value = true
  runError.value = ''
  try {
    const res = await runRagExperiment({
      kbId: selectedKbId.value,
      question: question.value.trim(),
      topK: topK.value,
      searchMode: searchMode.value,
      enableRerank: enableRerank.value,
      maxChunks: maxChunks.value,
      minScore: minScore.value,
      maxScoreGap: maxScoreGap.value,
    })
    if (res.code === 200 && res.data) {
      result.value = res.data
    } else {
      result.value = null
      runError.value = res.message || '实验运行失败'
    }
  } catch (err: unknown) {
    result.value = null
    const message = err instanceof Error ? err.message : '无法连接后端服务'
    runError.value = `请求失败：${message}`
  } finally {
    running.value = false
  }
}

function goToDetail(queryLogId: number) {
  router.push(`/debug/${queryLogId}`)
}

onMounted(async () => {
  const res = await listKnowledgeBases()
  if (res.code === 200) {
    knowledgeBases.value = res.data
    if (res.data.length > 0) {
      selectedKbId.value = res.data[0].id
    }
  }
})
</script>

<style scoped>
.rag-experiment-page {
  max-width: 1200px;
}

.panel {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.experiment-form {
  max-width: 960px;
}

.result-alert {
  margin-bottom: 16px;
}

.answer-text {
  margin: 0;
  line-height: 1.8;
  white-space: pre-wrap;
  color: #303133;
}

.code-block {
  margin: 0;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
