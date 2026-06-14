import { useEffect, useState } from 'react'

/**
 * Local countdown. Remount it with a `key` whenever the turn/phase changes so it
 * restarts cleanly; it also resets if the `seconds` prop changes.
 */
export default function Timer({ seconds, total }: { seconds: number; total: number }) {
  const [remaining, setRemaining] = useState(seconds)

  useEffect(() => {
    setRemaining(seconds)
    const id = setInterval(() => setRemaining((r) => (r > 0 ? r - 1 : 0)), 1000)
    return () => clearInterval(id)
  }, [seconds])

  const pct = total > 0 ? Math.max(0, (remaining / total) * 100) : 0
  const urgent = remaining <= 10

  return (
    <div className="flex items-center gap-2">
      <div
        className={`flex h-10 w-10 items-center justify-center rounded-full bg-white font-extrabold shadow ${
          urgent ? 'text-rose-500' : 'text-slate-700'
        }`}
      >
        {remaining}
      </div>
      <div className="hidden h-2 w-28 overflow-hidden rounded-full bg-white/40 sm:block">
        <div
          className={`h-full transition-all duration-1000 ease-linear ${urgent ? 'bg-rose-400' : 'bg-emerald-400'}`}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  )
}
