package com.lshzzz.mato.controller;

import com.lshzzz.mato.model.mapsongs.dto.MapSongRequestDto;
import com.lshzzz.mato.model.mapsongs.dto.MapSongResponseDto;
import com.lshzzz.mato.model.song.dto.AnswerDto;
import com.lshzzz.mato.model.song.dto.AnswerRequestDto;
import com.lshzzz.mato.model.song.dto.AnswerResponseDto;
import com.lshzzz.mato.model.song.dto.HintDto;
import com.lshzzz.mato.model.song.dto.HintRequestDto;
import com.lshzzz.mato.model.song.dto.HintResponseDto;
import com.lshzzz.mato.model.users.CustomUserDetails;
import com.lshzzz.mato.service.AnswerService;
import com.lshzzz.mato.service.HintService;
import com.lshzzz.mato.service.MapSongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/maps")
public class MapSongController {
	private final MapSongService mapSongService;
	private final AnswerService answerService;
	private final HintService hintService;

	// ✅ 맵에 노래 추가
	@PostMapping("/{mapId}/songs")
	public ResponseEntity<MapSongResponseDto> addSongToMap(
		@PathVariable Long mapId,
		@RequestBody @Valid MapSongRequestDto requestDto,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		String authenticatedUserId = userDetails.getUsername();
		MapSongResponseDto added = mapSongService.addSongToMap(mapId, requestDto, authenticatedUserId);
		return ResponseEntity.status(HttpStatus.CREATED).body(added);
	}

	// ✅ 정답 추가 (맵-노래 기준)
	@PostMapping("/songs/{mapSongId}/answers")
	public ResponseEntity<List<AnswerResponseDto>> addAnswers(
		@PathVariable Long mapSongId,
		@RequestBody AnswerRequestDto requestDto,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		String authenticatedUserId = userDetails.getUsername();
		List<AnswerResponseDto> responses = answerService.addAnswers(mapSongId, requestDto, authenticatedUserId);
		return ResponseEntity.ok(responses);
	}

	@GetMapping("/songs/{mapSongId}/answers")
	public ResponseEntity<List<AnswerDto>> getAnswers(@PathVariable Long mapSongId) {
		return ResponseEntity.ok(answerService.getAnswersByMapSong(mapSongId));
	}

	// ✅ 힌트 추가 (맵-노래 기준)
	@PostMapping("/songs/{mapSongId}/hints")
	public ResponseEntity<List<HintResponseDto>> addHints(
		@PathVariable Long mapSongId,
		@RequestBody HintRequestDto requestDto,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		String authenticatedUserId = userDetails.getUsername();
		List<HintResponseDto> responses = hintService.addHintsToMapSong(mapSongId, requestDto, authenticatedUserId);
		return ResponseEntity.ok(responses);
	}

	@GetMapping("/songs/{mapSongId}/hints")
	public ResponseEntity<List<HintDto>> getHints(@PathVariable Long mapSongId) {
		return ResponseEntity.ok(hintService.getHintsByMapSong(mapSongId));
	}

	@PatchMapping("/{mapId}/songs/{mapSongId}")
	public ResponseEntity<MapSongResponseDto> updateMapSong(
		@PathVariable Long mapSongId,
		@RequestBody @Valid MapSongRequestDto requestDto,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		String authenticatedUserId = userDetails.getUsername();
		MapSongResponseDto updated = mapSongService.updateMapSong(mapSongId, requestDto, authenticatedUserId);
		return ResponseEntity.ok(updated);
	}

	@PutMapping("/songs/{mapSongId}/answers")
	public ResponseEntity<List<AnswerResponseDto>> updateAnswers(
		@PathVariable Long mapSongId,
		@RequestBody AnswerRequestDto requestDto,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		String authenticatedUserId = userDetails.getUsername();
		return ResponseEntity.ok(answerService.updateAnswers(mapSongId, requestDto, authenticatedUserId));
	}


	@PutMapping("/songs/{mapSongId}/hints")
	public ResponseEntity<List<HintResponseDto>> updateHints(
		@PathVariable Long mapSongId,
		@RequestBody HintRequestDto requestDto,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		String authenticatedUserId = userDetails.getUsername();
		return ResponseEntity.ok(hintService.updateHints(mapSongId, requestDto, authenticatedUserId));
	}


	// ✅ 맵의 노래 목록 조회
	@GetMapping("/{mapId}/songs")
	public ResponseEntity<List<MapSongResponseDto>> getSongsByMap(@PathVariable Long mapId) {
		List<MapSongResponseDto> songs = mapSongService.getSongsByMapId(mapId);
		return ResponseEntity.ok(songs);
	}

	// ✅ 맵에서 노래 제거
	@DeleteMapping("/songs/{mapSongId}")
	public ResponseEntity<Void> removeSongFromMap(
		@PathVariable Long mapSongId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		String authenticatedUserId = userDetails.getUsername();
		mapSongService.removeSongFromMap(mapSongId, authenticatedUserId);
		return ResponseEntity.noContent().build();
	}


}
