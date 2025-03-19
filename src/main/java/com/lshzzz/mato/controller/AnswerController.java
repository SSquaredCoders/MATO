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

	@PostMapping("/{songId}/answers")
	public ResponseEntity<List<AnswerResponseDto>> addAnswers(@PathVariable Long songId,
		@RequestBody AnswerRequestDto requestDto) {
		return ResponseEntity.ok(answerService.addAnswersToSong(songId, requestDto));
	}

	@GetMapping("/{songId}")
	public ResponseEntity<List<AnswerDto>> getAnswers(@PathVariable Long songId) {
		return ResponseEntity.ok(answerService.getAnswersBySong(songId));
	}
}
