package com.lshzzz.mato.model.room;

import lombok.Getter;

@Getter
public enum GameStatus {
    WAITING("대기 중"),
    PLAYING("게임 중"),
    FINISHED("게임 종료");

    private final String description;

    GameStatus(String description) {
        this.description = description;
    }
}