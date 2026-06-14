import { useGame } from '../context/GameContext'
import Avatar from './Avatar'

export default function PlayerList() {
  const { players, me, drawerId, hostId } = useGame()

  return (
    <div className="flex flex-col gap-1">
      {players.map((p, i) => (
        <div
          key={p.id}
          className={`flex items-center gap-2 rounded-lg px-2 py-1.5 ${
            p.guessed ? 'bg-emerald-50' : 'bg-white'
          } ${p.connected ? '' : 'opacity-40'}`}
        >
          <span className="w-5 text-center text-xs font-bold text-slate-400">{i + 1}</span>
          <Avatar avatar={p.avatar} size={32} />
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-1 truncate text-sm font-bold text-slate-800">
              <span className="truncate">{p.name}</span>
              {p.id === me.id && <span className="text-xs font-normal text-slate-400">(you)</span>}
              {p.id === hostId && <span title="host">👑</span>}
            </div>
            <div className="text-xs text-slate-500">{p.score} pts</div>
          </div>
          {p.id === drawerId && <span title="drawing">✏️</span>}
          {p.guessed && <span title="guessed" className="text-emerald-500">✓</span>}
        </div>
      ))}
    </div>
  )
}
