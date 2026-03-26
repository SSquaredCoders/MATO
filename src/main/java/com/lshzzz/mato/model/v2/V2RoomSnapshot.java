package com.lshzzz.mato.model.v2;

import java.util.List;

public record V2RoomSnapshot(
    String roomName,
    String hostNickname,
    V2GamePhase phase,
    V2MapSummary map,
    int maxParticipants,
    int round,
    int totalRounds,
    String currentPrompt,
    String currentHint,
    String hintRevealAt,
    String lastEvent,
    String currentReveal,
    List<V2RoomParticipant> participants
) {
}
