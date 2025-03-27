package com.lshzzz.mato.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lshzzz.mato.model.song.Song;

public interface SongRepository extends JpaRepository<Song, Long> {
    // URL로 노래 검색
    Optional<Song> findByYoutubeUrl(String youtubeUrl);
    
    // 제목으로 노래 검색 (부분 일치)
    List<Song> findByTitleContaining(String title);
}
