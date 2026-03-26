package com.lshzzz.mato.model.v2;

public record V2RoomEventPayload(
    V2RoomSnapshot snapshot,
    String message,
    String actorNickname,
    Boolean accepted,
    V2RoomChatMessage chatMessage
) {
}
