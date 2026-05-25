<template>
  <div class="rag-page project-overview-page">
    <div class="rag-page-header">
      <h1>AI RAG Lab Platform</h1>
      <p>企业级 RAG 知识库实验平台 — 文档导入、多模式检索、评测与工程化可观测</p>
    </div>

    <el-tabs v-model="activeTab" class="overview-tabs">
      <el-tab-pane label="项目概览" name="overview">
        <el-card shadow="never" class="rag-section-card hero-compact">
          <p class="hero-positioning">
            可用于个人学习、RAG 能力实验、知识库问答实践与开源参考。从文档导入到多模式检索、重排、评测与可观测，形成完整 RAG 实验闭环。
          </p>
        </el-card>

        <el-row :gutter="16" class="overview-top-row">
          <el-col :xs="24" :md="12">
            <el-card shadow="never" class="rag-section-card">
              <template #header><span>当前访问身份</span></template>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="用户">{{ authStore.username }}</el-descriptions-item>
                <el-descriptions-item label="角色">{{ authStore.roleLabel }}</el-descriptions-item>
                <el-descriptions-item label="模式">
                  <el-tag :type="authStore.isGuest ? 'warning' : 'success'" size="small">
                    {{ authStore.modeLabel }}
                  </el-tag>
                </el-descriptions-item>
              </el-descriptions>
              <el-alert
                v-if="authStore.isGuest"
                type="warning"
                :closable="false"
                show-icon
                class="guest-alert"
                title="只读体验模式：可浏览 Debug、评测、参数实验台与可观测看板；不可创建知识库、上传文档或重建索引。"
              />
            </el-card>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-card shadow="never" class="rag-section-card">
              <template #header><span>当前部署状态</span></template>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="部署">V9 双服务器已联调</el-descriptions-item>
                <el-descriptions-item label="访问">公网 IP（备案前）</el-descriptions-item>
                <el-descriptions-item label="内网">应用层 ↔ 数据层已验证</el-descriptions-item>
              </el-descriptions>
              <el-button type="primary" link class="deploy-link" @click="activeTab = 'deploy'">
                查看部署详情 →
              </el-button>
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="never" class="rag-section-card">
          <template #header><span>核心能力入口</span></template>
          <div class="rag-grid rag-grid-3">
            <el-card
              v-for="entry in featureEntries"
              :key="entry.path"
              shadow="hover"
              class="rag-entry-card"
              @click="goTo(entry.path)"
            >
              <div class="entry-title">{{ entry.title }}</div>
              <p class="rag-muted entry-desc">{{ entry.desc }}</p>
              <el-tag size="small" type="info">{{ entry.tag }}</el-tag>
            </el-card>
          </div>
        </el-card>

        <el-card shadow="never" class="rag-section-card">
          <template #header><span>推荐使用路径</span></template>
          <el-steps :active="recommendedSteps.length" finish-status="success" align-center>
            <el-step
              v-for="step in recommendedSteps"
              :key="step.title"
              :title="step.title"
              :description="step.desc"
            />
          </el-steps>
          <div class="step-links">
            <el-button
              v-for="step in recommendedSteps"
              :key="`${step.title}-btn`"
              type="primary"
              link
              @click="goTo(step.path)"
            >
              {{ step.title }} →
            </el-button>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="能力路线" name="roadmap">
        <el-card shadow="never" class="rag-section-card">
          <template #header>
            <el-space>
              <span>版本能力路线</span>
              <el-tag type="success" size="small">V0～V10.5</el-tag>
            </el-space>
          </template>
          <div class="rag-table-scroll">
            <el-table :data="versionRoadmap" stripe size="small">
              <el-table-column prop="version" label="版本" width="88" />
              <el-table-column prop="title" label="名称" width="140" />
              <el-table-column prop="desc" label="核心能力" min-width="320" show-overflow-tooltip />
              <el-table-column label="状态" width="88">
                <template #default="{ row }">
                  <el-tag :type="row.statusType || 'success'" size="small">
                    {{ row.status || '已完成' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="部署状态" name="deploy">
        <el-card shadow="never" class="rag-section-card">
          <template #header>
            <el-space>
              <span>双服务器部署架构</span>
              <el-tag type="warning" size="small">V9</el-tag>
            </el-space>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="应用入口层">
              轻量服务器 2C4G — Nginx、前端静态资源、RAG Java 后端
            </el-descriptions-item>
            <el-descriptions-item label="数据检索层">
              ECS 4C8G — PostgreSQL + PgVector、Elasticsearch、Redis（仅内网）
            </el-descriptions-item>
            <el-descriptions-item label="内网通信">
              <el-tag type="success" size="small">已验证</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="当前访问方式">
              公网 IP（页面不展示真实地址；备案前通过 Nginx 统一入口）
            </el-descriptions-item>
          </el-descriptions>

          <el-descriptions
            title="线上状态"
            :column="1"
            border
            class="online-status-block"
          >
            <el-descriptions-item
              v-for="row in onlineDeploymentStatus"
              :key="row.label"
              :label="row.label"
            >
              <el-tag :type="row.type" size="small">{{ row.value }}</el-tag>
              <span v-if="row.note" class="rag-muted status-note">{{ row.note }}</span>
            </el-descriptions-item>
          </el-descriptions>

          <div class="rag-table-scroll" style="margin-top: 16px">
            <el-table :data="deploymentReadiness" stripe size="small">
              <el-table-column prop="item" label="准备项" width="180" show-overflow-tooltip />
              <el-table-column prop="desc" label="说明" min-width="220" show-overflow-tooltip />
              <el-table-column prop="path" label="文档/路径" min-width="200" show-overflow-tooltip>
                <template #default="{ row }">
                  <code class="deploy-path">{{ row.path }}</code>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="row.statusType || 'success'" size="small">
                    {{ row.status || '已准备' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="快速开始" name="quickstart">
        <el-card shadow="never" class="rag-section-card">
          <template #header><span>快速开始</span></template>
          <el-row :gutter="16">
            <el-col
              v-for="item in quickStartFlow"
              :key="item.title"
              :xs="24"
              :sm="12"
              :lg="8"
            >
              <el-card shadow="hover" class="quick-card">
                <div class="quick-title">{{ item.title }}</div>
                <p class="rag-muted">{{ item.desc }}</p>
                <el-space wrap>
                  <template v-if="item.action === 'initSample'">
                    <el-button
                      v-if="canWrite"
                      type="primary"
                      size="small"
                      :loading="initSampleLoading"
                      @click="handleInitSample"
                    >
                      一键初始化
                    </el-button>
                    <el-tooltip v-else content="当前为体验账号，不支持该操作" placement="top">
                      <el-button type="primary" size="small" disabled>一键初始化</el-button>
                    </el-tooltip>
                  </template>
                  <el-button
                    v-if="item.path"
                    type="primary"
                    size="small"
                    plain
                    @click="goTo(item.path)"
                  >
                    {{ item.linkLabel || '前往' }}
                  </el-button>
                </el-space>
              </el-card>
            </el-col>
          </el-row>
          <el-divider />
          <p class="rag-muted section-hint">本地开发环境</p>
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12">
              <el-card shadow="never" class="dev-hint-card">
                <div class="quick-title">启动后端</div>
                <pre class="quick-command">cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev</pre>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-card shadow="never" class="dev-hint-card">
                <div class="quick-title">启动前端</div>
                <pre class="quick-command">cd frontend && npm install && npm run dev</pre>
              </el-card>
            </el-col>
          </el-row>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="适用场景" name="scenes">
        <el-card shadow="never" class="rag-section-card">
          <template #header><span>适用场景</span></template>
          <div class="rag-grid rag-grid-3">
            <el-card
              v-for="scene in useCases"
              :key="scene.title"
              shadow="hover"
              class="scene-card"
            >
              <div class="scene-title">{{ scene.title }}</div>
              <p class="rag-muted">{{ scene.desc }}</p>
            </el-card>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { initSampleData } from '@/api/sample'
import { useAuthStore } from '@/stores/auth'
import { usePermission } from '@/composables/usePermission'

const router = useRouter()
const authStore = useAuthStore()
const { canWrite, requireWrite } = usePermission()
const initSampleLoading = ref(false)
const activeTab = ref('overview')

interface RoadmapRow {
  version: string
  title: string
  desc: string
  status?: string
  statusType?: 'success' | 'warning' | 'info'
}

const versionRoadmap: RoadmapRow[] = [
  { version: 'V0', title: '项目骨架', desc: 'Spring Boot + Vue 3 骨架、健康检查、Dashboard' },
  { version: 'V1', title: '文档导入与分块', desc: '知识库、Markdown/TXT 上传、固定分块' },
  { version: 'V2', title: 'Naive RAG', desc: 'PgVector 向量检索 + Chat 问答闭环' },
  { version: 'V2.5', title: '真实模型接入', desc: 'Qwen Embedding、DeepSeek Chat Provider' },
  { version: 'V3', title: 'Debug 可观察', desc: '召回 / Context / Prompt / 耗时全链路可观测' },
  { version: 'V4', title: 'BM25', desc: 'Elasticsearch 关键词检索' },
  { version: 'V5', title: 'Hybrid Search', desc: 'Vector + BM25 应用层融合' },
  { version: 'V6', title: 'Reranker', desc: '轻量本地重排，可观察排名变化' },
  { version: 'V7', title: 'Evaluation', desc: '评测中心：Top1 命中、多模式对比' },
  { version: 'V8', title: '工程化增强', desc: '系统状态、指标、日志、慢查询、参数实验台' },
  { version: 'V9', title: '云部署', desc: '双服务器生产部署与在线访问环境' },
  { version: 'V10', title: '访问保护', desc: '登录鉴权、admin/guest、只读体验模式' },
  { version: 'V10.5', title: 'UI/UX 优化', desc: '侧边栏导航、总览 Tab 化、宽表横向滚动', status: '当前', statusType: 'warning' },
]

const featureEntries = [
  { title: 'Debug 控制台', desc: 'Vector / BM25 / Hybrid + Reranker 全链路', path: '/debug', tag: 'V3+' },
  { title: 'Evaluation 评测', desc: '单模式与三模式 Top1 命中率对比', path: '/evaluation', tag: 'V7' },
  { title: '参数实验台', desc: 'Context 参数多组对比', path: '/rag-experiment', tag: 'V8' },
  { title: 'RAG 指标', desc: '查询次数、耗时、模式分布', path: '/rag-metrics', tag: 'V8' },
  { title: '查询日志', desc: '历史查询分页与详情', path: '/rag-query-logs', tag: 'V8' },
  { title: '慢查询分析', desc: '慢查询原因与优化建议', path: '/slow-query-analysis', tag: 'V8' },
]

const recommendedSteps = [
  { title: '登录系统', desc: '使用 admin 或 guest 账号', path: '/login' },
  { title: '查看总览', desc: '了解能力与部署状态', path: '/project-overview' },
  { title: 'Debug 查询', desc: 'Hybrid + Reranker 全链路', path: '/debug' },
  { title: 'Evaluation', desc: '多模式 Top1 命中率', path: '/evaluation' },
  { title: '参数实验台', desc: '调参并多组对比', path: '/rag-experiment' },
]

const quickStartFlow = [
  { title: '登录系统', desc: '访问平台并选择账号', path: '/login', linkLabel: '登录页' },
  { title: '查看项目总览', desc: '了解能力与推荐路径', path: '/project-overview', linkLabel: '本页' },
  { title: 'Debug 查询', desc: 'Vector / BM25 / Hybrid 全链路', path: '/debug' },
  { title: 'Evaluation 评测', desc: '内置用例批量验收', path: '/evaluation' },
  { title: '参数实验台', desc: '调整 Context 参数对比', path: '/rag-experiment' },
  { title: '日志与指标', desc: 'RAG 指标、查询日志、慢查询', path: '/rag-metrics', linkLabel: '指标' },
  {
    title: '初始化样例',
    desc: '导入内置 Markdown 到样例知识库',
    action: 'initSample' as const,
    path: '/kb',
    linkLabel: '知识库',
  },
]

const useCases = [
  { title: 'RAG 学习实验', desc: '按版本理解文档分块、检索、生成与可观察链路' },
  { title: '企业知识库问答原型', desc: '快速搭建知识库问答与引用来源展示' },
  { title: '检索策略对比', desc: 'Vector、BM25、Hybrid 与 Reranker 效果横向比较' },
  { title: 'RAG 效果评测', desc: '内置用例批量验收 Top1 文档命中率' },
  { title: '参数调优与问题排查', desc: '实验台调参、日志中心与慢查询分析定位瓶颈' },
  { title: 'AI 求职 Agent 底座', desc: '作为后续 AI 求职 Agent 的知识服务底座' },
]

const onlineDeploymentStatus = [
  { label: '线上部署', value: '已联调', type: 'success' as const, note: 'Runbook + 验收脚本 V9-06' },
  { label: '数据检索层', value: '已部署', type: 'success' as const, note: 'ECS：PG / ES / Redis' },
  { label: '应用入口层', value: '已部署', type: 'success' as const, note: 'Nginx + 前端 + Java 后端' },
  { label: '内网访问', value: '已验证', type: 'success' as const },
  { label: '当前访问方式', value: '公网 IP', type: 'info' as const, note: '备案前通过公网地址访问' },
]

const deploymentReadiness = [
  { item: '双服务器拓扑', desc: '应用入口层 + 数据检索层，内网互联', path: 'docs/09-production-deployment.md', status: '已配置' },
  { item: '数据层 Compose', desc: 'PG + PgVector、ES、Redis', path: 'deploy/data-layer/docker-compose.data.yml' },
  { item: '应用层部署', desc: '后端 jar、前端 dist、Nginx', path: 'deploy/app-layer/README.md' },
  { item: '联调 Runbook', desc: '部署步骤、检查清单', path: 'docs/11-dual-server-online-runbook.md' },
  { item: '验收脚本', desc: 'verify-online-deployment.sh', path: 'scripts/verify-online-deployment.sh' },
]

function goTo(path: string) {
  router.push(path)
}

async function handleInitSample() {
  if (!requireWrite()) return
  initSampleLoading.value = true
  try {
    const res = await initSampleData()
    if (res.code === 200) {
      ElMessage.success(res.data?.message || '样例数据初始化成功')
    } else {
      ElMessage.error(res.message || '初始化失败')
    }
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : '请求失败'
    ElMessage.error(message)
  } finally {
    initSampleLoading.value = false
  }
}
</script>

<style scoped>
.project-overview-page {
  max-width: 1200px;
}

.overview-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.hero-compact {
  background: linear-gradient(135deg, var(--rag-primary-soft) 0%, var(--rag-card-bg) 70%);
}

.hero-positioning {
  margin: 0;
  line-height: 1.7;
  color: var(--rag-text-secondary);
}

.overview-top-row {
  margin-bottom: 0;
}

.guest-alert {
  margin-top: 12px;
}

.deploy-link {
  margin-top: 12px;
}

.entry-title,
.quick-title,
.scene-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--rag-text-main);
  margin-bottom: 6px;
}

.entry-desc {
  margin: 0 0 10px;
  min-height: 36px;
}

.step-links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  justify-content: center;
  margin-top: 16px;
}

.quick-card {
  margin-bottom: 12px;
  min-height: 120px;
}

.quick-command {
  margin: 8px 0 0;
  padding: 8px 10px;
  background: var(--rag-bg);
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.4;
  white-space: pre-wrap;
  word-break: break-all;
}

.dev-hint-card {
  background: var(--rag-bg);
  border: 1px dashed var(--rag-border);
}

.online-status-block {
  margin-top: 16px;
}

.status-note {
  margin-left: 8px;
}

.deploy-path {
  font-size: 12px;
  color: var(--rag-primary);
}

.section-hint {
  margin: 0 0 12px;
}
</style>
