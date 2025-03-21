package com.lshzzz.mato.controller.rooms;

import com.lshzzz.mato.model.room.dto.RoomsCreateRequest;
import com.lshzzz.mato.model.room.dto.RoomsResponse;
import com.lshzzz.mato.model.room.dto.RoomsUpdateRequest;
import com.lshzzz.mato.service.rooms.RoomsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomsController {

    private final RoomsService roomsService;

    // 전체 방 조회
    @GetMapping
    public ResponseEntity<List<RoomsResponse>> getAllRooms() {
        return ResponseEntity.ok(roomsService.findAllRooms());
    }

    // 방 제목을 통한 방 조회
    @GetMapping("/{name}")
    public ResponseEntity<RoomsResponse> getRoomByName(@PathVariable String name) {
        return ResponseEntity.ok(roomsService.findByName(name));
    }

    // 방 생성
    @PostMapping
    public ResponseEntity<RoomsResponse> createRoom(@RequestBody RoomsCreateRequest request,
        HttpServletRequest httpRequest) {
        String hostNickname = roomsService.resolveNickname(httpRequest);
        return ResponseEntity.ok(roomsService.createRoom(request, hostNickname));
    }

    // 방 수정
    @PutMapping("/{id}")
    public ResponseEntity<RoomsResponse> updateRoom(@PathVariable Long id,
        @RequestBody RoomsUpdateRequest request,
        HttpServletRequest httpRequest) {
        String nickname = roomsService.resolveNickname(httpRequest);
        return ResponseEntity.ok(roomsService.updateRoom(id, request, nickname));
    }

    // 방 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id,
        HttpServletRequest httpRequest) {
        String nickname = roomsService.resolveNickname(httpRequest);
        roomsService.deleteRoom(id, nickname);
        return ResponseEntity.noContent().build();
    }
}