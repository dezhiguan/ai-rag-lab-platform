<template>
  <el-card shadow="never">
    <template #header>
      <span>关于</span>
    </template>
    <p>
      AI RAG Lab Platform 是企业级 RAG 知识库实验平台。
    </p>
    <p>
      当前为 <strong>V2 Naive RAG 问答版</strong>，在 V1 基础上支持 Chunk 向量化、PgVector 检索、Chat 问答与引用来源展示。
    </p>
    <p>
      版本规划请查看 <code>docs/ai-context.md</code> 与 <code>docs/current-version.md</code>。
    </p>
  </el-card>
</template>

<style scoped>
p {
  line-height: 1.8;
  margin: 0 0 12px;
  color: #606266;
}
</style>
