<template>
  <div class="page">
    <h2 class="page-title">操作审计</h2>
    <p class="page-desc">轧差执行与义务录入等关键操作的只读审计记录，点击行可展开详情</p>

    <div class="toolbar">
      <el-select v-model="filters.action" clearable placeholder="操作类型" style="width:160px">
        <el-option label="执行轧差" value="NETTING_EXECUTE" />
        <el-option label="新建义务" value="OBLIGATION_CREATE" />
      </el-select>
      <el-input v-model="filters.actor" clearable placeholder="操作人" style="width:160px" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <div class="card-panel">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="audit-detail">
              <div v-if="row.refId" class="audit-ref">
                关联 ID：<span class="mono">{{ row.refId }}</span>
              </div>
              <pre class="mono audit-json">{{ prettyDetail(row.detail) }}</pre>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">{{ actionLabel(row.action) }}</template>
        </el-table-column>
        <el-table-column prop="actor" label="操作人" width="110" />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.outcome === 'SUCCESS' ? 'success' : 'danger'">
              {{ row.outcome === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="摘要" min-width="320" show-overflow-tooltip />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '../api/client'

const rows = ref([])
const loading = ref(false)

const filters = reactive({
  action: '',
  actor: ''
})

const ACTION_LABELS = {
  NETTING_EXECUTE: '执行轧差',
  OBLIGATION_CREATE: '新建义务'
}

function actionLabel(action) {
  return ACTION_LABELS[action] || action
}

function formatTime(v) {
  if (!v) return '-'
  return new Date(v).toLocaleString()
}

function prettyDetail(detail) {
  if (!detail) return '（无详情）'
  try {
    return JSON.stringify(JSON.parse(detail), null, 2)
  } catch {
    return detail
  }
}

async function load() {
  loading.value = true
  try {
    const params = {}
    if (filters.action) params.action = filters.action
    if (filters.actor) params.actor = filters.actor
    const { data } = await api.get('/audit-events', { params })
    rows.value = data
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.audit-detail {
  padding: 8px 16px;
}
.audit-ref {
  margin-bottom: 8px;
  color: var(--muted);
}
.audit-json {
  margin: 0;
  padding: 12px;
  background: var(--bg);
  border: 1px solid var(--line);
  border-radius: 6px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
