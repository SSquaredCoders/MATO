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

/**
 * @deprecated Redis 전환으로 인해 더 이상 사용되지 않음.
 */
@Deprecated
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

}