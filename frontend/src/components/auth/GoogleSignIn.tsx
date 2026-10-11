import { useEffect, useRef, useState } from 'react'
import { authApi } from '../../api/auth'

// Google's own button (Google Identity Services): it opens Google's sign-in and hands back a signed ID token, which the
// backend verifies. The script is loaded from Google at the moment a login page needs it, so nothing is bundled and
// nothing loads for people who are already signed in.

interface GoogleCredentialResponse {
  credential?: string
}

interface GoogleAccountsId {
  initialize: (config: { client_id: string; callback: (response: GoogleCredentialResponse) => void }) => void
  renderButton: (parent: HTMLElement, options: Record<string, unknown>) => void
}

declare global {
  interface Window {
    google?: { accounts: { id: GoogleAccountsId } }
  }
}

const SCRIPT_SRC = 'https://accounts.google.com/gsi/client'
let scriptPromise: Promise<void> | null = null

function loadGoogleScript(): Promise<void> {
  if (window.google?.accounts?.id) return Promise.resolve()
  if (!scriptPromise) {
    scriptPromise = new Promise<void>((resolve, reject) => {
      const script = document.createElement('script')
      script.src = SCRIPT_SRC
      script.async = true
      script.onload = () => resolve()
      script.onerror = () => {
        scriptPromise = null // allow another try (e.g. a blocker was switched off)
        reject(new Error('Google sign-in script could not be loaded'))
      }
      document.head.appendChild(script)
    })
  }
  return scriptPromise
}

interface Props {
  onCredential: (credential: string) => void
}

/** Draws nothing at all unless this server has Google sign-in set up and Google's script is reachable. */
export function GoogleSignIn({ onCredential }: Props) {
  const containerRef = useRef<HTMLDivElement>(null)
  const handlerRef = useRef(onCredential)
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    handlerRef.current = onCredential
  }, [onCredential])

  useEffect(() => {
    let cancelled = false
    async function setUp() {
      try {
        const config = await authApi.googleConfig()
        if (!config.enabled || !config.clientId) return
        await loadGoogleScript()
        const container = containerRef.current
        if (cancelled || !container || !window.google) return
        window.google.accounts.id.initialize({
          client_id: config.clientId,
          callback: (response) => {
            if (response.credential) handlerRef.current(response.credential)
          },
        })
        window.google.accounts.id.renderButton(container, {
          type: 'standard',
          theme: 'outline',
          size: 'large',
          text: 'continue_with',
          shape: 'rectangular',
          logo_alignment: 'left',
          // Measured from the always-visible form box: this container is still display:none at this point, so it has no width.
          width: Math.min(360, Math.floor(container.closest('.auth-form-box')?.clientWidth ?? 360)),
        })
        setVisible(true)
      } catch {
        // Google sign-in is an extra way in: if it can't be offered, the email form still works.
      }
    }
    void setUp()
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div className="google-signin" hidden={!visible}>
      <div ref={containerRef} className="google-signin-button" />
      <div className="auth-divider">
        <span>or</span>
      </div>
    </div>
  )
}
