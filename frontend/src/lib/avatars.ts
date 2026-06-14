import type { Avatar } from '../types/messages'

export const AVATAR_FACES = [
  '🐵', '🐶', '🐱', '🦊', '🐼', '🐸', '🐧', '🦄',
  '🐙', '🐯', '🐰', '🐲', '🦁', '🐨', '🐷', '🐹',
]

export const AVATAR_COLORS = [
  '#f87171', '#fb923c', '#fbbf24', '#a3e635', '#34d399',
  '#22d3ee', '#60a5fa', '#a78bfa', '#f472b6',
]

export function randomAvatar(): Avatar {
  const face = AVATAR_FACES[Math.floor(Math.random() * AVATAR_FACES.length)]
  const color = AVATAR_COLORS[Math.floor(Math.random() * AVATAR_COLORS.length)]
  return { face, color }
}
