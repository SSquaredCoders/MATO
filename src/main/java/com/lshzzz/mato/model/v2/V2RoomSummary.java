package com.lshzzz.mato.model.v2;

public record V2RoomSummary(
    String name,
    String hostNickname,
    int participantCount,
    int maxParticipants,
    V2GamePhase phase,
    V2MapSummary map
) {
}
