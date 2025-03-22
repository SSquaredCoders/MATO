package com.lshzzz.mato.controller;

import com.lshzzz.mato.model.song.dto.HintDto;
import com.lshzzz.mato.model.song.dto.HintRequestDto;
import com.lshzzz.mato.model.song.dto.HintResponseDto;
import com.lshzzz.mato.service.HintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hints")
@RequiredArgsConstructor
public class HintController {
	private final HintService hintService;

	@PostMapping("/{mapSongId}")
	public ResponseEntity<List<HintResponseDto>> addHints(
		@PathVariable Long mapSongId,
		@RequestBody HintRequestDto requestDto) {
		List<HintResponseDto> responses = hintService.addHintsToMapSong(mapSongId, requestDto);
		return ResponseEntity.ok(responses);
	}

	@GetMapping("/{mapSongId}")
	public ResponseEntity<List<HintDto>> getHints(@PathVariable Long mapSongId) {
		return ResponseEntity.ok(hintService.getHintsByMapSong(mapSongId));
	}
}
