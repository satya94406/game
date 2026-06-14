import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useGame } from '../context/GameContext'
import PlayerList from '../components/PlayerList'
import RoomSettings from '../components/RoomSettings'

export default function LobbyView() {
  const { code, players, hostId, me, actions } = useGame()
  const navigate = useNavigate()
  const isHost = me.id === hostId
  const connectedCount = players.filter((p) => p.connected).length
  const canStart = isHost && connectedCount >= 2
  const [copied, setCopied] = useState(false)

  const link = `${window.location.origin}/room/${code}`
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(link)
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    } catch {
      /* clipboard may be blocked; ignore */
    }
  }

  return (
    <div className="mx-auto flex min-h-screen max-w-4xl flex-col gap-4 p-4">
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-white/85 p-4 shadow">
        <div>
          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Room code</div>
          <div className="text-3xl font-extrabold tracking-widest text-indigo-600">{code}</div>
        </div>
        <button
          type="button"
          onClick={copy}
          className="rounded-xl bg-slate-100 px-4 py-2 text-sm font-bold text-slate-700 hover:bg-slate-200"
        >
          {copied ? '✓ Copied link!' : '🔗 Copy invite link'}
        </button>
      </div>

      <div className="grid flex-1 gap-4 md:grid-cols-2">
        <div className="rounded-2xl bg-white/85 p-4 shadow">
          <h3 className="mb-2 text-base font-bold text-slate-700">
            Players <span className="text-slate-400">({connectedCount})</span>
          </h3>
          <PlayerList />
        </div>
        <RoomSettings />
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <button
          type="button"
          onClick={() => navigate('/')}
          className="rounded-xl bg-white/70 px-4 py-2 text-sm font-bold text-slate-600 hover:bg-white"
        >
          ← Leave
        </button>
        {isHost ? (
          <button
            type="button"
            disabled={!canStart}
            onClick={() => actions.start()}
            className="rounded-xl bg-emerald-500 px-8 py-3 text-lg font-extrabold text-white shadow transition hover:bg-emerald-600 disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {connectedCount < 2 ? 'Need 2+ players' : 'Start game'}
          </button>
        ) : (
          <span className="text-sm font-semibold text-slate-500">Waiting for the host to start…</span>
        )}
      </div>
    </div>
  )
}
