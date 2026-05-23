<template>
  <div class="dashboard-page">
    <!-- 统计卡片 -->
    <a-row :gutter="[16, 16]">
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-card-body">
            <div class="stat-info">
              <div class="stat-label">本月收入</div>
              <div class="stat-value income-value">
                ¥{{ formatNumber(summary.totalIncome) }}
              </div>
              <div class="stat-trend trend-up">
                <ArrowUpOutlined />
                较上月
              </div>
            </div>
            <div class="stat-icon income-icon">
              <RiseOutlined />
            </div>
          </div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-card-body">
            <div class="stat-info">
              <div class="stat-label">本月支出</div>
              <div class="stat-value expense-value">
                ¥{{ formatNumber(summary.totalExpense) }}
              </div>
              <div class="stat-trend trend-down">
                <FallOutlined />
                较上月
              </div>
            </div>
            <div class="stat-icon expense-icon">
              <FallOutlined />
            </div>
          </div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-card-body">
            <div class="stat-info">
              <div class="stat-label">当前结余</div>
              <div class="stat-value balance-value">
                ¥{{ formatNumber(summary.balance) }}
              </div>
              <div class="stat-trend neutral">
                <SwapOutlined />
                本月
              </div>
            </div>
            <div class="stat-icon balance-icon">
              <AccountBookOutlined />
            </div>
          </div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-card-body">
            <div class="stat-info">
              <div class="stat-label">交易笔数</div>
              <div class="stat-value count-value">
                {{ recentTransactions.length }}
              </div>
              <div class="stat-trend neutral">
                <BarsOutlined />
                本月
              </div>
            </div>
            <div class="stat-icon count-icon">
              <OrderedListOutlined />
            </div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 图表区域 -->
    <a-row :gutter="[16, 16]" class="mt-4">
      <a-col :xs="24" :md="12">
        <div class="chart-card">
          <div class="card-header">
            <PieChartOutlined class="card-header-icon" />
            <span>支出分类占比</span>
          </div>
          <div class="card-body">
            <div ref="pieChartRef" class="chart-container"></div>
          </div>
        </div>
      </a-col>
      <a-col :xs="24" :md="12">
        <div class="chart-card">
          <div class="card-header">
            <LineChartOutlined class="card-header-icon" />
            <span>近6月收支趋势</span>
          </div>
          <div class="card-body">
            <div ref="lineChartRef" class="chart-container"></div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 最近交易 -->
    <div class="recent-card mt-4">
      <div class="card-header">
        <OrderedListOutlined class="card-header-icon" />
        <span>最近交易</span>
        <router-link to="/transactions" class="view-all">查看全部</router-link>
      </div>
      <div class="card-body">
        <a-table
          :data-source="recentTransactions"
          :columns="recentColumns"
          :pagination="false"
          :show-header="false"
          row-key="id"
          class="recent-table"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'info'">
              <div class="recent-info">
                <div class="recent-category">
                  <div class="category-dot" :class="record.type === 'INCOME' ? 'dot-income' : 'dot-expense'"></div>
                  <span>{{ record.category }}</span>
                </div>
                <div class="recent-desc">{{ record.description || record.transactionDate }}</div>
              </div>
            </template>
            <template v-if="column.key === 'amount'">
              <span :class="record.type === 'INCOME' ? 'income-amount' : 'expense-amount'">
                {{ record.type === 'INCOME' ? '+' : '-' }}¥{{ formatNumber(Math.abs(record.amount)) }}
              </span>
            </template>
          </template>
        </a-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import {
  ArrowUpOutlined,
  RiseOutlined,
  FallOutlined,
  SwapOutlined,
  AccountBookOutlined,
  BarsOutlined,
  OrderedListOutlined,
  PieChartOutlined,
  LineChartOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { transactionApi } from '../api/transaction'
import type { DashboardSummary, Transaction, CategoryStat, MonthlyTrend } from '../api/transaction'

const summary = ref<DashboardSummary>({
  totalIncome: 0,
  totalExpense: 0,
  balance: 0,
  categoryStats: [],
  monthlyTrend: []
})

const recentTransactions = ref<Transaction[]>([])

const pieChartRef = ref<HTMLDivElement | null>(null)
const lineChartRef = ref<HTMLDivElement | null>(null)
let pieChart: echarts.ECharts | null = null
let lineChart: echarts.ECharts | null = null

const recentColumns = [
  { dataIndex: 'info', key: 'info' },
  { dataIndex: 'amount', key: 'amount', align: 'right' as const }
]

function formatNumber(num: number): string {
  return Math.abs(num).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

function initPieChart(stats: CategoryStat[]) {
  if (!pieChartRef.value) return

  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value)
  }

  const colorMap: Record<string, string> = {
    '餐饮': '#10b981',
    '交通': '#f59e0b',
    '购物': '#8b5cf6',
    '住房': '#3b82f6',
    '娱乐': '#ef4444',
    '工资': '#06b6d4',
    '其他': '#94a3b8'
  }

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: {
        fontSize: 12,
        color: '#64748b',
        fontFamily: 'Outfit'
      }
    },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold',
            fontFamily: 'Outfit'
          }
        },
        data: stats.map((s) => ({
          value: s.amount,
          name: s.category,
          itemStyle: { color: colorMap[s.category] || '#94a3b8' }
        }))
      }
    ]
  }

  pieChart.setOption(option, true)
}

