package com.lshzzz.mato.model.room.dto;

import com.lshzzz.mato.model.room.GameStatus;
import lombok.Builder;

/**
 * Redis에 저장되는 방 정보 DTO
 */
@Builder
public record RoomDto(
    String id,
    String name, 
    String password,
    int maxParticipants,
    String host,
    String gameStatus,
    Long mapId
) {
    /**
     * 새 방 생성
     */
    public static RoomDto createRoom(String id, String name, String password, int maxParticipants, String host, Long mapId) {
        return new RoomDto(
            id,
            name,
            password,
            maxParticipants,
            host,
            GameStatus.WAITING.name(),
            mapId
        );
    }
    
    /**
     * 방 상태 업데이트
     */
    public RoomDto withGameStatus(GameStatus gameStatus) {
        return new RoomDto(
            this.id,
            this.name,
            this.password,
            this.maxParticipants,
            this.host,
            gameStatus.name(),
            this.mapId
        );
    }
} 