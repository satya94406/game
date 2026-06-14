import { useGame } from '../context/GameContext'
import type { Tool } from './Canvas'

const COLORS = [
  '#000000', '#6b7280', '#ef4444', '#f97316', '#eab308', '#22c55e',
  '#14b8a6', '#3b82f6', '#8b5cf6', '#ec4899', '#a16207', '#ffffff',
]
const SIZES = [4, 10, 18, 30]

export default function Toolbar({ tool, setTool }: { tool: Tool; setTool: (t: Tool) => void }) {
  const { actions } = useGame()

  return (
    <div className="flex flex-wrap items-center gap-2 rounded-xl bg-white/80 p-2 shadow">
      <div className="flex flex-wrap gap-1">
        {COLORS.map((c) => (
          <button
            key={c}
            type="button"
            onClick={() => setTool({ ...tool, color: c, eraser: false })}
            className={`h-7 w-7 rounded-md border transition ${
              tool.color === c && !tool.eraser ? 'ring-2 ring-slate-800 ring-offset-1' : 'border-slate-300'
            }`}
            style={{ background: c }}
            aria-label={`color ${c}`}
          />
        ))}
      </div>

      <div className="mx-1 h-8 w-px bg-slate-200" />

      <div className="flex gap-1">
        {SIZES.map((s) => (
          <button
            key={s}
            type="button"
            onClick={() => setTool({ ...tool, size: s })}
            className={`flex h-8 w-8 items-center justify-center rounded-md bg-slate-100 ${
              tool.size === s ? 'ring-2 ring-slate-800' : ''
            }`}
            aria-label={`brush size ${s}`}
          >
            <span className="rounded-full bg-slate-800" style={{ width: s / 1.4, height: s / 1.4 }} />
          </button>
        ))}
      </div>

      <div className="mx-1 h-8 w-px bg-slate-200" />

      <button
        type="button"
        onClick={() => setTool({ ...tool, eraser: !tool.eraser })}
        className={`rounded-md px-3 py-1.5 text-sm font-semibold transition ${
          tool.eraser ? 'bg-amber-400 text-white' : 'bg-slate-100 text-slate-700'
        }`}
      >
        🧽 Eraser
      </button>
      <button
        type="button"
        onClick={() => actions.undo()}
        className="rounded-md bg-slate-100 px-3 py-1.5 text-sm font-semibold text-slate-700"
      >
        ↶ Undo
      </button>
      <button
        type="button"
        onClick={() => actions.clearCanvas()}
        className="rounded-md bg-rose-100 px-3 py-1.5 text-sm font-semibold text-rose-700"
      >
        🗑 Clear
      </button>
    </div>
  )
}
