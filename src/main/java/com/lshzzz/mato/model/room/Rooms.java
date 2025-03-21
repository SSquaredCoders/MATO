package com.lshzzz.mato.model.room;

import com.lshzzz.mato.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Builder
    public Rooms(String name, String password, String host, Integer participants,
        GameStatus gameStatus) {
        this.name = name;
        this.password = password;
        this.host = host;
        this.participants = (participants != null) ? participants : 1;
        this.gameStatus = (gameStatus != null) ? gameStatus : GameStatus.WAITING;
    }

    // 방 제목 수정 메서드
    public void updateName(String name) {
        this.name = name;
    }

    // 방 비밀번호 수정 메서드
    public void updatePassword(String password) {
        this.password = password;
    }

    // 참가 인원 증가 메서드
    public void increaseParticipants() {
        this.participants++;
    }

    // 참가 인원 감소 메서드
    public void decreaseParticipants() {
        if (this.participants > 0) {
            this.participants--;
        }
    }

    // 게임 상태 수정 메서드
    public void updateGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }
}