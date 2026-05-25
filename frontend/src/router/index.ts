import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '@/layouts/BasicLayout.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      redirect: '/project-overview',
    },
    {
      path: '/',
      component: BasicLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: 'project-overview',
          name: 'ProjectOverview',
          component: () => import('@/views/ProjectOverviewView.vue'),
        },
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/DashboardView.vue'),
        },
        {
          path: 'system-status',
          name: 'SystemStatus',
          component: () => import('@/views/SystemStatusView.vue'),
        },
        {
          path: 'rag-metrics',
          name: 'RagMetrics',
          component: () => import('@/views/RagMetricsView.vue'),
        },
        {
          path: 'rag-query-logs',
          name: 'RagQueryLogs',
          component: () => import('@/views/RagQueryLogsView.vue'),
        },
        {
          path: 'slow-query-analysis',
          name: 'SlowQueryAnalysis',
          component: () => import('@/views/SlowQueryAnalysisView.vue'),
        },
        {
          path: 'token-cost',
          name: 'TokenCost',
          component: () => import('@/views/TokenCostView.vue'),
        },
        {
          path: 'rag-experiment',
          name: 'RagExperiment',
          component: () => import('@/views/RagExperimentView.vue'),
        },
        {
          path: 'kb',
          name: 'KnowledgeBaseList',
          component: () => import('@/views/KbListView.vue'),
        },
        {
          path: 'kb/:kbId/documents',
          name: 'DocumentList',
          component: () => import('@/views/DocumentListView.vue'),
        },
        {
          path: 'documents/:documentId/chunks',
          name: 'ChunkList',
          component: () => import('@/views/ChunkListView.vue'),
        },
        {
          path: 'chat',
          name: 'Chat',
          component: () => import('@/views/ChatView.vue'),
        },
        {
          path: 'evaluation',
          name: 'Evaluation',
          component: () => import('@/views/EvaluationView.vue'),
        },
        {
          path: 'debug',
          name: 'Debug',
          component: () => import('@/views/DebugView.vue'),
        },
        {
          path: 'debug/:queryLogId',
          name: 'DebugDetail',
          component: () => import('@/views/DebugDetailView.vue'),
        },
        {
          path: 'about',
          name: 'About',
          component: () => import('@/views/AboutView.vue'),
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (!authStore.initialized) {
    await authStore.restoreSession()
  }

  if (to.meta.public) {
    if (authStore.isLoggedIn && to.path === '/login') {
      return '/project-overview'
    }
    return true
  }

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  return true
})

export default router
