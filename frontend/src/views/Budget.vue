<template>
  <div class="budget-page">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-icon">
          <DollarOutlined />
        </div>
        <div>
          <h1 class="page-title">预算管理</h1>
          <p class="page-subtitle">设定月度预算并跟踪支出情况</p>
        </div>
      </div>
    </div>

    <!-- 月度预算设置 -->
    <div class="budget-card">
      <div class="card-header">
        <SettingOutlined class="card-header-icon" />
        <span>月度预算设置</span>
      </div>
      <div class="card-body">
        <a-row :gutter="24" align="middle">
          <a-col :xs="24" :md="8">
            <div class="budget-input-group">
              <label class="budget-label">月度总预算 (元)</label>
              <a-input-number
                v-model:value="budgetLimit"
                :min="0"
                :max="99999999"
                :formatter="(value: any) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
                :parser="(value: any) => value.replace(/,/g, '')"
                size="large"
                class="budget-number-input"
                placeholder="请输入月度预算"
              />
            </div>
          </a-col>
          <a-col :xs="24" :md="8">
            <div class="budget-input-group">
              <label class="budget-label">本月已支出</label>
              <div class="expense-display">
                <span class="expense-amount">¥{{ totalExpense.toLocaleString() }}</span>
              </div>
            </div>
          </a-col>
          <a-col :xs="24" :md="8">
            <div class="budget-input-group">
              <label class="budget-label">剩余预算</label>
              <div class="remaining-display">
                <span class="remaining-amount" :class="remainingClass">
                  ¥{{ Math.max(0, (budgetLimit || 0) - totalExpense).toLocaleString() }}
                </span>
              </div>
            </div>
          </a-col>
        </a-row>

        <a-row :gutter="24" class="mt-4">
          <a-col :span="24">
            <div class="budget-progress-section">
              <div class="progress-header">
                <span class="progress-label">预算使用进度</span>
                <span class="progress-percentage" :style="{ color: progressColor }">
                  {{ budgetLimit ? Math.min(100, Math.round((totalExpense / budgetLimit) * 100)) : 0 }}%
                </span>
              </div>
              <a-progress
                :percent="budgetLimit ? Math.min(100, Math.round((totalExpense / budgetLimit) * 100)) : 0"
                :stroke-color="progressColor"
                :stroke-width="12"
                :show-info="false"
                class="budget-progress"
              />
            </div>
          </a-col>
        </a-row>

        <a-row :gutter="24" class="mt-4">
          <a-col :span="24">
            <a-button type="primary" size="large" class="save-budget-btn" @click="saveBudget">
              保存预算设置
            </a-button>
          </a-col>
        </a-row>
      </div>
    </div>

    <!-- 分类预算 -->
    <div class="budget-card">
      <div class="card-header">
        <PieChartOutlined class="card-header-icon" />
        <span>分类预算对比</span>
      </div>
      <div class="card-body">
        <a-table
          :data-source="categoryBudgets"
          :columns="columns"
          :pagination="false"
          :loading="loading"
          class="budget-table"
        >
          <template #headerCell="{ column }">
            <span class="table-header-text">{{ column.title }}</span>
          </template>

          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'category'">
              <span class="category-name">{{ record.category }}</span>
            </template>
            <template v-if="column.key === 'budget'">
              <span class="amount-text">¥{{ record.budget.toLocaleString() }}</span>
            </template>
            <template v-if="column.key === 'actual'">
              <span class="amount-text">¥{{ record.actual.toLocaleString() }}</span>
            </template>
            <template v-if="column.key === 'progress'">
              <div class="progress-cell">
                <a-progress
                  :percent="record.budget > 0 ? Math.min(100, Math.round((record.actual / record.budget) * 100)) : 0"
                  :stroke-color="getProgressColor(record.budget > 0 ? record.actual / record.budget : 0)"
                  :stroke-width="10"
                  :show-info="false"
                  class="category-progress"
                />
                <span class="progress-text" :style="{ color: getProgressColor(record.budget > 0 ? record.actual / record.budget : 0) }">
                  {{ record.budget > 0 ? Math.round((record.actual / record.budget) * 100) : 0 }}%
                </span>
              </div>
            </template>
          </template>
        </a-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { DollarOutlined, SettingOutlined, PieChartOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

const loading = ref(false)
const budgetLimit = ref<number>(5000)
const totalExpense = ref<number>(0)

interface CategoryBudget {
  key: string
  category: string
  budget: number
  actual: number
}

const categoryBudgets = ref<CategoryBudget[]>([
  { key: '1', category: '餐饮', budget: 1500, actual: 1200 },
  { key: '2', category: '交通', budget: 500, actual: 350 },
  { key: '3', category: '购物', budget: 1000, actual: 850 },
  { key: '4', category: '住房', budget: 2000, actual: 2000 },
  { key: '5', category: '娱乐', budget: 500, actual: 200 },
  { key: '6', category: '其他', budget: 500, actual: 100 }
])

const columns = [
  { title: '支出分类', dataIndex: 'category', key: 'category' },
  { title: '预算金额', dataIndex: 'budget', key: 'budget' },
  { title: '实际支出', dataIndex: 'actual', key: 'actual' },
  { title: '使用进度', dataIndex: 'progress', key: 'progress' }
]

const progressColor = computed(() => {
  if (!budgetLimit.value) return '#10b981'
  const ratio = totalExpense.value / budgetLimit.value
  if (ratio < 0.5) return '#10b981'
  if (ratio < 0.8) return '#f59e0b'
  return '#ef4444'
})

const remainingClass = computed(() => {
  if (!budgetLimit.value) return ''
  const ratio = totalExpense.value / budgetLimit.value
  if (ratio >= 0.8) return 'text-danger'
  if (ratio >= 0.5) return 'text-warning'
  return 'text-safe'
})

function getProgressColor(ratio: number): string {
  if (ratio < 0.5) return '#10b981'
  if (ratio < 0.8) return '#f59e0b'
  return '#ef4444'
}

onMounted(() => {
  // 从 localStorage 恢复预算设置
  const savedBudget = localStorage.getItem('monthlyBudget')
  if (savedBudget) {
    budgetLimit.value = Number(savedBudget)
  }

  // 计算总支出
  totalExpense.value = categoryBudgets.value.reduce((sum, item) => sum + item.actual, 0)
})

function saveBudget() {
  localStorage.setItem('monthlyBudget', String(budgetLimit.value))
  message.success('预算设置已保存！')
}
</script>

<style scoped>
.budget-page {
}

.page-header {
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.page-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #10b981, #059669);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  letter-spacing: -0.02em;
}

.page-subtitle {
  font-size: 13px;
  color: #94a3b8;
  margin: 2px 0 0;
}

.budget-card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.04),
    0 8px 24px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  margin-bottom: 24px;
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
  padding: 24px;
}

