package com.lshzzz.mato.service.game;

import com.lshzzz.mato.model.game.dto.GameStatusResponse;
import com.lshzzz.mato.model.room.GameStatus;
import com.lshzzz.mato.model.room.Rooms;
import com.lshzzz.mato.repository.RoomsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GameService {
    private final RoomsRepository roomsRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // 게임 상태 저장소
    private final Map<String, GameStatus> roomStatuses = new ConcurrentHashMap<>();
    // 점수 저장소
    private final Map<String, Map<String, Integer>> roomScores = new ConcurrentHashMap<>();
    // 현재 곡 인덱스 저장소
    private final Map<String, Integer> currentSongIndices = new ConcurrentHashMap<>();

    @Transactional
    public void startGame(String roomName) {
        Rooms room = roomsRepository.findByName(roomName)
            .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));

        room.updateGameStatus(GameStatus.PLAYING);
        roomStatuses.put(roomName, GameStatus.PLAYING);
        currentSongIndices.put(roomName, 0);

        // 초기 점수 초기화
        roomScores.put(roomName, new ConcurrentHashMap<>());

        // 게임 시작 메시지 브로드캐스트
        messagingTemplate.convertAndSend(
            "/topic/rooms/" + roomName,
            new GameStatusResponse(
                room.getId(),
                roomName,
                GameStatus.PLAYING,
                roomScores.get(roomName),
                null,
                currentSongIndices.get(roomName),
                room.getMap().getMapSongs().size(),
                LocalDateTime.now()
            )
        );
    }

    @Transactional
    public void endGame(String roomName) {
        Rooms room = roomsRepository.findByName(roomName)
            .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));

        room.updateGameStatus(GameStatus.FINISHED);
        roomStatuses.put(roomName, GameStatus.FINISHED);

        // 게임 종료 메시지 브로드캐스트
        messagingTemplate.convertAndSend(
            "/topic/rooms/" + roomName,
            new GameStatusResponse(
                room.getId(),
                roomName,
                GameStatus.FINISHED,
                roomScores.get(roomName),
                null,
                currentSongIndices.get(roomName),
                room.getMap().getMapSongs().size(),
                LocalDateTime.now()
            )
        );
    }

    @Transactional
    public void updateScore(String roomName, String playerNickname, int points, String reason) {
        Map<String, Integer> scores = roomScores.getOrDefault(roomName, new ConcurrentHashMap<>());
        scores.merge(playerNickname, points, Integer::sum);

        Rooms room = roomsRepository.findByName(roomName)
            .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));

        // 점수 업데이트 메시지 브로드캐스트
        messagingTemplate.convertAndSend(
            "/topic/rooms/" + roomName,
            new GameStatusResponse(
                room.getId(),
                roomName,
                roomStatuses.getOrDefault(roomName, GameStatus.WAITING),
                scores,
                null,
                currentSongIndices.get(roomName),
                room.getMap().getMapSongs().size(),
                LocalDateTime.now()
            )
        );
    }

    @Transactional
    public void nextSong(String roomName) {
        Rooms room = roomsRepository.findByName(roomName)
            .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));

        int currentIndex = currentSongIndices.getOrDefault(roomName, 0);
        int totalSongs = room.getMap().getMapSongs().size();

        if (currentIndex < totalSongs - 1) {
            currentSongIndices.put(roomName, currentIndex + 1);
            
            // 다음 곡 정보 브로드캐스트
            messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomName,
                new GameStatusResponse(
                    room.getId(),
                    roomName,
                    GameStatus.PLAYING,
                    roomScores.get(roomName),
                    null,
                    currentIndex + 1,
                    totalSongs,
                    LocalDateTime.now()
                )
            );
        } else {
            endGame(roomName);
        }
    }

    @Transactional
    public void skipSong(String roomName) {
        nextSong(roomName);
    }

    public GameStatusResponse getGameStatus(String roomName) {
        Rooms room = roomsRepository.findByName(roomName)
            .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));

        return new GameStatusResponse(
            room.getId(),
            roomName,
            roomStatuses.getOrDefault(roomName, GameStatus.WAITING),
            roomScores.getOrDefault(roomName, new ConcurrentHashMap<>()),
            null,
            currentSongIndices.getOrDefault(roomName, 0),
            room.getMap().getMapSongs().size(),
            LocalDateTime.now()
        );
    }

    public void resetGame(String roomName) {
        roomStatuses.remove(roomName);
        roomScores.remove(roomName);
        currentSongIndices.remove(roomName);

        Rooms room = roomsRepository.findByName(roomName)
            .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));
        room.updateGameStatus(GameStatus.WAITING);
    }
} 