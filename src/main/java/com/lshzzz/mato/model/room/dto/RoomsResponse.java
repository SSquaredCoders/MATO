package com.lshzzz.mato.model.room.dto;

import com.lshzzz.mato.model.room.GameStatus;

public record RoomsResponse(

    Long id,
    String name,
    String password,
    String host,
    Integer participants,
    GameStatus gameStatus
) {}