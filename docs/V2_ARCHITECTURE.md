# MATO v2 Architecture

## Why rebuild

The current project has drifted into three different problems at once:

- The frontend mixes legacy pages, multiple state models, and duplicated websocket hooks.
- The REST and websocket contracts do not match the backend implementation.
- The UI depends on utility-class patterns that are not actually wired into the build.

Trying to patch that piecemeal will keep recreating regressions. v2 should keep the domain idea and throw away the unstable interaction layer.

## Product scope for v2

Phase one only ships the core loop:

1. Lobby with room list
2. Room create and join
3. Ready toggle
4. Host starts game
5. Server streams round state
6. Player submits answers
7. Server validates score and advances rounds
8. Final scoreboard

Everything else is deferred:

- map editor polish
- media ingestion UX
- account settings polish
- admin tooling

## Core principles

- Server authority for game phase, current song, scoring, and winner calculation
- Single source of truth for room state snapshots
- One websocket contract for room realtime events
- Query-based REST reads for lobby and map metadata
- Client state only for view state and optimistic interaction
- Feature-based frontend structure instead of page-local ad hoc hooks

## Frontend structure

```text
src/
  app/
    layout/
    providers/
  features/
    lobby/
    room/
    maps/
    account/
    roadmap/
  shared/
    api/
    config/
    realtime/
    store/
    types/
```

Recommended frontend stack:

- React + Vite + TypeScript
- TanStack Query for server state
- Zustand for lightweight client session state
- Plain CSS or CSS modules first, then component-level styling only where needed

## Backend structure

Keep Spring Boot, but rebuild the room/game interaction model around explicit modules:

```text
controller/
  v2/
    LobbyController
    RoomsController
    GameSocketController
service/
  lobby/
  room/
  game/
  media/
domain/
  room/
  game/
  map/
  user/
infrastructure/
  redis/
  jpa/
  websocket/
```

Room presence and game runtime state should live in Redis-backed runtime objects. Persistent metadata stays in MySQL.

## REST contract

Base path: `/api/v2`

### Lobby

- `GET /lobby/rooms`
- `POST /rooms`
- `GET /rooms/{roomName}`

### Room membership

- `POST /rooms/{roomName}/join`
- `POST /rooms/{roomName}/leave`
- `POST /rooms/{roomName}/ready`

### Gameplay

- `POST /rooms/{roomName}/start`
- `POST /rooms/{roomName}/answer`
- `POST /rooms/{roomName}/next`
- `POST /rooms/{roomName}/finish`

### Maps

- `GET /maps`
- `GET /maps/{mapId}`
- `POST /maps`
- `PUT /maps/{mapId}`

## Websocket contract

Single channel: `/ws/game`

The client subscribes once per room and receives snapshot-driven updates.

Client event types:

- `room.join`
- `room.leave`
- `room.ready.set`
- `game.start`
- `game.answer.submit`
- `game.next.request`
- `presence.ping`

Server event types:

- `room.snapshot`
- `room.participant.changed`
- `game.phase.changed`
- `game.round.started`
- `game.answer.accepted`
- `game.answer.rejected`
- `game.score.changed`
- `game.finished`
- `error`

## Snapshot shape

Every important update should be able to rehydrate the room screen without extra guessing.

```json
{
  "roomName": "anime-rush",
  "phase": "PLAYING",
  "round": 3,
  "totalRounds": 10,
  "hostNickname": "host-01",
  "map": {
    "id": 12,
    "name": "Anime Rush",
    "songCount": 10,
    "difficulty": "normal"
  },
  "participants": [
    {
      "id": "u1",
      "nickname": "host-01",
      "ready": true,
      "score": 2,
      "connected": true
    }
  ],
  "currentPrompt": "Round 3 is live"
}
```

## Migration policy

- Do not adapt the old websocket event names.
- Do not keep dual REST contracts alive for long.
- Move one vertical slice at a time.
- Leave the old controllers in place only until the v2 room flow is stable.

## Implementation order

### Step 1

Ship the new frontend shell and route structure.

### Step 2

Implement backend `/api/v2/lobby/rooms`, `/api/v2/rooms`, `/api/v2/rooms/{roomName}`.

### Step 3

Implement `/ws/game` snapshot broadcasting and room membership events.

### Step 4

Wire room ready/start/answer flow end-to-end.

### Step 5

Bring back map creation and media ingestion on top of the stable core loop.
