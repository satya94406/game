import type { Avatar } from '../types/messages'

const PID_KEY = 'skribbl_player_id'
const PROFILE_KEY = 'skribbl_profile'

/**
 * A stable per-tab player id. sessionStorage (not localStorage) is deliberate:
 * a second browser tab gets its own identity, which makes it trivial to test a
 * multiplayer game with two tabs on one machine.
 */
export function getPlayerId(): string {
  let id = sessionStorage.getItem(PID_KEY)
  if (!id) {
    id = crypto.randomUUID()
    sessionStorage.setItem(PID_KEY, id)
  }
  return id
}

export interface Profile {
  name: string
  avatar: Avatar
}

export function saveProfile(profile: Profile): void {
  sessionStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
}

export function loadProfile(): Profile | null {
  const raw = sessionStorage.getItem(PROFILE_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as Profile
  } catch {
    return null
  }
}
