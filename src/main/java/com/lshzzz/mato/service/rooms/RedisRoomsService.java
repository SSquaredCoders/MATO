package com.lshzzz.mato.service.rooms;

import com.lshzzz.mato.exception.CustomException;
import com.lshzzz.mato.exception.ErrorCode;
import com.lshzzz.mato.model.map.Map;
import com.lshzzz.mato.model.map.dto.MapResponseDto;
import com.lshzzz.mato.model.map.dto.MapSummaryDto;
import com.lshzzz.mato.model.mapsongs.dto.MapSongResponseDto;
import com.lshzzz.mato.model.room.dto.RoomDto;
import com.lshzzz.mato.model.room.dto.RoomListResponseDto;
import com.lshzzz.mato.model.room.dto.RoomsCreateRequest;
import com.lshzzz.mato.model.room.dto.RoomsResponse;
import com.lshzzz.mato.model.room.dto.RoomsUpdateRequest;
import com.lshzzz.mato.repository.MapRepository;
import com.lshzzz.mato.repository.MapSongRepository;
import com.lshzzz.mato.repository.redis.RedisRoomsRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Primary // 같은 타입의 빈이 여러 개 있을 때 이 구현체를 우선 선택
@RequiredArgsConstructor
public class RedisRoomsService {

    private final RedisRoomsRepository redisRoomsRepository;
    private final MapRepository mapRepository;
    private final MapSongRepository mapSongRepository;

