import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import ProfilePicker from '../components/ProfilePicker'
import { createRoom, fetchPublicRooms, peekRoom, type RoomSummary } from '../lib/api'
import { randomAvatar } from '../lib/avatars'
import { loadProfile, saveProfile } from '../lib/identity'

export default function HomePage() {
  const navigate = useNavigate()
  const existing = useMemo(() => loadProfile(), [])
  const [name, setName] = useState(existing?.name ?? '')
  const [avatar, setAvatar] = useState(existing?.avatar ?? randomAvatar())
  const [joinCode, setJoinCode] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [publicRooms, setPublicRooms] = useState<RoomSummary[]>([])

  useEffect(() => {
    fetchPublicRooms().then(setPublicRooms).catch(() => undefined)
  }, [])

  const ensureProfile = (): boolean => {
    if (!name.trim()) {
      setError('Pick a name first!')
      return false
    }
    saveProfile({ name: name.trim(), avatar })
    return true
  }

  const onCreate = async () => {
    if (!ensureProfile()) {
      return
    }
    setBusy(true)
    setError('')
    try {
      const code = await createRoom({})
      navigate(`/room/${code}`)
    } catch {
      setError('Could not create a room. Is the server running?')
      setBusy(false)
    }
  }

  const onJoin = async (raw: string) => {
    if (!ensureProfile()) {
      return
    }
    const code = raw.trim().toUpperCase()
    if (!code) {
      setError('Enter a room code')
      return
    }
    setBusy(true)
    setError('')
    const room = await peekRoom(code).catch(() => null)
    if (!room) {
      setError(`Room "${code}" not found`)
      setBusy(false)
      return
    }
    navigate(`/room/${code}`)
  }

  return (
    <div className="mx-auto flex min-h-screen max-w-md flex-col justify-center gap-5 p-4">
      <div className="text-center">
        <h1 className="text-5xl font-extrabold tracking-tight text-indigo-600 drop-shadow-sm">skribbl</h1>
        <p className="mt-1 font-semibold text-slate-500">Draw, guess, and win with friends!</p>
      </div>

      <div className="rounded-2xl bg-white/90 p-5 shadow-xl">
        <ProfilePicker name={name} setName={setName} avatar={avatar} setAvatar={setAvatar} />

        {error && <p className="mt-3 text-center text-sm font-semibold text-rose-500">{error}</p>}

        <button
          type="button"
          onClick={onCreate}
          disabled={busy}
          className="mt-4 w-full rounded-xl bg-indigo-500 py-3 text-lg font-extrabold text-white shadow transition hover:bg-indigo-600 disabled:opacity-60"
        >
          Create private room
        </button>

        <div className="mt-4 flex items-center gap-2">
          <input
            value={joinCode}
            onChange={(e) => setJoinCode(e.target.value.toUpperCase())}
            onKeyDown={(e) => e.key === 'Enter' && onJoin(joinCode)}
            placeholder="ROOM CODE"
            maxLength={5}
            className="w-full rounded-xl border-2 border-slate-200 px-4 py-3 text-center text-lg font-bold uppercase tracking-widest outline-none focus:border-indigo-400"
          />
          <button
            type="button"
            onClick={() => onJoin(joinCode)}
            disabled={busy}
            className="shrink-0 rounded-xl bg-slate-800 px-5 py-3 text-lg font-extrabold text-white transition hover:bg-slate-900 disabled:opacity-60"
          >
            Join
          </button>
        </div>
      </div>

      {publicRooms.length > 0 && (
        <div className="rounded-2xl bg-white/80 p-4 shadow">
          <h2 className="mb-2 text-sm font-bold uppercase tracking-wide text-slate-400">Public rooms</h2>
          <div className="flex flex-col gap-2">
            {publicRooms.map((r) => (
              <button
                key={r.code}
                type="button"
                onClick={() => onJoin(r.code)}
                className="flex items-center justify-between rounded-xl bg-slate-50 px-4 py-2 text-left hover:bg-slate-100"
              >
                <span className="font-bold tracking-widest text-indigo-600">{r.code}</span>
                <span className="text-sm font-semibold text-slate-500">
                  {r.players}/{r.maxPlayers} players · {r.rounds} rounds
                </span>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
