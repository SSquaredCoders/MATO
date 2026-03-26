package com.lshzzz.mato.controller.v2;

import com.lshzzz.mato.model.v2.V2RoomSummary;
import com.lshzzz.mato.service.v2.V2RoomRuntimeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/lobby")
@RequiredArgsConstructor
public class V2LobbyController {

    private final V2RoomRuntimeService roomRuntimeService;

    @GetMapping("/rooms")
    public List<V2RoomSummary> getLobbyRooms() {
        return roomRuntimeService.getLobbyRooms();
    }
}
