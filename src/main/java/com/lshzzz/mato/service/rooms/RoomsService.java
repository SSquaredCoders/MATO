package com.lshzzz.mato.service.rooms;

import com.lshzzz.mato.model.room.dto.RoomsCreateRequest;
import com.lshzzz.mato.model.room.dto.RoomsResponse;
import com.lshzzz.mato.model.room.dto.RoomsUpdateRequest;
import com.lshzzz.mato.repository.RoomsRepository;
import com.lshzzz.mato.repository.MapRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @deprecated JPA 기반 방 서비스. Redis 기반으로 전환 중이므로 {@link RedisRoomsService}를 사용하세요.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Deprecated
public class RoomsService {

    private final RoomsRepository roomsRepository;
    private final MapRepository mapRepository;
    private final RedisRoomsService redisRoomsService;

    // 로그인 여부 확인 뒤 비회원 닉네임 랜덤 부여
    public String resolveNickname(HttpServletRequest request) {
        return redisRoomsService.resolveNickname(request);
    }

    // 전체 방 조회
    public List<RoomsResponse> findAllRooms() {
        return redisRoomsService.findAllRooms();
    }

    // 방 제목을 통한 방 조회
    public RoomsResponse findByName(String name) {
        return redisRoomsService.findByName(name);
    }

    // 방의 참가자 목록 조회
    public List<HashMap<String, Object>> getRoomParticipants(String name) {
        return redisRoomsService.getRoomParticipants(name);
    }

    // 방 생성
    @Transactional
    public RoomsResponse createRoom(RoomsCreateRequest request, String hostNickname) {
        return redisRoomsService.createRoom(request, hostNickname);
    }

    // 방 수정
    @Transactional
    public RoomsResponse updateRoom(Long roomId, RoomsUpdateRequest request, String nickname) {
        return redisRoomsService.updateRoom(roomId, request, nickname);
    }

    // 방 삭제
    @Transactional
    public void deleteRoom(Long roomId, String nickname) {
        redisRoomsService.deleteRoom(roomId, nickname);
    }

    // 비밀번호 검증
    public boolean validatePassword(String roomName, String inputPassword) {
        return redisRoomsService.validatePassword(roomName, inputPassword);
    }

    // 참가자 추가
    @Transactional
    public void addParticipant(String roomName, String nickname) {
        redisRoomsService.addParticipant(roomName, nickname);
    }

    // 참가자 제거
    @Transactional
    public void removeParticipant(String roomName, String nickname) {
        redisRoomsService.removeParticipant(roomName, nickname);
    }

    // 참가자 준비 상태 변경
    @Transactional
    public void setParticipantReady(String roomName, String nickname, boolean ready) {
        redisRoomsService.setParticipantReady(roomName, nickname, ready);
    }

    // 방 참가자 목록 조회
    public Set<String> getParticipants(String roomName) {
        return redisRoomsService.getParticipants(roomName);
    }

    @Transactional
    public void cleanupParticipantsByPattern(String roomName, String nicknamePattern) {
        redisRoomsService.cleanupParticipantsByPattern(roomName, nicknamePattern);
    }

    // 방 이름으로 방 삭제
    @Transactional
    public void deleteRoomByName(String roomName) {
        redisRoomsService.deleteRoomByName(roomName);
    }
}
