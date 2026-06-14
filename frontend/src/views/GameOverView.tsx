import { useNavigate } from 'react-router-dom'
import { useGame } from '../context/GameContext'
import Avatar from '../components/Avatar'

const MEDALS = ['🥇', '🥈', '🥉']

export default function GameOverView() {
  const { gameOver, me, hostId, actions } = useGame()
  const navigate = useNavigate()
  const isHost = me.id === hostId
  const board = gameOver?.leaderboard ?? []

  return (
    <div className="mx-auto flex min-h-screen max-w-lg flex-col items-center justify-center gap-6 p-4">
      <div className="text-center">
        <div className="text-6xl">🏆</div>
        <h1 className="mt-2 text-3xl font-extrabold text-slate-800">
          {gameOver?.winnerName ? `${gameOver.winnerName} wins!` : 'Game over!'}
        </h1>
      </div>

      <div className="w-full rounded-2xl bg-white/90 p-4 shadow-xl">
        <h2 className="mb-2 text-center text-sm font-bold uppercase tracking-wide text-slate-400">
          Final leaderboard
        </h2>
        <div className="flex flex-col gap-1.5">
          {board.map((p, i) => (
            <div
              key={p.id}
              className={`flex items-center gap-3 rounded-xl px-3 py-2 ${
                i === 0 ? 'bg-amber-50 ring-1 ring-amber-200' : 'bg-slate-50'
              }`}
            >
              <span className="w-6 text-center text-lg">{MEDALS[i] ?? i + 1}</span>
              <Avatar avatar={p.avatar} size={34} />
              <span className="flex-1 truncate font-bold text-slate-700">
                {p.name}
                {p.id === me.id && <span className="ml-1 text-xs font-normal text-slate-400">(you)</span>}
              </span>
              <span className="font-extrabold text-indigo-600">{p.score}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="flex items-center gap-3">
        {isHost ? (
          <button
            type="button"
            onClick={() => actions.playAgain()}
            className="rounded-xl bg-emerald-500 px-6 py-3 text-lg font-extrabold text-white shadow hover:bg-emerald-600"
          >
            Play again
          </button>
        ) : (
          <span className="text-sm font-semibold text-slate-500">Waiting for the host…</span>
        )}
        <button
          type="button"
          onClick={() => navigate('/')}
          className="rounded-xl bg-white/80 px-6 py-3 text-lg font-bold text-slate-600 hover:bg-white"
        >
          Leave
        </button>
      </div>
    </div>
  )
}
