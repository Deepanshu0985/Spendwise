import { useEffect, useState } from 'react'

const SLOW_AFTER_MS = 4000

/**
 * Shown while the first call to the backend is in flight. The free hosting tier puts the
 * backend to sleep when idle, so the first request after a pause can take about a minute;
 * saying so beats an unexplained spinner (or a blank page).
 */
export function ServerLoading() {
  const [slow, setSlow] = useState(false)

  useEffect(() => {
    const timer = window.setTimeout(() => setSlow(true), SLOW_AFTER_MS)
    return () => window.clearTimeout(timer)
  }, [])

  return (
    <div className="loading-state" style={{ flexDirection: 'column', gap: 10 }}>
      <div>
        <span className="spinner" /> Loading&hellip;
      </div>
      {slow && (
        <div style={{ maxWidth: 360, textAlign: 'center', color: 'var(--ink-muted)', fontSize: 14 }}>
          The server is waking up after being idle. This can take up to a minute - please keep this tab open.
        </div>
      )}
    </div>
  )
}
