package com.lshzzz.mato.service;

import com.lshzzz.mato.model.song.Answer;
import com.lshzzz.mato.model.song.Song;
import com.lshzzz.mato.model.song.dto.AnswerDto;
import com.lshzzz.mato.model.song.dto.AnswerRequestDto;
import com.lshzzz.mato.model.song.dto.AnswerResponseDto;
import com.lshzzz.mato.repository.AnswerRepository;
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
	private final SongRepository songRepository;

	@Transactional
	public List<AnswerResponseDto> addAnswersToSong(Long songId, AnswerRequestDto requestDto) {
		Song song = songRepository.findById(songId)
			.orElseThrow(() -> new IllegalArgumentException("노래를 찾을 수 없습니다. ID: " + songId));

		List<Answer> answers = requestDto.answerTexts().stream()
			.map(text -> Answer.builder()
				.song(song)
				.answerText(text)
				.build())
			.collect(Collectors.toList());

		answerRepository.saveAll(answers);

		return answers.stream()
			.map(answer -> new AnswerResponseDto(answer.getId(), songId, answer.getAnswerText()))
			.collect(Collectors.toList());
	}

	public List<AnswerDto> getAnswersBySong(Long songId) {
		return answerRepository.findBySongId(songId)
			.stream()
			.map(answer -> new AnswerDto(answer.getId(), songId, answer.getAnswerText()))
			.collect(Collectors.toList());
	}
}
