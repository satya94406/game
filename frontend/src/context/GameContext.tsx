import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useReducer,
  useRef,
  useState,
} from 'react'
import type { ReactNode } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { Profile } from '../lib/identity'
import { getPlayerId } from '../lib/identity'
import type {
  ChatEntry,
  DrawBatch,
  Phase,
  PlayerDto,
  RoomSettings,
  ScoreRow,
  ServerMessage,
} from '../types/messages'

const WS_URL = (import.meta.env.VITE_WS_URL as string) || '/ws'

// Canvas traffic is kept out of React state (it is too high-frequency); the Canvas
// component subscribes to this event stream directly.
export type CanvasEvent =
  | { kind: 'DRAW'; batch: DrawBatch }
  | { kind: 'CLEAR' }
  | { kind: 'UNDO' }

interface GameState {
  code: string
  phase: Phase
  round: number
  totalRounds: number
  drawerId: string | null
  drawerName: string | null
  maskedWord: string
  wordLength: number
  timeLeft: number
  totalTime: number
  timerKey: string
  players: PlayerDto[]
  hostId: string | null
  settings: RoomSettings | null
  wordOptions: string[] | null
  chosenWord: string | null
  chooseTime: number
  messages: ChatEntry[]
  roundEnd: { word: string; scores: ScoreRow[] } | null
  gameOver: { leaderboard: PlayerDto[]; winnerName: string | null } | null
  replay: ServerMessage[] | null
  error: string | null
}

const initialState: GameState = {
  code: '',
  phase: 'LOBBY',
  round: 0,
  totalRounds: 0,
  drawerId: null,
  drawerName: null,
  maskedWord: '',
  wordLength: 0,
  timeLeft: 0,
  totalTime: 0,
  timerKey: 'init',
  players: [],
  hostId: null,
  settings: null,
  wordOptions: null,
  chosenWord: null,
  chooseTime: 15,
  messages: [],
  roundEnd: null,
  gameOver: null,
  replay: null,
  error: null,
}

interface Action {
  type: string
  data?: any
}

const MAX_MESSAGES = 200
const rid = () => crypto.randomUUID()
const cap = (list: ChatEntry[]) => (list.length > MAX_MESSAGES ? list.slice(-MAX_MESSAGES) : list)

function reducer(state: GameState, action: Action): GameState {
  const { type, data } = action
  switch (type) {
    case 'JOINED':
      return {
        ...state,
        code: data.code,
        settings: data.settings,
        players: data.players,
        hostId: data.hostId,
        phase: data.phase,
      }
    case 'PLAYERS':
      return { ...state, players: data.players, hostId: data.hostId }
    case 'STATE': {
      const next: GameState = {
        ...state,
        phase: data.phase,
        round: data.round,
        totalRounds: data.totalRounds,
        drawerId: data.drawerId,
        drawerName: data.drawerName,
        maskedWord: data.maskedWord,
        wordLength: data.wordLength,
        timeLeft: data.timeLeft,
        totalTime: data.totalTime,
        timerKey: `${data.phase}-${data.round}-${data.drawerId}-${data.timeLeft}`,
        players: data.players,
        hostId: data.hostId,
        settings: data.settings,
      }
      if (data.replay) {
        next.replay = data.replay
      }
      if (data.phase === 'LOBBY') {
        next.roundEnd = null
        next.gameOver = null
        next.chosenWord = null
        next.wordOptions = null
      }
      return next
    }
    case 'ROUND_START':
      return {
        ...state,
        phase: 'CHOOSING',
        round: data.round,
        totalRounds: data.totalRounds,
        drawerId: data.drawerId,
        drawerName: data.drawerName,
        chooseTime: data.chooseTime,
        timeLeft: data.chooseTime,
        totalTime: data.chooseTime,
        timerKey: `CHOOSING-${data.round}-${data.drawerId}`,
        wordOptions: null,
        chosenWord: null,
        maskedWord: '',
        roundEnd: null,
      }
    case 'WORD_OPTIONS':
      return { ...state, wordOptions: data.words, chooseTime: data.chooseTime }
    case 'LOCAL_CHOSE_WORD':
      return { ...state, chosenWord: data, wordOptions: null }
    case 'HINT':
      return { ...state, maskedWord: data.maskedWord }
    case 'GUESS': {
      const entry: ChatEntry = data.correct
        ? { id: rid(), kind: 'guess-correct', playerName: data.playerName, text: `${data.playerName} guessed the word!` }
        : { id: rid(), kind: 'guess-wrong', playerName: data.playerName, text: data.text ?? '' }
      return { ...state, messages: cap([...state.messages, entry]) }
    }
    case 'CHAT':
      return {
        ...state,
        messages: cap([...state.messages, { id: rid(), kind: 'chat', playerName: data.playerName, text: data.text }]),
      }
    case 'SYSTEM':
      return { ...state, messages: cap([...state.messages, { id: rid(), kind: 'system', text: data.text }]) }
    case 'ROUND_END':
      return {
        ...state,
        phase: 'ROUND_END',
        roundEnd: { word: data.word, scores: data.scores },
        messages: cap([...state.messages, { id: rid(), kind: 'system', text: `The word was "${data.word}"` }]),
      }
    case 'GAME_OVER':
      return {
        ...state,
        phase: 'GAME_OVER',
        gameOver: { leaderboard: data.leaderboard, winnerName: data.winnerName },
      }
    case 'ERROR':
      return {
        ...state,
        error: data.message,
        messages: cap([...state.messages, { id: rid(), kind: 'system', text: `⚠ ${data.message}` }]),
      }
    case 'CLEAR_ERROR':
      return { ...state, error: null }
    default:
      return state
  }
}

