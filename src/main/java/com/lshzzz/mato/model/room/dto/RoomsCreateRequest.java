package com.lshzzz.mato.model.room.dto;

import com.lshzzz.mato.model.room.GameStatus;

public record RoomsCreateRequest(

    String name,
    String password,
    GameStatus gameStatus
) {}