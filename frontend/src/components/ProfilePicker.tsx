import { AVATAR_COLORS, AVATAR_FACES } from '../lib/avatars'
import type { Avatar } from '../types/messages'

interface Props {
  name: string
  setName: (name: string) => void
  avatar: Avatar
  setAvatar: (avatar: Avatar) => void
}

export default function ProfilePicker({ name, setName, avatar, setAvatar }: Props) {
  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-3">
        <div
          className="flex h-16 w-16 shrink-0 items-center justify-center rounded-full text-3xl shadow-inner"
          style={{ background: avatar.color }}
        >
          {avatar.face}
        </div>
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          maxLength={16}
          placeholder="Your name"
          className="w-full rounded-xl border-2 border-slate-200 px-4 py-3 text-lg font-bold text-slate-700 outline-none focus:border-indigo-400"
        />
      </div>

      <div className="flex flex-wrap gap-1.5">
        {AVATAR_FACES.map((f) => (
          <button
            key={f}
            type="button"
            onClick={() => setAvatar({ ...avatar, face: f })}
            className={`h-9 w-9 rounded-lg text-xl transition ${
              avatar.face === f ? 'bg-indigo-100 ring-2 ring-indigo-500' : 'bg-slate-100 hover:bg-slate-200'
            }`}
          >
            {f}
          </button>
        ))}
      </div>

      <div className="flex flex-wrap gap-1.5">
        {AVATAR_COLORS.map((c) => (
          <button
            key={c}
            type="button"
            onClick={() => setAvatar({ ...avatar, color: c })}
            className={`h-7 w-7 rounded-full transition ${
              avatar.color === c ? 'ring-2 ring-slate-800 ring-offset-1' : ''
            }`}
            style={{ background: c }}
            aria-label={`avatar color ${c}`}
          />
        ))}
      </div>
    </div>
  )
}
