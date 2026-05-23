import api from './auth'

export interface SocialGift {
  id: number
  userId: number
  eventType: string
  personName: string
  amount: number
  direction: 'GIVE' | 'RECEIVE'
  eventDate: string
  description: string
  createdAt: string
}

export interface SocialGiftSummary {
  totalGiven: number
  totalReceived: number
  netAmount: number
}

export const socialGiftApi = {
  list(params?: { month?: number; year?: number }) {
    return api.get<SocialGift[]>('/social-gifts', { params })
  },
  getSummary(params?: { month?: number; year?: number }) {
    return api.get<SocialGiftSummary>('/social-gifts/summary', { params })
  },
  create(data: { eventType: string; personName: string; amount: number; direction: string; eventDate: string; description?: string }) {
    return api.post<SocialGift>('/social-gifts', data)
  },
  update(id: number, data: Partial<SocialGift>) {
    return api.put<SocialGift>(`/social-gifts/${id}`, data)
  },
  delete(id: number) {
    return api.delete(`/social-gifts/${id}`)
  }
}
