package com.lshzzz.mato.controller.v2;

import com.lshzzz.mato.model.v2.V2CreateRoomRequest;
import com.lshzzz.mato.model.v2.V2RoomSnapshot;
import com.lshzzz.mato.service.v2.V2RoomRuntimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/rooms")
@RequiredArgsConstructor
public class V2RoomsController {

    private final V2RoomRuntimeService roomRuntimeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public V2RoomSnapshot createRoom(@Valid @RequestBody V2CreateRoomRequest request) {
        return roomRuntimeService.createRoom(request);
    }

    @GetMapping("/{roomName}")
    public V2RoomSnapshot getRoom(@PathVariable String roomName) {
        return roomRuntimeService.getRoomSnapshot(roomName);
    }
}
