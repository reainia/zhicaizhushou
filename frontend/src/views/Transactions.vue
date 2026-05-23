<template>
  <div class="transactions-page">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-icon">
          <OrderedListOutlined />
        </div>
        <div>
          <h1 class="page-title">交易记录</h1>
          <p class="page-subtitle">查看和管理您的所有收支记录</p>
        </div>
      </div>
      <a-button type="primary" size="large" class="add-btn" @click="openAddModal">
        <PlusOutlined />
        记一笔
      </a-button>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-card">
      <div class="card-body">
        <a-row :gutter="16" align="middle">
          <a-col :xs="12" :md="6">
            <label class="filter-label">选择月份</label>
            <a-month-picker
              v-model:value="selectedMonth"
              :format="'YYYY-MM'"
              size="large"
              class="filter-input"
              @change="handleMonthChange"
            />
          </a-col>
          <a-col :xs="12" :md="6">
            <label class="filter-label">交易类型</label>
            <a-select
              v-model:value="filterType"
              size="large"
              class="filter-input"
              placeholder="全部类型"
              @change="fetchTransactions"
            >
              <a-select-option value="">全部类型</a-select-option>
              <a-select-option value="INCOME">收入</a-select-option>
              <a-select-option value="EXPENSE">支出</a-select-option>
            </a-select>
          </a-col>
        </a-row>
      </div>
    </div>

    <!-- 交易列表 -->
    <div class="transactions-card">
      <div class="card-header">
        <OrderedListOutlined class="card-header-icon" />
        <span>交易明细</span>
        <span class="record-count">共 {{ transactions.length }} 条记录</span>
      </div>
      <div class="card-body">
        <a-table
          :data-source="filteredTransactions"
          :columns="columns"
          :pagination="{ pageSize: 10, showSizeChanger: true, showTotal: (total: number) => `共 ${total} 条` }"
          :loading="loading"
          row-key="id"
          class="transaction-table"
        >
          <template #headerCell="{ column }">
            <span class="table-header-text">{{ column.title }}</span>
          </template>

          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'type'">
              <a-tag :color="record.type === 'INCOME' ? 'green' : 'red'">
                {{ record.type === 'INCOME' ? '收入' : '支出' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'amount'">
              <span :class="record.type === 'INCOME' ? 'income-amount' : 'expense-amount'">
                {{ record.type === 'INCOME' ? '+' : '-' }}¥{{ Math.abs(record.amount).toLocaleString() }}
              </span>
            </template>
            <template v-if="column.key === 'category'">
              <span class="category-tag">{{ record.category }}</span>
            </template>
            <template v-if="column.key === 'transactionDate'">
              <span class="date-text">{{ record.transactionDate }}</span>
            </template>
            <template v-if="column.key === 'description'">
              <span class="description-text">{{ record.description || '--' }}</span>
            </template>
            <template v-if="column.key === 'action'">
              <div class="action-btns">
                <a-button type="link" size="small" class="edit-btn" @click="openEditModal(record)">
                  <EditOutlined />
                </a-button>
                <a-button type="link" size="small" class="delete-btn" @click="confirmDelete(record)">
                  <DeleteOutlined />
                </a-button>
              </div>
            </template>
          </template>

          <template #emptyText>
            <div class="empty-state">
              <InboxOutlined class="empty-icon" />
              <p class="empty-text">暂无交易记录</p>
              <p class="empty-hint">点击右上角「记一笔」添加您的第一条记录</p>
            </div>
          </template>
        </a-table>
      </div>
    </div>

    <!-- 添加/编辑 Modal -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑交易' : '记一笔'"
      :footer="null"
      :destroy-on-close="true"
      :width="480"
      class="transaction-modal"
    >
      <a-form
        :model="formState"
        :rules="formRules"
        layout="vertical"
        @finish="handleSubmit"
        class="transaction-form"
      >
        <a-form-item name="type" label="类型">
          <a-radio-group v-model:value="formState.type" size="large" class="type-radio">
            <a-radio-button value="EXPENSE">
              <ShoppingCartOutlined /> 支出
            </a-radio-button>
            <a-radio-button value="INCOME">
              <RocketOutlined /> 收入
            </a-radio-button>
          </a-radio-group>
        </a-form-item>

        <a-form-item name="amount" label="金额">
          <a-input-number
            v-model:value="formState.amount"
            :min="0"
            :precision="2"
            size="large"
            class="full-width-input"
            placeholder="请输入金额"
          >
            <template #prefix>
              <span class="input-prefix">¥</span>
            </template>
          </a-input-number>
        </a-form-item>

        <a-form-item name="category" label="分类">
          <a-select
            v-model:value="formState.category"
            size="large"
            class="full-width-input"
            placeholder="请选择分类"
          >
            <a-select-option
              v-for="cat in categories"
              :key="cat.value"
              :value="cat.value"
            >
              {{ cat.label }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item name="transactionDate" label="日期">
          <a-date-picker
            v-model:value="formState.transactionDate"
            size="large"
            class="full-width-input"
            placeholder="选择日期"
          />
        </a-form-item>

        <a-form-item name="description" label="备注">
          <a-textarea
            v-model:value="formState.description"
            size="large"
            :rows="3"
            placeholder="可选填写备注信息"
            class="full-width-input"
          />
        </a-form-item>

        <a-form-item class="form-submit">
          <a-space style="width: 100%; justify-content: flex-end">
            <a-button size="large" @click="modalVisible = false">取消</a-button>
            <a-button
              type="primary"
              html-type="submit"
              :loading="submitting"
              size="large"
              class="submit-btn"
            >
              {{ editingId ? '保存修改' : '添加记录' }}
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 删除确认 -->
    <a-modal
      v-model:open="deleteModalVisible"
      title="确认删除"
      :footer="null"
      :width="400"
      class="delete-modal"
    >
      <div class="delete-content">
        <ExclamationCircleOutlined class="delete-icon" />
        <p class="delete-text">确定要删除这条交易记录吗？</p>
        <p class="delete-hint">此操作不可撤销</p>
      </div>
      <div class="delete-actions">
        <a-button size="large" @click="deleteModalVisible = false" style="flex: 1">取消</a-button>
        <a-button
          danger
          size="large"
          :loading="deleting"
          @click="handleDelete"
          style="flex: 1"
        >
          确认删除
        </a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import {
  OrderedListOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  InboxOutlined,
  ShoppingCartOutlined,
  RocketOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import dayjs, { Dayjs } from 'dayjs'
import { transactionApi } from '../api/transaction'
import type { Transaction } from '../api/transaction'

const loading = ref(false)
const submitting = ref(false)
const deleting = ref(false)
const modalVisible = ref(false)
const deleteModalVisible = ref(false)
const editingId = ref<number | null>(null)
const deleteTarget = ref<Transaction | null>(null)

const transactions = ref<Transaction[]>([])
const selectedMonth = ref<Dayjs>(dayjs())
const filterType = ref<string>('')

const categories = [
  { value: '餐饮', label: '餐饮' },
  { value: '交通', label: '交通' },
  { value: '购物', label: '购物' },
  { value: '住房', label: '住房' },
  { value: '娱乐', label: '娱乐' },
  { value: '工资', label: '工资' },
  { value: '其他', label: '其他' }
]

interface FormState {
  type: string
  amount: number | null
  category: string
  transactionDate: Dayjs | null
  description: string
}

const formState = reactive<FormState>({
  type: 'EXPENSE',
  amount: null,
  category: '',
  transactionDate: dayjs(),
  description: ''
})

const formRules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  transactionDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
}

const columns = [
  { title: '日期', dataIndex: 'transactionDate', key: 'transactionDate' },
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '分类', dataIndex: 'category', key: 'category' },
  { title: '金额', dataIndex: 'amount', key: 'amount' },
  { title: '备注', dataIndex: 'description', key: 'description' },
  { title: '操作', dataIndex: 'action', key: 'action', width: 100 }
]

const filteredTransactions = computed(() => {
  if (!filterType.value) return transactions.value
  return transactions.value.filter((t) => t.type === filterType.value)
})

onMounted(() => {
  fetchTransactions()
})

async function fetchTransactions() {
  loading.value = true
  try {
    const params: { month?: number; year?: number } = {}
    if (selectedMonth.value) {
      params.month = selectedMonth.value.month() + 1
      params.year = selectedMonth.value.year()
    }
    const res = await transactionApi.list(params)
    transactions.value = res.data
  } catch (error: any) {
    const msg = error?.response?.data?.message || error?.message || '获取交易记录失败'
    message.error(msg)
  } finally {
    loading.value = false
  }
}

function handleMonthChange() {
  fetchTransactions()
}

function openAddModal() {
  editingId.value = null
  formState.type = 'EXPENSE'
  formState.amount = null
  formState.category = ''
  formState.transactionDate = dayjs()
  formState.description = ''
  modalVisible.value = true
}

function openEditModal(record: Transaction) {
  editingId.value = record.id
  formState.type = record.type
  formState.amount = record.amount
  formState.category = record.category
  formState.transactionDate = dayjs(record.transactionDate)
  formState.description = record.description
  modalVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    const data = {
      amount: formState.amount!,
      type: formState.type,
      category: formState.category,
      transactionDate: formState.transactionDate!.format('YYYY-MM-DD'),
      description: formState.description
    }

    let res
    if (editingId.value) {
      res = await transactionApi.update(editingId.value, data)
    } else {
      res = await transactionApi.create(data)
    }

    // 只有 HTTP 200 时才关闭弹框并刷新列表
    if (res.status === 200) {
      message.success(editingId.value ? '交易记录已更新！' : '交易记录已添加！')
      modalVisible.value = false
      await fetchTransactions()
    }
  } catch (error: any) {
    // 非 200 状态：不关闭弹框，不刷新列表
    const msg = error?.response?.data?.message
      || error?.response?.data
      || error?.message
      || '操作失败'
    message.error(typeof msg === 'string' ? msg : '操作失败')
  } finally {
    submitting.value = false
  }
}

function confirmDelete(record: Transaction) {
  deleteTarget.value = record
  deleteModalVisible.value = true
}

async function handleDelete() {
  if (!deleteTarget.value) return
  deleting.value = true
  try {
    await transactionApi.delete(deleteTarget.value.id)
    message.success('交易记录已删除！')
    deleteModalVisible.value = false
    deleteTarget.value = null
    fetchTransactions()
  } catch (error: any) {
    const msg = error?.response?.data?.message || error?.message || '删除失败'
    message.error(msg)
  } finally {
    deleting.value = false
  }
}
</script>

<style scoped>
.transactions-page {
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

.add-btn {
  height: 44px;
  font-size: 14px;
  font-weight: 600;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #10b981, #059669);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.add-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(16, 185, 129, 0.4);
}

.add-btn:active {
  transform: scale(0.98);
}

/* Filter card */
.filter-card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.04),
    0 8px 24px rgba(0, 0, 0, 0.06);
  margin-bottom: 24px;
}