function initLineChart(trend: MonthlyTrend[]) {
  if (!lineChartRef.value) return

  if (!lineChart) {
    lineChart = echarts.init(lineChartRef.value)
  }

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderWidth: 0,
      borderRadius: 12,
      padding: [12, 16],
      textStyle: {
        fontSize: 12,
        color: '#0f172a',
        fontFamily: 'Outfit'
      }
    },
    legend: {
      data: ['收入', '支出'],
      bottom: 0,
      left: 'center',
      textStyle: {
        fontSize: 12,
        color: '#64748b',
        fontFamily: 'Outfit'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '20%',
      top: '8%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: trend.map((t) => t.month),
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: {
        color: '#94a3b8',
        fontSize: 11,
        fontFamily: 'Outfit'
      }
    },
    yAxis: {
      type: 'value',
      splitLine: {
        lineStyle: {
          color: '#f1f5f9',
          type: 'dashed'
        }
      },
      axisLabel: {
        color: '#94a3b8',
        fontSize: 11,
        fontFamily: 'Outfit',
        formatter: (value: number) => `¥${value >= 1000 ? (value / 1000).toFixed(0) + 'k' : value}`
      }
    },
    series: [
      {
        name: '收入',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {
          color: '#10b981',
          width: 2
        },
        itemStyle: {
          color: '#10b981'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(16, 185, 129, 0.15)' },
            { offset: 1, color: 'rgba(16, 185, 129, 0.01)' }
          ])
        },
        data: trend.map((t) => t.income)
      },
      {
        name: '支出',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {
          color: '#ef4444',
          width: 2
        },
        itemStyle: {
          color: '#ef4444'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(239, 68, 68, 0.12)' },
            { offset: 1, color: 'rgba(239, 68, 68, 0.01)' }
          ])
        },
        data: trend.map((t) => t.expense)
      }
    ]
  }

  lineChart.setOption(option, true)
}

function handleResize() {
  pieChart?.resize()
  lineChart?.resize()
}

