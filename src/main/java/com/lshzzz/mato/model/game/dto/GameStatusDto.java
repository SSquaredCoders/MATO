package com.lshzzz.mato.model.game.dto;

import com.lshzzz.mato.model.room.GameStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 게임 상태 정보를 위한 DTO
 */
public record GameStatusDto(
    Long roomId,
    String roomName,
    GameStatus status,
    Map<String, Integer> scores,
    CurrentSongDto currentSong,
    int currentSongIndex,
    int totalSongs,
    LocalDateTime lastUpdated
) {
    /**
     * GameStatusResponse로 변환
     */
    public GameStatusResponse toResponse() {
        return new GameStatusResponse(
            this.roomId(),
            this.roomName(),
            this.status(),
            this.scores(),
            this.currentSong() != null ? this.currentSong().song() : null,
            this.currentSongIndex(),
            this.totalSongs(),
            this.lastUpdated()
        );
    }
} 