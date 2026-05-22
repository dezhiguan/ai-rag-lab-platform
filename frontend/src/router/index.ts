import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '@/layouts/BasicLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/dashboard',
    },
    {
      path: '/',
      component: BasicLayout,
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/DashboardView.vue'),
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

export default router
