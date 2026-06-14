import type { Avatar as AvatarType } from '../types/messages'

export default function Avatar({ avatar, size = 36 }: { avatar: AvatarType; size?: number }) {
  return (
    <div
      className="flex shrink-0 items-center justify-center rounded-full shadow-inner"
      style={{ width: size, height: size, background: avatar.color, fontSize: size * 0.55 }}
    >
      <span>{avatar.face}</span>
    </div>
  )
}
