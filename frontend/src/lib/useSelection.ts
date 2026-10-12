import { useCallback, useState } from 'react'

/** A set of selected row ids with the handful of operations a table of checkboxes needs. */
export function useSelection() {
  const [selected, setSelected] = useState<ReadonlySet<string>>(new Set())

  const toggle = useCallback((id: string) => {
    setSelected((current) => {
      const next = new Set(current)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }, [])

  const replaceWith = useCallback((ids: string[]) => setSelected(new Set(ids)), [])
  const clear = useCallback(() => setSelected(new Set()), [])

  return { selected, toggle, replaceWith, clear, count: selected.size }
}
