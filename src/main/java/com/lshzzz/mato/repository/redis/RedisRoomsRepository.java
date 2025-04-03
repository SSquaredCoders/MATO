package com.lshzzz.mato.repository.redis;

import com.lshzzz.mato.model.room.dto.RoomDto;
import com.lshzzz.mato.model.room.GameStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisRoomsRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String ROOMS_PREFIX = "rooms:";
    private static final String ROOMS_INDEX = "rooms:index";

    /**
     * 방 정보를 Redis에 저장
     * @param room 저장할 방 정보
     */
    public void save(RoomDto room) {
        String roomKey = ROOMS_PREFIX + room.name();
        Map<String, Object> roomData = new HashMap<>();
        
        roomData.put("id", room.id());
        roomData.put("name", room.name());
        roomData.put("password", room.password());
        roomData.put("maxParticipants", room.maxParticipants());
        roomData.put("host", room.host());
        roomData.put("gameStatus", room.gameStatus());
        roomData.put("mapId", room.mapId());
        
        // 방 정보 저장
        redisTemplate.opsForHash().putAll(roomKey, roomData);
        
        // 방 인덱스에 추가
        redisTemplate.opsForSet().add(ROOMS_INDEX, room.name());
        
        // 방 유효 시간 설정 (예: 3시간 후 자동 삭제)
        redisTemplate.expire(roomKey, 3, TimeUnit.HOURS);
        
        log.info("방 정보 저장 완료: {}", room.name());
    }

    /**
     * 이름으로 방 찾기
     * @param name 방 이름
     * @return 방 정보
     */
    public Optional<RoomDto> findByName(String name) {
        String roomKey = ROOMS_PREFIX + name;
        
        if (Boolean.FALSE.equals(redisTemplate.hasKey(roomKey))) {
            return Optional.empty();
        }
        
        Map<Object, Object> roomData = redisTemplate.opsForHash().entries(roomKey);
        return Optional.of(mapToRoomDto(roomData));
    }

    /**
     * 방 목록 조회
     * @return 전체 방 목록
     */
    public List<RoomDto> findAll() {
        Set<Object> roomNames = redisTemplate.opsForSet().members(ROOMS_INDEX);
        if (roomNames == null || roomNames.isEmpty()) {
            return Collections.emptyList();
        }
        
        return roomNames.stream()
            .map(name -> findByName((String) name))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    /**
     * 방 정보 삭제
     * @param name 방 이름
     */
    public void deleteByName(String name) {
        String roomKey = ROOMS_PREFIX + name;
        String participantsKey = roomKey + ":participants";
        String readyKey = roomKey + ":ready";
        
        // 방 정보 및 관련 데이터 삭제
        redisTemplate.delete(roomKey);
        redisTemplate.delete(participantsKey);
        redisTemplate.delete(readyKey);
        
        // 인덱스에서 제거
        redisTemplate.opsForSet().remove(ROOMS_INDEX, name);
        
        log.info("방 정보 삭제 완료: {}", name);
    }

    /**
     * 방 업데이트
     * @param room 업데이트할 방 정보
     */
    public void update(RoomDto room) {
        save(room); // 동일한 키로 저장하면 덮어쓰기 됨
    }

    /**
     * 게임 상태 업데이트
     * @param name 방 이름
     * @param gameStatus 새 게임 상태
     */
    public void updateGameStatus(String name, GameStatus gameStatus) {
        String roomKey = ROOMS_PREFIX + name;
        redisTemplate.opsForHash().put(roomKey, "gameStatus", gameStatus.name());
    }

    /**
     * 참가자 추가
     * @param roomName 방 이름
     * @param nickname 참가자 닉네임
     */
    public void addParticipant(String roomName, String nickname) {
        String participantsKey = ROOMS_PREFIX + roomName + ":participants";
        String readyKey = ROOMS_PREFIX + roomName + ":ready";
        
        redisTemplate.opsForSet().add(participantsKey, nickname);
        redisTemplate.opsForHash().put(readyKey, nickname, false);
    }

    /**
     * 참가자 제거
     * @param roomName 방 이름
     * @param nickname 참가자 닉네임
     */
    public void removeParticipant(String roomName, String nickname) {
        String participantsKey = ROOMS_PREFIX + roomName + ":participants";
        String readyKey = ROOMS_PREFIX + roomName + ":ready";
        
        redisTemplate.opsForSet().remove(participantsKey, nickname);
        redisTemplate.opsForHash().delete(readyKey, nickname);
    }

    /**
     * 참가자 준비 상태 변경
     * @param roomName 방 이름
     * @param nickname 참가자 닉네임
     * @param ready 준비 상태
     */
    public void setParticipantReady(String roomName, String nickname, boolean ready) {
        String readyKey = ROOMS_PREFIX + roomName + ":ready";
        redisTemplate.opsForHash().put(readyKey, nickname, ready);
    }

    /**
     * 방의 참가자 목록 조회
     * @param roomName 방 이름
     * @return 참가자 닉네임 목록
     */
    public Set<String> getParticipants(String roomName) {
        String participantsKey = ROOMS_PREFIX + roomName + ":participants";
        Set<Object> members = redisTemplate.opsForSet().members(participantsKey);
        
        if (members == null) {
            return new HashSet<>();
        }
        
        return members.stream()
            .map(Object::toString)
            .collect(Collectors.toSet());
    }

    /**
     * 방의 참가자 준비 상태 조회
     * @param roomName 방 이름
     * @return 참가자별 준비 상태 맵
     */
    public Map<String, Boolean> getParticipantReadyStatus(String roomName) {
        String readyKey = ROOMS_PREFIX + roomName + ":ready";
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(readyKey);
        
        Map<String, Boolean> result = new HashMap<>();
        entries.forEach((k, v) -> result.put(k.toString(), Boolean.TRUE.equals(v)));
        
        return result;
    }

    /**
     * 참가자 목록과 준비 상태를 결합하여 반환
     * @param roomName 방 이름
     * @return 참가자 정보 목록
     */
    public List<HashMap<String, Object>> getParticipantsWithStatus(String roomName) {
        Set<String> participants = getParticipants(roomName);
        Map<String, Boolean> readyStatus = getParticipantReadyStatus(roomName);
        
        List<HashMap<String, Object>> result = new ArrayList<>();
        
        for (String nickname : participants) {
            HashMap<String, Object> participant = new HashMap<>();
            participant.put("nickname", nickname);
            participant.put("ready", readyStatus.getOrDefault(nickname, false));
            result.add(participant);
        }
        
        return result;
    }

    /**
     * Redis 해시맵을 RoomDto로 변환
     */
    private RoomDto mapToRoomDto(Map<Object, Object> roomData) {
        return new RoomDto(
            roomData.get("id").toString(),
            roomData.get("name").toString(),
            roomData.get("password") != null ? roomData.get("password").toString() : null,
            Integer.parseInt(roomData.get("maxParticipants").toString()),
            roomData.get("host").toString(),
            roomData.get("gameStatus").toString(),
            Long.parseLong(roomData.get("mapId").toString())
        );
    }
} 