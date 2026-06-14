# skribbl.io clone — build checklist

Stack: Spring Boot 3.5.15 (Java 21) + JPA/Hibernate + MySQL · React+Vite+TS+Tailwind · STOMP/SockJS.

## Phase 1 — Scaffold ✅
- [x] Monorepo dirs (backend/, frontend/, tasks/)
- [x] Spring Boot 3.5.15 backend via Initializr (Java 21, Maven wrapper)
- [x] docker-compose.yml (MySQL 8 on :3309)
- [x] .gitignore + README + git init

## Phase 2 — Backend boot + MySQL + word seed ✅
- [x] application.properties (datasource :3309, JPA ddl-auto=update)
- [x] Word entity + WordRepository + words.json + DataSeeder
- [x] VERIFIED: boots on Java 21, connects to MySQL, seeded 170 words

## Phase 3 — OOP engine + STOMP config + DTOs ✅
- [x] GamePhase, RoomSettings, Player, Game, Room, RoomManager
- [x] WebSocketConfig (/ws + SockJS, /app + /topic), CorsConfig, AppConfig (scheduler)
- [x] Inbound (In) + outbound (Out) DTO records, ServerMessage envelope, GameBroadcastService

## Phase 4 — Controllers + game logic ✅
- [x] RoomController (REST) + Room/Draw/Chat STOMP controllers
- [x] WordService, GameTimerService, GameHistoryService, GameService orchestrator
- [x] Scoring, hints, word-matching, disconnect handling, GameHistory persistence
- [x] VERIFIED: REST endpoints (create/peek/categories/history/public list, 404)

## Phase 5 — Frontend scaffold ✅
- [x] Vite + React + TS + Tailwind, react-router
- [x] stompClient (SockJS) inside GameContext, shared message types, lib helpers

## Phase 6 — Frontend UI + wire STOMP ✅
- [x] Home / Lobby / Game / GameOver, JoinGate for invite links
- [x] Canvas, Toolbar, ChatPanel, PlayerList, WordPicker, WordHint, Timer, RoomSettings, Avatar
- [x] All STOMP events wired; VERIFIED: tsc --noEmit + vite build pass

## Phase 7 — Tests + verify + README ✅
- [x] Backend unit tests (Game + Room) — 13/13 pass on H2
- [x] Two-client e2e (frontend/e2e.mjs) over real STOMP/SockJS — all assertions pass
- [x] README + architecture overview + run instructions

---

## Review

**What was built:** a complete, locally-runnable skribbl.io clone — Spring Boot backend
(in-memory OOP game engine + STOMP/SockJS realtime + MySQL for word bank & game history) and a
React/Vite/Tailwind frontend (lobby, live canvas, chat/guessing, scoring, leaderboard).

**Verification performed:**
- Backend unit tests: 13/13 pass (`./mvnw test`, H2-backed, no MySQL needed).
- Backend boots on Java 21, connects to MySQL, seeds 170 words; REST endpoints checked.
- End-to-end two-client game over real STOMP/SockJS (`node e2e.mjs`): join/broadcast,
  host assignment, drawer rotation (4 turns), drawer-private word options, masked word hidden
  from guessers, real-time stroke relay, correct-guess scoring, round-end reveal, game-over
  leaderboard, and MySQL persistence — all asserted green.
- Frontend: `tsc --noEmit` + `vite build` succeed.

**Not done (by design / limitation):**
- **Live visual/pixel testing of the React UI** was not automated (no browser was connected to
  the automation tools). The UI compiles, builds, and is wired to the e2e-verified message
  contract — but the user should click through it in the browser (two tabs).
- **Deployment** deferred per request — see README §Deployment.
- "Everything" tier (kick/ban/votekick, spectator, replay, custom words, word modes) out of scope.

**Decisions worth noting:**
- Spring Boot 3.5.15 (not 4.x) for the most battle-tested STOMP+SockJS support.
- TypeScript frontend (PDF's recommended stack; typed message contract prevents realtime bugs).
- In-memory game state + MySQL only for durable data (word bank, history) — never on the hot path.
- MySQL on host port 3309 (3306–3308 were already taken on this machine).
