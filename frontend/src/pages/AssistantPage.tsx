import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiRequestError } from '../api/client'
import { assistantApi } from '../api/assistant'
import type { AiStatus, AssistantTurn } from '../api/types'
import { SendIcon } from '../components/icons'
import { Button } from '../components/ui/Button'

interface Message extends AssistantTurn {
  basedOn?: { name: string; context: string }[]
  fallback?: boolean
}

const SUGGESTIONS = [
  'How much did I spend this month?',
  'What are my top merchants this month?',
  'Which subscriptions am I paying for?',
  'How am I doing on my budgets?',
  'How close am I to my goals?',
  'How did my spending change over the last 6 months?',
]

// The server keeps no conversation, so the browser sends it back each time; ten turns is the most the server accepts.
const MAX_TURNS_SENT = 10

export function AssistantPage() {
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [status, setStatus] = useState<AiStatus | null>(null)
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    assistantApi.status().then(setStatus, () => setStatus(null))
  }, [])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, sending])

  async function send(question: string) {
    const text = question.trim()
    if (!text || sending) return
    setError(null)
    setInput('')
    const next: Message[] = [...messages, { role: 'user', content: text }]
    setMessages(next)
    setSending(true)
    try {
      const reply = await assistantApi.chat(next.slice(-MAX_TURNS_SENT).map(({ role, content }) => ({ role, content })))
      setMessages([...next, { role: 'assistant', content: reply.answer, basedOn: reply.toolsUsed, fallback: reply.fallback }])
      setStatus((current) => (current ? { ...current, remainingMessagesToday: reply.remainingMessagesToday } : current))
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
      setMessages(messages) // the question was not answered, so it is not kept in the conversation
      setInput(text)
    } finally {
      setSending(false)
    }
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    void send(input)
  }

  const limitReached = status !== null && status.enabled && status.remainingMessagesToday <= 0
  const tooLong = messages.length >= MAX_TURNS_SENT

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 80px)', minHeight: 420 }}>
      <div className="page-header">
        <div>
          <h1>Assistant</h1>
          <div className="page-subtitle">Ask about your spending. Answers use only your confirmed transactions, budgets and goals.</div>
        </div>
        {messages.length > 0 && (
          <Button variant="secondary" onClick={() => { setMessages([]); setError(null) }}>
            New conversation
          </Button>
        )}
      </div>

      {status && !status.enabled && (
        <div className="form-error-banner" style={{ marginBottom: 12 }}>The assistant is switched off right now. The rest of the app works as usual.</div>
      )}

      <div className="card" style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 14 }}>
        {messages.length === 0 && (
          <div>
            <div className="page-subtitle" style={{ marginBottom: 12 }}>Try one of these:</div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
              {SUGGESTIONS.map((suggestion) => (
                <button
                  key={suggestion}
                  type="button"
                  className="badge"
                  style={{ cursor: 'pointer', border: '1px solid var(--border)', padding: '8px 12px', background: 'var(--card)', color: 'var(--ink)' }}
                  disabled={!status?.enabled || limitReached}
                  onClick={() => void send(suggestion)}
                >
                  {suggestion}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((message, index) => (
          <div key={index} style={{ alignSelf: message.role === 'user' ? 'flex-end' : 'flex-start', maxWidth: '85%' }}>
            <div
              style={{
                padding: '10px 14px',
                borderRadius: 12,
                whiteSpace: 'pre-wrap',
                background: message.role === 'user' ? 'var(--accent)' : 'var(--border-soft)',
                color: message.role === 'user' ? '#fff' : 'var(--ink)',
              }}
            >
              {message.content}
            </div>
            {message.role === 'assistant' && message.basedOn && message.basedOn.length > 0 && (
              <div className="page-subtitle" style={{ marginTop: 4 }}>
                Based on: {message.basedOn.map((t) => t.context).join(' · ')}
              </div>
            )}
            {message.fallback && (
              <div className="page-subtitle" style={{ marginTop: 2 }}>
                Shown as plain figures because the assistant's wording couldn't be verified against your data.
              </div>
            )}
          </div>
        ))}

        {sending && (
          <div className="page-subtitle" style={{ alignSelf: 'flex-start' }}>
            <span className="spinner" /> Looking at your data…
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {error && <div className="form-error-banner" style={{ marginTop: 12 }}>{error}</div>}
      {tooLong && <div className="page-subtitle" style={{ marginTop: 8 }}>This conversation is getting long - start a new one to keep going.</div>}

      <form onSubmit={handleSubmit} style={{ display: 'flex', gap: 10, marginTop: 12, alignItems: 'flex-end' }}>
        <textarea
          aria-label="Your question"
          value={input}
          maxLength={1000}
          rows={2}
          placeholder="Ask about your spending…"
          disabled={!status?.enabled || limitReached || tooLong}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault()
              void send(input)
            }
          }}
          style={{ flex: 1, resize: 'none', padding: '10px 12px', borderRadius: 10, border: '1px solid var(--border)', font: 'inherit' }}
        />
        <Button type="submit" disabled={sending || !input.trim() || !status?.enabled || limitReached || tooLong}>
          <SendIcon />
          Send
        </Button>
      </form>
      {status?.enabled && (
        <div className="page-subtitle" style={{ marginTop: 6 }}>
          {limitReached ? "You've used today's messages. They reset tomorrow." : `${status.remainingMessagesToday} of ${status.dailyMessageLimit} messages left today`}
        </div>
      )}
    </div>
  )
}
