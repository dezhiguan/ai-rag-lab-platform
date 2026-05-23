<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="logo">AI RAG Lab Platform</div>
      <el-menu
        mode="horizontal"
        :default-active="activeMenu"
        router
        class="menu"
      >
        <el-menu-item index="/dashboard">Dashboard</el-menu-item>
        <el-menu-item index="/system-status">系统状态</el-menu-item>
        <el-menu-item index="/rag-metrics">RAG 指标</el-menu-item>
        <el-menu-item index="/rag-query-logs">查询日志中心</el-menu-item>
        <el-menu-item index="/slow-query-analysis">慢查询分析</el-menu-item>
        <el-menu-item index="/kb">知识库</el-menu-item>
        <el-menu-item index="/chat">问答</el-menu-item>
        <el-menu-item index="/evaluation">评测中心</el-menu-item>
        <el-menu-item index="/debug">Debug</el-menu-item>
        <el-menu-item index="/about">About</el-menu-item>
      </el-menu>
    </el-header>
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => {
  if (route.path.startsWith('/debug')) return '/debug'
  if (route.path.startsWith('/evaluation')) return '/evaluation'
  return route.path
})
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

.header {
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--el-border-color-light);
  background: #fff;
  padding: 0 24px;
}

.logo {
  font-size: 18px;
  font-weight: 600;
  margin-right: 32px;
  white-space: nowrap;
}

.menu {
  flex: 1;
  border-bottom: none;
}

.main {
  background: #f5f7fa;
  padding: 24px;
}
</style>
