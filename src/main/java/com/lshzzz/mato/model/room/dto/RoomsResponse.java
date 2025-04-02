package com.lshzzz.mato.model.room.dto;

import com.lshzzz.mato.model.map.Map;
import com.lshzzz.mato.model.room.Rooms;
import com.lshzzz.mato.model.room.GameStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public record RoomsResponse(
    Long id,
    String name,
    String password,
    int maxParticipants,
    String host,
    int participantCount,
    GameStatus gameStatus,
    Long mapId,
    String mapName,
    List<HashMap<String, Object>> participantList
) {
    public static RoomsResponse from(Rooms rooms) {
        List<HashMap<String, Object>> participants = rooms.getParticipants();
        if (participants == null) participants = new ArrayList<>();

        return new RoomsResponse(
            rooms.getId(),
            rooms.getName(),
            rooms.getPassword(),
            rooms.getMaxParticipants() != null ? rooms.getMaxParticipants() : 10,
            rooms.getHost(),
            participants.size(),
            rooms.getGameStatus(),
            rooms.getMap().getId(),
            rooms.getMap().getName(),
            participants
        );
    }
}
