import type { RoomSettings } from '../types/messages'

// Empty base => same origin; the Vite dev server proxies /api to the backend.
const BASE = (import.meta.env.VITE_API_BASE as string) || ''

export async function createRoom(settings: Partial<RoomSettings>): Promise<string> {
  const res = await fetch(`${BASE}/api/rooms`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ settings }),
  })
  if (!res.ok) {
    throw new Error('Could not create room')
  }
  const json = await res.json()
  return json.code as string
}

export interface RoomSummary {
  code: string
  players: number
  maxPlayers: number
  rounds: number
  phase?: string
  started?: boolean
  full?: boolean
}

export async function peekRoom(code: string): Promise<RoomSummary | null> {
  const res = await fetch(`${BASE}/api/rooms/${code}`)
  if (res.status === 404) {
    return null
  }
  if (!res.ok) {
    throw new Error('Could not look up room')
  }
  return res.json()
}

export async function fetchPublicRooms(): Promise<RoomSummary[]> {
  const res = await fetch(`${BASE}/api/rooms`)
  return res.ok ? res.json() : []
}

export async function fetchCategories(): Promise<string[]> {
  const res = await fetch(`${BASE}/api/categories`)
  return res.ok ? res.json() : []
}
