package com.lshzzz.mato.controller.rooms;

import com.lshzzz.mato.model.chat.dto.ChatMessage;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomsController {

    private final RoomsService roomsService;
    private final SimpMessagingTemplate messagingTemplate;

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
        
        // 이미 참가자인지 확인
        Set<String> participants = roomsService.getParticipants(name);
        if (participants.contains(nickname)) {
            log.info("이미 방에 참가 중인 사용자입니다: {}, 방: {}. 추가 요청 무시.", nickname, name);
            return ResponseEntity.ok().build(); // 이미 참가 중이면 OK 반환하고 종료
        }
        
        log.info("새 참가자 추가: {}, 방: {}", nickname, name);
        roomsService.addParticipant(name, nickname);
        
        // 로비에 참가자 변경 알림
        sendLobbyUpdateMessage(name, "PARTICIPANT_CHANGE");
        
        return ResponseEntity.ok().build();
    }

    // 참가자 제거
    @DeleteMapping("/{name}/participants")
    public ResponseEntity<Void> removeParticipant(@PathVariable String name, HttpServletRequest request) {
        String nickname = roomsService.resolveNickname(request);
        roomsService.removeParticipant(name, nickname);
        
        // 로비에 참가자 변경 알림
        sendLobbyUpdateMessage(name, "PARTICIPANT_CHANGE");
        
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
        RoomsResponse response = roomsService.createRoom(request, hostNickname);
        
        // 로비에 방 생성 알림
        sendLobbyUpdateMessage(request.name(), "ROOM_CREATE");
        
        return ResponseEntity.ok(response);
    }

    // 방 수정
    @PutMapping("/{id}")
    public ResponseEntity<RoomsResponse> updateRoom(@PathVariable Long id,
        @RequestBody @Valid RoomsUpdateRequest request,
        HttpServletRequest httpRequest) {
        String nickname = roomsService.resolveNickname(httpRequest);
        RoomsResponse response = roomsService.updateRoom(id, request, nickname);
        
        // 로비에 방 정보 업데이트 알림
        sendLobbyUpdateMessage(response.name(), "ROOM_UPDATE");
        
        return ResponseEntity.ok(response);
    }

    // 방 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id,
        HttpServletRequest httpRequest) {
        String nickname = roomsService.resolveNickname(httpRequest);
        
        // 방 이름 객체를 미리 알 수 없으므로, 삭제 후 로그만 남김
        roomsService.deleteRoom(id, nickname);
        
        // 로비에 방 삭제 알림 (방 이름 대신 id 문자열 사용)
        sendLobbyUpdateMessage("room_" + id, "ROOM_DELETE");
        
        return ResponseEntity.noContent().build();
    }

    // 비밀번호 검증
    @PostMapping("/validate")
    public ResponseEntity<Boolean> validatePassword(
        @RequestBody RoomPasswordValidationRequest request) {
        boolean result = roomsService.validatePassword(request.name(), request.password());
        return ResponseEntity.ok(result);
    }
    
    // 로비에 방 업데이트 메시지 전송하는 유틸리티 메서드
    private void sendLobbyUpdateMessage(String roomName, String type) {
        try {
            ChatMessage lobbyUpdateMessage = new ChatMessage("SYSTEM", roomName, "LOBBY", type);
            messagingTemplate.convertAndSend("/topic/lobby", lobbyUpdateMessage);
            log.info("로비에 {} 이벤트 전송 완료: 방 {}", type, roomName);
        } catch (Exception e) {
            log.error("로비 메시지 전송 중 오류 발생", e);
        }
    }

    // 로비 강제 갱신 요청 (클라이언트에서 호출)
    @PostMapping("/notify-lobby-update")
    public ResponseEntity<Void> notifyLobbyUpdate(@RequestBody Map<String, String> request) {
        String type = request.getOrDefault("type", "FORCE_REFRESH");
        String roomName = request.getOrDefault("roomName", "unknown");
        
        // WebSocket을 통해 로비에 메시지 전송
        log.info("로비 강제 갱신 HTTP 요청 수신: 타입={}, 방={}", type, roomName);
        
        sendLobbyUpdateMessage(roomName, type);
        
        return ResponseEntity.ok().build();
    }

    // 게스트 사용자 일괄 제거 엔드포인트 
    @DeleteMapping("/{name}/guests")
    public ResponseEntity<Map<String, Object>> removeGuestUsers(@PathVariable String name) {
        log.info("방 {} 게스트 사용자 일괄 제거 요청", name);
        
        try {
            // 게스트로 시작하는 닉네임을 가진 참가자들 제거
            roomsService.cleanupParticipantsByPattern(name, "게스트");
            
            // 남은 참가자 목록 조회
            List<HashMap<String, Object>> remainingParticipants = roomsService.getRoomParticipants(name);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "게스트 사용자가 성공적으로 제거되었습니다.");
            response.put("remainingParticipantsCount", remainingParticipants.size());
            
            log.info("방 {} 게스트 사용자 제거 완료, 남은 참가자: {}명", name, remainingParticipants.size());
            
            // 로비 업데이트 메시지 전송
            sendLobbyUpdateMessage(name, "PARTICIPANT_CHANGE");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("게스트 사용자 제거 중 오류 발생: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "게스트 사용자 제거 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // 아래 메서드들은 API v2에서 사용될 예정입니다.
    // 프론트엔드가 준비되면 주석을 해제하세요.

    /*
    @GetMapping("/v2")
    public ResponseEntity<List<RoomListResponseDto>> getAllRoomsV2() {
        return ResponseEntity.ok(roomsService.findAllRoomsAsDto());
    }

    @GetMapping("/v2/{name}")
    public ResponseEntity<RoomListResponseDto> getRoomByNameV2(@PathVariable String name) {
        return ResponseEntity.ok(roomsService.findRoomDetailByName(name));
    }
    */
}