import { useEffect, useState } from 'react'

/** True when the person has asked their system for less motion; animations then jump straight to the final state. */
export function prefersReducedMotion(): boolean {
  try {
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches
  } catch {
    return false
  }
}

/** False on the first paint and true just after, so a CSS transition from the "before" style to the "after" style runs. */
export function useReveal(delayMs = 40): boolean {
  const [shown, setShown] = useState(prefersReducedMotion())
  useEffect(() => {
    if (shown) return
    const timer = window.setTimeout(() => setShown(true), delayMs)
    return () => window.clearTimeout(timer)
  }, [shown, delayMs])
  return shown
}

/** Counts from 0 up to the target with an ease-out curve; the final value is always exactly the target. */
export function useCountUp(target: number, durationMs = 900): number {
  const [value, setValue] = useState(prefersReducedMotion() ? target : 0)
  useEffect(() => {
    if (prefersReducedMotion()) {
      setValue(target)
      return
    }
    let frame = 0
    const start = performance.now()
    const tick = (now: number) => {
      const t = Math.min(1, (now - start) / durationMs)
      const eased = 1 - Math.pow(1 - t, 3)
      setValue(target * eased)
      if (t < 1) frame = requestAnimationFrame(tick)
    }
    frame = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(frame)
  }, [target, durationMs])
  return value
}
