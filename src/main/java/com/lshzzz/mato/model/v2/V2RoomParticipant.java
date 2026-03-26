package com.lshzzz.mato.model.v2;

public record V2RoomParticipant(
    String id,
    String nickname,
    boolean ready,
    int score,
    boolean connected
) {
}
