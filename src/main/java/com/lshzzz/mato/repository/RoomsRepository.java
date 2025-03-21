package com.lshzzz.mato.repository;

import com.lshzzz.mato.model.room.Rooms;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomsRepository extends JpaRepository<Rooms, Long> {

    // 방 제목으로 방 찾기
    Optional<Rooms> findByName(String name);
}