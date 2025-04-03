package com.lshzzz.mato.model.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomsUpdateRequest(

    @NotBlank
    String name,

    String password,

    @NotNull(message = "맵 선택은 필수입니다.")
    Long mapId

    // @NotBlank String id ← 필요한 경우 추가

) {}