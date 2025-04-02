package com.lshzzz.mato.controller.rooms;

import com.lshzzz.mato.model.room.dto.ParticipantReadyRequest;
import com.lshzzz.mato.model.room.dto.RoomPasswordValidationRequest;
import com.lshzzz.mato.model.room.dto.RoomsCreateRequest;
import com.lshzzz.mato.model.room.dto.RoomsResponse;
import com.lshzzz.mato.model.room.dto.RoomsUpdateRequest;
import com.lshzzz.mato.service.rooms.RoomsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/rooms")
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

    // 방의 참가자 목록 조회
    @GetMapping("/{name}/participants")
    public ResponseEntity<List<HashMap<String, Object>>> getRoomParticipants(@PathVariable String name) {
        log.info("방 {} 참가자 목록 조회 요청", name);
        List<HashMap<String, Object>> participants = roomsService.getRoomParticipants(name);
        log.info("방 {} 참가자 목록 조회 결과: {} 명", name, participants.size());
        return ResponseEntity.ok(participants);
    }

    // 참가자 추가
    @PostMapping("/{name}/participants")
    public ResponseEntity<Void> addParticipant(@PathVariable String name, HttpServletRequest request) {
        String nickname = roomsService.resolveNickname(request);
        roomsService.addParticipant(name, nickname);
        return ResponseEntity.ok().build();
    }

    // 참가자 제거
    @DeleteMapping("/{name}/participants")
    public ResponseEntity<Void> removeParticipant(@PathVariable String name, HttpServletRequest request) {
        String nickname = roomsService.resolveNickname(request);
        roomsService.removeParticipant(name, nickname);
        return ResponseEntity.ok().build();
    }

    // 참가자 준비 상태 변경
    @PatchMapping("/{name}/ready")
    public ResponseEntity<Void> setParticipantReady(
        @PathVariable String name,
        @RequestBody ParticipantReadyRequest request) {
        roomsService.setParticipantReady(name, request.nickname(), request.ready());
        return ResponseEntity.ok().build();
    }


    // 방 생성
    @PostMapping
    public ResponseEntity<RoomsResponse> createRoom(@RequestBody @Valid RoomsCreateRequest request,
        HttpServletRequest httpRequest) {
        String hostNickname = roomsService.resolveNickname(httpRequest);
        return ResponseEntity.ok(roomsService.createRoom(request, hostNickname));
    }

    // 방 수정
    @PutMapping("/{id}")
    public ResponseEntity<RoomsResponse> updateRoom(@PathVariable Long id,
        @RequestBody @Valid RoomsUpdateRequest request,
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

    // 비밀번호 검증
    @PostMapping("/validate")
    public ResponseEntity<Boolean> validatePassword(
        @RequestBody RoomPasswordValidationRequest request) {
        boolean result = roomsService.validatePassword(request.name(), request.password());
        return ResponseEntity.ok(result);
    }
}