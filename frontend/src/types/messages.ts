
export type Phase = 'LOBBY' | 'CHOOSING' | 'DRAWING' | 'ROUND_END' | 'GAME_OVER'

export interface Avatar {
  face: string
  color: string
}

export interface PlayerDto {
  id: string
  name: string
  avatar: Avatar
  score: number
  host: boolean
  drawing: boolean
  guessed: boolean
  connected: boolean
}

export interface RoomSettings {
  maxPlayers: number
  rounds: number
  drawTime: number
  wordCount: number
  hints: number
  publicRoom: boolean
  language: string
  categories: string[]
}

export interface ServerMessage<T = unknown> {
  type: string
  data: T
  ts: number
}

export interface Point {
  x: number
  y: number
}

export interface DrawBatch {
  strokeId: string
  color: string
  size: number
  eraser: boolean
  points: Point[]
  done: boolean
}


export interface JoinedData {
  playerId: string
  code: string
  settings: RoomSettings
  players: PlayerDto[]
  hostId: string
  phase: Phase
}

export interface StateData {
  phase: Phase
  round: number
  totalRounds: number
  drawerId: string | null
  drawerName: string | null
  maskedWord: string
  wordLength: number
  timeLeft: number
  totalTime: number
  players: PlayerDto[]
  hostId: string
  settings: RoomSettings
  replay: ServerMessage[] | null
}

export interface RoundStartData {
  round: number
  totalRounds: number
  drawerId: string
  drawerName: string
  chooseTime: number
}

export interface WordOptionsData {
  words: string[]
  chooseTime: number
}

export interface GuessData {
  playerId: string
  playerName: string
  correct: boolean
  text: string | null
}

export interface ScoreRow {
  playerId: string
  playerName: string
  total: number
  gained: number
}

export interface RoundEndData {
  word: string
  scores: ScoreRow[]
  nextDrawerId: string | null
  reason: string
}

export interface GameOverData {
  leaderboard: PlayerDto[]
  winnerId: string | null
  winnerName: string | null
}


export type ChatKind = 'guess-correct' | 'guess-wrong' | 'chat' | 'system'

export interface ChatEntry {
  id: string
  kind: ChatKind
  playerName?: string
  text: string
}
