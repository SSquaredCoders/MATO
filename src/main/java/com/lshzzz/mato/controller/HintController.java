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

	@PostMapping("/{songId}/hints")
	public ResponseEntity<List<HintResponseDto>> addHints(@PathVariable Long songId,
		@RequestBody HintRequestDto requestDto) {
		return ResponseEntity.ok(hintService.addHintsToSong(songId, requestDto));
	}

	@GetMapping("/{songId}")
	public ResponseEntity<List<HintDto>> getHints(@PathVariable Long songId) {
		return ResponseEntity.ok(hintService.getHintsBySong(songId));
	}
}
