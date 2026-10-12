import { Button } from './ui/Button'

interface Props {
  message: string
  busy: boolean
  onUndo: () => void
  onDismiss: () => void
  /** Sit above the bulk bar while rows are selected. */
  lifted?: boolean
}

/** "12 rows set to Food & Dining" with an Undo, shown after a bulk change until dismissed or replaced by the next one. */
export function UndoNotice({ message, busy, onUndo, onDismiss, lifted }: Props) {
  return (
    <div className="undo-notice" role="status" style={lifted ? { bottom: 86 } : undefined}>
      <span>{message}</span>
      <Button small variant="secondary" loading={busy} onClick={onUndo}>
        Undo
      </Button>
      <button type="button" className="undo-dismiss" aria-label="Dismiss" onClick={onDismiss} disabled={busy}>
        &times;
      </button>
    </div>
  )
}
