package com.lshzzz.mato.controller;

import com.lshzzz.mato.model.song.dto.AnswerDto;
import com.lshzzz.mato.model.song.dto.AnswerRequestDto;
import com.lshzzz.mato.model.song.dto.AnswerResponseDto;
import com.lshzzz.mato.service.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/answers")
@RequiredArgsConstructor
public class AnswerController {
	private final AnswerService answerService;

	@PostMapping("/{mapSongId}")
	public ResponseEntity<List<AnswerResponseDto>> addAnswers(
		@PathVariable Long mapSongId,
		@RequestBody AnswerRequestDto requestDto) {
		return ResponseEntity.ok(answerService.addAnswers(mapSongId, requestDto));
	}

	@GetMapping("/{mapSongId}")
	public ResponseEntity<List<AnswerDto>> getAnswers(@PathVariable Long mapSongId) {
		return ResponseEntity.ok(answerService.getAnswersByMapSong(mapSongId));
	}
}
