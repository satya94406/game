import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useGame } from '../context/GameContext'
import type { ChatEntry } from '../types/messages'

function ChatLine({ m }: { m: ChatEntry }) {
  if (m.kind === 'system') {
    return <div className="px-1 py-0.5 text-center text-xs italic text-slate-400">{m.text}</div>
  }
  if (m.kind === 'guess-correct') {
    return (
      <div className="rounded px-2 py-0.5 text-sm font-semibold text-emerald-600">{m.text}</div>
    )
  }
  return (
    <div className="px-2 py-0.5 text-sm text-slate-700">
      <span className="font-bold text-slate-900">{m.playerName}: </span>
      {m.text}
    </div>
  )
}

export default function ChatPanel() {
  const { messages, actions, isDrawer, phase, me, players } = useGame()
  const [text, setText] = useState('')
  const endRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const meGuessed = players.find((p) => p.id === me.id)?.guessed ?? false
  const drawing = phase === 'DRAWING'
  const disabled = drawing && (isDrawer || meGuessed)
  const placeholder = isDrawer && drawing
    ? "You're drawing — shhh!"
    : meGuessed && drawing
      ? 'You guessed it! 🎉'
      : 'Type your guess…'

  const submit = (e: FormEvent) => {
    e.preventDefault()
    const t = text.trim()
    if (!t) {
      return
    }
    actions.guess(t)
    setText('')
  }

  return (
    <div className="flex h-full flex-col">
      <div className="scrollbar-thin flex-1 space-y-0.5 overflow-y-auto py-1">
        {messages.length === 0 && (
          <div className="px-2 py-4 text-center text-xs text-slate-400">
            Guesses and chat appear here.
          </div>
        )}
        {messages.map((m) => (
          <ChatLine key={m.id} m={m} />
        ))}
        <div ref={endRef} />
      </div>
      <form onSubmit={submit} className="border-t border-slate-200 p-2">
        <input
          value={text}
          onChange={(e) => setText(e.target.value)}
          disabled={disabled}
          placeholder={placeholder}
          maxLength={120}
          className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-400 disabled:bg-slate-100 disabled:text-slate-400"
        />
      </form>
    </div>
  )
}
