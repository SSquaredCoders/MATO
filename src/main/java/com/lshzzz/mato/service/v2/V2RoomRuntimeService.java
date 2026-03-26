package com.lshzzz.mato.service.v2;

import com.lshzzz.mato.model.v2.V2ClientEnvelope;
import com.lshzzz.mato.model.v2.V2CreateRoomRequest;
import com.lshzzz.mato.model.v2.V2GamePhase;
import com.lshzzz.mato.model.v2.V2MapSummary;
import com.lshzzz.mato.model.v2.V2RoomChatMessage;
import com.lshzzz.mato.model.v2.V2RoomParticipant;
import com.lshzzz.mato.model.v2.V2RoomSnapshot;
import com.lshzzz.mato.model.v2.V2RoomSummary;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class V2RoomRuntimeService {

    private static final String DEMO_ROOM_NAME = "demo-room";
    private static final String DEMO_HOST_NICKNAME = "host-01";
    private static final String LOBBY_PROMPT = "준비를 마치고 방장이 게임을 시작할 때까지 기다리세요.";
    private static final V2MapSummary DEMO_MAP = new V2MapSummary(
        12L,
        "Anime Rush",
        4,
        "normal",
        "public"
    );

    private final Map<String, RuntimeRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, SessionMembership> sessionMemberships = new ConcurrentHashMap<>();
    private final Object monitor = new Object();

    public V2RoomRuntimeService() {
        RuntimeRoom demoRoom = buildRoom(DEMO_ROOM_NAME, DEMO_HOST_NICKNAME, true);
        demoRoom.lastEvent = "데모 방입니다. 접속한 사람만 참가자로 표시됩니다.";
        appendSystemMessage(demoRoom, demoRoom.lastEvent);
        rooms.put(demoRoom.roomName, demoRoom);
    }

    public List<V2RoomSummary> getLobbyRooms() {
        synchronized (monitor) {
            return rooms.values().stream()
                .sorted(Comparator.comparing(room -> room.roomName))
                .map(this::toSummary)
                .toList();
        }
    }

    public V2RoomSnapshot getRoomSnapshot(String roomName) {
        synchronized (monitor) {
            return toSnapshot(getRequiredRoom(roomName));
        }
    }

    public V2RoomSnapshot createRoom(V2CreateRoomRequest request) {
        synchronized (monitor) {
            String roomName = sanitizeRoomName(request.roomName());
            String hostNickname = sanitizeNickname(request.hostNickname(), DEMO_HOST_NICKNAME);

            if (rooms.containsKey(roomName)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 방입니다.");
            }

            RuntimeRoom room = buildRoom(roomName, hostNickname, false);
            room.lastEvent = hostNickname + "님이 방을 만들었습니다.";
            appendSystemMessage(room, room.lastEvent);
            rooms.put(roomName, room);
            return toSnapshot(room);
        }
    }

    public EventResult handleEvent(V2ClientEnvelope envelope) {
        return handleEvent(null, envelope);
    }

    public EventResult handleEvent(String sessionId, V2ClientEnvelope envelope) {
        String roomName = sanitizeRoomName(envelope.roomName());
        Map<String, Object> payload = envelope.payload() == null ? Map.of() : envelope.payload();

        return switch (Objects.requireNonNullElse(envelope.type(), "")) {
            case "room.join" -> joinRoom(sessionId, roomName, extractNickname(payload));
            case "room.leave" -> leaveRoom(sessionId, roomName, extractNickname(payload));
            case "room.ready.set" -> setReady(
                roomName,
                extractNickname(payload),
                extractBoolean(payload, "ready")
            );
            case "game.start" -> startGame(roomName, extractNickname(payload));
            case "game.answer.submit" -> submitAnswer(
                roomName,
                extractNickname(payload),
                extractString(payload, "answer")
            );
            case "game.next.request" -> nextRound(roomName, extractNickname(payload));
            case "presence.ping" -> snapshotEvent(roomName, "현재 방 상태를 새로고침했습니다.");
            default -> errorEvent(roomName, "지원하지 않는 클라이언트 이벤트입니다.");
        };
    }

    public EventResult handleDisconnect(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }

        synchronized (monitor) {
            SessionMembership membership = sessionMemberships.remove(sessionId);
            if (membership == null) {
                return null;
            }

            RuntimeRoom room = rooms.get(membership.roomName());
            if (room == null) {
                return null;
            }

            RuntimeParticipant participant = findParticipant(room, membership.nickname());
            if (participant == null) {
                return null;
            }

            participant.connected = false;
            participant.ready = false;
            room.lastEvent = membership.nickname() + "님의 연결이 끊어졌습니다.";
            appendSystemMessage(room, room.lastEvent);
            reassignHostIfNeeded(room);

            if (activeParticipantCount(room) == 0) {
                return clearOrRemoveIfEmpty(room, room.lastEvent);
            }

            return successEvent(
                "room.participant.changed",
                room,
                room.lastEvent,
                membership.nickname(),
                true
            );
        }
    }

    public EventResult joinRoom(String roomName, String nickname) {
        return joinRoom(null, roomName, nickname);
    }

    public EventResult joinRoom(String sessionId, String roomName, String nickname) {
        synchronized (monitor) {
            RuntimeRoom room = getRequiredRoom(roomName);
            String safeNickname = sanitizeNickname(
                nickname,
                "guest-" + (room.participants.size() + 1)
            );

            detachPreviousMembership(sessionId, roomName, safeNickname);

            RuntimeParticipant existingParticipant = findParticipant(room, safeNickname);
            if (existingParticipant != null) {
                boolean reconnected = !existingParticipant.connected;
                existingParticipant.connected = true;
                registerMembership(sessionId, roomName, safeNickname);
                room.lastEvent = safeNickname
                    + (reconnected ? "님이 다시 입장했습니다." : "님 상태를 동기화했습니다.");
                appendSystemMessage(room, room.lastEvent);
                reassignHostIfNeeded(room);
                return successEvent(
                    "room.participant.changed",
                    room,
                    room.lastEvent,
                    safeNickname,
                    true
                );
            }

            if (activeParticipantCount(room) >= room.maxParticipants) {
                return errorEvent(roomName, "방이 가득 찼습니다.");
            }

            room.participants.add(new RuntimeParticipant(safeNickname, false, true));
            registerMembership(sessionId, roomName, safeNickname);
            room.lastEvent = safeNickname + "님이 입장했습니다.";
            appendSystemMessage(room, room.lastEvent);
            reassignHostIfNeeded(room);
            return successEvent("room.participant.changed", room, room.lastEvent, safeNickname, true);
        }
    }

    public EventResult leaveRoom(String roomName, String nickname) {
        return leaveRoom(null, roomName, nickname);
    }

    public EventResult leaveRoom(String sessionId, String roomName, String nickname) {
        synchronized (monitor) {
            RuntimeRoom room = getRequiredRoom(roomName);
            String safeNickname = resolveNickname(sessionId, roomName, nickname);
            if (safeNickname == null) {
                return errorEvent(roomName, "닉네임이 필요합니다.");
            }

            unregisterMembership(sessionId, roomName, safeNickname);

            boolean removed = room.participants.removeIf(
                participant -> participant.nickname.equals(safeNickname)
            );
            if (!removed) {
                return errorEvent(roomName, "방 안에 해당 참가자가 없습니다.");
            }

            room.lastEvent = safeNickname + "님이 방을 나갔습니다.";
            appendSystemMessage(room, room.lastEvent);
            reassignHostIfNeeded(room);

            if (activeParticipantCount(room) == 0) {
                return clearOrRemoveIfEmpty(room, room.lastEvent);
            }

            return successEvent("room.participant.changed", room, room.lastEvent, safeNickname, true);
        }
    }

    public EventResult setReady(String roomName, String nickname, boolean ready) {
        synchronized (monitor) {
            RuntimeRoom room = getRequiredRoom(roomName);
            RuntimeParticipant participant = getRequiredConnectedParticipant(room, nickname);

            if (room.phase != V2GamePhase.LOBBY) {
                return errorEvent(roomName, "준비 상태는 대기 화면에서만 바꿀 수 있습니다.");
            }

            participant.ready = ready;
            room.lastEvent = participant.nickname
                + (ready ? "님이 준비를 완료했습니다." : "님이 준비를 해제했습니다.");
            return successEvent("room.participant.changed", room, room.lastEvent, participant.nickname, true);
        }
    }

    public EventResult startGame(String roomName, String nickname) {
        synchronized (monitor) {
            RuntimeRoom room = getRequiredRoom(roomName);
            String safeNickname = sanitizeNickname(nickname, null);

            if (!room.hostNickname.equals(safeNickname)) {
                return errorEvent(roomName, "방장만 게임을 시작할 수 있습니다.");
            }

            RuntimeParticipant host = getRequiredConnectedParticipant(room, safeNickname);

            if (room.phase != V2GamePhase.LOBBY) {
                return errorEvent(roomName, "이미 게임이 시작되었습니다.");
            }

            pruneDisconnectedParticipants(room);

            List<RuntimeParticipant> connectedParticipants = getConnectedParticipants(room);
            boolean everyoneReady = connectedParticipants.stream()
                .allMatch(participant -> participant.nickname.equals(host.nickname) || participant.ready);

            if (connectedParticipants.size() < 2 || !everyoneReady) {
                return errorEvent(
                    roomName,
                    "최소 2명 이상 필요하고, 게스트 전원이 준비 완료여야 합니다."
                );
            }

            connectedParticipants.forEach(participant -> participant.score = 0);
            room.phase = V2GamePhase.PLAYING;
            room.round = 1;
            room.currentReveal = null;
            room.currentPrompt = room.songs.getFirst().clue;
            room.lastEvent = "게임이 시작되었습니다.";
            appendSystemMessage(room, room.lastEvent);
            return successEvent("game.phase.changed", room, room.lastEvent, safeNickname, true);
        }
    }

    public EventResult submitAnswer(String roomName, String nickname, String answer) {
        synchronized (monitor) {
            RuntimeRoom room = getRequiredRoom(roomName);
            RuntimeParticipant participant = getRequiredConnectedParticipant(room, nickname);
            String safeAnswer = Objects.requireNonNullElse(answer, "").trim();

            if (safeAnswer.isBlank()) {
                return errorEvent(roomName, "정답을 입력하세요.");
            }

            if (room.phase != V2GamePhase.PLAYING) {
                V2RoomChatMessage chatMessage = buildChatMessage(
                    room.roomName,
                    participant.nickname,
                    safeAnswer,
                    "chat",
                    "public"
                );
                room.lastEvent = participant.nickname + "님이 채팅을 보냈습니다.";
                return successEvent(
                    "room.chat.message",
                    room,
                    participant.nickname + "님이 채팅을 보냈습니다.",
                    participant.nickname,
                    null,
                    chatMessage
                );
            }

            RuntimeSong currentSong = room.songs.get(room.round - 1);
            boolean correct = currentSong.answers.stream()
                .map(this::normalize)
                .anyMatch(candidate -> normalize(safeAnswer).contains(candidate));
            V2RoomChatMessage chatMessage = buildChatMessage(
                room.roomName,
                participant.nickname,
                correct ? currentSong.title + " - " + currentSong.artist : safeAnswer,
                correct ? "correct" : "chat",
                "public"
            );

            if (!correct) {
                room.lastEvent = participant.nickname + "님이 \"" + safeAnswer + "\" 를 입력했습니다.";
                return successEvent(
                    "room.chat.message",
                    room,
                    "오답입니다. 계속 맞혀보세요.",
                    participant.nickname,
                    false,
                    chatMessage
                );
            }

            participant.score += 1;
            room.currentReveal = currentSong.title + " - " + currentSong.artist;

            if (room.round >= room.songs.size()) {
                room.phase = V2GamePhase.FINISHED;
                room.currentPrompt = "게임이 종료되었습니다. 최종 점수가 확정되었습니다.";
                RuntimeParticipant winner = getConnectedParticipants(room).stream()
                    .max(Comparator.comparingInt(candidate -> candidate.score))
                    .orElse(participant);
                room.lastEvent = "우승: " + winner.nickname + " (" + winner.score + "점)";
                appendSystemMessage(room, room.lastEvent);
                return successEvent(
                    "game.finished",
                    room,
                    participant.nickname + "님의 정답: " + currentSong.title,
                    participant.nickname,
                    true,
                    chatMessage
                );
            }

            room.round += 1;
            room.currentPrompt = room.songs.get(room.round - 1).clue;
            room.lastEvent = room.round + "라운드가 시작되었습니다.";
            appendSystemMessage(room, room.lastEvent);
            return successEvent(
                "game.answer.accepted",
                room,
                participant.nickname + "님의 정답: " + currentSong.title,
                participant.nickname,
                true,
                chatMessage
            );
        }
    }

    public EventResult nextRound(String roomName, String nickname) {
        synchronized (monitor) {
            RuntimeRoom room = getRequiredRoom(roomName);
            RuntimeParticipant participant = getRequiredConnectedParticipant(room, nickname);

            if (!room.hostNickname.equals(participant.nickname)) {
                return errorEvent(roomName, "방장만 다음 라운드로 넘길 수 있습니다.");
            }

            if (room.phase != V2GamePhase.PLAYING) {
                return errorEvent(roomName, "게임이 진행 중이 아닙니다.");
            }

            RuntimeSong currentSong = room.songs.get(room.round - 1);
            room.currentReveal = currentSong.title + " - " + currentSong.artist;

            if (room.round >= room.songs.size()) {
                room.phase = V2GamePhase.FINISHED;
                room.currentPrompt = "게임이 종료되었습니다. 최종 점수가 확정되었습니다.";
                RuntimeParticipant winner = getConnectedParticipants(room).stream()
                    .max(Comparator.comparingInt(candidate -> candidate.score))
                    .orElse(participant);
                room.lastEvent = "우승: " + winner.nickname + " (" + winner.score + "점)";
                appendSystemMessage(room, room.lastEvent);
                return successEvent("game.finished", room, "게임이 종료되었습니다.", participant.nickname, true);
            }

            room.round += 1;
            room.currentPrompt = room.songs.get(room.round - 1).clue;
            room.lastEvent = room.round + "라운드가 시작되었습니다.";
            appendSystemMessage(room, room.lastEvent);
            return successEvent("game.round.started", room, room.lastEvent, participant.nickname, true);
        }
    }

    public EventResult snapshotEvent(String roomName, String message) {
        synchronized (monitor) {
            RuntimeRoom room = getRequiredRoom(roomName);
            return successEvent("room.snapshot", room, message, null, true);
        }
    }

    private EventResult successEvent(
        String type,
        RuntimeRoom room,
        String message,
        String actorNickname,
        Boolean accepted
    ) {
        return successEvent(type, room, message, actorNickname, accepted, null);
    }

    private EventResult successEvent(
        String type,
        RuntimeRoom room,
        String message,
        String actorNickname,
        Boolean accepted,
        V2RoomChatMessage chatMessage
    ) {
        return new EventResult(
            type,
            room.roomName,
            toSnapshot(room),
            message,
            actorNickname,
            accepted,
            chatMessage
        );
    }

    private EventResult errorEvent(String roomName, String message) {
        RuntimeRoom room = rooms.get(roomName);
        V2RoomSnapshot snapshot = room == null ? null : toSnapshot(room);
        return new EventResult("error", roomName, snapshot, message, null, false, null);
    }

    private RuntimeRoom buildRoom(String roomName, String hostNickname, boolean persistent) {
        RuntimeRoom room = new RuntimeRoom();
        room.roomName = roomName;
        room.initialHostNickname = hostNickname;
        room.hostNickname = hostNickname;
        room.persistent = persistent;
        room.phase = V2GamePhase.LOBBY;
        room.maxParticipants = 6;
        room.round = 0;
        room.currentPrompt = LOBBY_PROMPT;
        room.currentReveal = null;
        room.lastEvent = "로비 대기 중";
        room.map = DEMO_MAP;
        room.participants.add(new RuntimeParticipant(hostNickname, false, false));
        room.songs.addAll(List.of(
            new RuntimeSong(
                "A Cruel Angel's Thesis",
                "Yoko Takahashi",
                "문제: 일본 애니메이션 에반게리온 오프닝입니다. 곡 제목을 입력하세요.",
                List.of("a cruel angel's thesis", "zankoku na tenshi no thesis")
            ),
            new RuntimeSong(
                "Gurenge",
                "LiSA",
                "문제: 귀멸의 칼날 1기 오프닝입니다.",
                List.of("gurenge")
            ),
            new RuntimeSong(
                "Again",
                "YUI",
                "문제: 강철의 연금술사 브라더후드 1기 오프닝입니다.",
                List.of("again")
            ),
            new RuntimeSong(
                "Silhouette",
                "KANA-BOON",
                "문제: 나루토 질풍전 16기 오프닝입니다.",
                List.of("silhouette")
            )
        ));
        return room;
    }

    private RuntimeRoom getRequiredRoom(String roomName) {
        RuntimeRoom room = rooms.get(roomName);
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "방을 찾을 수 없습니다.");
        }
        return room;
    }

    private RuntimeParticipant getRequiredConnectedParticipant(RuntimeRoom room, String nickname) {
        RuntimeParticipant participant = findParticipant(room, sanitizeNickname(nickname, null));
        if (participant == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "참가자를 찾을 수 없습니다.");
        }
        if (!participant.connected) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 연결된 참가자가 아닙니다.");
        }
        return participant;
    }

    private RuntimeParticipant findParticipant(RuntimeRoom room, String nickname) {
        if (nickname == null) {
            return null;
        }

        return room.participants.stream()
            .filter(participant -> participant.nickname.equals(nickname))
            .findFirst()
            .orElse(null);
    }

    private void registerMembership(String sessionId, String roomName, String nickname) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        sessionMemberships.put(sessionId, new SessionMembership(roomName, nickname));
    }

    private void unregisterMembership(String sessionId, String roomName, String nickname) {
        if (sessionId != null && !sessionId.isBlank()) {
            sessionMemberships.remove(sessionId);
        }

        sessionMemberships.entrySet().removeIf(entry ->
            entry.getValue().roomName().equals(roomName) && entry.getValue().nickname().equals(nickname)
        );
    }

    private String resolveNickname(String sessionId, String roomName, String nickname) {
        String safeNickname = sanitizeNickname(nickname, null);
        if (safeNickname != null) {
            return safeNickname;
        }

        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }

        SessionMembership membership = sessionMemberships.get(sessionId);
        if (membership == null || !membership.roomName().equals(roomName)) {
            return null;
        }

        return membership.nickname();
    }

    private void detachPreviousMembership(String sessionId, String roomName, String nickname) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        SessionMembership membership = sessionMemberships.get(sessionId);
        if (membership == null) {
            return;
        }

        if (membership.roomName().equals(roomName) && membership.nickname().equals(nickname)) {
            return;
        }

        RuntimeRoom previousRoom = rooms.get(membership.roomName());
        if (previousRoom == null) {
            sessionMemberships.remove(sessionId);
            return;
        }

        RuntimeParticipant previousParticipant = findParticipant(previousRoom, membership.nickname());
        if (previousParticipant != null) {
            previousParticipant.connected = false;
            previousParticipant.ready = false;
            previousRoom.lastEvent = membership.nickname() + "님의 연결 상태를 정리했습니다.";
            appendSystemMessage(previousRoom, previousRoom.lastEvent);
            reassignHostIfNeeded(previousRoom);
            if (activeParticipantCount(previousRoom) == 0) {
                clearOrRemoveIfEmpty(previousRoom, previousRoom.lastEvent);
            }
        }

        sessionMemberships.remove(sessionId);
    }

    private void reassignHostIfNeeded(RuntimeRoom room) {
        RuntimeParticipant currentHost = findParticipant(room, room.hostNickname);
        if (currentHost != null && currentHost.connected) {
            return;
        }

        RuntimeParticipant nextHost = room.participants.stream()
            .filter(participant -> participant.connected)
            .findFirst()
            .orElse(null);

        if (nextHost != null) {
            room.hostNickname = nextHost.nickname;
            return;
        }

        room.hostNickname = room.initialHostNickname;
    }

    private EventResult clearOrRemoveIfEmpty(RuntimeRoom room, String message) {
        unregisterAllMemberships(room.roomName);

        if (!room.persistent) {
            rooms.remove(room.roomName);
            return new EventResult("room.participant.changed", room.roomName, null, message, null, true, null);
        }

        resetPersistentRoom(room);
        room.lastEvent = message;
        appendSystemMessage(room, message);
        return successEvent("room.participant.changed", room, message, null, true);
    }

    private void unregisterAllMemberships(String roomName) {
        sessionMemberships.entrySet().removeIf(entry -> entry.getValue().roomName().equals(roomName));
    }

    private void resetPersistentRoom(RuntimeRoom room) {
        room.hostNickname = room.initialHostNickname;
        room.phase = V2GamePhase.LOBBY;
        room.round = 0;
        room.currentPrompt = LOBBY_PROMPT;
        room.currentReveal = null;
        room.participants.clear();
        room.participants.add(new RuntimeParticipant(room.initialHostNickname, false, false));
    }

    private List<RuntimeParticipant> getConnectedParticipants(RuntimeRoom room) {
        return room.participants.stream()
            .filter(participant -> participant.connected)
            .toList();
    }

    private int activeParticipantCount(RuntimeRoom room) {
        return (int) room.participants.stream()
            .filter(participant -> participant.connected)
            .count();
    }

    private void pruneDisconnectedParticipants(RuntimeRoom room) {
        room.participants.removeIf(participant -> !participant.connected);
    }

    private void appendPlayerMessage(RuntimeRoom room, String nickname, String content, String tone) {
    }

    private V2RoomChatMessage buildChatMessage(
        String roomName,
        String nickname,
        String content,
        String tone,
        String visibility
    ) {
        if (content == null || content.isBlank()) {
            return null;
        }

        return new V2RoomChatMessage(
            roomName + "-chat-" + System.nanoTime(),
            nickname,
            content,
            tone,
            visibility
        );
    }

    private void appendSystemMessage(RuntimeRoom room, String content) {
    }

    private V2RoomSummary toSummary(RuntimeRoom room) {
        return new V2RoomSummary(
            room.roomName,
            room.hostNickname,
            activeParticipantCount(room),
            room.maxParticipants,
            room.phase,
            room.map
        );
    }

    private V2RoomSnapshot toSnapshot(RuntimeRoom room) {
        List<V2RoomParticipant> participants = room.participants.stream()
            .filter(participant -> participant.connected)
            .sorted(Comparator.comparing((RuntimeParticipant participant) -> participant.nickname))
            .map(participant -> new V2RoomParticipant(
                participant.nickname,
                participant.nickname,
                participant.ready,
                participant.score,
                participant.connected
            ))
            .toList();

        return new V2RoomSnapshot(
            room.roomName,
            room.hostNickname,
            room.phase,
            room.map,
            room.maxParticipants,
            room.round,
            room.songs.size(),
            room.currentPrompt,
            room.lastEvent,
            room.currentReveal,
            participants
        );
    }

    private String sanitizeRoomName(String roomName) {
        String candidate = Objects.requireNonNullElse(roomName, "")
            .trim()
            .toLowerCase()
            .replaceAll("\\s+", "-");

        if (candidate.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "방 이름이 필요합니다.");
        }

        return candidate;
    }

    private String sanitizeNickname(String nickname, String fallback) {
        String candidate = Objects.requireNonNullElse(nickname, "").trim();
        return candidate.isBlank() ? fallback : candidate;
    }

    private String extractNickname(Map<String, Object> payload) {
        return extractString(payload, "nickname");
    }

    private String extractString(Map<String, Object> payload, String key) {
        Object rawValue = payload.get(key);
        return rawValue == null ? null : String.valueOf(rawValue);
    }

    private boolean extractBoolean(Map<String, Object> payload, String key) {
        Object rawValue = payload.get(key);
        if (rawValue instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(String.valueOf(rawValue));
    }

    private String normalize(String value) {
        return Objects.requireNonNullElse(value, "").trim().toLowerCase();
    }

    public record EventResult(
        String type,
        String roomName,
        V2RoomSnapshot snapshot,
        String message,
        String actorNickname,
        Boolean accepted,
        V2RoomChatMessage chatMessage
    ) {
    }

    private static final class RuntimeRoom {
        private String roomName;
        private String initialHostNickname;
        private String hostNickname;
        private boolean persistent;
        private V2GamePhase phase;
        private int maxParticipants;
        private int round;
        private String currentPrompt;
        private String currentReveal;
        private String lastEvent;
        private V2MapSummary map;
        private final List<RuntimeParticipant> participants = new ArrayList<>();
        private final List<RuntimeSong> songs = new ArrayList<>();
    }

    private static final class RuntimeParticipant {
        private final String nickname;
        private boolean ready;
        private int score;
        private boolean connected;

        private RuntimeParticipant(String nickname, boolean ready, boolean connected) {
            this.nickname = nickname;
            this.ready = ready;
            this.score = 0;
            this.connected = connected;
        }
    }

    private record RuntimeSong(
        String title,
        String artist,
        String clue,
        List<String> answers
    ) {
    }

    private record SessionMembership(
        String roomName,
        String nickname
    ) {
    }
}
