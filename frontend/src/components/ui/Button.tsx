import type { ButtonHTMLAttributes } from 'react'

type Variant = 'primary' | 'secondary' | 'danger'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  block?: boolean
  small?: boolean
  /** Shows a spinner and blocks clicks. Not needed when the label itself says it is working ("Saving…"): that is detected. */
  loading?: boolean
}

/** The app writes a label that is in progress with a trailing ellipsis ("Saving…", "Uploading…"); those get the spinner for free. */
function labelIsInProgress(children: ButtonProps['children']): boolean {
  return typeof children === 'string' && /(…|\.\.\.)$/.test(children.trim())
}

export function Button({ variant = 'primary', block, small, loading, className, children, disabled, ...rest }: ButtonProps) {
  const busy = loading ?? labelIsInProgress(children)
  const classes = ['btn', `btn-${variant}`, block ? 'btn-block' : '', small ? 'btn-sm' : '', className ?? '']
    .filter(Boolean)
    .join(' ')
  return (
    <button className={classes} disabled={disabled || busy} aria-busy={busy || undefined} {...rest}>
      {busy && <span className="spinner btn-spinner" aria-hidden="true" />}
      {children}
    </button>
  )
}
