import { BrandLoader } from './BrandLoader'

/**
 * Shown while the first call to the backend is in flight. The free hosting tier puts the backend to sleep when idle, so
 * the first request after a pause can take about a minute; BrandLoader says so after a few seconds.
 */
export function ServerLoading() {
  return (
    <div className="splash">
      <BrandLoader title="Getting things ready" />
    </div>
  )
}
