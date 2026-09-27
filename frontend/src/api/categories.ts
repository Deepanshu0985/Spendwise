import { apiClient } from './client'
import type { Category, CategoryType } from './types'

export interface CreateCategoryRequest {
  name: string
  categoryType: CategoryType
}

export const categoriesApi = {
  list: () => apiClient.get<Category[]>('/categories'),
  create: (body: CreateCategoryRequest) => apiClient.post<Category>('/categories', body),
}
