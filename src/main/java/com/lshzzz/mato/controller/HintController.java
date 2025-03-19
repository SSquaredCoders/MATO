package com.lshzzz.mato.controller;

import com.lshzzz.mato.model.song.dto.HintDto;
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

	@GetMapping("/{songId}")
	public ResponseEntity<List<HintDto>> getHints(@PathVariable Long songId) {
		return ResponseEntity.ok(hintService.getHintsBySong(songId));
	}
}
