import api from './auth'

export interface Transaction {
  id: number
  userId: number
  amount: number
  type: 'EXPENSE' | 'INCOME'
  category: string
  transactionDate: string
  description: string
  createdAt: string
}

export interface CategoryStat {
  category: string
  amount: number
  percentage: number
}

export interface MonthlyTrend {
  month: string
  income: number
  expense: number
}

export interface DashboardSummary {
  totalIncome: number
  totalExpense: number
  balance: number
  categoryStats: CategoryStat[]
  monthlyTrend: MonthlyTrend[]
}

export const transactionApi = {
  list(params?: { month?: number; year?: number }) {
    return api.get<Transaction[]>('/transactions', { params })
  },
  create(data: { amount: number; type: string; category: string; transactionDate: string; description?: string }) {
    return api.post<Transaction>('/transactions', data)
  },
  update(id: number, data: Partial<Transaction>) {
    return api.put<Transaction>(`/transactions/${id}`, data)
  },
  delete(id: number) {
    return api.delete(`/transactions/${id}`)
  },
  getDashboard() {
    return api.get<DashboardSummary>('/dashboard/summary')
  }
}
