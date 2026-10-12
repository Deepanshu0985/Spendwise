/**
 * For work that takes a few seconds and changes things (reading a PDF, asking the AI, importing): a small card over a dimmed
 * page so it is clear something is happening and nothing else should be clicked. Like the top bar it only appears if the work
 * is not instant (delay in the CSS), and it is not rendered at all when idle.
 */
export function BusyOverlay({ active, title, detail }: { active: boolean; title: string; detail?: string }) {
  if (!active) return null
  return (
    <div className="busy-overlay" role="status" aria-live="polite">
      <div className="busy-card">
        <div className="bl-bars busy-bars" aria-hidden="true">
          <span />
          <span />
          <span />
          <span />
          <span />
        </div>
        <div className="busy-title">{title}</div>
        {detail && <div className="busy-detail">{detail}</div>}
      </div>
    </div>
  )
}
