package com.lshzzz.mato.service.game;

import com.lshzzz.mato.exception.CustomException;
import com.lshzzz.mato.exception.ErrorCode;
import com.lshzzz.mato.model.game.dto.CurrentSongDto;
import com.lshzzz.mato.model.game.dto.GameStatusDto;
import com.lshzzz.mato.model.game.dto.GameStatusResponse;
import com.lshzzz.mato.model.map.Map;
import com.lshzzz.mato.model.mapsongs.MapSong;
import com.lshzzz.mato.model.room.GameStatus;
import com.lshzzz.mato.model.room.dto.RoomDto;
import com.lshzzz.mato.repository.MapRepository;
import com.lshzzz.mato.repository.redis.RedisGameRepository;
import com.lshzzz.mato.repository.redis.RedisRoomsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class RedisGameService {
    private final RedisRoomsRepository redisRoomsRepository;
    private final RedisGameRepository redisGameRepository;
    private final MapRepository mapRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void startGame(String roomName) {
        RoomDto room = redisRoomsRepository.findByName(roomName)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        redisRoomsRepository.updateGameStatus(roomName, GameStatus.PLAYING);
        redisGameRepository.setGameStatus(roomName, GameStatus.PLAYING);
        redisGameRepository.setCurrentSongIndex(roomName, 0);

        Map map = mapRepository.findById(room.mapId())
            .orElseThrow(() -> new CustomException(ErrorCode.MAP_NOT_FOUND));
        int totalSongs = map.getMapSongs().size();
        redisGameRepository.setTotalSongs(roomName, totalSongs);

        broadcastGameStatus(room);
        log.info("게임 시작: {}", roomName);
    }

    @Transactional
    public void endGame(String roomName) {
        RoomDto room = redisRoomsRepository.findByName(roomName)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        redisRoomsRepository.updateGameStatus(roomName, GameStatus.FINISHED);
        redisGameRepository.setGameStatus(roomName, GameStatus.FINISHED);

        broadcastGameStatus(room);
        log.info("게임 종료: {}", roomName);
    }

    @Transactional
    public void updateScore(String roomName, String playerNickname, int points, String reason) {
        RoomDto room = redisRoomsRepository.findByName(roomName)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        redisGameRepository.updateScore(roomName, playerNickname, points);
        broadcastGameStatus(room);

        log.info("점수 업데이트: {}, 플레이어: {}, 점수: {}, 이유: {}",
            roomName, playerNickname, points, reason);
    }

    @Transactional
    public void nextSong(String roomName) {
        RoomDto room = redisRoomsRepository.findByName(roomName)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        Map map = mapRepository.findById(room.mapId())
            .orElseThrow(() -> new CustomException(ErrorCode.MAP_NOT_FOUND));

        int currentIndex = redisGameRepository.getCurrentSongIndex(roomName);
        int totalSongs = map.getMapSongs().size();

        if (currentIndex < totalSongs - 1) {
            redisGameRepository.setCurrentSongIndex(roomName, currentIndex + 1);
            broadcastGameStatus(room);
            log.info("다음 곡으로 이동: {}, 인덱스: {}/{}", roomName, currentIndex + 1, totalSongs);
        } else {
            endGame(roomName);
        }
    }

    @Transactional
    public void skipSong(String roomName) {
        nextSong(roomName);
    }

    public GameStatusResponse getGameStatus(String roomName) {
        RoomDto room = redisRoomsRepository.findByName(roomName)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        
        // GameStatusDto를 생성해 GameStatusResponse로 변환
        GameStatusDto statusDto = createGameStatusDto(room);
        return convertToResponse(statusDto);
    }

    public void resetGame(String roomName) {
        Optional<RoomDto> roomOpt = redisRoomsRepository.findByName(roomName);
        roomOpt.ifPresent(room -> redisRoomsRepository.updateGameStatus(roomName, GameStatus.WAITING));
        redisGameRepository.resetGame(roomName);
        log.info("게임 초기화: {}", roomName);
    }

    private void broadcastGameStatus(RoomDto room) {
        // GameStatusDto를 생성해 GameStatusResponse로 변환 후 브로드캐스트
        GameStatusDto statusDto = createGameStatusDto(room);
        GameStatusResponse response = convertToResponse(statusDto);
        messagingTemplate.convertAndSend("/topic/rooms/" + room.name(), response);
    }

    private GameStatusDto createGameStatusDto(RoomDto room) {
        Map map = mapRepository.findById(room.mapId())
            .orElseThrow(() -> new CustomException(ErrorCode.MAP_NOT_FOUND));
        
        String roomName = room.name();
        int currentSongIndex = redisGameRepository.getCurrentSongIndex(roomName);
        GameStatus gameStatus = redisGameRepository.getGameStatus(roomName);
        
        // 현재 노래 정보 가져오기
        CurrentSongDto currentSongDto = null;
        int totalSongs = 0;
        
        try {
            List<MapSong> mapSongs = map.getMapSongs();
            totalSongs = mapSongs.size();
            
            if (!mapSongs.isEmpty() && currentSongIndex >= 0 && currentSongIndex < mapSongs.size() 
                    && (gameStatus == GameStatus.PLAYING || gameStatus == GameStatus.FINISHED)) {
                MapSong currentSong = mapSongs.get(currentSongIndex);
                currentSongDto = CurrentSongDto.fromEntity(currentSong);
            }
        } catch (Exception e) {
            log.error("게임 상태 조회 중 오류 발생: {}", e.getMessage(), e);
        }
        
        return new GameStatusDto(
            Long.parseLong(room.id()),
            roomName,
            gameStatus,
            redisGameRepository.getScores(roomName),
            currentSongDto,
            currentSongIndex,
            totalSongs,
            LocalDateTime.now()
        );
    }
    
    // 이전 버전 호환성을 위한 변환 메서드
    private GameStatusResponse convertToResponse(GameStatusDto dto) {
        return dto.toResponse();
    }

    /**
     * 새 DTO 구조를 사용하여 게임 상태 조회
     * (향후 API 버전 2를 위한 준비)
     */
    public GameStatusDto getGameStatusAsDto(String roomName) {
        RoomDto room = redisRoomsRepository.findByName(roomName)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        
        return createGameStatusDto(room);
    }
}