.budget-input-group {
  margin-bottom: 16px;
}

.budget-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  margin-bottom: 8px;
}

.budget-number-input {
  width: 100% !important;
  border-radius: 10px !important;
}

.budget-number-input :deep(.ant-input-number-input) {
  border-radius: 10px !important;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
}

.expense-display,
.remaining-display {
  height: 46px;
  display: flex;
  align-items: center;
  padding: 0 14px;
  border-radius: 10px;
  border: 1.5px solid #e2e8f0;
  background: #fff;
}

.expense-amount {
  font-size: 18px;
  font-weight: 700;
  color: #ef4444;
}

.remaining-amount {
  font-size: 18px;
  font-weight: 700;
}

.text-safe {
  color: #10b981;
}

.text-warning {
  color: #f59e0b;
}

.text-danger {
  color: #ef4444;
}

.mt-4 {
  margin-top: 16px;
}

.budget-progress-section {
  padding: 16px 0;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.progress-label {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
}

.progress-percentage {
  font-size: 18px;
  font-weight: 700;
}

.budget-progress {
  margin-bottom: 8px;
}

.budget-progress :deep(.ant-progress-bg) {
  border-radius: 6px !important;
}

.save-budget-btn {
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #10b981, #059669);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
  transition: all 0.2s ease;
}

.save-budget-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(16, 185, 129, 0.4);
}

.save-budget-btn:active {
  transform: scale(0.98);
}

.category-name {
  font-weight: 500;
  color: #0f172a;
}

.amount-text {
  font-weight: 600;
  color: #334155;
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.category-progress {
  flex: 1;
  max-width: 200px;
}

.category-progress :deep(.ant-progress-bg) {
  border-radius: 5px !important;
}

.progress-text {
  font-size: 13px;
  font-weight: 600;
  min-width: 36px;
  text-align: right;
}

.budget-table :deep(.ant-table-thead > tr > th) {
  background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
  padding: 14px 16px;
}

.budget-table :deep(.ant-table-tbody > tr > td) {
  padding: 14px 16px;
  border-bottom: 1px solid #f8fafc;
}

.budget-table :deep(.ant-table-tbody > tr:hover > td) {
  background: #f8fafc;
}

.table-header-text {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
}
</style>
