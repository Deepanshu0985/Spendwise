import { useEffect, useState } from 'react'
import { apiClient } from '../api/client'

interface HealthStatus {
  status: string
}

// Phase 0 proof-of-life only: confirms the API client layer reaches the backend
// through the Vite dev proxy. Replaced by real dashboard data in Phase 4/5.
export function DashboardPage() {
  const [health, setHealth] = useState<'checking' | 'up' | 'down'>('checking')

  useEffect(() => {
    apiClient
      .get<HealthStatus>('/health')
      .then((result) => setHealth(result.status === 'UP' ? 'up' : 'down'))
      .catch(() => setHealth('down'))
  }, [])

  return (
    <section>
      <h1>Dashboard</h1>
      <p>Coming in Phase 4/5. See docs/09-project/phase-plan.md.</p>
      <p>
        Backend connectivity: <strong>{health}</strong>
      </p>
    </section>
  )
}
