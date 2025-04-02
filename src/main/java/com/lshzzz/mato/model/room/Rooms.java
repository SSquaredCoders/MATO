package com.lshzzz.mato.model.room;

import com.lshzzz.mato.model.BaseEntity;
import com.lshzzz.mato.model.map.Map;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Rooms extends BaseEntity {

    // ID(PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Room name
    @Column(nullable = false, unique = true, length = 20)
    private String name;

    // Password
    @Column(length = 20)
    private String password;

    // Max Participants
    @Column(nullable = false)
    private Integer maxParticipants;

    // Host name
    @Column(nullable = false, length = 20)
    private String host;

    // Participants Count
    @Column(nullable = false)
    private Integer participants = 1;

    // Game Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus gameStatus = GameStatus.WAITING;

    // 맵 연관관계
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "map_id", nullable = false)
    private Map map;

    @ElementCollection
    @CollectionTable(name = "room_participants", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "nickname")
    private List<String> participantNicknames = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "room_participant_status", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "ready")
    private List<Boolean> participantReadyStatus = new ArrayList<>();

    public void updateName(String name) {
        this.name = name;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void increaseParticipants() {
        this.participants++;
    }

    public void decreaseParticipants() {
        if (this.participants > 0) {
            this.participants--;
        }
    }

    public void updateGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }

    public void updateMap(Map map) {
        this.map = map;
    }

    /**
     * 참가자 추가
     * 
     * @param nickname 추가할 참가자 닉네임
     * @throws IllegalStateException 방이 가득 찼을 경우
     */
    public void addParticipant(String nickname) {
        Objects.requireNonNull(nickname, "참가자 닉네임은 null일 수 없습니다.");
        
        if (participants >= this.maxParticipants) {
            throw new IllegalStateException("방이 가득 찼습니다.");
        }
        
        // 중복 참가자 검사
        if (participantNicknames.contains(nickname)) {
            return; // 이미 참가 중인 경우 무시
        }
        
        participantNicknames.add(nickname);
        participantReadyStatus.add(false);
        participants++;
    }

    /**
     * 참가자 제거
     * 
     * @param nickname 제거할 참가자 닉네임
     */
    public void removeParticipant(String nickname) {
        Objects.requireNonNull(nickname, "참가자 닉네임은 null일 수 없습니다.");
        
        int index = participantNicknames.indexOf(nickname);
        if (index != -1) {
            participantNicknames.remove(index);
            
            // 안전하게 인덱스 체크 후 제거
            if (index < participantReadyStatus.size()) {
                participantReadyStatus.remove(index);
            }
            
            participants = Math.max(0, participants - 1);
        }
    }

    /**
     * 참가자 준비 상태 변경
     * 
     * @param nickname 준비 상태를 변경할 참가자 닉네임
     * @param ready 준비 상태 (true: 준비 완료, false: 준비 취소)
     */
    public void setParticipantReady(String nickname, boolean ready) {
        Objects.requireNonNull(nickname, "참가자 닉네임은 null일 수 없습니다.");
        
        int index = participantNicknames.indexOf(nickname);
        if (index != -1 && index < participantReadyStatus.size()) {
            participantReadyStatus.set(index, ready);
        }
    }

    /**
     * 모든 참가자의 목록을 반환
     * 
     * @return 참가자 정보 목록 (닉네임과 준비 상태)
     */
    public List<HashMap<String, Object>> getParticipants() {
        List<HashMap<String, Object>> result = new ArrayList<>();
        
        // 두 리스트의 길이 확인 및 조정
        int minSize = Math.min(participantNicknames.size(), participantReadyStatus.size());
        
        for (int i = 0; i < minSize; i++) {
            HashMap<String, Object> participant = new HashMap<>();
            participant.put("nickname", participantNicknames.get(i));
            participant.put("ready", participantReadyStatus.get(i));
            result.add(participant);
        }
        
        return result;
    }
    
    /**
     * 참가자의 닉네임 목록 반환
     */
    public List<String> getParticipantNicknames() {
        return new ArrayList<>(participantNicknames);
    }
    
    /**
     * 참가자의 준비 상태 목록 반환
     */
    public List<Boolean> getParticipantReadyStatus() {
        return new ArrayList<>(participantReadyStatus);
    }
}