package com.lshzzz.mato.model.v2;

public record V2ServerEnvelope(
    String type,
    String roomName,
    V2RoomEventPayload payload,
    String serverTimestamp
) {
}
