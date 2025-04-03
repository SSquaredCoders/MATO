package com.lshzzz.mato.model.room.dto;

import com.lshzzz.mato.model.map.dto.MapResponseDto;
import com.lshzzz.mato.model.map.dto.MapSummaryDto;
import com.lshzzz.mato.model.room.GameStatus;
import com.lshzzz.mato.model.room.Rooms;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * 방 목록 조회 시 사용하는 DTO
 */
public record RoomListResponseDto(
    String id,
    String name,
    String host,
    int participantCount,
    int maxParticipants,
    GameStatus gameStatus,
    boolean hasPassword,
    MapSummaryDto map
) {
    /**
     * JPA 엔티티로부터 DTO 생성
     */
    public static RoomListResponseDto fromEntity(Rooms room) {
        return new RoomListResponseDto(
            room.getId().toString(),
            room.getName(),
            room.getHost(),
            room.getParticipants().size(),
            room.getMaxParticipants(),
            room.getGameStatus(),
            room.getPassword() != null && !room.getPassword().isEmpty(),
            MapSummaryDto.fromEntity(room.getMap())
        );
    }
    
    /**
     * Redis DTO로부터 생성
     */
    public static RoomListResponseDto fromDto(
        RoomDto roomDto,
        int participantCount,
        MapSummaryDto mapDto
    ) {
        return new RoomListResponseDto(
            roomDto.id(),
            roomDto.name(),
            roomDto.host(),
            participantCount,
            roomDto.maxParticipants(),
            GameStatus.valueOf(roomDto.gameStatus()),
            roomDto.password() != null && !roomDto.password().isEmpty(),
            mapDto
        );
    }
    
    /**
     * RoomsResponse로부터 생성
     */
    public static RoomListResponseDto fromRoomsResponse(RoomsResponse response) {
        return new RoomListResponseDto(
            response.id(),
            response.name(),
            response.host(),
            response.participantCount(),
            response.maxParticipants(),
            response.gameStatus(),
            response.password() != null && !response.password().isEmpty(),
            MapSummaryDto.fromMapResponseDto(response.map())
        );
    }
    
    /**
     * RoomsResponse로 변환
     */
    public RoomsResponse toRoomsResponse(List<HashMap<String, Object>> participants) {
        return new RoomsResponse(
            this.id(),
            this.name(),
            this.hasPassword() ? "********" : "", // 보안을 위해 실제 비밀번호 대신 마스킹
            this.maxParticipants(),
            this.host(),
            this.participantCount(),
            this.gameStatus(),
            MapResponseDto.fromSummaryDto(this.map()),
            participants != null ? participants : Collections.emptyList()
        );
    }
} 