interface GameActions {
  start(): void
  chooseWord(word: string): void
  guess(text: string): void
  chat(text: string): void
  updateSettings(settings: RoomSettings): void
  playAgain(): void
  sendDraw(batch: DrawBatch): void
  clearCanvas(): void
  undo(): void
  clearError(): void
}

interface GameContextValue extends GameState {
  me: { id: string; name: string; avatar: Profile['avatar'] }
  isDrawer: boolean
  connected: boolean
  actions: GameActions
  subscribeCanvas(handler: (event: CanvasEvent) => void): () => void
}

const GameContext = createContext<GameContextValue | null>(null)

export function GameProvider({
  code,
  profile,
  children,
}: {
  code: string
  profile: Profile
  children: ReactNode
}) {
  const [state, dispatch] = useReducer(reducer, initialState)
  const [connected, setConnected] = useState(false)

  const clientRef = useRef<Client | null>(null)
  const canvasHandlers = useRef<Set<(event: CanvasEvent) => void>>(new Set())
  const playerId = useMemo(() => getPlayerId(), [])

  const emitCanvas = useCallback((event: CanvasEvent) => {
    canvasHandlers.current.forEach((handler) => handler(event))
  }, [])

  const subscribeCanvas = useCallback((handler: (event: CanvasEvent) => void) => {
    canvasHandlers.current.add(handler)
    return () => {
      canvasHandlers.current.delete(handler)
    }
  }, [])

  const handleMessage = useCallback(
    (message: ServerMessage) => {
      switch (message.type) {
        case 'DRAW':
          emitCanvas({ kind: 'DRAW', batch: message.data as DrawBatch })
          return
        case 'CLEAR':
          emitCanvas({ kind: 'CLEAR' })
          return
        case 'UNDO':
          emitCanvas({ kind: 'UNDO' })
          return
        case 'ROUND_START':
          emitCanvas({ kind: 'CLEAR' }) // fresh canvas for the new turn
          dispatch({ type: message.type, data: message.data })
          return
        default:
          dispatch({ type: message.type, data: message.data })
      }
    },
    [emitCanvas],
  )

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL) as never,
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    })

    client.onConnect = () => {
      setConnected(true)
      client.subscribe(`/topic/room/${code}`, (frame) => handleMessage(JSON.parse(frame.body)))
      client.subscribe(`/topic/room/${code}/u/${playerId}`, (frame) => handleMessage(JSON.parse(frame.body)))
      client.publish({
        destination: `/app/room/${code}/join`,
        body: JSON.stringify({ playerId, name: profile.name, avatar: profile.avatar }),
      })
    }
    client.onDisconnect = () => setConnected(false)
    client.onWebSocketClose = () => setConnected(false)

    client.activate()
    clientRef.current = client

    return () => {
      try {
        if (client.connected) {
          client.publish({ destination: `/app/room/${code}/leave`, body: '{}' })
        }
      } catch {
        /* ignore */
      }
      client.deactivate()
      clientRef.current = null
    }
  }, [code, playerId, profile.name, profile.avatar, handleMessage])

  const publish = useCallback((suffix: string, body: unknown = {}) => {
    const client = clientRef.current
    if (client && client.connected) {
      client.publish({ destination: `/app/room/${code}/${suffix}`, body: JSON.stringify(body) })
    }
  }, [code])

  const actions = useMemo<GameActions>(
    () => ({
      start: () => publish('start'),
      chooseWord: (word: string) => {
        dispatch({ type: 'LOCAL_CHOSE_WORD', data: word })
        publish('word', { word })
      },
      guess: (text: string) => publish('guess', { text }),
      chat: (text: string) => publish('chat', { text }),
      updateSettings: (settings: RoomSettings) => publish('settings', { settings }),
      playAgain: () => publish('playAgain'),
      sendDraw: (batch: DrawBatch) => publish('draw', batch),
      clearCanvas: () => publish('clear'),
      undo: () => publish('undo'),
      clearError: () => dispatch({ type: 'CLEAR_ERROR' }),
    }),
    [publish],
  )

  const value: GameContextValue = {
    ...state,
    me: { id: playerId, name: profile.name, avatar: profile.avatar },
    isDrawer: state.drawerId === playerId,
    connected,
    actions,
    subscribeCanvas,
  }

  return <GameContext.Provider value={value}>{children}</GameContext.Provider>
}

export function useGame(): GameContextValue {
  const ctx = useContext(GameContext)
  if (!ctx) {
    throw new Error('useGame must be used within a GameProvider')
  }
  return ctx
}
