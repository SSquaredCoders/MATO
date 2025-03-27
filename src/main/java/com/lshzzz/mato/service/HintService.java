package com.lshzzz.mato.service;

import com.lshzzz.mato.model.map.Map;
import com.lshzzz.mato.model.mapsongs.MapSong;
import com.lshzzz.mato.model.song.Hint;
import com.lshzzz.mato.model.song.dto.HintDto;
import com.lshzzz.mato.model.song.dto.HintRequestDto;
import com.lshzzz.mato.model.song.dto.HintResponseDto;
import com.lshzzz.mato.repository.HintRepository;
import com.lshzzz.mato.repository.MapSongRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HintService {
	private final HintRepository hintRepository;
	private final MapSongRepository mapSongRepository;
	
	// 인증 검사 헬퍼 메서드
	private void checkMapOwnership(Map map, String authenticatedUserId) {
		if (!map.getUserId().equals(authenticatedUserId)) {
			throw new IllegalArgumentException("맵에 대한 권한이 없습니다.");
		}
	}

	@Transactional
	public List<HintResponseDto> addHintsToMapSong(Long mapSongId, HintRequestDto requestDto, String authenticatedUserId) {
		MapSong mapSong = mapSongRepository.findById(mapSongId)
			.orElseThrow(() -> new IllegalArgumentException("MapSong 없음"));
			
		// 인증 검사
		checkMapOwnership(mapSong.getMap(), authenticatedUserId);

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
	public List<HintResponseDto> updateHints(Long mapSongId, HintRequestDto requestDto, String authenticatedUserId) {
		MapSong mapSong = mapSongRepository.findById(mapSongId)
			.orElseThrow(() -> new IllegalArgumentException("MapSong을 찾을 수 없습니다."));
			
		// 인증 검사
		checkMapOwnership(mapSong.getMap(), authenticatedUserId);

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
