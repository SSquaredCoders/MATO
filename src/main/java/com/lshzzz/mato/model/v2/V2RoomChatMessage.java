package com.lshzzz.mato.model.v2;

public record V2RoomChatMessage(
    String id,
    String nickname,
    String content,
    String tone,
    String visibility
) {
}
