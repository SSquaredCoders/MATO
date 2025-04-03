package com.lshzzz.mato.model.room.dto;

import com.lshzzz.mato.model.map.dto.MapDetailDto;
import com.lshzzz.mato.model.room.GameStatus;
import com.lshzzz.mato.model.room.Rooms;

import java.util.List;

/**
 * 방 상세 정보 조회 시 사용하는 DTO
 */
public record RoomDetailResponseDto(
    String id,
    String name,
    String host,
    List<ParticipantDto> participants,
    int maxParticipants,
    GameStatus gameStatus,
    boolean hasPassword,
    MapDetailDto map
) {
    /**
     * JPA 엔티티로부터 DTO 생성
     */
    public static RoomDetailResponseDto fromEntity(Rooms room, List<ParticipantDto> participants) {
        return new RoomDetailResponseDto(
            room.getId().toString(),
            room.getName(),
            room.getHost(),
            participants,
            room.getMaxParticipants(),
            room.getGameStatus(),
            room.getPassword() != null && !room.getPassword().isEmpty(),
            MapDetailDto.fromEntity(room.getMap())
        );
    }
    
    /**
     * Redis DTO로부터 생성
     */
    public static RoomDetailResponseDto fromDto(
        RoomDto roomDto,
        List<ParticipantDto> participants,
        MapDetailDto mapDto
    ) {
        return new RoomDetailResponseDto(
            roomDto.id(),
            roomDto.name(),
            roomDto.host(),
            participants,
            roomDto.maxParticipants(),
            GameStatus.valueOf(roomDto.gameStatus()),
            roomDto.password() != null && !roomDto.password().isEmpty(),
            mapDto
        );
    }
} 