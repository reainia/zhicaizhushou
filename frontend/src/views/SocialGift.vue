<template>
  <div class="social-gift-page">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-icon">
          <GiftOutlined />
        </div>
        <div>
          <h1 class="page-title">人情账单</h1>
          <p class="page-subtitle">记录份子钱收支，礼尚往来心中有数</p>
        </div>
      </div>
      <a-button type="primary" size="large" class="add-btn" @click="openAddModal">
        <PlusOutlined />
        记一笔
      </a-button>
    </div>

    <!-- 汇总统计 -->
    <a-row :gutter="[16, 16]" class="mb-4">
      <a-col :xs="24" :sm="8">
        <div class="stat-card">
          <div class="stat-label">随礼支出</div>
          <div class="stat-value give-value">¥{{ formatNumber(summary.totalGiven) }}</div>
        </div>
      </a-col>
      <a-col :xs="24" :sm="8">
        <div class="stat-card">
          <div class="stat-label">收礼收入</div>
          <div class="stat-value receive-value">¥{{ formatNumber(summary.totalReceived) }}</div>
        </div>
      </a-col>
      <a-col :xs="24" :sm="8">
        <div class="stat-card">
          <div class="stat-label">净收支</div>
          <div class="stat-value" :class="summary.netAmount >= 0 ? 'receive-value' : 'give-value'">
            {{ summary.netAmount >= 0 ? '+' : '' }}¥{{ formatNumber(summary.netAmount) }}
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 筛选 -->
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
            <label class="filter-label">收支方向</label>
            <a-select
              v-model:value="filterDirection"
              size="large"
              class="filter-input"
              placeholder="全部"
              @change="applyFilters"
            >
              <a-select-option value="">全部</a-select-option>
              <a-select-option value="GIVE">随礼</a-select-option>
              <a-select-option value="RECEIVE">收礼</a-select-option>
            </a-select>
          </a-col>
        </a-row>
      </div>
    </div>

    <!-- 列表 -->
    <div class="list-card">
      <div class="card-header">
        <GiftOutlined class="card-header-icon" />
        <span>人情记录</span>
        <span class="record-count">共 {{ filteredGifts.length }} 条</span>
      </div>
      <div class="card-body">
        <a-table
          :data-source="filteredGifts"
          :columns="columns"
          :pagination="{ pageSize: 10, showSizeChanger: true, showTotal: (total: number) => `共 ${total} 条` }"
          :loading="loading"
          row-key="id"
          class="gift-table"
        >
          <template #headerCell="{ column }">
            <span class="table-header-text">{{ column.title }}</span>
          </template>

          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'eventType'">
              <a-tag :color="getEventColor(record.eventType)">{{ record.eventType }}</a-tag>
            </template>
            <template v-if="column.key === 'personName'">
              <span class="person-name">{{ record.personName }}</span>
            </template>
            <template v-if="column.key === 'direction'">
              <a-tag :color="record.direction === 'GIVE' ? 'orange' : 'green'">
                {{ record.direction === 'GIVE' ? '随礼' : '收礼' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'amount'">
              <span :class="record.direction === 'GIVE' ? 'give-amount' : 'receive-amount'">
                {{ record.direction === 'GIVE' ? '-' : '+' }}¥{{ Math.abs(record.amount).toLocaleString() }}
              </span>
            </template>
            <template v-if="column.key === 'eventDate'">
              <span class="date-text">{{ record.eventDate }}</span>
            </template>
            <template v-if="column.key === 'description'">
              <span class="desc-text">{{ record.description || '--' }}</span>
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
              <p class="empty-text">暂无人情记录</p>
              <p class="empty-hint">点击右上角「记一笔」添加第一条记录</p>
            </div>
          </template>
        </a-table>
      </div>
    </div>

    <!-- 新增/编辑 Modal -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑人情记录' : '记一笔'"
      :footer="null"
      :destroy-on-close="true"
      :width="480"
      class="gift-modal"
    >
      <a-form
        :model="formState"
        :rules="formRules"
        layout="vertical"
        @finish="handleSubmit"
        class="gift-form"
      >
        <a-form-item name="direction" label="方向">
          <a-radio-group v-model:value="formState.direction" size="large" class="direction-radio">
            <a-radio-button value="GIVE">
              <ArrowDownOutlined /> 随礼
            </a-radio-button>
            <a-radio-button value="RECEIVE">
              <ArrowUpOutlined /> 收礼
            </a-radio-button>
          </a-radio-group>
        </a-form-item>

        <a-form-item name="eventType" label="人情类型">
          <a-select
            v-model:value="formState.eventType"
            size="large"
            class="full-width-input"
            placeholder="请选择类型"
          >
            <a-select-option
              v-for="et in eventTypes"
              :key="et.value"
              :value="et.value"
            >
              {{ et.label }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item name="personName" label="对象">
          <a-input
            v-model:value="formState.personName"
            size="large"
            class="full-width-input"
            placeholder="请输入对方姓名"
          >
            <template #prefix>
              <UserOutlined class="input-prefix" />
            </template>
          </a-input>
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

        <a-form-item name="eventDate" label="日期">
          <a-date-picker
            v-model:value="formState.eventDate"
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
            placeholder="可选填写备注，如酒席地点等"
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
        <p class="delete-text">确定要删除这条人情记录吗？</p>
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
  GiftOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  InboxOutlined,
  UserOutlined,
  ArrowDownOutlined,
  ArrowUpOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import dayjs, { Dayjs } from 'dayjs'
import { socialGiftApi } from '../api/socialGift'
import type { SocialGift } from '../api/socialGift'

const loading = ref(false)
const submitting = ref(false)
const deleting = ref(false)
const modalVisible = ref(false)
const deleteModalVisible = ref(false)
const editingId = ref<number | null>(null)
const deleteTarget = ref<SocialGift | null>(null)

const gifts = ref<SocialGift[]>([])
const selectedMonth = ref<Dayjs>(dayjs())
const filterDirection = ref<string>('')

const summary = reactive({
  totalGiven: 0,
  totalReceived: 0,
  netAmount: 0
})

const eventTypes = [
  { value: '婚礼', label: '💒 婚礼' },
  { value: '满月', label: '👶 满月' },
  { value: '丧事', label: '🕯️ 丧事' },
  { value: '升学', label: '🎓 升学' },
  { value: '生日', label: '🎂 生日' },
  { value: '乔迁', label: '🏠 乔迁' },
  { value: '生病探望', label: '🏥 生病探望' },
  { value: '节日', label: '🎉 节日' },
  { value: '其他', label: '📌 其他' }
]

interface FormState {
  direction: string
  eventType: string
  personName: string
  amount: number | null
  eventDate: Dayjs | null
  description: string
}

const formState = reactive<FormState>({
  direction: 'GIVE',
  eventType: '',
  personName: '',
  amount: null,
  eventDate: dayjs(),
  description: ''
})

const formRules = {
  eventType: [{ required: true, message: '请选择人情类型', trigger: 'change' }],
  personName: [{ required: true, message: '请输入对方姓名', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  eventDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
}

const columns = [
  { title: '日期', dataIndex: 'eventDate', key: 'eventDate', width: 110 },
  { title: '类型', dataIndex: 'eventType', key: 'eventType', width: 100 },
  { title: '对象', dataIndex: 'personName', key: 'personName', width: 100 },
  { title: '方向', dataIndex: 'direction', key: 'direction', width: 70 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 120 },
  { title: '备注', dataIndex: 'description', key: 'description' },
  { title: '操作', dataIndex: 'action', key: 'action', width: 100 }
]

const filteredGifts = computed(() => {
  if (!filterDirection.value) return gifts.value
  return gifts.value.filter((g) => g.direction === filterDirection.value)
})

function formatNumber(num: number): string {
  return Math.abs(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function getEventColor(type: string): string {
  const colorMap: Record<string, string> = {
    '婚礼': 'red',
    '满月': 'pink',
    '丧事': 'gray',
    '升学': 'blue',
    '生日': 'orange',
    '乔迁': 'green',
    '生病探望': 'purple',
    '节日': 'gold',
    '其他': 'default'
  }
  return colorMap[type] || 'default'
}

onMounted(() => {
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const params: { month?: number; year?: number } = {}
    if (selectedMonth.value) {
      params.month = selectedMonth.value.month() + 1
      params.year = selectedMonth.value.year()
    }
    const [listRes, summaryRes] = await Promise.all([
      socialGiftApi.list(params),
      socialGiftApi.getSummary(params)
    ])
    gifts.value = listRes.data
    Object.assign(summary, summaryRes.data)
  } catch (error: any) {
    const msg = error?.response?.data?.message || error?.message || '加载失败'
    message.error(msg)
  } finally {
    loading.value = false
  }
}

function handleMonthChange() {
  fetchData()
}

function applyFilters() {
  // computed 会自动过滤
}

function openAddModal() {
  editingId.value = null
  formState.direction = 'GIVE'
  formState.eventType = ''
  formState.personName = ''
  formState.amount = null
  formState.eventDate = dayjs()
  formState.description = ''
  modalVisible.value = true
}

function openEditModal(record: SocialGift) {
  editingId.value = record.id
  formState.direction = record.direction
  formState.eventType = record.eventType
  formState.personName = record.personName
  formState.amount = record.amount
  formState.eventDate = dayjs(record.eventDate)
  formState.description = record.description
  modalVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    const data: { direction: 'GIVE' | 'RECEIVE'; eventType: string; personName: string; amount: number; eventDate: string; description: string } = {
      direction: formState.direction as 'GIVE' | 'RECEIVE',
      eventType: formState.eventType,
      personName: formState.personName,
      amount: formState.amount!,
      eventDate: formState.eventDate!.format('YYYY-MM-DD'),
      description: formState.description
    }

    if (editingId.value) {
      await socialGiftApi.update(editingId.value, data)
    } else {
      await socialGiftApi.create(data)
    }

    message.success(editingId.value ? '记录已更新！' : '记录已添加！')
    modalVisible.value = false
    await fetchData()
  } catch (error: any) {
    const msg = error?.response?.data?.message || error?.message || '操作失败'
    message.error(typeof msg === 'string' ? msg : '操作失败')
  } finally {
    submitting.value = false
  }
}

function confirmDelete(record: SocialGift) {
  deleteTarget.value = record
  deleteModalVisible.value = true
}

async function handleDelete() {
  if (!deleteTarget.value) return
  deleting.value = true
  try {
    await socialGiftApi.delete(deleteTarget.value.id)
    message.success('记录已删除！')
    deleteModalVisible.value = false
    deleteTarget.value = null
    fetchData()
  } catch (error: any) {
    const msg = error?.response?.data?.message || error?.message || '删除失败'
    message.error(msg)
  } finally {
    deleting.value = false
  }
}
</script>

<style scoped>
.social-gift-page {
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
  background: linear-gradient(135deg, #f59e0b, #d97706);
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
  background: linear-gradient(135deg, #f59e0b, #d97706);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.add-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(245, 158, 11, 0.4);
}

.add-btn:active {
  transform: scale(0.98);
}

.mb-4 {
  margin-bottom: 16px;
}

.stat-card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 8px 24px rgba(0, 0, 0, 0.06);
  padding: 20px 24px;
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
}

.give-value {
  color: #ef4444;
}

.receive-value {
  color: #10b981;
}

/* Filter card */
.filter-card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 8px 24px rgba(0, 0, 0, 0.06);
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
  border-color: #f59e0b !important;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.1) !important;
}

/* List card */
.list-card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 8px 24px rgba(0, 0, 0, 0.06);
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
  color: #f59e0b;
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

/* Table */
.gift-table :deep(.ant-table-thead > tr > th) {
  background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
  padding: 14px 16px;
}

.gift-table :deep(.ant-table-tbody > tr > td) {
  padding: 14px 16px;
  border-bottom: 1px solid #f8fafc;
}

.gift-table :deep(.ant-table-tbody > tr:hover > td) {
  background: #f8fafc;
}

.table-header-text {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
}

.person-name {
  font-weight: 500;
  color: #0f172a;
}

.give-amount {
  color: #ef4444;
  font-weight: 600;
  font-size: 14px;
}

.receive-amount {
  color: #10b981;
  font-weight: 600;
  font-size: 14px;
}

.date-text {
  color: #64748b;
  font-size: 13px;
}

.desc-text {
  color: #94a3b8;
  font-size: 13px;
}

.action-btns {
  display: flex;
  gap: 4px;
}

.edit-btn {
  color: #f59e0b !important;
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

/* Modal */
.gift-form :deep(.ant-form-item) {
  margin-bottom: 20px;
}

.gift-form :deep(.ant-form-item-label) {
  padding-bottom: 6px;
}

.gift-form :deep(.ant-form-item-label label) {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  height: auto;
}

.direction-radio {
  width: 100%;
  display: flex;
}

.direction-radio :deep(.ant-radio-button-wrapper) {
  flex: 1;
  text-align: center;
  height: 44px;
  line-height: 44px;
  font-size: 14px;
}

.direction-radio :deep(.ant-radio-button-wrapper:first-child) {
  border-radius: 10px 0 0 10px;
}

.direction-radio :deep(.ant-radio-button-wrapper:last-child) {
  border-radius: 0 10px 10px 0;
}

.direction-radio :deep(.ant-radio-button-wrapper-checked) {
  border-color: #f59e0b;
  color: #f59e0b;
}

.direction-radio :deep(.ant-radio-button-wrapper-checked::before) {
  background: #f59e0b;
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
  border-color: #f59e0b !important;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.1) !important;
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
  background: linear-gradient(135deg, #f59e0b, #d97706);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
  transition: all 0.2s ease;
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(245, 158, 11, 0.4);
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

.gift-modal :deep(.ant-modal-header),
.delete-modal :deep(.ant-modal-header) {
  padding: 24px 24px 0;
  border-bottom: none;
}

.gift-modal :deep(.ant-modal-content),
.delete-modal :deep(.ant-modal-content) {
  border-radius: 16px;
  padding: 24px;
}

.gift-modal :deep(.ant-modal-body),
.delete-modal :deep(.ant-modal-body) {
  padding: 20px 0 0;
}
</style>
