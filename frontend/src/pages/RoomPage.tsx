import { useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { GameProvider, useGame } from '../context/GameContext'
import ProfilePicker from '../components/ProfilePicker'
import { randomAvatar } from '../lib/avatars'
import { loadProfile, saveProfile, type Profile } from '../lib/identity'
import LobbyView from '../views/LobbyView'
import GameView from '../views/GameView'
import GameOverView from '../views/GameOverView'

function ConnectingBanner() {
  return (
    <div className="fixed inset-x-0 top-0 z-50 bg-amber-400 py-1 text-center text-sm font-bold text-amber-900">
      Connecting…
    </div>
  )
}

function ErrorToast({ message, onClose }: { message: string; onClose: () => void }) {
  return (
    <div className="fixed left-1/2 top-4 z-50 -translate-x-1/2 rounded-xl bg-rose-500 px-4 py-2 text-sm font-bold text-white shadow-lg">
      {message}
      <button type="button" onClick={onClose} className="ml-3 opacity-80 hover:opacity-100">
        ✕
      </button>
    </div>
  )
}

function RoomScreens() {
  const { phase, connected, error, actions } = useGame()
  return (
    <>
      {!connected && <ConnectingBanner />}
      {error && <ErrorToast message={error} onClose={actions.clearError} />}
      {phase === 'LOBBY' && <LobbyView />}
      {(phase === 'CHOOSING' || phase === 'DRAWING' || phase === 'ROUND_END') && <GameView />}
      {phase === 'GAME_OVER' && <GameOverView />}
    </>
  )
}

function JoinGate({ code, onReady }: { code: string; onReady: (p: Profile) => void }) {
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [avatar, setAvatar] = useState(randomAvatar())

  return (
    <div className="mx-auto flex min-h-screen max-w-md flex-col justify-center gap-4 p-4">
      <h1 className="text-center text-2xl font-extrabold text-slate-700">
        Joining room <span className="tracking-widest text-indigo-600">{code}</span>
      </h1>
      <div className="rounded-2xl bg-white/90 p-5 shadow-xl">
        <ProfilePicker name={name} setName={setName} avatar={avatar} setAvatar={setAvatar} />
        <button
          type="button"
          disabled={!name.trim()}
          onClick={() => onReady({ name: name.trim(), avatar })}
          className="mt-4 w-full rounded-xl bg-indigo-500 py-3 text-lg font-extrabold text-white shadow transition hover:bg-indigo-600 disabled:opacity-60"
        >
          Join game
        </button>
        <button
          type="button"
          onClick={() => navigate('/')}
          className="mt-2 w-full rounded-xl bg-slate-100 py-2 text-sm font-bold text-slate-600"
        >
          ← Back
        </button>
      </div>
    </div>
  )
}

export default function RoomPage() {
  const { code = '' } = useParams()
  const upperCode = useMemo(() => code.toUpperCase(), [code])
  const [profile, setProfile] = useState<Profile | null>(() => loadProfile())

  if (!profile) {
    return (
      <JoinGate
        code={upperCode}
        onReady={(p) => {
          saveProfile(p)
          setProfile(p)
        }}
      />
    )
  }

  return (
    <GameProvider code={upperCode} profile={profile}>
      <RoomScreens />
    </GameProvider>
  )
}
