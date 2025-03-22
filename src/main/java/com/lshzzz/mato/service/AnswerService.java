package com.lshzzz.mato.service;

import com.lshzzz.mato.model.mapsongs.MapSong;
import com.lshzzz.mato.model.song.Answer;
import com.lshzzz.mato.model.song.Song;
import com.lshzzz.mato.model.song.dto.AnswerDto;
import com.lshzzz.mato.model.song.dto.AnswerRequestDto;
import com.lshzzz.mato.model.song.dto.AnswerResponseDto;
import com.lshzzz.mato.repository.AnswerRepository;
import com.lshzzz.mato.repository.MapSongRepository;
import com.lshzzz.mato.repository.SongRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnswerService {
	private final AnswerRepository answerRepository;
	private final MapSongRepository mapSongRepository;

	@Transactional
	public List<AnswerResponseDto> addAnswers(Long mapSongId, AnswerRequestDto requestDto) {
		MapSong mapSong = mapSongRepository.findById(mapSongId)
			.orElseThrow(() -> new IllegalArgumentException("해당 맵-노래(mapSong)를 찾을 수 없습니다. ID: " + mapSongId));

		List<Answer> answers = requestDto.answerTexts().stream()
			.map(text -> Answer.builder()
				.mapSong(mapSong)
				.answerText(text)
				.build())
			.toList();

		answerRepository.saveAll(answers);

		return answers.stream()
			.map(AnswerResponseDto::new)
			.toList();
	}

	public List<AnswerDto> getAnswersByMapSong(Long mapSongId) {
		return answerRepository.findByMapSongId(mapSongId)
			.stream()
			.map(answer -> new AnswerDto(answer.getId(), mapSongId, answer.getAnswerText()))
			.toList();
	}
}
