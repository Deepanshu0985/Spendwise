import { useState } from 'react'
import { tipForDate } from '../lib/tips'

const DISMISSED_KEY = 'moneyTipDismissedOn'

function today(): string {
  const d = new Date()
  return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`
}

function dismissedToday(): boolean {
  try {
    return window.localStorage.getItem(DISMISSED_KEY) === today()
  } catch {
    return false
  }
}

/**
 * A small tip or fact about the app. "strip" sits under a page header, can be stepped to the next tip and hidden for the day;
 * "inline" is for loading states and has neither.
 */
export function MoneyTip({ variant = 'strip' }: { variant?: 'strip' | 'inline' }) {
  const [offset, setOffset] = useState(0)
  const [hidden, setHidden] = useState(variant === 'strip' && dismissedToday())
  if (hidden) return null
  const tip = tipForDate(new Date(), offset)

  if (variant === 'inline') {
    return <div className="tip-inline">{tip.kind === 'app' ? 'Did you know? ' : 'Tip: '}{tip.text}</div>
  }

  return (
    <div className="tip-strip" role="note">
      <span className={`tip-badge ${tip.kind}`}>{tip.kind === 'app' ? 'Did you know' : 'Money tip'}</span>
      <span className="tip-text">{tip.text}</span>
      <button type="button" className="tip-action" onClick={() => setOffset((current) => current + 1)}>
        Next
      </button>
      <button
        type="button"
        className="tip-action"
        aria-label="Hide tips for today"
        onClick={() => {
          setHidden(true)
          try {
            window.localStorage.setItem(DISMISSED_KEY, today())
          } catch {
            // hiding for the day is a convenience only
          }
        }}
      >
        ×
      </button>
    </div>
  )
}
