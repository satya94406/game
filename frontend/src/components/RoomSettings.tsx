import { useEffect, useState } from 'react'
import { useGame } from '../context/GameContext'
import { fetchCategories } from '../lib/api'
import type { RoomSettings as Settings } from '../types/messages'

const DRAW_TIMES = [30, 45, 60, 80, 100, 120, 150, 180, 240]
const range = (lo: number, hi: number) => Array.from({ length: hi - lo + 1 }, (_, i) => lo + i)

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-3 py-1.5">
      <span className="text-sm font-semibold text-slate-600">{label}</span>
      {children}
    </div>
  )
}

const selectCls =
  'rounded-lg border border-slate-300 bg-white px-2 py-1 text-sm font-semibold text-slate-700 disabled:bg-slate-100 disabled:text-slate-500'

export default function RoomSettings() {
  const { settings, hostId, me, actions } = useGame()
  const isHost = me.id === hostId
  const [categories, setCategories] = useState<string[]>([])

  useEffect(() => {
    fetchCategories().then(setCategories).catch(() => setCategories([]))
  }, [])

  if (!settings) {
    return null
  }

  const update = (patch: Partial<Settings>) => actions.updateSettings({ ...settings, ...patch })
  const toggleCat = (c: string) => {
    if (!isHost) {
      return
    }
    const has = settings.categories.includes(c)
    update({ categories: has ? settings.categories.filter((x) => x !== c) : [...settings.categories, c] })
  }

  return (
    <div className="rounded-xl bg-white/80 p-4 shadow">
      <h3 className="mb-2 text-base font-bold text-slate-700">Settings</h3>

      <Row label="Players">
        <select className={selectCls} disabled={!isHost} value={settings.maxPlayers}
          onChange={(e) => update({ maxPlayers: Number(e.target.value) })}>
          {range(2, 20).map((n) => <option key={n} value={n}>{n}</option>)}
        </select>
      </Row>
      <Row label="Rounds">
        <select className={selectCls} disabled={!isHost} value={settings.rounds}
          onChange={(e) => update({ rounds: Number(e.target.value) })}>
          {range(2, 10).map((n) => <option key={n} value={n}>{n}</option>)}
        </select>
      </Row>
      <Row label="Draw time">
        <select className={selectCls} disabled={!isHost} value={settings.drawTime}
          onChange={(e) => update({ drawTime: Number(e.target.value) })}>
          {DRAW_TIMES.map((n) => <option key={n} value={n}>{n}s</option>)}
        </select>
      </Row>
      <Row label="Word choices">
        <select className={selectCls} disabled={!isHost} value={settings.wordCount}
          onChange={(e) => update({ wordCount: Number(e.target.value) })}>
          {range(1, 5).map((n) => <option key={n} value={n}>{n}</option>)}
        </select>
      </Row>
      <Row label="Hints">
        <select className={selectCls} disabled={!isHost} value={settings.hints}
          onChange={(e) => update({ hints: Number(e.target.value) })}>
          {range(0, 5).map((n) => <option key={n} value={n}>{n}</option>)}
        </select>
      </Row>
      <Row label="Public room">
        <input type="checkbox" className="h-5 w-5 accent-indigo-500" disabled={!isHost}
          checked={settings.publicRoom} onChange={(e) => update({ publicRoom: e.target.checked })} />
      </Row>

      <div className="mt-2">
        <span className="text-sm font-semibold text-slate-600">Categories</span>
        <div className="mt-1 flex flex-wrap gap-1.5">
          {categories.map((c) => {
            const active = settings.categories.length === 0 || settings.categories.includes(c)
            return (
              <button key={c} type="button" onClick={() => toggleCat(c)} disabled={!isHost}
                className={`rounded-full px-2.5 py-1 text-xs font-semibold capitalize transition ${
                  settings.categories.includes(c) ? 'bg-indigo-500 text-white' : active ? 'bg-indigo-100 text-indigo-700' : 'bg-slate-100 text-slate-400'
                } ${isHost ? '' : 'cursor-default'}`}>
                {c}
              </button>
            )
          })}
        </div>
        <p className="mt-1 text-xs text-slate-400">None selected = all categories.</p>
      </div>

      {!isHost && <p className="mt-3 text-xs italic text-slate-400">Only the host can change settings.</p>}
    </div>
  )
}
