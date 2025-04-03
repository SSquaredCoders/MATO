package com.lshzzz.mato.model.game.dto;

import com.lshzzz.mato.model.room.GameStatus;
import com.lshzzz.mato.model.song.dto.SongResponseDto;

import java.time.LocalDateTime;
import java.util.Map;

public record GameStatusResponse(
    Long roomId,
    String roomName,
    GameStatus status,
    Map<String, Integer> scores,
    SongResponseDto currentSong,
    Integer currentSongIndex,
    Integer totalSongs,
    LocalDateTime lastUpdated
) {} 