package com.lshzzz.mato.repository.redis;

import com.lshzzz.mato.model.room.GameStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisGameRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String GAME_PREFIX = "game:";
    private static final String STATUS_SUFFIX = ":status";
    private static final String SCORES_SUFFIX = ":scores";
    private static final String SONG_INDEX_SUFFIX = ":songIndex";
    private static final String TOTAL_SONGS_SUFFIX = ":totalSongs";
    private static final long EXPIRY_HOURS = 3; // 3시간 후 자동 삭제

    /**
     * 게임 상태 설정
     */
    public void setGameStatus(String roomName, GameStatus status) {
        String key = GAME_PREFIX + roomName + STATUS_SUFFIX;
        redisTemplate.opsForValue().set(key, status.name());
        redisTemplate.expire(key, EXPIRY_HOURS, TimeUnit.HOURS);
        log.debug("게임 상태 저장: {}, 상태: {}", roomName, status);
    }

    /**
     * 게임 상태 조회
     */
    public GameStatus getGameStatus(String roomName) {
        String key = GAME_PREFIX + roomName + STATUS_SUFFIX;
        String status = (String) redisTemplate.opsForValue().get(key);
        
        if (status == null) {
            return GameStatus.WAITING; // 기본값
        }
        
        return GameStatus.valueOf(status);
    }

    /**
     * 점수 업데이트
     */
    public void updateScore(String roomName, String playerNickname, int points) {
        String key = GAME_PREFIX + roomName + SCORES_SUFFIX;
        redisTemplate.opsForHash().increment(key, playerNickname, points);
        redisTemplate.expire(key, EXPIRY_HOURS, TimeUnit.HOURS);
        log.debug("점수 업데이트: {}, 플레이어: {}, 점수: {}", roomName, playerNickname, points);
    }

    /**
     * 전체 점수 조회
     */
    public Map<String, Integer> getScores(String roomName) {
        String key = GAME_PREFIX + roomName + SCORES_SUFFIX;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        
        Map<String, Integer> result = new HashMap<>();
        entries.forEach((k, v) -> result.put(k.toString(), Integer.parseInt(v.toString())));
        
        return result;
    }

    /**
     * 현재 곡 인덱스 설정
     */
    public void setCurrentSongIndex(String roomName, int index) {
        String key = GAME_PREFIX + roomName + SONG_INDEX_SUFFIX;
        redisTemplate.opsForValue().set(key, index);
        redisTemplate.expire(key, EXPIRY_HOURS, TimeUnit.HOURS);
        log.debug("현재 곡 인덱스 설정: {}, 인덱스: {}", roomName, index);
    }

    /**
     * 현재 곡 인덱스 조회
     */
    public int getCurrentSongIndex(String roomName) {
        String key = GAME_PREFIX + roomName + SONG_INDEX_SUFFIX;
        Object value = redisTemplate.opsForValue().get(key);
        
        if (value == null) {
            return 0; // 기본값
        }
        
        return Integer.parseInt(value.toString());
    }

    /**
     * 총 곡 수 설정
     */
    public void setTotalSongs(String roomName, int totalSongs) {
        String key = GAME_PREFIX + roomName + TOTAL_SONGS_SUFFIX;
        redisTemplate.opsForValue().set(key, totalSongs);
        redisTemplate.expire(key, EXPIRY_HOURS, TimeUnit.HOURS);
    }

    /**
     * 총 곡 수 조회
     */
    public int getTotalSongs(String roomName) {
        String key = GAME_PREFIX + roomName + TOTAL_SONGS_SUFFIX;
        Object value = redisTemplate.opsForValue().get(key);
        
        if (value == null) {
            return 0; // 기본값
        }
        
        return Integer.parseInt(value.toString());
    }

    /**
     * 게임 데이터 초기화
     */
    public void resetGame(String roomName) {
        String statusKey = GAME_PREFIX + roomName + STATUS_SUFFIX;
        String scoresKey = GAME_PREFIX + roomName + SCORES_SUFFIX;
        String songIndexKey = GAME_PREFIX + roomName + SONG_INDEX_SUFFIX;
        String totalSongsKey = GAME_PREFIX + roomName + TOTAL_SONGS_SUFFIX;
        
        redisTemplate.delete(statusKey);
        redisTemplate.delete(scoresKey);
        redisTemplate.delete(songIndexKey);
        redisTemplate.delete(totalSongsKey);
        
        log.info("게임 데이터 초기화: {}", roomName);
    }
} 