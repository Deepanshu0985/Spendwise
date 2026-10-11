import { useEffect, useState } from 'react'

const SLOW_AFTER_MS = 4000

interface Props {
  title: string
  detail?: string
  /** Free hosting puts the backend to sleep; after a few seconds, say so instead of leaving an unexplained wait. */
  explainSlowServer?: boolean
}

/** The Spendwise bars rising and falling like a chart, over the wordmark. Pure CSS; still when motion is reduced. */
export function BrandLoader({ title, detail, explainSlowServer = true }: Props) {
  const [slow, setSlow] = useState(false)

  useEffect(() => {
    if (!explainSlowServer) return
    const timer = window.setTimeout(() => setSlow(true), SLOW_AFTER_MS)
    return () => window.clearTimeout(timer)
  }, [explainSlowServer])

  return (
    <div className="brand-loader" role="status" aria-live="polite">
      <div className="bl-bars" aria-hidden="true">
        <span />
        <span />
        <span />
        <span />
        <span />
      </div>
      <div className="bl-word">Spendwise</div>
      <div className="bl-title">{title}</div>
      {detail && <div className="bl-detail">{detail}</div>}
      <div className="bl-track" aria-hidden="true">
        <span />
      </div>
      {slow && (
        <div className="bl-slow">
          The server is waking up after being idle. This can take up to a minute - please keep this tab open.
        </div>
      )}
    </div>
  )
}

export type SplashPhase = 'in' | 'out' | 'gone'

/** A full-screen splash around BrandLoader. Phase "out" fades it away instead of cutting it off; "gone" removes it. */
export function SplashScreen({ phase, title, detail }: Props & { phase: SplashPhase }) {
  if (phase === 'gone') return null
  return (
    <div className={`splash ${phase === 'out' ? 'splash-out' : ''}`}>
      <BrandLoader title={title} detail={detail} />
    </div>
  )
}
