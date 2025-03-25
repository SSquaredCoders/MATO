package com.lshzzz.mato.utils.rooms;

import com.lshzzz.mato.model.room.Rooms;
import com.lshzzz.mato.model.room.dto.RoomsResponse;

public class RoomsMapper {

    public static RoomsResponse toResponse(Rooms room) {
        return new RoomsResponse(
            room.getId(),
            room.getName(),
            room.getPassword(),
            room.getHost(),
            room.getParticipants(),
            room.getGameStatus(),
            room.getMap().getId(),
            room.getMap().getName()
        );
    }
}