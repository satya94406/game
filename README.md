# skribbl.io clone

A real-time multiplayer drawing & guessing game (a [skribbl.io](https://skribbl.io) clone).
Players join a room, take turns drawing a chosen word while everyone else races to guess it in
chat, points are awarded for guessing quickly, and the highest score after all rounds wins.

## What's used

| Layer       | Technology                                                  |
| ----------- | ----------------------------------------------------------- |
| Backend     | Java 21 · Spring Boot 3.5 · Spring WebSocket (STOMP)        |
| Persistence | JPA / Hibernate · MySQL 8                                    |
| Real-time   | STOMP over SockJS                                           |
| Frontend    | React 18 · Vite · TypeScript · Tailwind CSS · HTML5 Canvas  |
| Build / run | Maven (wrapper) · npm · Docker + Docker Compose             |

**Key libraries:** Spring Boot starters (`web`, `websocket`, `data-jpa`, `validation`), MySQL
Connector/J, H2 (tests) · React Router, `@stomp/stompjs`, `sockjs-client`. nginx serves the
frontend and proxies to the backend in the Docker setup.

> **Status:** runs locally (native or Docker); not yet deployed — see [Deployment](#deployment).

---

## Quick start on a new laptop

The easiest path needs **only [Docker Desktop](https://www.docker.com/products/docker-desktop/)** —
no Java, Node, or MySQL to install.

1. **Install Docker Desktop** and start it (wait until the whale icon says it's running).
2. **Get the project** onto the laptop:
   - on GitHub? &nbsp;`git clone <repo-url>` &nbsp;then&nbsp; `cd Game`
   - otherwise copy the whole `Game/` folder across (zip / USB / scp), then `cd Game`
3. **Start everything** with one command:
   ```sh
   docker compose up --build
   ```
   (First run downloads dependencies and builds the images — a few minutes. Later runs are cached
   and start in seconds.)
4. Open **http://localhost:8088**. Open a second tab for a second player — or play from
   **another laptop or your phone** on the same Wi-Fi by opening `http://<this-machine-ip>:8088`
   (use the IP, not `localhost` — see
   [Play from another laptop or phone](#play-from-another-laptop-or-phone-same-wi-fi)).

Stop it with `docker compose down` (add `-v` to also wipe the database). Prefer hot-reload while
developing? See [Run natively](#run-natively-for-hot-reload-development).

---

## How it works (architecture overview)

### In-memory game engine + MySQL for durable data

Live game state — active rooms, players, the current drawer, in-progress canvas strokes, live
scores, and round timers — lives **entirely in memory**, managed by a singleton `RoomManager`
over a `ConcurrentHashMap`. You cannot afford a database round-trip on every brush stroke or
guess, so the hot path never touches MySQL.

**MySQL (via JPA/Hibernate) holds the data that should outlive a game:**

- **Word bank** — the `words` table (word, category, language), seeded on first startup from
  [`words.json`](backend/src/main/resources/words.json); the drawer's choices come from here.
- **Game history** — the `game_history` table records each finished match (room code, rounds,
  players, winner, full leaderboard JSON, timestamp), surfaced at `GET /api/history`.

### OOP game model (the "bonus" architecture)

Game/room logic is encapsulated in framework-free, unit-testable classes:

- [`Player`](backend/src/main/java/com/skribbl/game/Player.java) — identity, score, flags.
- [`Room`](backend/src/main/java/com/skribbl/game/Room.java) — the durable lobby: players, settings, host, canvas draw-log, current `Game`.
- [`Game`](backend/src/main/java/com/skribbl/game/Game.java) — the active match: turn order, current word, hints, guesses, scoring.
- [`RoomManager`](backend/src/main/java/com/skribbl/game/RoomManager.java) — registry of all rooms + room-code generation.

Orchestration (timers, broadcasts, persistence) lives in the service layer
([`GameService`](backend/src/main/java/com/skribbl/service/GameService.java) is the hub), keeping the model classes pure.

### Real-time messaging (STOMP over SockJS)

The browser opens a SockJS connection to `/ws`. **Client → server** messages go to
`/app/room/{code}/...` (`@MessageMapping` controllers). **Server → clients** broadcasts go through
an in-memory broker to `/topic/room/{code}` (everyone) and `/topic/room/{code}/u/{playerId}` (one
player — e.g. the drawer's private word choices). Every server message is a typed envelope
`{ type, data, ts }`. A player's id is generated client-side, sent on `join`, and stored in the
STOMP session so later actions are attributed to them and disconnects are detected.

### Drawing sync

Pointer moves are captured as **normalized [0,1] coordinates** (identical rendering at any screen
size), batched (~40 ms), sent to the server, and **broadcast to everyone including the drawer**, so
all canvases stay pixel-identical. A late joiner receives the current draw-log as a `replay`
snapshot. See [`Canvas.tsx`](frontend/src/components/Canvas.tsx).

### Turns, scoring & word matching

`LOBBY → CHOOSING → DRAWING → ROUND_END → … → GAME_OVER`. A shared `TaskScheduler` drives the
word-choice timeout, draw countdown, hint reveals, and inter-turn gap; every transition cancels
prior timers first. A turn ends when time runs out *or* everyone guessed. A correct guesser earns
`50 + round(400 × fractionOfTimeLeft)`; the drawer earns based on how many guessed and how fast.
Guesses are normalized (`trim`/lowercase/collapse-spaces) and matched exactly; a single-edit
near-miss privately tells the guesser "you're close!".

---

## Run with Docker (one command)

Needs **only Docker** — Java and Node are not required on the host. From the project folder:

```sh
cd Game                          # the project folder
docker compose up --build        # first build downloads deps; later runs are cached
```

Open **http://localhost:8088** (or `http://<your-machine-ip>:8088` from another laptop or phone on
the same Wi-Fi — see [Play from another laptop or phone](#play-from-another-laptop-or-phone-same-wi-fi)). nginx
serves the built SPA and reverse-proxies `/api` and `/ws` to the backend container, which reaches
MySQL by service name — a single origin, no CORS, WebSockets upgraded through nginx. First boot
takes ~15–20s while the backend connects to MySQL; a request that 502s just needs a retry. Stop
with `docker compose down` (add `-v` to wipe the DB volume).

> The backend port isn't published (nginx reaches it internally). Uncomment the `ports:` block
> under `backend:` in [`docker-compose.yml`](docker-compose.yml) to hit the API directly on `:8080`.

---

## Run natively (for hot-reload development)

*Prerequisites (not needed for the Docker path):* **JDK 21** (any distribution — the repo has a
[`.sdkmanrc`](backend/.sdkmanrc) so [SDKMAN](https://sdkman.io) users can `cd backend && sdk env install`
once; Maven isn't needed, the `./mvnw` wrapper is bundled), **Node ≥ 20** + npm, and **Docker**
(just for the MySQL container, or your own MySQL 8).

Three processes — **MySQL → backend → frontend**:

```sh
# 1. MySQL (from the project root)
docker compose up -d mysql        # MySQL 8 on host port 3309, db "skribbl"

# 2. Backend (port 8080)
cd backend
sdk env                            # activate Java 21 (or set JAVA_HOME to any JDK 21)
./mvnw spring-boot:run

# 3. Frontend (Vite dev server, port 5173) — in a new terminal
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**. Vite proxies `/api` and `/ws` to the backend (single origin, no
CORS). Not using Docker for MySQL? Create a `skribbl` database in your own MySQL and override
`SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`.

---

## How to play

Open the app in **two browser tabs** (each tab is a separate player). In tab 1, pick a name +
avatar and **Create private room**; copy the invite link or 5-letter code into tab 2 (or just
open the link). With 2+ players in, the host clicks **Start**. Players take turns drawing while
the others guess in chat. Correct guesses score points (faster = more); after every player has
drawn across all rounds, the leaderboard crowns a winner.

### Play from another laptop or phone (same Wi-Fi)

From another device, **don't use `localhost`** — there `localhost` means *that* device. Use **this
machine's network IP** instead. Both the Docker frontend (**:8088**) and the native Vite server
(**:5173**) bind to all interfaces, so any laptop or phone on the same Wi-Fi can join.

1. Find this machine's IP address:
   - **macOS:** `ipconfig getifaddr en0`
   - **Linux:** `hostname -I | awk '{print $1}'`
   - **Windows:** `ipconfig` (use the IPv4 address)
2. In the browser on the other laptop / phone, open `http://<that-ip>:8088` (Docker) or
   `http://<that-ip>:5173` (native) — for example `http://192.168.1.5:8088`.
3. Share the room code or invite link from this machine, and join from the other device.

Allow the port through the firewall if prompted. All traffic flows through that one origin, so no
CORS setup is needed (`app.cors.allowed-origins` is `*` for dev — lock it down before deploying).

---

## Tests

**Backend unit tests** (engine: scoring, word-matching, hints, round rotation, room lifecycle) —
self-contained, use in-memory H2, so MySQL need not be running:

```sh
cd backend && ./mvnw test
```

**End-to-end test** — drives a full two-client game over real STOMP/SockJS and asserts the whole
flow (join, broadcast, drawer-private words, stroke relay, scoring, rounds, game-over, MySQL
persistence). Start the stack first, then:

```sh
cd frontend && node e2e.mjs                                             # native backend on :8080
# or against the Docker stack (through nginx):
API_BASE=http://localhost:8088 WS_URL=http://localhost:8088/ws node e2e.mjs
```

---

## WebSocket / REST reference

**Client → server** (`/app/room/{code}/…`): `join`, `leave`, `start`, `word`, `settings`,
`playAgain`, `draw`, `clear`, `undo`, `guess`, `chat`.

**Server → clients** (envelope `type`): `JOINED`, `PLAYERS`, `STATE`, `ROUND_START`,
`WORD_OPTIONS` (drawer-only), `DRAW`, `CLEAR`, `UNDO`, `GUESS`, `CHAT`, `HINT`, `ROUND_END`,
`GAME_OVER`, `SYSTEM`, `ERROR`.

**REST** (`/api`): `POST /rooms`, `GET /rooms`, `GET /rooms/{code}`, `GET /categories`, `GET /history`.

---

## Project structure

```
Game/
├── docker-compose.yml         # mysql + backend + frontend (one-command full stack)
├── backend/                   # Spring Boot 3.5 (Java 21, Maven wrapper)
│   ├── Dockerfile             # multi-stage: build jar -> slim JRE
│   └── src/main/java/com/skribbl/{config,game,controller,service,listener,entity,repository,dto,exception}
└── frontend/                  # React + Vite + TS + Tailwind
    ├── Dockerfile             # multi-stage: vite build -> nginx
    ├── nginx.conf             # serves SPA + proxies /api and /ws to the backend
    └── src/{pages,views,components,context,lib,types}
```

---

## Configuration

| Setting               | Default                          | Override                                            |
| --------------------- | -------------------------------- | --------------------------------------------------- |
| Frontend (Docker)     | http://localhost:8088 (nginx)    | `ports:` under `frontend:` in `docker-compose.yml`  |
| Frontend (native dev) | http://localhost:5173 (Vite)     | `vite.config.ts`                                    |
| Backend port          | 8080                             | `server.port`                                       |
| MySQL                 | host `:3309`, db `skribbl`       | `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` |
| Allowed origins (CORS)| `*` (dev)                        | `app.cors.allowed-origins`                          |

---

## Scope

**Implemented** — all Must-Have features (rooms, lobby, turn-based rounds, real-time stroke sync,
word selection, guessing, scoring + leaderboard, winner, brush/colors/undo/clear) plus Should-Have
(hints, chat, draw-time countdown, private rooms via code/link) plus bonuses (OOP server
architecture, configurable room settings, word categories, eraser, avatars, public-rooms browser).

**Not implemented** (could be added later): kick/ban/votekick moderation, spectator mode, last-round
replay, host custom word lists, and the Hidden/Combination word modes.

---

## Deployment

Not deployed yet. When ready, host the backend on a platform with **WebSocket support** (Render,
Railway, Fly.io — *not* Vercel/Netlify serverless for the WS server). The Docker images here are
deploy-ready; point the frontend at the backend origin (or keep the nginx reverse-proxy), and set
`app.cors.allowed-origins` to the real frontend origin. The in-memory STOMP broker is
single-instance; horizontal scaling would need an external broker (e.g. RabbitMQ).

---

## Notes

- `npm install` reports 2 advisories in `sockjs-client`'s transitive deps; harmless for local dev.
  Avoid `npm audit fix --force` (it force-bumps to breaking versions).
- Ports 3306–3308 are often already in use, so MySQL is mapped to host **3309**.
