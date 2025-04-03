package com.lshzzz.mato.service.game;

import com.lshzzz.mato.model.game.dto.GameStatusResponse;
import com.lshzzz.mato.model.room.GameStatus;
import com.lshzzz.mato.repository.RoomsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @deprecated JPA 기반 게임 서비스. Redis 기반으로 전환 중이므로 {@link RedisGameService}를 사용하세요.
 */
@Service
@RequiredArgsConstructor
@Deprecated
public class GameService {
    private final RoomsRepository roomsRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisGameService redisGameService;

    // 게임 상태 저장소
    private final Map<String, GameStatus> roomStatuses = new ConcurrentHashMap<>();
    // 점수 저장소
    private final Map<String, Map<String, Integer>> roomScores = new ConcurrentHashMap<>();
    // 현재 곡 인덱스 저장소
    private final Map<String, Integer> currentSongIndices = new ConcurrentHashMap<>();

    @Transactional
    public void startGame(String roomName) {
        redisGameService.startGame(roomName);
    }

    @Transactional
    public void endGame(String roomName) {
        redisGameService.endGame(roomName);
    }

    @Transactional
    public void updateScore(String roomName, String playerNickname, int points, String reason) {
        redisGameService.updateScore(roomName, playerNickname, points, reason);
    }

    @Transactional
    public void nextSong(String roomName) {
        redisGameService.nextSong(roomName);
    }

    @Transactional
    public void skipSong(String roomName) {
        redisGameService.skipSong(roomName);
    }

    public GameStatusResponse getGameStatus(String roomName) {
        return redisGameService.getGameStatus(roomName);
    }

    public void resetGame(String roomName) {
        redisGameService.resetGame(roomName);
    }
} 