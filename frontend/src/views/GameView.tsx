import { useState } from 'react'
import { useGame } from '../context/GameContext'
import Canvas, { type Tool } from '../components/Canvas'
import Toolbar from '../components/Toolbar'
import PlayerList from '../components/PlayerList'
import ChatPanel from '../components/ChatPanel'
import WordHint from '../components/WordHint'
import WordPicker from '../components/WordPicker'
import Timer from '../components/Timer'

function RoundEndOverlay() {
  const { roundEnd } = useGame()
  if (!roundEnd) {
    return null
  }
  const gained = roundEnd.scores.filter((s) => s.gained > 0)
  return (
    <div className="absolute inset-0 z-10 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm">
      <div className="animate-pop rounded-2xl bg-white px-8 py-6 text-center shadow-2xl">
        <p className="text-sm font-semibold uppercase tracking-wide text-slate-400">The word was</p>
        <p className="mb-3 text-3xl font-extrabold text-indigo-600">{roundEnd.word}</p>
        <div className="mx-auto flex max-w-xs flex-col gap-1">
          {gained.length === 0 && <p className="text-sm text-slate-400">Nobody guessed it!</p>}
          {gained.map((s) => (
            <div key={s.playerId} className="flex justify-between text-sm font-semibold text-slate-700">
              <span>{s.playerName}</span>
              <span className="text-emerald-600">+{s.gained}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export default function GameView() {
  const { round, totalRounds, phase, isDrawer, timeLeft, totalTime, timerKey } = useGame()
  const [tool, setTool] = useState<Tool>({ color: '#000000', size: 10, eraser: false })

  return (
    <div className="mx-auto flex h-screen max-w-6xl flex-col gap-3 p-3">
      <div className="flex items-center justify-between gap-3 rounded-xl bg-white/85 px-4 py-2 shadow">
        <div className="text-sm font-bold text-slate-600">
          Round {round}/{totalRounds}
        </div>
        {phase === 'DRAWING' ? (
          <WordHint />
        ) : (
          <div className="text-sm font-semibold text-slate-400">
            {phase === 'CHOOSING' ? 'Choosing a word…' : 'Round over'}
          </div>
        )}
        <Timer key={timerKey} seconds={timeLeft} total={totalTime} />
      </div>

      <div className="flex min-h-0 flex-1 flex-col gap-3 lg:flex-row">
        <div className="flex min-h-0 flex-1 flex-col gap-2">
          <div className="relative min-h-0 flex-1">
            <Canvas tool={tool} />
            <WordPicker />
            {phase === 'ROUND_END' && <RoundEndOverlay />}
          </div>
          {isDrawer && phase === 'DRAWING' && <Toolbar tool={tool} setTool={setTool} />}
        </div>

        <div className="flex w-full shrink-0 flex-col gap-3 lg:h-full lg:w-72">
          <div className="max-h-48 overflow-y-auto rounded-xl bg-white/85 p-2 shadow lg:max-h-none lg:flex-1">
            <PlayerList />
          </div>
          <div className="flex min-h-[220px] flex-col rounded-xl bg-white/85 shadow lg:flex-1">
            <ChatPanel />
          </div>
        </div>
      </div>
    </div>
  )
}
