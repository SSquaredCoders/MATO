package com.lshzzz.mato.service.v2;

import static org.assertj.core.api.Assertions.assertThat;

import com.lshzzz.mato.model.v2.V2CreateMapRequest;
import com.lshzzz.mato.model.v2.V2CreateRoomRequest;
import com.lshzzz.mato.model.v2.V2GamePhase;
import com.lshzzz.mato.model.v2.V2MapSongDefinition;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(
    classes = V2ServiceTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class V2RoomRuntimeServiceTest {

    @Autowired
    private V2MapCatalogService mapCatalogService;

    @Autowired
    private V2RoomRuntimeService roomRuntimeService;

    @BeforeEach
    void setUp() {
        mapCatalogService.resetForTests();
    }

    @Test
    void createsRoomWithNormalizedName() {
        var snapshot = roomRuntimeService.createRoom(
            new V2CreateRoomRequest(" Ranked Demo ", "guest-host", null)
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
        assertThat(started.snapshot().currentPrompt()).isNotBlank();
        assertThat(started.snapshot().currentHint()).isNotBlank();
        assertThat(started.snapshot().hintRevealAt()).isNotBlank();

        var answered = roomRuntimeService.submitAnswer(
            "demo-room",
            "guest-01",
            "zankoku na tenshi no thesis"
        );

        assertThat(answered.type()).isEqualTo("game.answer.accepted");
        assertThat(answered.snapshot()).isNotNull();
        assertThat(answered.snapshot().round()).isEqualTo(2);
        assertThat(answered.chatMessage()).isNotNull();
        assertThat(answered.chatMessage().content()).isEqualTo("A Cruel Angel's Thesis - Yoko Takahashi");
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

        assertThat(roomRuntimeService.getLobbyRooms()).isEmpty();

        var recreatedSnapshot = roomRuntimeService.getRoomSnapshot("demo-room");
        assertThat(recreatedSnapshot.participants()).isEmpty();
    }

    @Test
    void removesCreatedRoomWhenLastParticipantLeaves() {
        roomRuntimeService.createRoom(new V2CreateRoomRequest("temp room", "maker", null));
        roomRuntimeService.joinRoom("session-maker", "temp-room", "maker");

        roomRuntimeService.handleDisconnect("session-maker");

        assertThat(roomRuntimeService.getLobbyRooms())
            .extracting("name")
            .doesNotContain("temp-room");
    }

    @Test
    void storesLobbyChatMessagesBeforeGameStarts() {
        roomRuntimeService.joinRoom("session-host", "demo-room", "host-01");

        var chatEvent = roomRuntimeService.submitAnswer("demo-room", "host-01", "test chat");

        assertThat(chatEvent.type()).isEqualTo("room.chat.message");
        assertThat(chatEvent.snapshot()).isNotNull();
        assertThat(chatEvent.chatMessage()).isNotNull();
        assertThat(chatEvent.chatMessage().content()).isEqualTo("test chat");
        assertThat(chatEvent.chatMessage().visibility()).isEqualTo("public");
    }

    @Test
    void createsRoomFromSelectedMap() {
        var createdMap = mapCatalogService.createMap(
            new V2CreateMapRequest(
                "Boss Battle",
                "host owned map",
                "host-01",
                "hard",
                "public",
                false,
                "single-lock",
                "advance-on-correct",
                25,
                6,
                List.of(
                    new V2MapSongDefinition(
                        "Hint: CODE GEASS OP",
                        "COLORS",
                        "FLOW",
                        List.of("colors")
                    ),
                    new V2MapSongDefinition(
                        "Hint: Naruto opening",
                        "Haruka Kanata",
                        "ASIAN KUNG-FU GENERATION",
                        List.of("haruka kanata")
                    ),
                    new V2MapSongDefinition(
                        "Hint: SAO OP",
                        "crossing field",
                        "LiSA",
                        List.of("crossing field")
                    )
                )
            )
        );

        var snapshot = roomRuntimeService.createRoom(
            new V2CreateRoomRequest("boss queue", "host-01", createdMap.id())
        );

        assertThat(snapshot.map()).isNotNull();
        assertThat(snapshot.map().name()).isEqualTo("Boss Battle");
        assertThat(snapshot.totalRounds()).isEqualTo(3);
    }

    @Test
    void keepsRoundOpenForMultiScoreMapsUntilEveryoneAnswers() {
        var createdMap = mapCatalogService.createMap(
            new V2CreateMapRequest(
                "Free For All",
                "multi score map",
                "host-01",
                "normal",
                "public",
                false,
                "multi-score",
                "timer-or-skip",
                18,
                0,
                List.of(
                    new V2MapSongDefinition(
                        "Hint: first song",
                        "File Round",
                        "Codex",
                        List.of("file round")
                    ),
                    new V2MapSongDefinition(
                        "Hint: second song",
                        "Second Round",
                        "Codex",
                        List.of("second round")
                    )
                )
            )
        );

        roomRuntimeService.createRoom(
            new V2CreateRoomRequest("ffa room", "host-01", createdMap.id())
        );
        roomRuntimeService.joinRoom("session-host", "ffa-room", "host-01");
        roomRuntimeService.joinRoom("session-guest", "ffa-room", "guest-01");
        roomRuntimeService.setReady("ffa-room", "host-01", true);
        roomRuntimeService.setReady("ffa-room", "guest-01", true);
        roomRuntimeService.startGame("ffa-room", "host-01");

        var firstCorrect = roomRuntimeService.submitAnswer(
            "ffa-room",
            "host-01",
            "file round"
        );

        assertThat(firstCorrect.snapshot()).isNotNull();
        assertThat(firstCorrect.snapshot().round()).isEqualTo(1);
        assertThat(firstCorrect.snapshot().participants())
            .filteredOn(participant -> participant.nickname().equals("host-01"))
            .singleElement()
            .satisfies(participant -> assertThat(participant.score()).isEqualTo(1));

        var secondCorrect = roomRuntimeService.submitAnswer(
            "ffa-room",
            "guest-01",
            "file round"
        );

        assertThat(secondCorrect.snapshot()).isNotNull();
        assertThat(secondCorrect.snapshot().round()).isEqualTo(2);
        assertThat(secondCorrect.snapshot().participants())
            .filteredOn(participant -> participant.nickname().equals("guest-01"))
            .singleElement()
            .satisfies(participant -> assertThat(participant.score()).isEqualTo(1));
    }
}
