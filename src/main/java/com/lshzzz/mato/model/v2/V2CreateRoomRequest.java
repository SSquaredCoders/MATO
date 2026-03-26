package com.lshzzz.mato.model.v2;

import jakarta.validation.constraints.NotBlank;

public record V2CreateRoomRequest(
    @NotBlank String roomName,
    @NotBlank String hostNickname,
    Long mapId
) {
}
