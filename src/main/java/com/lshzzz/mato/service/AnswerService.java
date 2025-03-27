package com.lshzzz.mato.service;

import com.lshzzz.mato.model.map.Map;
import com.lshzzz.mato.model.mapsongs.MapSong;
import com.lshzzz.mato.model.song.Answer;
import com.lshzzz.mato.model.song.dto.AnswerDto;
import com.lshzzz.mato.model.song.dto.AnswerRequestDto;
import com.lshzzz.mato.model.song.dto.AnswerResponseDto;
import com.lshzzz.mato.repository.AnswerRepository;
import com.lshzzz.mato.repository.MapSongRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerService {
	private final AnswerRepository answerRepository;
	private final MapSongRepository mapSongRepository;

	// 인증 검사 헬퍼 메서드
	private void checkMapOwnership(Map map, String authenticatedUserId) {
		if (!map.getUserId().equals(authenticatedUserId)) {
			throw new IllegalArgumentException("맵에 대한 권한이 없습니다.");
		}
	}

	@Transactional
	public List<AnswerResponseDto> addAnswers(Long mapSongId, AnswerRequestDto requestDto, String authenticatedUserId) {
		MapSong mapSong = mapSongRepository.findById(mapSongId)
			.orElseThrow(() -> new IllegalArgumentException("해당 맵-노래(mapSong)를 찾을 수 없습니다. ID: " + mapSongId));
			
		// 인증 검사
		checkMapOwnership(mapSong.getMap(), authenticatedUserId);

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

	@Transactional
	public List<AnswerResponseDto> updateAnswers(Long mapSongId, AnswerRequestDto requestDto, String authenticatedUserId) {
		MapSong mapSong = mapSongRepository.findById(mapSongId)
			.orElseThrow(() -> new IllegalArgumentException("MapSong을 찾을 수 없습니다."));
			
		// 인증 검사
		checkMapOwnership(mapSong.getMap(), authenticatedUserId);

		// 기존 정답 삭제
		answerRepository.deleteByMapSongId(mapSongId);

		// 새 정답 저장
		List<Answer> saved = requestDto.answerTexts().stream()
			.map(text -> Answer.builder()
				.answerText(text)
				.mapSong(mapSong)
				.build())
			.toList();

		answerRepository.saveAll(saved);
		return saved.stream().map(AnswerResponseDto::new).toList();
	}
}
