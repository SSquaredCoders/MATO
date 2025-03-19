package com.lshzzz.mato.service;

import com.lshzzz.mato.model.song.dto.HintDto;
import com.lshzzz.mato.repository.HintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HintService {
	private final HintRepository hintRepository;

	public List<HintDto> getHintsBySong(Long songId) {
		return hintRepository.findBySongId(songId)
			.stream()
			.map(hint -> new HintDto(hint.getId(), songId, hint.getHintText(), hint.getHintTime()))
			.collect(Collectors.toList());
	}
}
