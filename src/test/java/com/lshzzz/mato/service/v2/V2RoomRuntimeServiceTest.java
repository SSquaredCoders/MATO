package com.lshzzz.mato.service.v2;

import static org.assertj.core.api.Assertions.assertThat;

import com.lshzzz.mato.model.v2.V2CreateRoomRequest;
import com.lshzzz.mato.model.v2.V2GamePhase;
import org.junit.jupiter.api.Test;

class V2RoomRuntimeServiceTest {

    private final V2RoomRuntimeService roomRuntimeService = new V2RoomRuntimeService();

    @Test
    void createsRoomWithNormalizedName() {
        var snapshot = roomRuntimeService.createRoom(
            new V2CreateRoomRequest(" Ranked Demo ", "guest-host")
        );

        assertThat(snapshot.roomName()).isEqualTo("ranked-demo");
        assertThat(snapshot.hostNickname()).isEqualTo("guest-host");
        assertThat(snapshot.participants()).isEmpty();
        assertThat(snapshot.phase()).isEqualTo(V2GamePhase.LOBBY);
    }

    @Test
    void startsDemoRoomAndAdvancesAfterCorrectAnswer() {
        roomRuntimeService.joinRoom("session-host", "demo-room", "host-01");
        roomRuntimeService.joinRoom("session-guest", "demo-room", "guest-01");
        roomRuntimeService.setReady("demo-room", "guest-01", true);

        var started = roomRuntimeService.startGame("demo-room", "host-01");
        assertThat(started.snapshot()).isNotNull();
        assertThat(started.snapshot().phase()).isEqualTo(V2GamePhase.PLAYING);
        assertThat(started.snapshot().round()).isEqualTo(1);

        var answered = roomRuntimeService.submitAnswer(
            "demo-room",
            "guest-01",
            "A Cruel Angel's Thesis"
        );

        assertThat(answered.type()).isEqualTo("game.answer.accepted");
        assertThat(answered.snapshot()).isNotNull();
        assertThat(answered.snapshot().round()).isEqualTo(2);
        assertThat(answered.chatMessage()).isNotNull();
        assertThat(answered.chatMessage().content()).isEqualTo("A Cruel Angel's Thesis");
        assertThat(answered.chatMessage().tone()).isEqualTo("correct");
        assertThat(answered.snapshot().participants())
            .filteredOn(participant -> participant.nickname().equals("guest-01"))
            .singleElement()
            .satisfies(participant -> assertThat(participant.score()).isEqualTo(1));
    }

    @Test
    void removesGhostParticipantsFromDemoRoomSnapshot() {
        var initialSnapshot = roomRuntimeService.getRoomSnapshot("demo-room");

        assertThat(initialSnapshot.participants()).isEmpty();
        assertThat(roomRuntimeService.getLobbyRooms())
            .singleElement()
            .extracting("participantCount")
            .isEqualTo(0);

        roomRuntimeService.joinRoom("session-host", "demo-room", "host-01");

        var joinedSnapshot = roomRuntimeService.getRoomSnapshot("demo-room");
        assertThat(joinedSnapshot.participants())
            .extracting(participant -> participant.nickname())
            .containsExactly("host-01");

        roomRuntimeService.handleDisconnect("session-host");

        var disconnectedSnapshot = roomRuntimeService.getRoomSnapshot("demo-room");
        assertThat(disconnectedSnapshot.participants()).isEmpty();
        assertThat(roomRuntimeService.getLobbyRooms())
            .singleElement()
            .extracting("participantCount")
            .isEqualTo(0);
    }

    @Test
    void storesLobbyChatMessagesBeforeGameStarts() {
        roomRuntimeService.joinRoom("session-host", "demo-room", "host-01");

        var chatEvent = roomRuntimeService.submitAnswer("demo-room", "host-01", "테스트 채팅");

        assertThat(chatEvent.type()).isEqualTo("room.chat.message");
        assertThat(chatEvent.snapshot()).isNotNull();
        assertThat(chatEvent.chatMessage()).isNotNull();
        assertThat(chatEvent.chatMessage().content()).isEqualTo("테스트 채팅");
        assertThat(chatEvent.chatMessage().visibility()).isEqualTo("public");
    }
}
