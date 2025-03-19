package com.lshzzz.mato.service;

import com.lshzzz.mato.model.song.Hint;
import com.lshzzz.mato.model.song.Song;
import com.lshzzz.mato.model.song.dto.HintDto;
import com.lshzzz.mato.model.song.dto.HintRequestDto;
import com.lshzzz.mato.model.song.dto.HintResponseDto;
import com.lshzzz.mato.repository.HintRepository;
import com.lshzzz.mato.repository.SongRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HintService {
	private final HintRepository hintRepository;
	private final SongRepository songRepository;

	@Transactional
	public List<HintResponseDto> addHintsToSong(Long songId, HintRequestDto requestDto) {
		Song song = songRepository.findById(songId)
			.orElseThrow(() -> new IllegalArgumentException("노래를 찾을 수 없습니다. ID: " + songId));

		List<Hint> hints = requestDto.hints().stream()
			.map(data -> Hint.builder()
				.song(song)
				.hintText(data.hintText())
				.revealTime(data.revealTime())
				.build())
			.collect(Collectors.toList());

		hintRepository.saveAll(hints);

		return hints.stream()
			.map(hint -> new HintResponseDto(hint.getId(), songId, hint.getHintText(), hint.getRevealTime()))
			.collect(Collectors.toList());
	}

	public List<HintDto> getHintsBySong(Long songId) {
		return hintRepository.findBySongId(songId)
			.stream()
			.map(hint -> new HintDto(hint.getId(), songId, hint.getHintText(), hint.getRevealTime()))
			.collect(Collectors.toList());
	}
}
