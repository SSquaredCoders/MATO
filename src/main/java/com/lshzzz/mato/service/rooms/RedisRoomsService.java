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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.Hibernate;

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
        // 세션에서 저장된 닉네임을 우선적으로 사용
        HttpSession session = request.getSession();
        String sessionNickname = (String) session.getAttribute("userNickname");
        if (sessionNickname != null && !sessionNickname.trim().isEmpty()) {
            log.info("세션에 저장된 닉네임 사용: {}", sessionNickname);
            return sessionNickname;
        }
        
        // 기존 게스트 닉네임이 있으면 사용
        String guestNick = (String) session.getAttribute("guestNickname");
        if (guestNick != null && !guestNick.trim().isEmpty()) {
            log.info("기존 게스트 닉네임 사용: {}", guestNick);
            return guestNick;
        }
        
        // 없으면 새로 생성
        guestNick = "게스트" + (int) (Math.random() * 9000 + 1000);
        session.setAttribute("guestNickname", guestNick);
        log.info("새 게스트 닉네임 생성: {}", guestNick);
        return guestNick;
    }
    
    // 직접 지정한 닉네임을 사용하는 새 메서드
    public String resolveNickname(HttpServletRequest request, String specifiedNickname) {
        if (specifiedNickname != null && !specifiedNickname.trim().isEmpty()) {
            log.info("직접 지정한 닉네임 사용: {}", specifiedNickname);
            
            // 세션에도 저장 (다음 요청에서 사용)
            HttpSession session = request.getSession();
            session.setAttribute("userNickname", specifiedNickname);
            
            return specifiedNickname;
        }
        
        // 지정된 닉네임이 없으면 기존 메서드 호출
        return resolveNickname(request);
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
    @Transactional(readOnly = true)
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
            nickname, 
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
        try {
            // 방 존재 확인
            RoomDto room = redisRoomsRepository.findByName(roomName)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
            
            // 삭제 전 방에 남아있는 참가자 목록 확인
            Set<String> remainingParticipants = redisRoomsRepository.getParticipants(roomName);
            if (!remainingParticipants.isEmpty()) {
                log.info("방 {} 삭제 전 남아있는 참가자 {} 명을 모두 제거합니다", 
                        roomName, remainingParticipants.size());
                
                // 모든 참가자 제거
                for (String participant : remainingParticipants) {
                    redisRoomsRepository.removeParticipant(roomName, participant);
                    log.info("방 {} 삭제 전 참가자 {} 제거 완료", roomName, participant);
                }
                
                // 다시 한번 참가자 목록 확인
                Set<String> finalCheck = redisRoomsRepository.getParticipants(roomName);
                if (!finalCheck.isEmpty()) {
                    log.warn("방 {} 삭제 전 참가자 제거 후에도 {} 명의 참가자가 남아있습니다", 
                            roomName, finalCheck.size());
                } else {
                    log.info("방 {} 삭제 전 모든 참가자 제거 완료", roomName);
                }
            }
            
            // Redis에서 방 삭제
            log.info("방 이름으로 방 삭제 요청: {}", roomName);
            redisRoomsRepository.deleteByName(roomName);
            log.info("방 이름으로 방 삭제 성공: {}", roomName);
            
            // 방이 정말 삭제되었는지 확인
            try {
                redisRoomsRepository.findByName(roomName);
                log.warn("방 {} 삭제 명령 후에도 여전히 존재합니다", roomName);
                // 다시 한번 삭제 시도
                redisRoomsRepository.deleteByName(roomName);
            } catch (Exception e) {
                // 방을 찾을 수 없으면 정상적으로 삭제된 것
                log.info("방 {} 삭제 확인 완료", roomName);
            }
        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.ROOM_NOT_FOUND) {
                log.info("방 {}이 이미 존재하지 않습니다", roomName);
            } else {
                throw e;
            }
        } catch (Exception e) {
            log.error("방 {} 삭제 중 오류 발생: {}", roomName, e.getMessage(), e);
            throw new CustomException(ErrorCode.SERVER_ERROR);
        }
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
        
        // 방장 정보 확인
        String hostName = room.host();
        
        // 이미 참가자로 등록되어 있는지 확인
        Set<String> participants = redisRoomsRepository.getParticipants(roomName);
        
        // 1. 정확히 같은 닉네임으로 이미 참가 중인지 확인
        if (participants.contains(nickname)) {
            log.info("이미 방에 참가 중인 사용자입니다: {}, 방: {}", nickname, roomName);
            return; // 이미 참가 중이면 중복 추가하지 않고 조용히 반환
        }
        
        // 2. 방장이 입장하는 경우 특별 처리
        if (nickname.equals(hostName)) {
            log.info("방장 ({})이 입장합니다. 이전 참가자 확인: {}", hostName, participants);
            
            // 참가자 중에 방장과 동일한 닉네임을 가진 사용자가 있는지 확인 (다른 세션일 수 있음)
            // 이 경우는 발생하지 않아야 하지만, 중복 방지를 위해 체크
            if (!participants.contains(hostName)) {
                // 방장은 항상 첫 번째 참가자여야 함
                // 방장이 아직 참가자 목록에 없다면, 방장을 참가자로 추가
                redisRoomsRepository.addParticipant(roomName, hostName);
                log.info("방장 ({})을 참가자로 추가했습니다.", hostName);
            }
            
            return; // 방장은 이미 참가자로 추가했으므로 여기서 종료
        }
        
        // 3. 특수 케이스: 방에 방장만 있는 상태에서 다른 닉네임으로 입장 시도하는 경우
        if (participants.size() == 1) {
            String existingParticipant = participants.iterator().next();
            
            // 만약 참가자 목록의 유일한 사용자가 방장이 아닌 경우 (비정상 상태)
            if (!existingParticipant.equals(hostName)) {
                log.warn("방 {}에 방장({})이 아닌 참가자({})가 유일하게 있는 비정상 상태입니다.", 
                        roomName, hostName, existingParticipant);
                
                // 해당 참가자가 방장과 동일한 사용자일 가능성이 있음 (세션 변경 등)
                // 이 경우 참가자 목록 정리 후 다시 시작하는 것이 안전함
                if (!participants.contains(hostName)) {
                    log.info("방장({})이 참가자 목록에 없습니다. 방장을 참가자로 추가합니다.", hostName);
                    redisRoomsRepository.addParticipant(roomName, hostName);
                }
                
                // 비정상 참가자 제거 여부 (필요시 활성화)
                // log.info("비정상 참가자({})를 제거합니다.", existingParticipant);
                // redisRoomsRepository.removeParticipant(roomName, existingParticipant);
            }
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
            
            // 제거가 성공했는지 확인
            if (remainingParticipants.contains(nickname)) {
                log.warn("참가자 {} 제거 후에도 여전히 {} 방 목록에 존재합니다. 다시 제거 시도", nickname, roomName);
                // 다시 한번 제거 시도
                redisRoomsRepository.removeParticipant(roomName, nickname);
                // 다시 참가자 수 확인
                remainingParticipants = redisRoomsRepository.getParticipants(roomName);
                participantCount = remainingParticipants.size();
                log.info("재시도 후 방 {} 남은 참가자 수: {}", roomName, participantCount);
            }
            
            // 참가자가 모두 나가면 방 삭제
            if (participantCount == 0) {
                log.info("방 {} 참가자가 모두 나갔으므로 방을 자동 삭제합니다.", roomName);
                try {
                    redisRoomsRepository.deleteByName(roomName);
                    log.info("방 {} 삭제 완료", roomName);
                } catch (Exception e) {
                    log.error("방 삭제 중 오류: {}", e.getMessage());
                }
                
                // 방이 실제로 삭제되었는지 확인
                try {
                    if (redisRoomsRepository.findByName(roomName).isPresent()) {
                        log.warn("방 {} 삭제 시도 후에도 여전히 존재합니다. 다시 삭제 시도", roomName);
                        redisRoomsRepository.deleteByName(roomName);
                    } else {
                        log.info("방 {} 삭제 확인 완료", roomName);
                    }
                } catch (Exception ex) {
                    log.info("방 {} 이미 삭제됨", roomName);
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
                
            // 명시적으로 맵 컬렉션 초기화 (지연 로딩 문제 해결)
            Hibernate.initialize(map.getMapSongs());
                
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
            log.error("방 정보 변환 중 오류 발생: {}", e.getMessage());
            
            // 오류 발생시 기본 정보만 담은 폴백 응답 생성
            MapSummaryDto fallbackMapSummary = new MapSummaryDto(
                roomDto.mapId(), 
                "정보 로딩 중", 
                "정보를 불러오는 중입니다", 
                true,
                0, 
                null,
                null,
                null
            );
            
            // 기본 정보로 DTO 생성하여 에러 방지
            return RoomListResponseDto.fromDto(
                roomDto,
                redisRoomsRepository.getParticipantsWithStatus(roomDto.name()).size(),
                fallbackMapSummary
            );
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

    /**
     * 사용자 식별자 관계 확인
     * 같은 사용자가 다른 식별자로 접속했을 가능성을 판단
     *
     * @param user1 첫 번째 식별자
     * @param user2 두 번째 식별자
     * @return 같은 사용자일 가능성이 있으면 true
     */
    public boolean isPossiblyRelatedUser(String user1, String user2) {
        // 완전히 동일한 경우
        if (user1.equals(user2)) {
            return true;
        }
        
        // 로그인/비로그인 상태에 따른 변경 패턴 확인
        // 예: "게스트1234"와 "test1" 같은 패턴
        boolean isGuest1 = user1.startsWith("게스트");
        boolean isGuest2 = user2.startsWith("게스트");
        
        // 하나는 게스트, 하나는 아닌 경우 - 로그인/로그아웃에 따른 전환일 수 있음
        if (isGuest1 != isGuest2) {
            log.info("사용자 식별자 {} / {} 관계 분석: 게스트/회원 전환 가능성 있음", user1, user2);
            // 여기서는 단순히 가능성만 반환
            // 실제 구현에서는 세션이나 IP 등 추가 정보로 검증 필요
            return true;
        }
        
        // 그 외의 경우는 관계없음으로 판단
        return false;
    }
} 