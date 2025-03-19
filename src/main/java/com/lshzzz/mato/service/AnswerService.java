package com.lshzzz.mato.service;

import com.lshzzz.mato.model.song.Answer;
import com.lshzzz.mato.model.song.dto.AnswerDto;
import com.lshzzz.mato.repository.AnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnswerService {
	private final AnswerRepository answerRepository;

	public List<AnswerDto> getAnswersBySong(Long songId) {
		return answerRepository.findBySongId(songId)
			.stream()
			.map(answer -> new AnswerDto(answer.getId(), songId, answer.getAnswerText()))
			.collect(Collectors.toList());
	}
}
