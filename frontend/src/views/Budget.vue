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

    <!-- 月度总预算 -->
    <div class="budget-card">
      <div class="card-header">
        <SettingOutlined class="card-header-icon" />
        <span>月度总预算</span>
      </div>
      <div class="card-body">
        <a-row :gutter="24" align="middle">
          <a-col :xs="24" :md="8">
            <div class="budget-input-group">
              <label class="budget-label">月度总预算 (元)</label>
              <a-input-number
                v-model:value="overallBudget"
                :min="0"
                :max="99999999"
                :formatter="(value: any) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
                :parser="(value: any) => value.replace(/,/g, '')"
                size="large"
                class="budget-number-input"
                placeholder="请输入月度总预算"
              />
            </div>
          </a-col>
          <a-col :xs="24" :md="8">
            <div class="budget-input-group">
              <label class="budget-label">本月已支出</label>
              <div class="expense-display">
                <span class="expense-amount">¥{{ summary.totalExpense.toLocaleString() }}</span>
              </div>
            </div>
          </a-col>
          <a-col :xs="24" :md="8">
            <div class="budget-input-group">
              <label class="budget-label">剩余预算</label>
              <div class="remaining-display">
                <span class="remaining-amount" :class="remainingClass">
                  ¥{{ Math.max(0, (overallBudget || 0) - summary.totalExpense).toLocaleString() }}
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
                  {{ overallBudget ? Math.min(100, Math.round((summary.totalExpense / overallBudget) * 100)) : 0 }}%
                </span>
              </div>
              <a-progress
                :percent="overallBudget ? Math.min(100, Math.round((summary.totalExpense / overallBudget) * 100)) : 0"
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
            <a-button type="primary" size="large" class="save-budget-btn" :loading="saving" @click="saveOverallBudget">
              保存总预算
            </a-button>
          </a-col>
        </a-row>
      </div>
    </div>

    <!-- 分类预算 -->
    <div class="budget-card">
      <div class="card-header">
        <PieChartOutlined class="card-header-icon" />
        <span>分类预算</span>
        <a-button type="link" size="small" class="add-category-btn" @click="showAddCategory = true">
          <PlusOutlined /> 添加分类
        </a-button>
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
              <a-input-number
                v-model:value="record.budget"
                :min="0"
                :max="99999999"
                size="small"
                class="budget-edit-input"
                placeholder="预算"
              />
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
            <template v-if="column.key === 'action'">
              <a-button type="link" danger size="small" @click="removeCategory(record)">
                <DeleteOutlined />
              </a-button>
            </template>
          </template>

          <template #emptyText>
            <div class="empty-state">
              <p class="empty-text">暂无分类预算</p>
              <p class="empty-hint">点击「添加分类」为各支出类别设定预算</p>
            </div>
          </template>
        </a-table>

        <!-- 添加分类行 -->
        <div v-if="showAddCategory" class="add-category-row">
          <a-select
            v-model:value="newCategory"
            size="large"
            class="category-select"
            placeholder="选择支出分类"
          >
            <a-select-option
              v-for="cat in availableCategories"
              :key="cat.value"
              :value="cat.value"
            >
              {{ cat.label }}
            </a-select-option>
          </a-select>
          <a-input-number
            v-model:value="newBudgetAmount"
            :min="0"
            size="large"
            class="budget-amount-input"
            placeholder="预算金额"
          />
          <a-space>
            <a-button type="primary" size="large" @click="addCategory">确定</a-button>
            <a-button size="large" @click="cancelAddCategory">取消</a-button>
          </a-space>
        </div>

        <div class="save-category-area">
          <a-button type="primary" size="large" class="save-budget-btn" :loading="saving" @click="saveCategoryBudgets">
            保存分类预算
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { DollarOutlined, SettingOutlined, PieChartOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { budgetApi } from '../api/budget'
import dayjs from 'dayjs'

const loading = ref(false)
const saving = ref(false)

// 总预算
const overallBudget = ref<number>(0)
let overallBudgetId: number | null = null

// 预算汇总数据
const summary = reactive({
  totalExpense: 0,
  categoryActuals: [] as { category: string; amount: number }[]
})

interface CategoryBudgetItem {
  id: number | null  // BudgetConfig.id
  key: string
  category: string
  budget: number
  actual: number
}

const categoryBudgets = ref<CategoryBudgetItem[]>([])

// 添加分类相关
const showAddCategory = ref(false)
const newCategory = ref<string>('')
const newBudgetAmount = ref<number>(0)

const allCategories = [
  { value: '餐饮', label: '餐饮' },
  { value: '交通', label: '交通' },
  { value: '购物', label: '购物' },
  { value: '住房', label: '住房' },
  { value: '娱乐', label: '娱乐' },
  { value: '工资', label: '工资' },
  { value: '其他', label: '其他' }
]

const availableCategories = computed(() => {
  const used = new Set(categoryBudgets.value.map(c => c.category))
  return allCategories.filter(c => !used.has(c.value))
})

const columns = [
  { title: '支出分类', dataIndex: 'category', key: 'category' },
  { title: '预算金额', dataIndex: 'budget', key: 'budget' },
  { title: '实际支出', dataIndex: 'actual', key: 'actual' },
  { title: '使用进度', dataIndex: 'progress', key: 'progress' },
  { title: '操作', dataIndex: 'action', key: 'action', width: 60 }
]

const progressColor = computed(() => {
  if (!overallBudget.value) return '#10b981'
  const ratio = summary.totalExpense / overallBudget.value
  if (ratio < 0.5) return '#10b981'
  if (ratio < 0.8) return '#f59e0b'
  return '#ef4444'
})

const remainingClass = computed(() => {
  if (!overallBudget.value) return ''
  const ratio = summary.totalExpense / overallBudget.value
  if (ratio >= 0.8) return 'text-danger'
  if (ratio >= 0.5) return 'text-warning'
  return 'text-safe'
})

function getProgressColor(ratio: number): string {
  if (ratio < 0.5) return '#10b981'
  if (ratio < 0.8) return '#f59e0b'
  return '#ef4444'
}

function getActualForCategory(category: string): number {
  const item = summary.categoryActuals.find(a => a.category === category)
  return item ? item.amount : 0
}

async function loadBudgetData() {
  loading.value = true
  try {
    const now = dayjs()
    const res = await budgetApi.getSummary({
      month: now.month() + 1,
      year: now.year()
    })
    
    summary.totalExpense = res.data.totalExpense
    summary.categoryActuals = res.data.categoryActuals

    // 解析总预算
    overallBudgetId = null
    overallBudget.value = 0
    const items: CategoryBudgetItem[] = []

    for (const b of res.data.budgets) {
      if (b.category === null) {
        overallBudget.value = b.amount
        overallBudgetId = b.id
      } else {
        items.push({
          id: b.id,
          key: String(b.id || b.category),
          category: b.category,
          budget: b.amount,
          actual: getActualForCategory(b.category)
        })
      }
    }

    // 补充有实际支出但没有预算的分类
    for (const actual of res.data.categoryActuals) {
      if (!items.find(i => i.category === actual.category)) {
        items.push({
          id: null,
          key: 'new-' + actual.category,
          category: actual.category,
          budget: 0,
          actual: actual.amount
        })
      }
    }

    categoryBudgets.value = items
  } catch (error: any) {
    const msg = error?.response?.data?.message || error?.message || '加载预算数据失败'
    message.error(msg)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadBudgetData()
})

// 保存总预算
async function saveOverallBudget() {
  saving.value = true
  try {
    const now = dayjs()
    const res = await budgetApi.save({
      id: overallBudgetId,
      category: null,
      amount: overallBudget.value,
      month: now.month() + 1,
      year: now.year()
    })
    overallBudgetId = res.data.id
    message.success('总预算已保存！')
  } catch (error: any) {
    const msg = error?.response?.data?.message || error?.message || '保存失败'
    message.error(msg)
  } finally {
    saving.value = false
  }
}

// 添加分类预算
function addCategory() {
  if (!newCategory.value) {
    message.warning('请选择支出分类')
    return
  }
  if (newBudgetAmount.value <= 0) {
    message.warning('请输入预算金额')
    return
  }
  categoryBudgets.value.push({
    id: null,
    key: 'new-' + newCategory.value,
    category: newCategory.value,
    budget: newBudgetAmount.value,
    actual: getActualForCategory(newCategory.value)
  })
  newCategory.value = ''
  newBudgetAmount.value = 0
  showAddCategory.value = false
}

function cancelAddCategory() {
  showAddCategory.value = false
  newCategory.value = ''
  newBudgetAmount.value = 0
}

function removeCategory(record: CategoryBudgetItem) {
  categoryBudgets.value = categoryBudgets.value.filter(c => c.key !== record.key)
}

// 保存分类预算
async function saveCategoryBudgets() {
  saving.value = true
  try {
    const now = dayjs()
    for (const item of categoryBudgets.value) {
      if (item.budget > 0) {
        await budgetApi.save({
          id: item.id,
          category: item.category,
          amount: item.budget,
          month: now.month() + 1,
          year: now.year()
        })
      } else if (item.id !== null) {
        // 预算为 0 且已有记录，删除
        await budgetApi.delete(item.id)
      }
    }
    message.success('分类预算已保存！')
    // 刷新数据以获取最新 id
    await loadBudgetData()
  } catch (error: any) {
    const msg = error?.response?.data?.message || error?.message || '保存失败'
    message.error(msg)
  } finally {
    saving.value = false
  }
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

.add-category-btn {
  margin-left: auto;
  color: #10b981 !important;
  font-size: 13px;
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

.budget-edit-input {
  width: 120px !important;
  border-radius: 8px !important;
}

.budget-edit-input :deep(.ant-input-number-input) {
  border-radius: 8px !important;
  height: 36px;
  font-size: 14px;
  font-weight: 500;
}

.add-category-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 0;
  border-top: 1px solid #f1f5f9;
  margin-top: 16px;
}

.category-select {
  width: 200px !important;
  border-radius: 10px !important;
}

.budget-amount-input {
  width: 180px !important;
  border-radius: 10px !important;
}

.save-category-area {
  padding-top: 20px;
  border-top: 1px solid #f1f5f9;
  margin-top: 16px;
}

.empty-state {
  padding: 32px 0;
  text-align: center;
}

.empty-text {
  font-size: 14px;
  font-weight: 500;
  color: #94a3b8;
  margin: 0 0 4px;
}

.empty-hint {
  font-size: 13px;
  color: #cbd5e1;
  margin: 0;
}
</style>
