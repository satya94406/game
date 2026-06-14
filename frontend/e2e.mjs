// Two-client end-to-end gameplay test against the live backend.
// Uses the same libraries the browser app uses (SockJS + @stomp/stompjs).
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

const API = process.env.API_BASE || 'http://localhost:8080'
const WS = process.env.WS_URL || 'http://localhost:8080/ws'
const log = (...a) => console.log(...a)
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

let failures = 0
const assert = (cond, msg) => {
  if (cond) log('  ✓', msg)
  else {
    failures++
    log('  ✗ FAIL:', msg)
  }
}
const ofType = (t) => (m) => m.type === t

function makeClient(name, playerId, code) {
  const buf = []
  const waiters = []
  const client = new Client({ webSocketFactory: () => new SockJS(WS), reconnectDelay: 0 })
  const onMsg = (m) => {
    const idx = buf.length
    buf.push(m)
    for (let i = waiters.length - 1; i >= 0; i--) {
      const w = waiters[i]
      if (idx >= w.since && w.pred(m)) {
        waiters.splice(i, 1)
        w.resolve(m)
      }
    }
  }
  const ready = new Promise((resolve) => {
    client.onConnect = () => {
      client.subscribe(`/topic/room/${code}`, (f) => onMsg(JSON.parse(f.body)))
      client.subscribe(`/topic/room/${code}/u/${playerId}`, (f) => onMsg(JSON.parse(f.body)))
      resolve()
    }
  })
  client.activate()
  return {
    name,
    playerId,
    buf,
    ready,
    publish: (suffix, body = {}) =>
      client.publish({ destination: `/app/room/${code}/${suffix}`, body: JSON.stringify(body) }),
    waitFor: (pred, since = 0, timeout = 15000) =>
      new Promise((resolve, reject) => {
        for (let i = since; i < buf.length; i++) {
          if (pred(buf[i])) return resolve(buf[i])
        }
        const w = { pred, since, resolve }
        waiters.push(w)
        setTimeout(() => {
          const k = waiters.indexOf(w)
          if (k >= 0) {
            waiters.splice(k, 1)
            reject(new Error(`${name}: timed out waiting for message`))
          }
        }, timeout)
      }),
    deactivate: () => client.deactivate(),
  }
}

async function main() {
  const res = await fetch(`${API}/api/rooms`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ settings: { rounds: 2, drawTime: 40, hints: 1, wordCount: 3, maxPlayers: 8 } }),
  })
  const { code } = await res.json()
  log('\nRoom created:', code)

  const A = makeClient('A', crypto.randomUUID(), code)
  const B = makeClient('B', crypto.randomUUID(), code)
  await Promise.all([A.ready, B.ready])

  log('\n[join + broadcast]')
  A.publish('join', { playerId: A.playerId, name: 'Alice', avatar: { face: '🐶', color: '#f87171' } })
  const aJoined = await A.waitFor(ofType('JOINED'))
  assert(aJoined.data.hostId === A.playerId, 'first joiner (A) is host')
  B.publish('join', { playerId: B.playerId, name: 'Bob', avatar: { face: '🐱', color: '#60a5fa' } })
  await B.waitFor(ofType('JOINED'))
  const players2 = await A.waitFor((m) => m.type === 'PLAYERS' && m.data.players.length === 2)
  assert(players2.data.players.length === 2, 'both clients see 2 players (broadcast)')

  log('\n[start game]')
  let baseA = A.buf.length
  let baseB = B.buf.length
  A.publish('start')

  for (let turn = 1; turn <= 4; turn++) {
    const rs = await A.waitFor(ofType('ROUND_START'), baseA)
    const drawerId = rs.data.drawerId
    const D = drawerId === A.playerId ? A : B
    const G = drawerId === A.playerId ? B : A
    const baseD = drawerId === A.playerId ? baseA : baseB

    log(`\n[turn ${turn}] drawer=${D.name} guesser=${G.name}`)
    const wordOpts = await D.waitFor(ofType('WORD_OPTIONS'), baseD)
    assert(wordOpts.data.words.length > 0, `turn ${turn}: drawer gets private WORD_OPTIONS`)
    if (turn === 1) {
      assert(!B.buf.some(ofType('WORD_OPTIONS')), 'word options are NOT sent to the guesser')
    }
    const word = wordOpts.data.words[0]

    let gm = G.buf.length
    D.publish('word', { word })
    const gState = await G.waitFor((m) => m.type === 'STATE' && m.data.phase === 'DRAWING', gm)
    assert(gState.data.maskedWord.length === word.length, `turn ${turn}: guesser sees masked word, right length`)
    assert(!gState.data.maskedWord.toLowerCase().includes(word.toLowerCase()), `turn ${turn}: word hidden from guesser`)

    gm = G.buf.length
    D.publish('draw', {
      strokeId: crypto.randomUUID(), color: '#000', size: 8, eraser: false,
      points: [{ x: 0.1, y: 0.2 }, { x: 0.5, y: 0.6 }], done: true,
    })
    const gDraw = await G.waitFor(ofType('DRAW'), gm)
    assert(gDraw.data.points.length === 2, `turn ${turn}: drawer's stroke relayed to guesser (real-time draw)`)

    gm = G.buf.length
    G.publish('guess', { text: word })
    const gGuess = await G.waitFor((m) => m.type === 'GUESS' && m.data.correct, gm)
    assert(gGuess.data.playerId === G.playerId, `turn ${turn}: correct guess recognized`)
    const roundEnd = await G.waitFor(ofType('ROUND_END'), gm)
    assert(roundEnd.data.word.toLowerCase() === word.toLowerCase(), `turn ${turn}: ROUND_END reveals the word`)

    baseA = A.buf.length
    baseB = B.buf.length
  }

  log('\n[game over]')
  const over = await A.waitFor(ofType('GAME_OVER'), 0, 20000)
  assert(over.data.leaderboard.length === 2, 'GAME_OVER leaderboard lists both players')
  assert(!!over.data.winnerName, `GAME_OVER has a winner (${over.data.winnerName})`)
  assert(over.data.leaderboard[0].score > 0, 'winner has a positive score')

  log('\n[persistence]')
  await sleep(800)
  const history = await (await fetch(`${API}/api/history`)).json()
  assert(history.some((h) => h.roomCode === code), 'finished game persisted to MySQL (GameHistory)')

  A.deactivate()
  B.deactivate()
  log(`\n${failures === 0 ? 'ALL PASSED ✓' : failures + ' FAILURE(S) ✗'}`)
  process.exit(failures === 0 ? 0 : 1)
}

main().catch((e) => {
  console.error('FATAL', e)
  process.exit(1)
})