    // 로그인 여부 확인 뒤 비회원 닉네임 랜덤 부여
    public String resolveNickname(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken)) {
            return authentication.getName();
        } else {
            HttpSession session = request.getSession();
            String guestNick = (String) session.getAttribute("guestNickname");
            if (guestNick == null) {
                guestNick = "게스트" + (int) (Math.random() * 9000 + 1000);
                session.setAttribute("guestNickname", guestNick);
            }
            return guestNick;
        }
    }

    /**
     * 전체 방 목록 조회
     */
    public List<RoomsResponse> findAllRooms() {
        return redisRoomsRepository.findAll().stream()
            .map(roomDto -> {
                // 내부적으로 새 DTO 구조 사용
                RoomListResponseDto roomListDto = mapToRoomListDto(roomDto);
                // 참가자 목록 조회 (호환성 유지)
                List<HashMap<String, Object>> participants = 
                    redisRoomsRepository.getParticipantsWithStatus(roomDto.name());
                // 기존 응답 형식으로 변환
                return roomListDto.toRoomsResponse(participants);
            })
            .collect(Collectors.toList());
    }

    /**
     * 방 이름으로 방 조회
     */
    public RoomsResponse findByName(String name) {
        RoomDto roomDto = redisRoomsRepository.findByName(name)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        
        // 내부적으로 새 DTO 구조 사용
        RoomListResponseDto roomListDto = mapToRoomListDto(roomDto);
        
        // 참가자 목록 조회 (호환성 유지)
        List<HashMap<String, Object>> participants = 
            redisRoomsRepository.getParticipantsWithStatus(name);
        
        // 기존 응답 형식으로 변환
        return roomListDto.toRoomsResponse(participants);
    }

    // 방의 참가자 목록 조회
    public List<HashMap<String, Object>> getRoomParticipants(String name) {
        if (!redisRoomsRepository.findByName(name).isPresent()) {
            throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
        }
        
        return redisRoomsRepository.getParticipantsWithStatus(name);
    }

    /**
     * 방 생성
     */
    @Transactional
    public RoomsResponse createRoom(RoomsCreateRequest request, String hostNickname) {
        // 맵 정보 조회
        Map map = mapRepository.findById(request.mapId())
            .orElseThrow(() -> new CustomException(ErrorCode.MAP_NOT_FOUND));
        
        // 중복 방 이름 검사
        redisRoomsRepository.findByName(request.name()).ifPresent(room -> {
            throw new CustomException(ErrorCode.ROOM_ALREADY_EXISTS);
        });

        // Redis에 방 생성
        String roomId = UUID.randomUUID().toString();
        RoomDto roomDto = RoomDto.createRoom(
            roomId,
            request.name(),
            request.password(),
            request.maxParticipants(),
            hostNickname,
            map.getId()
        );
        
        // 방 저장
        redisRoomsRepository.save(roomDto);
        
        // 방장을 첫 참가자로 추가
        redisRoomsRepository.addParticipant(request.name(), hostNickname);
        
        // 내부적으로 새 DTO 구조 사용
        RoomListResponseDto roomListDto = mapToRoomListDto(roomDto);
        
        // 참가자 목록 조회 (호환성 유지)
        List<HashMap<String, Object>> participants = 
            redisRoomsRepository.getParticipantsWithStatus(request.name());
        
        // 기존 응답 형식으로 변환
        return roomListDto.toRoomsResponse(participants);
    }

    /**
     * 방 정보 업데이트
     */
    @Transactional
    public RoomsResponse updateRoom(Long roomId, RoomsUpdateRequest request, String nickname) {
        // 방 존재 확인
        RoomDto roomDto = redisRoomsRepository.findByName(request.name())
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        
        // 권한 확인
        if (!roomDto.host().equals(nickname)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        
        // 맵 존재 확인
        Map map = mapRepository.findById(request.mapId())
            .orElseThrow(() -> new CustomException(ErrorCode.MAP_NOT_FOUND));
        
        // 방 업데이트
        RoomDto updatedRoom = new RoomDto(
            roomDto.id(),
            request.name(),
            request.password(),
            roomDto.maxParticipants(),
            roomDto.host(), 
            roomDto.gameStatus(),
            map.getId()
        );
        
        // Redis에 저장
        redisRoomsRepository.save(updatedRoom);
        
        return mapToRoomsResponse(updatedRoom);
    }

    // 방 삭제
    @Transactional
    public void deleteRoom(Long roomId, String nickname) {
        // 방 존재 확인 (Redis에서는 ID가 아닌 이름으로 관리)
        Optional<RoomDto> roomOpt = redisRoomsRepository.findAll().stream()
            .filter(room -> room.id().equals(roomId.toString()))
            .findFirst();
        
        if (roomOpt.isEmpty()) {
            throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
        }
        
        RoomDto room = roomOpt.get();
        
        // 권한 확인
        if (!room.host().equals(nickname)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        
        // Redis에서 삭제
        redisRoomsRepository.deleteByName(room.name());
    }

    // 방 이름으로 방 삭제
    @Transactional
    public void deleteRoomByName(String roomName) {
        // 방 존재 확인
        redisRoomsRepository.findByName(roomName)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        
        // Redis에서 방 삭제
        log.info("방 이름으로 방 삭제 요청: {}", roomName);
        redisRoomsRepository.deleteByName(roomName);
        log.info("방 이름으로 방 삭제 성공: {}", roomName);
    }

    // 비밀번호 검증
    public boolean validatePassword(String roomName, String inputPassword) {
        RoomDto room = redisRoomsRepository.findByName(roomName)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        
        // 비밀번호가 없으면 true 반환
        if (room.password() == null || room.password().isEmpty()) {
            return true;
        }
        
        return room.password().equals(inputPassword);
    }

    // 참가자 추가
    @Transactional
    public void addParticipant(String roomName, String nickname) {
        RoomDto room = redisRoomsRepository.findByName(roomName)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        
        // 이미 참가자로 등록되어 있는지 확인
        Set<String> participants = redisRoomsRepository.getParticipants(roomName);
        if (participants.contains(nickname)) {
            log.info("이미 방에 참가 중인 사용자입니다: {}, 방: {}", nickname, roomName);
            return; // 이미 참가 중이면 중복 추가하지 않고 조용히 반환
        }
        
        // 참가자 수 제한 확인
        if (participants.size() >= room.maxParticipants()) {
            throw new CustomException(ErrorCode.ROOM_FULL);
        }
        
        // 참가자 추가
        redisRoomsRepository.addParticipant(roomName, nickname);
        log.info("참가자 추가 성공: {}, 방: {}", nickname, roomName);
    }

    // 특정 패턴의 닉네임을 가진 참가자들을 일괄 제거 (예: 게스트 사용자)
    @Transactional
    public void cleanupParticipantsByPattern(String roomName, String nicknamePattern) {
        try {
            // 방 존재 확인
            RoomDto room = redisRoomsRepository.findByName(roomName)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
            
            // 현재 참가자 목록 가져오기
            Set<String> participants = redisRoomsRepository.getParticipants(roomName);
            log.info("방 {} 청소 전 참가자 수: {}", roomName, participants.size());
            
            // 패턴에 맞는 참가자들 필터링
            List<String> participantsToRemove = participants.stream()
                .filter(nickname -> nickname.startsWith(nicknamePattern))
                .collect(Collectors.toList());
            
            log.info("방 {}에서 제거할 '{}' 패턴 참가자 수: {}", roomName, nicknamePattern, participantsToRemove.size());
            
            // 필터링된 참가자들 제거
            int removedCount = 0;
            for (String nickname : participantsToRemove) {
                redisRoomsRepository.removeParticipant(roomName, nickname);
                log.info("패턴 일치 참가자 제거: {} (방: {})", nickname, roomName);
                removedCount++;
            }
            
            log.info("방 {}에서 총 {}명의 {}* 패턴 참가자 제거 완료", roomName, removedCount, nicknamePattern);
            
            // 남은 참가자 확인
            Set<String> remainingParticipants = redisRoomsRepository.getParticipants(roomName);
            log.info("방 {} 청소 후 남은 참가자 수: {}", roomName, remainingParticipants.size());
            
            // 참가자가 모두 나갔으면 방 삭제
            if (remainingParticipants.isEmpty()) {
                log.info("방 {}의 참가자가 모두 제거되어 방을 자동 삭제합니다", roomName);
                redisRoomsRepository.deleteByName(roomName);
            }
            
        } catch (Exception e) {
            log.error("참가자 패턴 일괄 제거 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    // 참가자 제거
    @Transactional
    public void removeParticipant(String roomName, String nickname) {
        try {
            // 방 존재 확인
            RoomDto room = redisRoomsRepository.findByName(roomName)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
            
            // 이미 참가자 목록에 존재하는지 확인
            Set<String> participants = redisRoomsRepository.getParticipants(roomName);
            if (!participants.contains(nickname)) {
                log.info("참가자 제거 건너뜀: {} 사용자는 {} 방의 참가자가 아닙니다", nickname, roomName);
                return;
            }
            
            // 참가자 제거
            log.info("참가자 제거 시작: {} (방: {})", nickname, roomName);
            redisRoomsRepository.removeParticipant(roomName, nickname);
            log.info("참가자 제거 완료: {} (방: {})", nickname, roomName);
            
            // 참가자 제거 후 남은 참가자 수 확인 (Redis에서 직접 조회하여 정확히 확인)
            Set<String> remainingParticipants = redisRoomsRepository.getParticipants(roomName);
            int participantCount = remainingParticipants.size();
            log.info("방 {} 남은 참가자 수: {}", roomName, participantCount);
            
            // 참가자가 모두 나가면 방 삭제
            if (participantCount == 0) {
                log.info("방 {} 참가자가 모두 나갔으므로 방을 자동 삭제합니다.", roomName);
                try {
                    redisRoomsRepository.deleteByName(roomName);
                    log.info("방 {} 삭제 완료", roomName);
                } catch (Exception e) {
                    log.error("방 삭제 중 오류: {}", e.getMessage());
                }
                
                // 마지막 참가자 퇴장으로 방이 삭제되었다는 로그 추가
                log.info("마지막 참가자 {} 퇴장으로 방 {} 자동 삭제됨", nickname, roomName);
            } else if (room.host().equals(nickname)) {
                // 방장이 나간 경우 새 방장 설정
                String newHost = remainingParticipants.iterator().next(); // 첫 번째 참가자를 새 방장으로
                log.info("방장 {} 퇴장으로 새 방장 설정: {} (방: {})", nickname, newHost, roomName);
                
                // 방장 정보 업데이트
                RoomDto updatedRoom = new RoomDto(
                    room.id(),
                    room.name(),
                    room.password(),
                    room.maxParticipants(),
                    newHost,
                    room.gameStatus(),
                    room.mapId()
                );
                redisRoomsRepository.save(updatedRoom);
                log.info("새 방장 정보 저장 완료: {} (방: {})", newHost, roomName);
            }
        } catch (Exception e) {
            // 방이 이미 삭제되었거나 기타 오류가 발생한 경우
            log.error("참가자 제거 중 오류 발생: {}", e.getMessage());
            // 오류가 발생해도 작업은 계속 진행 - 사용자는 방을 나갈 수 있어야 함
        }
    }

    // 참가자 준비 상태 변경
    @Transactional
    public void setParticipantReady(String roomName, String nickname, boolean ready) {
        // 방 존재 확인
        redisRoomsRepository.findByName(roomName)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        
        // 참가자 준비 상태 변경
        redisRoomsRepository.setParticipantReady(roomName, nickname, ready);
    }

    /**
     * RoomDto를 RoomsResponse로 변환
     */
    private RoomsResponse mapToRoomsResponse(RoomDto roomDto) {
        // 맵 정보 조회
        Map map = mapRepository.findById(roomDto.mapId())
            .orElseThrow(() -> new CustomException(ErrorCode.MAP_NOT_FOUND));

        // MapSong 목록 조회 (선택적: 예시로 songs에 빈 리스트 넣을 수도 있음)
        List<MapSongResponseDto> songs = mapSongRepository.findByMapId(map.getId())
            .stream()
            .map(MapSongResponseDto::fromEntity)
            .toList();

        MapResponseDto mapDto = new MapResponseDto(map, songs);

        // 참가자 목록 조회
        List<HashMap<String, Object>> participants = redisRoomsRepository.getParticipantsWithStatus(roomDto.name());

        // RoomsResponse.fromDto를 사용하여 변환
        return RoomsResponse.fromDto(roomDto, mapDto, participants);
    }

    /**
     * RoomDto를 RoomListResponseDto로 변환 (내부 사용)
     */
    private RoomListResponseDto mapToRoomListDto(RoomDto roomDto) {
        try {
            // 맵 정보 조회
            Map map = mapRepository.findById(roomDto.mapId())
                .orElseThrow(() -> new CustomException(ErrorCode.MAP_NOT_FOUND));
                
            // MapSummaryDto 생성
            MapSummaryDto mapSummaryDto = MapSummaryDto.fromEntity(map);
            
            // 참가자 수 조회
            int participantCount = redisRoomsRepository
                .getParticipantsWithStatus(roomDto.name()).size();
                
            // RoomListResponseDto 생성
            return RoomListResponseDto.fromDto(
                roomDto,
                participantCount,
                mapSummaryDto
            );
        } catch (Exception e) {
            log.error("방 정보 변환 중 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.SERVER_ERROR);
        }
    }

    /**
     * 새 DTO 구조를 사용하여 전체 방 목록 조회 
     * (향후 API 버전 2를 위한 준비)
     */
    public List<RoomListResponseDto> findAllRoomsAsDto() {
        return redisRoomsRepository.findAll().stream()
            .map(this::mapToRoomListDto)
            .collect(Collectors.toList());
    }

    /**
     * 새 DTO 구조를 사용하여 방 상세 정보 조회
     * (향후 API 버전 2를 위한 준비)
     */
    public RoomListResponseDto findRoomDetailByName(String name) {
        RoomDto roomDto = redisRoomsRepository.findByName(name)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        
        return mapToRoomListDto(roomDto);
    }

    // 참가자 목록 가져오기
    public Set<String> getParticipants(String roomName) {
        // 방 존재 확인
        redisRoomsRepository.findByName(roomName)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        
        // 저장소에서 참가자 목록 조회하여 반환
        return redisRoomsRepository.getParticipants(roomName);
    }
} 