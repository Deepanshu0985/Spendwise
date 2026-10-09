import type { MouseEvent, ReactNode } from 'react'
import { CloseIcon } from '../icons'

interface ModalProps {
  title: string
  onClose: () => void
  wide?: boolean
  children: ReactNode
}

export function Modal({ title, onClose, wide, children }: ModalProps) {
  function stop(e: MouseEvent) {
    e.stopPropagation()
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className={wide ? 'modal-box modal-box-wide' : 'modal-box'} onClick={stop} role="dialog" aria-modal="true" aria-label={title}>
        <div className="modal-header">
          <h2 className="modal-title">{title}</h2>
          <button type="button" className="modal-close" aria-label="Close" onClick={onClose}>
            <CloseIcon />
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}
