package com.lshzzz.mato.repository;

import com.lshzzz.mato.model.song.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
	List<Answer> findByMapSongId(Long mapSongId);

	void deleteByMapSongId(Long mapSongId);
}
