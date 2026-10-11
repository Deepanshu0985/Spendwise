import { useSyncExternalStore } from 'react'
import { getInFlight, subscribeInFlight } from '../../api/client'

/**
 * A thin bar across the top of the page while anything is being saved, deleted or loaded. It appears only if a call takes more
 * than a moment (the delay is in the CSS, so quick calls never flash it) and goes away by itself when the last one finishes.
 */
export function TopProgressBar() {
  const pending = useSyncExternalStore(subscribeInFlight, getInFlight, () => 0)
  if (pending === 0) return null
  return (
    <div className="top-progress" role="progressbar" aria-label="Working">
      <span />
    </div>
  )
}
