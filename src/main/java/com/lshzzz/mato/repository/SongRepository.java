package com.lshzzz.mato.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lshzzz.mato.model.song.Song;

public interface SongRepository extends JpaRepository<Song, Long> {
}
