package com.lshzzz.mato.service;

import com.lshzzz.mato.model.mapsongs.MapSong;
import com.lshzzz.mato.model.song.Hint;
import com.lshzzz.mato.model.song.Song;
import com.lshzzz.mato.model.song.dto.HintDto;
import com.lshzzz.mato.model.song.dto.HintRequestDto;
import com.lshzzz.mato.model.song.dto.HintResponseDto;
import com.lshzzz.mato.repository.HintRepository;
import com.lshzzz.mato.repository.MapSongRepository;
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
	private final MapSongRepository mapSongRepository;

	@Transactional
	public List<HintResponseDto> addHintsToMapSong(Long mapSongId, HintRequestDto requestDto) {
		MapSong mapSong = mapSongRepository.findById(mapSongId)
			.orElseThrow(() -> new IllegalArgumentException("MapSong 없음"));

		List<Hint> hints = requestDto.hints().stream()
			.map(data -> Hint.builder()
				.mapSong(mapSong)
				.hintText(data.hintText())
				.revealTime(data.revealTime())
				.build())
			.toList();

		hintRepository.saveAll(hints);

		return hints.stream()
			.map(hint -> new HintResponseDto(hint.getId(), mapSongId, hint.getHintText(), hint.getRevealTime()))
			.toList();
	}

	public List<HintDto> getHintsByMapSong(Long mapSongId) {
		return hintRepository.findByMapSongId(mapSongId).stream()
			.map(h -> new HintDto(h.getId(), h.getMapSong().getId(), h.getHintText(), h.getRevealTime()))
			.toList();
	}

	@Transactional
	public List<HintResponseDto> updateHints(Long mapSongId, HintRequestDto requestDto) {
		MapSong mapSong = mapSongRepository.findById(mapSongId)
			.orElseThrow(() -> new IllegalArgumentException("MapSong을 찾을 수 없습니다."));

		// 기존 힌트 삭제
		hintRepository.deleteByMapSongId(mapSongId);

		// 새 힌트 저장
		List<Hint> saved = requestDto.hints().stream()
			.map(h -> Hint.builder()
				.hintText(h.hintText())
				.revealTime(h.revealTime())
				.mapSong(mapSong)
				.build())
			.toList();

		hintRepository.saveAll(saved);
		return saved.stream().map(HintResponseDto::new).toList();
	}

}