.filter-card .card-body {
  padding: 20px 24px;
}

.filter-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  margin-bottom: 6px;
}

.filter-input {
  width: 100% !important;
  border-radius: 10px !important;
}

.filter-input :deep(.ant-picker),
.filter-input :deep(.ant-select-selector) {
  border-radius: 10px !important;
  border: 1.5px solid #e2e8f0 !important;
  min-height: 44px;
}

.filter-input :deep(.ant-picker:hover),
.filter-input :deep(.ant-select-selector:hover) {
  border-color: #94a3b8 !important;
}

.filter-input :deep(.ant-picker-focused),
.filter-input :deep(.ant-select-focused .ant-select-selector) {
  border-color: #10b981 !important;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1) !important;
}

/* Transactions card */
.transactions-card {
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

.record-count {
  font-size: 12px;
  font-weight: 400;
  color: #94a3b8;
  margin-left: auto;
}

.card-body {
  padding: 0;
}

/* Table styles */
.transaction-table :deep(.ant-table-thead > tr > th) {
  background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
  padding: 14px 16px;
}

.transaction-table :deep(.ant-table-tbody > tr > td) {
  padding: 14px 16px;
  border-bottom: 1px solid #f8fafc;
}

.transaction-table :deep(.ant-table-tbody > tr:hover > td) {
  background: #f8fafc;
}

.table-header-text {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
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

.category-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 6px;
  background: #f1f5f9;
  color: #475569;
  font-size: 13px;
  font-weight: 500;
}

.date-text {
  color: #64748b;
  font-size: 13px;
}

.description-text {
  color: #94a3b8;
  font-size: 13px;
}

.action-btns {
  display: flex;
  gap: 4px;
}

.edit-btn {
  color: #10b981 !important;
}

.delete-btn {
  color: #ef4444 !important;
}

/* Empty state */
.empty-state {
  padding: 48px 0;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  color: #e2e8f0;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 15px;
  font-weight: 500;
  color: #94a3b8;
  margin: 0 0 6px;
}

.empty-hint {
  font-size: 13px;
  color: #cbd5e1;
  margin: 0;
}

/* Modal form */
.transaction-form :deep(.ant-form-item) {
  margin-bottom: 20px;
}

.transaction-form :deep(.ant-form-item-label) {
  padding-bottom: 6px;
}

.transaction-form :deep(.ant-form-item-label label) {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  height: auto;
}

.type-radio {
  width: 100%;
  display: flex;
}

.type-radio :deep(.ant-radio-button-wrapper) {
  flex: 1;
  text-align: center;
  height: 44px;
  line-height: 44px;
  border-radius: 0;
  font-size: 14px;
}

.type-radio :deep(.ant-radio-button-wrapper:first-child) {
  border-radius: 10px 0 0 10px;
}

.type-radio :deep(.ant-radio-button-wrapper:last-child) {
  border-radius: 0 10px 10px 0;
}

.type-radio :deep(.ant-radio-button-wrapper-checked) {
  border-color: #10b981;
  color: #10b981;
}

.type-radio :deep(.ant-radio-button-wrapper-checked::before) {
  background: #10b981;
}

.full-width-input {
  width: 100% !important;
  border-radius: 10px !important;
}

.full-width-input :deep(.ant-input),
.full-width-input :deep(.ant-input-number-input),
.full-width-input :deep(.ant-select-selector),
.full-width-input :deep(.ant-picker) {
  border-radius: 10px !important;
  border: 1.5px solid #e2e8f0 !important;
  min-height: 44px;
}

.full-width-input :deep(.ant-input:hover),
.full-width-input :deep(.ant-input-number-input:hover),
.full-width-input :deep(.ant-select-selector:hover),
.full-width-input :deep(.ant-picker:hover) {
  border-color: #94a3b8 !important;
}

.full-width-input :deep(.ant-input:focus),
.full-width-input :deep(.ant-input-number-input:focus),
.full-width-input :deep(.ant-select-focused .ant-select-selector),
.full-width-input :deep(.ant-picker-focused) {
  border-color: #10b981 !important;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1) !important;
}

.input-prefix {
  font-size: 16px;
  font-weight: 600;
  color: #64748b;
}

.form-submit {
  margin-bottom: 0 !important;
  padding-top: 8px;
}

.submit-btn {
  height: 44px;
  font-size: 14px;
  font-weight: 600;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #10b981, #059669);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
  transition: all 0.2s ease;
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(16, 185, 129, 0.4);
}

.submit-btn:active {
  transform: scale(0.98);
}

/* Delete modal */
.delete-content {
  text-align: center;
  padding: 16px 0;
}

.delete-icon {
  font-size: 48px;
  color: #ef4444;
  margin-bottom: 16px;
}

.delete-text {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 6px;
}

.delete-hint {
  font-size: 13px;
  color: #94a3b8;
  margin: 0;
}

.delete-actions {
  display: flex;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid #f1f5f9;
}

/* Modal overrides */
.transaction-modal :deep(.ant-modal-header),
.delete-modal :deep(.ant-modal-header) {
  padding: 24px 24px 0;
  border-bottom: none;
}

.transaction-modal :deep(.ant-modal-content),
.delete-modal :deep(.ant-modal-content) {
  border-radius: 16px;
  padding: 24px;
}

.transaction-modal :deep(.ant-modal-body),
.delete-modal :deep(.ant-modal-body) {
  padding: 20px 0 0;
}
</style>
