import api from './auth'

export interface BudgetConfig {
  id: number | null
  userId?: number
  category: string | null // null = 整体预算
  amount: number
  month: number
  year: number
}

export interface CategoryActual {
  category: string
  amount: number
}

export interface BudgetSummary {
  budgets: BudgetConfig[]
  totalExpense: number
  categoryActuals: CategoryActual[]
}

export const budgetApi = {
  getSummary(params?: { month?: number; year?: number }) {
    return api.get<BudgetSummary>('/budgets/summary', { params })
  },
  list(params?: { month?: number; year?: number }) {
    return api.get<BudgetConfig[]>('/budgets', { params })
  },
  save(data: { id?: number | null; category?: string | null; amount: number; month?: number; year?: number }) {
    return api.post<BudgetConfig>('/budgets', data)
  },
  delete(id: number) {
    return api.delete(`/budgets/${id}`)
  }
}
