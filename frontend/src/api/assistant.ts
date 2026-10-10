import { apiClient } from './client'
import type { AiStatus, AssistantReply, AssistantTurn } from './types'

export const assistantApi = {
  status: () => apiClient.get<AiStatus>('/ai/status'),
  // The server stores nothing: the whole conversation (at most ten turns) is sent each time.
  chat: (messages: AssistantTurn[]) => apiClient.post<AssistantReply>('/ai/chat', { messages }),
}
