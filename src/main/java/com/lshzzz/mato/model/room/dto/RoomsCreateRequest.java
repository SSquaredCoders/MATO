package com.lshzzz.mato.model.room.dto;

import com.lshzzz.mato.model.room.GameStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomsCreateRequest(

    @NotBlank
    String name,

    String password,

    @NotNull
    GameStatus gameStatus,

    @NotNull(message = "맵 선택은 필수입니다.")
    Long mapId
) {}