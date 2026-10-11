import type { CSSProperties } from 'react'

/** A shimmering placeholder block. Skeletons stand in for content while it loads, in the shape the content will take. */
export function Skeleton({ width = '100%', height = 14, radius = 8, style }: { width?: number | string; height?: number | string; radius?: number; style?: CSSProperties }) {
  return <span className="skeleton" style={{ width, height, borderRadius: radius, ...style }} />
}

function Busy({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div role="status" aria-busy="true" aria-label={label}>
      <span className="visually-hidden">{label}</span>
      {children}
    </div>
  )
}

/** Rows of a table: used inside a card for transactions and statements. */
export function TableSkeleton({ label, rows = 7, columns = 5 }: { label: string; rows?: number; columns?: number }) {
  return (
    <Busy label={label}>
      <div className="sk-table" aria-hidden="true">
        {Array.from({ length: rows }, (_, row) => (
          <div className="sk-row" key={row} style={{ animationDelay: `${row * 60}ms`, gridTemplateColumns: `1.1fr 2.6fr repeat(${columns - 2}, 1fr)` }}>
            {Array.from({ length: columns }, (_, col) => (
              <Skeleton key={col} width={col === 1 ? `${60 + ((row * 17) % 35)}%` : `${48 + ((row * 11 + col * 7) % 40)}%`} height={13} />
            ))}
          </div>
        ))}
      </div>
    </Busy>
  )
}

/** Stacked cards, each a title, a progress bar and a line: budgets, goals and recurring payments. */
export function CardListSkeleton({ label, count = 3 }: { label: string; count?: number }) {
  return (
    <Busy label={label}>
      <div className="sk-stack" aria-hidden="true">
        {Array.from({ length: count }, (_, i) => (
          <div className="card sk-card" key={i} style={{ animationDelay: `${i * 90}ms` }}>
            <div className="sk-card-head">
              <Skeleton width={`${28 + ((i * 13) % 20)}%`} height={16} />
              <Skeleton width={72} height={16} />
            </div>
            <Skeleton height={10} radius={5} />
            <Skeleton width="40%" height={12} />
          </div>
        ))}
      </div>
    </Busy>
  )
}

/** The accounts grid. */
export function AccountGridSkeleton({ label, count = 3 }: { label: string; count?: number }) {
  return (
    <Busy label={label}>
      <div className="accounts-grid" aria-hidden="true">
        {Array.from({ length: count }, (_, i) => (
          <div className="account-card sk-card" key={i} style={{ animationDelay: `${i * 90}ms` }}>
            <div className="sk-card-head">
              <Skeleton width="55%" height={16} />
              <Skeleton width={54} height={20} radius={10} />
            </div>
            <Skeleton width="45%" height={26} />
            <Skeleton width="30%" height={12} />
          </div>
        ))}
      </div>
    </Busy>
  )
}

/** The dashboard: four figures, two charts, two lists. */
export function DashboardSkeleton() {
  return (
    <Busy label="Loading your dashboard">
      <div aria-hidden="true">
        <div className="summary-grid">
          {Array.from({ length: 4 }, (_, i) => (
            <div className="summary-card sk-card" key={i} style={{ animationDelay: `${i * 70}ms` }}>
              <Skeleton width="40%" height={11} />
              <Skeleton width="65%" height={26} style={{ marginTop: 12 }} />
            </div>
          ))}
        </div>
        <div className="dashboard-grid">
          <div className="card sk-card" style={{ animationDelay: '280ms' }}>
            <Skeleton width="38%" height={14} />
            <div className="sk-bars">
              {[78, 56, 44, 30, 18].map((w, i) => (
                <div className="sk-bar-row" key={i}>
                  <Skeleton width={90} height={12} />
                  <Skeleton width={`${w}%`} height={12} radius={6} />
                </div>
              ))}
            </div>
          </div>
          <div className="card sk-card" style={{ animationDelay: '350ms' }}>
            <Skeleton width="30%" height={14} />
            <div className="sk-columns">
              {[40, 62, 48, 80, 58, 70].map((h, i) => (
                <Skeleton key={i} width="100%" height={`${h}%`} radius={6} />
              ))}
            </div>
          </div>
        </div>
        <div className="dashboard-grid-secondary">
          {[0, 1].map((i) => (
            <div className="card sk-card" key={i} style={{ animationDelay: `${420 + i * 70}ms` }}>
              <Skeleton width="34%" height={14} />
              <div className="sk-lines">
                {[0, 1, 2, 3].map((j) => (
                  <div className="sk-line" key={j}>
                    <Skeleton width={`${40 + ((j * 19) % 30)}%`} height={12} />
                    <Skeleton width={60} height={12} />
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>
    </Busy>
  )
}

/** Three dots that bob while the assistant is working. */
export function TypingDots({ label }: { label: string }) {
  return (
    <span className="typing">
      <span className="typing-dots" aria-hidden="true">
        <span />
        <span />
        <span />
      </span>
      {label}
    </span>
  )
}
