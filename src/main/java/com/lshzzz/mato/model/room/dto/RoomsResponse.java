package com.lshzzz.mato.model.room.dto;

import com.lshzzz.mato.model.map.dto.MapResponseDto;
import com.lshzzz.mato.model.room.GameStatus;

import java.util.List;
import java.util.HashMap;

/**
 * 방 정보 응답 DTO
 */
public record RoomsResponse(
    String id,
    String name,
    String password,
    int maxParticipants,
    String host,
    int participantCount,
    GameStatus gameStatus,
    MapResponseDto map,
    List<HashMap<String, Object>> participantList
) {
    public static RoomsResponse fromDto(
        RoomDto roomDto,
        MapResponseDto  mapDto,
        List<HashMap<String, Object>> participants
    ) {
        return new RoomsResponse(
            roomDto.id(),
            roomDto.name(),
            roomDto.password(),
            roomDto.maxParticipants(),
            roomDto.host(),
            participants.size(),
            GameStatus.valueOf(roomDto.gameStatus()),
            mapDto,
            participants
        );
    }
}
