package com.lshzzz.mato.service.rooms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 사용자 세션과 방 관계를 추적하는 유틸리티 클래스
 */
@Slf4j
@Component
public class SessionTracker {
    
    // 세션ID -> 사용자 이름 매핑
    private final Map<String, String> sessionToUser = new ConcurrentHashMap<>();
    
    // 세션ID -> 방 이름 매핑
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();
    
    // 사용자 이름 -> 세션ID 집합 매핑 (한 사용자가 여러 세션 가질 수 있음)
    private final Map<String, Set<String>> userToSessions = new ConcurrentHashMap<>();
    
    // 방 이름 -> 세션ID 집합 매핑 (한 방에 여러 세션 있음)
    private final Map<String, Set<String>> roomToSessions = new ConcurrentHashMap<>();
    
    /**
     * 새 세션 등록
     *
     * @param sessionId 세션 ID
     * @param username 사용자 이름
     * @param roomName 방 이름
     */
    public void registerSession(String sessionId, String username, String roomName) {
        log.info("새 세션 등록: 세션={}, 사용자={}, 방={}", sessionId, username, roomName);
        
        // 세션ID -> 사용자 이름, 방 이름 매핑
        sessionToUser.put(sessionId, username);
        sessionToRoom.put(sessionId, roomName);
        
        // 사용자 이름 -> 세션ID 집합 매핑
        userToSessions.computeIfAbsent(username, k -> new CopyOnWriteArraySet<>()).add(sessionId);
        
        // 방 이름 -> 세션ID 집합 매핑
        roomToSessions.computeIfAbsent(roomName, k -> new CopyOnWriteArraySet<>()).add(sessionId);
        
        // 현재 상태 로그
        logCurrentStatus(username, roomName);
    }
    
    /**
     * 세션 제거
     *
     * @param sessionId 세션 ID
     */
    public void removeSession(String sessionId) {
        String username = sessionToUser.get(sessionId);
        String roomName = sessionToRoom.get(sessionId);
        
        log.info("세션 제거: 세션={}, 사용자={}, 방={}", sessionId, username, roomName);
        
        // 세션ID -> 사용자 이름, 방 이름 매핑 제거
        sessionToUser.remove(sessionId);
        sessionToRoom.remove(sessionId);
        
        // 사용자 이름 -> 세션ID 집합 매핑 업데이트
        if (username != null) {
            Set<String> userSessions = userToSessions.get(username);
            if (userSessions != null) {
                userSessions.remove(sessionId);
                if (userSessions.isEmpty()) {
                    userToSessions.remove(username);
                }
            }
        }
        
        // 방 이름 -> 세션ID 집합 매핑 업데이트
        if (roomName != null) {
            Set<String> roomSessions = roomToSessions.get(roomName);
            if (roomSessions != null) {
                roomSessions.remove(sessionId);
                if (roomSessions.isEmpty()) {
                    roomToSessions.remove(roomName);
                }
            }
        }
        
        // 현재 상태 로그
        if (username != null && roomName != null) {
            logCurrentStatus(username, roomName);
        }
    }
    
    /**
     * 사용자가 해당 방에 다른 세션으로 접속 중인지 확인
     *
     * @param username 사용자 이름
     * @param roomName 방 이름
     * @return 이미 접속 중이면 true
     */
    public boolean isUserInRoom(String username, String roomName) {
        Set<String> userSessions = userToSessions.get(username);
        Set<String> roomSessions = roomToSessions.get(roomName);
        
        if (userSessions == null || roomSessions == null) {
            return false;
        }
        
        // 사용자의 세션과 방의 세션 교집합 확인
        for (String sessionId : userSessions) {
            if (roomSessions.contains(sessionId)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 사용자의 다른 모든 세션 찾기
     *
     * @param username 사용자 이름
     * @return 사용자의 모든 세션 ID 집합
     */
    public Set<String> getUserSessions(String username) {
        return userToSessions.getOrDefault(username, new CopyOnWriteArraySet<>());
    }
    
    /**
     * 방의 모든 세션 찾기
     *
     * @param roomName 방 이름
     * @return 방의 모든 세션 ID 집합
     */
    public Set<String> getRoomSessions(String roomName) {
        return roomToSessions.getOrDefault(roomName, new CopyOnWriteArraySet<>());
    }
    
    /**
     * 세션에 연결된 사용자 찾기
     *
     * @param sessionId 세션 ID
     * @return 사용자 이름
     */
    public String getUserBySession(String sessionId) {
        return sessionToUser.get(sessionId);
    }
    
    /**
     * 세션에 연결된 방 찾기
     *
     * @param sessionId 세션 ID
     * @return 방 이름
     */
    public String getRoomBySession(String sessionId) {
        return sessionToRoom.get(sessionId);
    }
    
    /**
     * 현재 상태 로그 출력
     *
     * @param username 사용자 이름
     * @param roomName 방 이름
     */
    private void logCurrentStatus(String username, String roomName) {
        Set<String> userSessions = userToSessions.get(username);
        Set<String> roomSessions = roomToSessions.get(roomName);
        
        if (userSessions != null) {
            log.info("사용자 {} 세션 수: {}", username, userSessions.size());
        }
        
        if (roomSessions != null) {
            log.info("방 {} 세션 수: {}", roomName, roomSessions.size());
        }
    }
} 