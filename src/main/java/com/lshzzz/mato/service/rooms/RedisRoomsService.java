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
        
        // 참가자 수 제한 확인
        Set<String> participants = redisRoomsRepository.getParticipants(roomName);
        if (participants.size() >= room.maxParticipants()) {
            throw new CustomException(ErrorCode.ROOM_FULL);
        }
        
        // 참가자 추가
        redisRoomsRepository.addParticipant(roomName, nickname);
    }

    // 참가자 제거
    @Transactional
    public void removeParticipant(String roomName, String nickname) {
        // 방 존재 확인
        redisRoomsRepository.findByName(roomName)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        
        // 참가자 제거
        redisRoomsRepository.removeParticipant(roomName, nickname);
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
} 