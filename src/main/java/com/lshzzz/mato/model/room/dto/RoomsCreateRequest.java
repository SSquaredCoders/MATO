package com.lshzzz.mato.model.room.dto;

import com.lshzzz.mato.model.room.GameStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomsCreateRequest(

    @NotBlank
    String name,

    String password,

    @NotNull
    GameStatus gameStatus,

    @NotNull(message = "맵 선택은 필수입니다.")
    Long mapId,
    
    @NotNull
    @Min(value = 2, message = "최소 인원은 2명 이상이어야 합니다.")
    @Max(value = 10, message = "최대 인원은 10명까지 가능합니다.")
    Integer maxParticipants,
    
    String hostNickname
) {}