onMounted(async () => {
  try {
    const res = await transactionApi.getDashboard()
    summary.value = res.data
    recentTransactions.value = []
  } catch {
    // 使用模拟数据
    summary.value = {
      totalIncome: 28500,
      totalExpense: 12350,
      balance: 16150,
      categoryStats: [
        { category: '餐饮', amount: 3800, percentage: 30.8 },
        { category: '交通', amount: 850, percentage: 6.9 },
        { category: '购物', amount: 2400, percentage: 19.4 },
        { category: '住房', amount: 3500, percentage: 28.3 },
        { category: '娱乐', amount: 1200, percentage: 9.7 },
        { category: '其他', amount: 600, percentage: 4.9 }
      ],
      monthlyTrend: [
        { month: '12月', income: 22000, expense: 9800 },
        { month: '1月', income: 25000, expense: 11200 },
        { month: '2月', income: 28000, expense: 10500 },
        { month: '3月', income: 26500, expense: 12800 },
        { month: '4月', income: 29000, expense: 11800 },
        { month: '5月', income: 28500, expense: 12350 }
      ]
    }
    recentTransactions.value = [
      { id: 1, userId: 1, amount: 4500, type: 'INCOME', category: '工资', transactionDate: '2026-05-15', description: '5月工资', createdAt: '' },
      { id: 2, userId: 1, amount: 128, type: 'EXPENSE', category: '餐饮', transactionDate: '2026-05-15', description: '午餐', createdAt: '' },
      { id: 3, userId: 1, amount: 35, type: 'EXPENSE', category: '交通', transactionDate: '2026-05-14', description: '地铁', createdAt: '' },
      { id: 4, userId: 1, amount: 599, type: 'EXPENSE', category: '购物', transactionDate: '2026-05-13', description: '日用品', createdAt: '' },
      { id: 5, userId: 1, amount: 200, type: 'EXPENSE', category: '娱乐', transactionDate: '2026-05-12', description: '电影票', createdAt: '' }
    ]
  }

  await nextTick()
  initPieChart(summary.value.categoryStats)
  initLineChart(summary.value.monthlyTrend)

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  lineChart?.dispose()
})
</script>

<style scoped>
.dashboard-page {
}

.mt-4 {
  margin-top: 16px;
}

/* Stat cards */
.stat-card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.04),
    0 8px 24px rgba(0, 0, 0, 0.06);
  padding: 20px 24px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.04),
    0 12px 32px rgba(0, 0, 0, 0.08);
}

.stat-card-body {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 13px;
  font-weight: 500;
  color: #94a3b8;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.02em;
  margin-bottom: 8px;
}

.income-value {
  color: #10b981;
}

.expense-value {
  color: #ef4444;
}

.balance-value {
  color: #0f172a;
}

.count-value {
  color: #10b981;
}

.stat-trend {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.trend-up {
  color: #10b981;
}

.trend-down {
  color: #ef4444;
}

.neutral {
  color: #94a3b8;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.income-icon {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.expense-icon {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.balance-icon {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.count-icon {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

/* Chart cards */
.chart-card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.04),
    0 8px 24px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  margin-bottom: 16px;
}

.card-header {
  padding: 20px 24px 16px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.card-header-icon {
  color: #10b981;
  font-size: 18px;
}

.card-body {
  padding: 8px;
}

.chart-container {
  width: 100%;
  height: 300px;
}

/* Recent transactions */
.recent-card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.04),
    0 8px 24px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.recent-card .card-header {
  padding: 20px 24px 16px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.view-all {
  margin-left: auto;
  font-size: 13px;
  font-weight: 500;
  color: #10b981;
}

.view-all:hover {
  color: #059669;
}

.recent-card .card-body {
  padding: 0;
}

.recent-table :deep(.ant-table-tbody > tr > td) {
  padding: 12px 24px;
  border-bottom: 1px solid #f8fafc;
}

.recent-table :deep(.ant-table-tbody > tr:last-child > td) {
  border-bottom: none;
}

.recent-table :deep(.ant-table-tbody > tr:hover > td) {
  background: #f8fafc;
}

.recent-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.recent-category {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #0f172a;
}

.category-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot-income {
  background: #10b981;
}

.dot-expense {
  background: #ef4444;
}

.recent-desc {
  font-size: 12px;
  color: #94a3b8;
}

.income-amount {
  color: #10b981;
  font-weight: 600;
  font-size: 14px;
}

.expense-amount {
  color: #ef4444;
  font-weight: 600;
  font-size: 14px;
}
</style>
