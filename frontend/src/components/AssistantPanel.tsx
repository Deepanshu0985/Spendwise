import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { useLocation } from 'react-router-dom'
import { ApiRequestError } from '../api/client'
import { assistantApi } from '../api/assistant'
import type { AiStatus, AssistantSource, AssistantTurn } from '../api/types'
import { formatCurrency, formatDate } from '../lib/format'
import { CloseIcon, SendIcon } from './icons'

interface Message extends AssistantTurn {
  basedOn?: { name: string; context: string }[]
  sources?: AssistantSource[]
  fallback?: boolean
}

/** Shows **bold** as bold and everything else as plain text. Never builds HTML from the text, so nothing in it can act as markup. */
function renderText(text: string) {
  return text.split(/(\*\*[^*\n]+\*\*)/g).map((part, index) =>
    part.startsWith('**') && part.endsWith('**') && part.length > 4 ? <strong key={index}>{part.slice(2, -2)}</strong> : part,
  )
}

const DEFAULT_SUGGESTIONS = [
  'How much did I spend this month?',
  'What are my top merchants this month?',
  'How did my spending change over the last 6 months?',
]

// What is most likely worth asking on the page you are looking at.
const SUGGESTIONS_BY_PAGE: Record<string, string[]> = {
  '/': DEFAULT_SUGGESTIONS,
  '/transactions': ['What were my five biggest expenses this month?', 'How much did I spend on food last month?', 'Find my payments to Uber this year'],
  '/accounts': ['How much did I spend this month?', 'How much did I save last month?'],
  '/statements': ['How much did I spend last month?', 'What are my top merchants over the last 3 months?'],
  '/recurring': ['Which recurring payments am I paying for?', 'How much do my recurring payments cost each month?'],
  '/budgets': ['How am I doing on my budgets?', 'Which budget is closest to its limit?'],
  '/goals': ['How close am I to my goals?', 'How much should I set aside each month for my goals?'],
}

// The server keeps no conversation, so the browser sends it back each time; ten turns is the most the server accepts.
const MAX_TURNS_SENT = 10

interface Props {
  open: boolean
  onClose: () => void
}

/**
 * The assistant as a panel docked on the right of every page. It stays mounted (just hidden) when closed and lives in the
 * layout above the routes, so the conversation survives closing the panel and moving between pages.
 */
export function AssistantPanel({ open, onClose }: Props) {
  const location = useLocation()
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [status, setStatus] = useState<AiStatus | null>(null)
  const [statusLoaded, setStatusLoaded] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLTextAreaElement>(null)

  // Looked up the first time the panel is opened, not on every page load.
  useEffect(() => {
    if (open && !statusLoaded) {
      setStatusLoaded(true)
      assistantApi.status().then(setStatus, () => setStatus(null))
    }
  }, [open, statusLoaded])

  useEffect(() => {
    if (open) inputRef.current?.focus()
  }, [open])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, sending, open])

  useEffect(() => {
    if (!open) return
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

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
      setMessages([...next, { role: 'assistant', content: reply.answer, basedOn: reply.toolsUsed, sources: reply.sources, fallback: reply.fallback }])
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

  const off = status !== null && !status.enabled
  const limitReached = status !== null && status.enabled && status.remainingMessagesToday <= 0
  const tooLong = messages.length >= MAX_TURNS_SENT
  const blocked = off || limitReached || tooLong
  const page = '/' + (location.pathname.split('/')[1] ?? '')
  const suggestions = SUGGESTIONS_BY_PAGE[page] ?? DEFAULT_SUGGESTIONS

  return (
    <aside className="assistant-panel" hidden={!open} aria-label="Assistant">
      <div className="assistant-header">
        <div>
          <div className="assistant-title">Assistant</div>
          <div className="assistant-subtitle">Answers come from your confirmed transactions, budgets and goals.</div>
        </div>
        <div className="assistant-header-actions">
          {messages.length > 0 && (
            <button type="button" className="assistant-link" onClick={() => { setMessages([]); setError(null) }}>
              New chat
            </button>
          )}
          <button type="button" className="icon-button" aria-label="Close assistant" onClick={onClose}>
            <CloseIcon size={16} />
          </button>
        </div>
      </div>

      {off && <div className="form-error-banner assistant-banner">The assistant is switched off right now. The rest of the app works as usual.</div>}

      <div className="assistant-messages">
        {messages.length === 0 && (
          <div>
            <div className="assistant-hint">Ask me about your spending, budgets, goals or recurring payments. For example:</div>
            <div className="assistant-chips">
              {suggestions.map((suggestion) => (
                <button key={suggestion} type="button" className="assistant-chip" disabled={blocked} onClick={() => void send(suggestion)}>
                  {suggestion}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((message, index) => (
          <div key={index} className={`assistant-msg ${message.role}`}>
            <div className="assistant-bubble">{renderText(message.content)}</div>
            {message.role === 'assistant' && message.basedOn && message.basedOn.length > 0 && (
              <div className="assistant-note">Based on: {message.basedOn.map((t) => t.context).join(' · ')}</div>
            )}
            {message.sources && message.sources.length > 0 && (
              <details className="assistant-sources">
                <summary>Transactions looked at ({message.sources.length})</summary>
                <ul>
                  {message.sources.map((source, i) => (
                    <li key={i}>
                      <span className="assistant-source-main">
                        {source.description}
                        {source.merchant && source.merchant !== source.description ? ` · ${source.merchant}` : ''}
                      </span>
                      <span className="assistant-source-meta">
                        {formatDate(source.date)} · {formatCurrency(source.amount, source.currency)}
                      </span>
                    </li>
                  ))}
                </ul>
              </details>
            )}
            {message.fallback && (
              <div className="assistant-note">Shown as plain figures because the assistant's wording couldn't be verified against your data.</div>
            )}
          </div>
        ))}

        {sending && (
          <div className="assistant-note">
            <span className="spinner" /> Looking at your data…
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {error && <div className="form-error-banner assistant-banner">{error}</div>}
      {tooLong && <div className="assistant-note assistant-pad">This chat is getting long - start a new one to keep going.</div>}

      <form className="assistant-composer" onSubmit={handleSubmit}>
        <textarea
          ref={inputRef}
          aria-label="Your question"
          value={input}
          maxLength={1000}
          rows={2}
          placeholder="Ask about your money…"
          disabled={blocked}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault()
              void send(input)
            }
          }}
        />
        <button type="submit" className="assistant-send" aria-label="Send" disabled={sending || !input.trim() || blocked}>
          <SendIcon />
        </button>
      </form>
      {status?.enabled && (
        <div className="assistant-note assistant-pad assistant-allowance">
          {limitReached ? "You've used today's messages. They reset tomorrow." : `${status.remainingMessagesToday} of ${status.dailyMessageLimit} messages left today`}
        </div>
      )}
    </aside>
  )
}
