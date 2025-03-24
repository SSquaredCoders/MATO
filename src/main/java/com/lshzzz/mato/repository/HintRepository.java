package com.lshzzz.mato.repository;

import com.lshzzz.mato.model.song.Hint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HintRepository extends JpaRepository<Hint, Long> {
	List<Hint> findByMapSongId(Long mapSongId);

	void deleteByMapSongId(Long mapSongId);
}
