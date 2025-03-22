package com.lshzzz.mato.controller;

import com.lshzzz.mato.model.mapsongs.dto.MapSongRequestDto;
import com.lshzzz.mato.model.mapsongs.dto.MapSongResponseDto;
import com.lshzzz.mato.model.song.dto.AnswerRequestDto;
import com.lshzzz.mato.model.song.dto.AnswerResponseDto;
import com.lshzzz.mato.model.song.dto.HintRequestDto;
import com.lshzzz.mato.model.song.dto.HintResponseDto;
import com.lshzzz.mato.service.MapSongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/maps")
public class MapSongController {
	private final MapSongService mapSongService;

	// 특정 맵에 노래 추가 (기존 노래 또는 새로운 노래 등록 가능)
	@PostMapping("/{mapId}/songs")
	public ResponseEntity<MapSongResponseDto> addSongToMap(@PathVariable Long mapId,
		@RequestBody @Valid MapSongRequestDto requestDto) {
		MapSongResponseDto added = mapSongService.addSongToMap(mapId, requestDto);
		return ResponseEntity.status(HttpStatus.CREATED).body(added);
	}

	@PostMapping("/answers")
	public ResponseEntity<List<AnswerResponseDto>> addAnswers(
		@RequestBody AnswerRequestDto requestDto) {
		List<AnswerResponseDto> responses = answerService.addAnswers(requestDto);
		return ResponseEntity.ok(responses);
	}

	@PostMapping("/hints")
	public ResponseEntity<List<HintResponseDto>> addHints(
		@RequestBody HintRequestDto requestDto) {
		List<HintResponseDto> responses = hintService.addHints(requestDto);
		return ResponseEntity.ok(responses);
	}

	// 특정 맵에 연결된 모든 노래 조회
	@GetMapping("/{mapId}/songs")
	public ResponseEntity<List<MapSongResponseDto>> getSongsByMap(@PathVariable Long mapId) {
		List<MapSongResponseDto> songs = mapSongService.getSongsByMapId(mapId);
		return ResponseEntity.ok(songs);
	}

	// 특정 맵에서 노래 제거 (MapSong ID 기준으로 삭제)
	@DeleteMapping("/songs/{mapSongId}")
	public ResponseEntity<Void> removeSongFromMap(@PathVariable Long mapSongId) {
		mapSongService.removeSongFromMap(mapSongId);
		return ResponseEntity.noContent().build();
	}
}
