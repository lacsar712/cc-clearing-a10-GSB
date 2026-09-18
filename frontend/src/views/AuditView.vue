<template>
  <div class="page">
    <h2 class="page-title">操作审计</h2>
    <p class="page-desc">轧差执行与义务创建的只读审计记录，展开行查看详情</p>

    <div class="toolbar">
      <el-select v-model="filters.eventType" clearable placeholder="事件类型" style="width:160px" @change="load">
        <el-option label="轧差执行" value="NETTING_EXECUTED" />
        <el-option label="义务创建" value="OBLIGATION_CREATED" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <div class="card-panel">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="audit-detail">
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="事件 ID" :span="2">
                  <span class="mono">{{ row.eventId }}</span>
                </el-descriptions-item>
                <el-descriptions-item v-for="item in detailItems(row)" :key="item.label" :label="item.label">
                  <span :class="{ mono: item.mono }">{{ item.value }}</span>
                </el-descriptions-item>
              </el-descriptions>
              <div v-if="row.eventType === 'NETTING_EXECUTED' && row.refId" style="margin-top:10px">
                <router-link class="mono" :to="`/netting-runs/${row.refId}`">查看关联批次 {{ row.refId }}</router-link>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="tagType(row)">{{ typeLabel(row.eventType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="actor" label="操作人" width="120" />
        <el-table-column prop="summary" label="摘要" min-width="320" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="!loading && rows.length === 0" description="暂无审计记录" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '../api/client'

const rows = ref([])
const loading = ref(false)

const filters = reactive({
  eventType: ''
})

const TYPE_LABELS = {
  NETTING_EXECUTED: '轧差执行',
  OBLIGATION_CREATED: '义务创建'
}

const FIELD_LABELS = {
  runId: '批次 ID',
  obligationId: '义务 ID',
  settleDate: '交割日',
  tradeDate: '交易日',
  currency: '币种',
  status: '状态',
  obligationCount: '义务笔数',
  positionCount: '净头寸笔数',
  sumNetAmount: 'Σ净额',
  failureReason: '失败原因',
  payerMemberId: '付款方',
  payeeMemberId: '收款方',
  amount: '金额'
}

const MONO_KEYS = new Set(['runId', 'obligationId', 'payerMemberId', 'payeeMemberId'])

function typeLabel(t) {
  return TYPE_LABELS[t] || t
}

function tagType(row) {
  if (row.parsed?.status === 'FAILED') return 'danger'
  return row.eventType === 'NETTING_EXECUTED' ? 'success' : 'info'
}

function formatTime(v) {
  return v ? new Date(v).toLocaleString() : '-'
}

function parseDetail(raw) {
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (e) {
    return null
  }
}

function detailItems(row) {
  const d = row.parsed
  if (!d) {
    return row.detail ? [{ label: '详情', value: row.detail, mono: false }] : []
  }
  return Object.entries(d).map(([k, v]) => ({
    label: FIELD_LABELS[k] || k,
    value: v == null || v === '' ? '-' : String(v),
    mono: MONO_KEYS.has(k)
  }))
}

async function load() {
  loading.value = true
  try {
    const params = {}
    if (filters.eventType) params.eventType = filters.eventType
    const { data } = await api.get('/audit-events', { params })
    rows.value = data.map((r) => ({ ...r, parsed: parseDetail(r.detail) }))
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.audit-detail {
  padding: 12px 16px;
  background: #f8fafc;
}
</style>
